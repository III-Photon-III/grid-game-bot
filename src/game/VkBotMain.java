package game;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * VK entry point: starts one game session per dialog.
 *
 * <p>Incoming messages are read through the Bots Long Poll API. The community
 * access token is taken from the {@code VK_TOKEN} environment variable or from
 * the first command line argument, the community id from {@code VK_GROUP_ID}
 * or from the second argument.
 */
public final class VkBotMain {

    /**
     * A single game session bound to one peer (a dialog or a conversation).
     */
    private static final class Session {

        private final BlockingQueue<String> input = new LinkedBlockingQueue<>();

        private Session(long peerId, VkApi api, Map<Long, Session> sessions) {
            Thread thread = new Thread(() -> {
                try {
                    VkView view = new VkView(peerId, api, input);
                    GameController controller = new GameController(
                            new Game(new Levels(), Player.DEFAULT_MAX_HEALTH), view);
                    controller.run();
                } finally {
                    sessions.remove(peerId, this);
                }
            }, "game-peer-" + peerId);
            thread.setDaemon(true);
            thread.start();
        }
    }

    public static void main(String[] args) {
        useUtf8Console();

        String token = readEnv("VK_TOKEN", args, 0);
        if (token == null) {
            System.err.println("Укажите токен сообщества: переменная окружения VK_TOKEN "
                    + "или первый аргумент командной строки.");
            System.exit(1);
            return;
        }

        String groupIdText = readEnv("VK_GROUP_ID", args, 1);
        if (groupIdText == null) {
            System.err.println("Укажите id сообщества: переменная окружения VK_GROUP_ID "
                    + "или второй аргумент командной строки.");
            System.exit(1);
            return;
        }

        long groupId;
        try {
            groupId = Long.parseLong(groupIdText.trim());
        } catch (NumberFormatException e) {
            System.err.println("Некорректный id сообщества: " + groupIdText);
            System.exit(1);
            return;
        }

        VkClient client = new VkClient(token, groupId);
        Map<Long, Session> sessions = new ConcurrentHashMap<>();

        try {
            VkClient.LongPollServer longPoll = client.getLongPollServer();
            System.out.println("Бот запущен. Сообщество: " + groupId + ". Остановка — Ctrl+C.");
            runLongPoll(client, longPoll, sessions);
        } catch (VkApiException e) {
            System.err.println("Ошибка VK API: " + e.getMessage());
            System.err.println(explain(e.getErrorCode()));
            System.exit(1);
        } catch (UncheckedIOException e) {
            System.err.println("Сетевая ошибка VK: " + e.getMessage());
            System.exit(1);
        }
    }

    private static String explain(int errorCode) {
        switch (errorCode) {
            case 5:
                return "Проверьте токен сообщества: он должен быть получен в настройках "
                        + "сообщества и иметь права на сообщения и управление сообществом.";
            case 6:
                return "Слишком много запросов к VK API: подождите пару минут.";
            case 15:
                return "Некорректный id сообщества или недостаточно прав "
                        + "на Groups.getLongPollServer.";
            default:
                return "Подробности: https://dev.vk.com/ru/reference";
        }
    }

    private static void runLongPoll(VkClient client, VkClient.LongPollServer longPoll,
            Map<Long, Session> sessions) {
        String server = longPoll.getServer();
        String key = longPoll.getKey();
        String ts = longPoll.getTs();
        VkClient.LongPollServer longPollServerCache = longPoll;

        CountDownLatch stopped = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(stopped::countDown));

        while (stopped.getCount() > 0) {
            JsonNode payload;
            try {
                payload = client.poll(server, key, ts);
            } catch (RuntimeException e) {
                System.err.println("Ошибка Long Poll: " + e.getMessage());
                sleep(2000);
                continue;
            }

            JsonNode failed = payload.get("failed");
            if (failed != null && !failed.isNull()) {
                int failedCode = failed.asInt();
                if (failedCode == 4) {
                    // 4 — версия протокола устарела: подбираем совместимую из диапазона сервера.
                    Integer min = VkClient.versionInt(payload, "min_version");
                    Integer max = VkClient.versionInt(payload, "max_version");
                    Integer resolved = client.resolveLongPollVersion(client.getLongPollVersion(), min, max);
                    if (resolved != null) {
                        client.setLongPollVersion(resolved);
                        System.out.println("Long Poll: failed=4, версия протокола обновлена на " + resolved + ".");
                    } else {
                        System.err.println("Long Poll: failed=4, не удалось определить версию протокола, обновляю ключ.");
                    }
                    VkClient.LongPollServer fresh = refreshLongPoll(client);
                    if (fresh != null) {
                        longPollServerCache = fresh;
                        server = fresh.getServer();
                        key = fresh.getKey();
                        ts = fresh.getTs();
                    }
                    continue;
                }
                // 1 — история событий устарела, 2 — истёк ключ, 3 — потеряна информация.
                System.out.println("Long Poll: failed=" + failedCode + ", обновляю ключ.");
                VkClient.LongPollServer fresh2 = refreshLongPoll(client);
                if (fresh2 != null) {
                    longPollServerCache = fresh2;
                    server = fresh2.getServer();
                    key = fresh2.getKey();
                    ts = fresh2.getTs();
                }
                continue;
            }

            JsonNode newTs = payload.get("ts");
            if (newTs != null && !newTs.isNull()) {
                ts = newTs.asText(ts);
            }

            JsonNode updates = payload.get("updates");
            if (updates == null || !updates.isArray()) {
                System.out.println("Long Poll: ответ без updates, payload=" + payload);
                continue;
            }
            System.out.println("Long Poll: получено updates=" + updates.size());
            for (JsonNode update : updates) {
                handleUpdate(client, update, sessions);
            }
        }
    }

    /**
     * Handles a single Long Poll update: only {@code message_new} is used.
     */
    private static void handleUpdate(VkClient client, JsonNode update,
            Map<Long, Session> sessions) {
        String type = VkUpdates.type(update);
        String text = VkUpdates.text(update);
        long peerId = VkUpdates.peerId(update);
        long fromId = VkUpdates.fromId(update);
        System.out.println("DEBUG update: type=" + type + ", text=" + text
                + ", peerId=" + peerId + ", fromId=" + fromId
                + ", raw=" + update);

        if (!VkUpdates.EVENT_MESSAGE_NEW.equals(type)) {
            return;
        }

        if (text == null || text.isEmpty() || peerId == 0) {
            return;
        }
        if (fromId < 0) {
            // The message was sent by the community itself: skip the echo.
            return;
        }

        client.markAsRead(peerId);
        Session session = sessions.computeIfAbsent(peerId, id -> new Session(id, client, sessions));
        session.input.offer(text);
    }

    /**
     * Reads a setting from the environment, falling back to a command line
     * argument.
     *
     * @return the value or {@code null} when it is not configured
     */
    private static String readEnv(String name, String[] args, int argIndex) {
        String value = System.getenv(name);
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        if (args != null && args.length > argIndex && args[argIndex] != null
                && !args[argIndex].isBlank()) {
            return args[argIndex].trim();
        }
        return null;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Forces UTF-8 output so that the Russian messages are readable in the
     * default Windows console (cp866) as well.
     */
    private static void useUtf8Console() {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out),
                true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err),
                true, StandardCharsets.UTF_8));
    }

    /**
     * Пытается обновить Long Poll сервер (server/key/ts). При ошибке VK API или
     * сети пишет в stderr и возвращает {@code null}, чтобы бот не падал и
     * продолжал попытки на следующей итерации цикла.
     */
    private static VkClient.LongPollServer refreshLongPoll(VkClient client) {
        try {
            return client.getLongPollServer();
        } catch (RuntimeException e) {
            System.err.println("Ошибка обновления Long Poll сервера: " + e.getMessage());
            sleep(2000);
            return null;
        }
    }
}



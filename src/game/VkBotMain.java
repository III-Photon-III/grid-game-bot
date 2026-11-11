package game;

import exceptions.VkApiException;
import vk.SessionManager;
import vk.VkClient;
import vk.VkLongPollHandler;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

public final class VkBotMain {

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
        SessionManager sessionManager = new SessionManager(client);
        VkLongPollHandler longPollHandler = new VkLongPollHandler(client, sessionManager);

        try {
            VkClient.LongPollServer longPoll = client.getLongPollServer();
            System.out.println("Бот запущен. Сообщество: " + groupId + ". Остановка — Ctrl+C.");
            longPollHandler.start(longPoll);
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

    private static void useUtf8Console() {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out),
                true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err),
                true, StandardCharsets.UTF_8));
    }
}
package vk;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.concurrent.CountDownLatch;

/**
 * Цикл опроса Bots Long Poll API и диспетчеризация событий.
 */
public final class VkLongPollHandler {

    private final VkClient client;
    private final SessionManager sessionManager;
    private final CountDownLatch stopped = new CountDownLatch(1);

    public VkLongPollHandler(VkClient client, SessionManager sessionManager) {
        this.client = client;
        this.sessionManager = sessionManager;
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    public void start(VkClient.LongPollServer initialServer) {
        String server = initialServer.getServer();
        String key = initialServer.getKey();
        String ts = initialServer.getTs();

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

                if (failedCode == 1) {
                    ts = payload.path("ts").asText(ts);
                    continue;
                }
                if (failedCode == 4) {
                    Integer min = VkClient.versionInt(payload, "min_version");
                    Integer max = VkClient.versionInt(payload, "max_version");
                    Integer resolved = client.resolveLongPollVersion(client.getLongPollVersion(), min, max);
                    if (resolved != null) {
                        client.setLongPollVersion(resolved);
                        System.out.println("Long Poll: failed=4, версия протокола обновлена на " + resolved + ".");
                    } else {
                        System.err.println("Long Poll: failed=4, не удалось определить версию протокола, обновляю ключ.");
                    }
                    VkClient.LongPollServer fresh = refreshLongPoll();
                    if (fresh != null) {
                        server = fresh.getServer();
                        key = fresh.getKey();
                        ts = fresh.getTs();
                    }
                    continue;
                }

                System.out.println("Long Poll: failed=" + failedCode + ", обновляю ключ.");
                VkClient.LongPollServer fresh = refreshLongPoll();
                if (fresh != null) {
                    server = fresh.getServer();
                    key = fresh.getKey();
                    ts = fresh.getTs();
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
                processUpdate(update);
            }
        }
    }

    public void stop() {
        stopped.countDown();
    }

    private void processUpdate(JsonNode update) {
        String type = VkUpdates.type(update);
        String text = VkUpdates.text(update);
        long peerId = VkUpdates.peerId(update);
        long fromId = VkUpdates.fromId(update);

        System.out.println("DEBUG update: type=" + type + ", text=" + text
                + ", peerId=" + peerId + ", fromId=" + fromId
                + ", raw=" + update);

        if (VkUpdates.EVENT_MESSAGE_NEW.equals(type)) {
            sessionManager.handleMessage(peerId, fromId, text);
        }
    }

    private VkClient.LongPollServer refreshLongPoll() {
        try {
            return client.getLongPollServer();
        } catch (RuntimeException e) {
            System.err.println("Ошибка обновления Long Poll сервера: " + e.getMessage());
            sleep(2000);
            return null;
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
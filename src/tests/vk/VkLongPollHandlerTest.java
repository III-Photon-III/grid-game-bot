package vk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class VkLongPollHandlerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private TestableLongPollClient client;
    private SessionManager sessionManager;
    private VkLongPollHandler handler;

    @BeforeEach
    void setUp() {
        client = new TestableLongPollClient();
        sessionManager = new SessionManager(client);
        handler = new VkLongPollHandler(client, sessionManager);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("start: обрабатывает событие message_new и передает его в SessionManager")
    void testProcessesMessageNew() throws Exception {
        // Подготавливаем ответ Long Poll с одним входящим сообщением
        String payloadJson = "{"
                + "\"ts\": \"2\","
                + "\"updates\": ["
                + "  {"
                + "    \"type\": \"message_new\","
                + "    \"object\": {"
                + "      \"message\": {"
                + "        \"peer_id\": 777,"
                + "        \"from_id\": 777,"
                + "        \"text\": \"привет\""
                + "      }"
                + "    }"
                + "  }"
                + "]"
                + "}";
        client.pollResponses = new String[]{ payloadJson };

        // Останавливаем цикл сразу после первой итерации
        client.onPollCallback = () -> handler.stop();

        VkClient.LongPollServer initialServer = new VkClient.LongPollServer("https://lp.vk", "k", "1");
        handler.start(initialServer);

        assertEquals(1, sessionManager.getActiveSessionsCount(), "Сессия должна быть создана по входящему событию");
        sessionManager.handleMessage(777L, 777L, "q"); // закрываем сессию
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("start: обрабатывает failed=1 (обновление ts без сброса сервера)")
    void testHandlesFailedCode1() {
        client.pollResponses = new String[]{ "{\"failed\": 1, \"ts\": \"999\"}" };
        client.onPollCallback = () -> handler.stop();

        VkClient.LongPollServer initialServer = new VkClient.LongPollServer("https://lp.vk", "k", "1");
        assertDoesNotThrow(() -> handler.start(initialServer));
        assertEquals(0, client.refreshCount, "Для failed=1 переподключение сервера не требуется");
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("start: обрабатывает failed=2 (истек ключ -> обновление Long Poll сервера)")
    void testHandlesFailedCode2() {
        client.pollResponses = new String[]{ "{\"failed\": 2}" };
        client.onPollCallback = () -> handler.stop();

        VkClient.LongPollServer initialServer = new VkClient.LongPollServer("https://lp.vk", "k", "1");
        assertDoesNotThrow(() -> handler.start(initialServer));
        assertTrue(client.refreshCount >= 1, "При failed=2 должен быть запрошен новый ключ Long Poll");
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("start: обрабатывает failed=4 (устаревшая версия протокола)")
    void testHandlesFailedCode4() {
        client.pollResponses = new String[]{ "{\"failed\": 4, \"min_version\": 1, \"max_version\": 10}" };
        client.onPollCallback = () -> handler.stop();

        VkClient.LongPollServer initialServer = new VkClient.LongPollServer("https://lp.vk", "k", "1");
        handler.start(initialServer);

        assertTrue(client.refreshCount >= 1);
    }


    private class TestableLongPollClient extends VkClient {
        String[] pollResponses = new String[0];
        int pollIndex = 0;
        int refreshCount = 0;
        Runnable onPollCallback;

        TestableLongPollClient() {
            super("test_token", 12345L);
        }

        @Override
        public JsonNode poll(String server, String key, String ts) {
            if (onPollCallback != null) {
                onPollCallback.run();
            }
            if (pollIndex < pollResponses.length) {
                try {
                    return mapper.readTree(pollResponses[pollIndex++]);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return mapper.createObjectNode();
        }

        @Override
        public LongPollServer getLongPollServer() {
            refreshCount++;
            return new LongPollServer("https://lp.fresh.vk", "fresh_key", "100");
        }

        @Override
        String execute(Request request) {
            return "{\"response\": 1}";
        }

        @Override
        public void markAsRead(long peerId) {
        }

        @Override
        public void sendMessage(long peerId, String text) {
        }
    }
}
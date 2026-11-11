package vk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import exceptions.VkApiException;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okio.Buffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VkClientTest {

    private TestableVkClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        client = new TestableVkClient("test_token", 123456L);
    }

    @Test
    @DisplayName("buildLongPollUrl: корректно собирает URL и параметры запроса")
    void testBuildLongPollUrl() {
        HttpUrl url = VkClient.buildLongPollUrl("https://lp.vk.com/wh123", "secret_key", "100", 5);

        assertNotNull(url);
        assertEquals("https", url.scheme());
        assertEquals("lp.vk.com", url.host());
        assertEquals("a_check", url.queryParameter("act"));
        assertEquals("secret_key", url.queryParameter("key"));
        assertEquals("100", url.queryParameter("ts"));
        assertEquals("25", url.queryParameter("wait"));
        assertEquals("2", url.queryParameter("mode"));
        assertEquals("5", url.queryParameter("version"));
    }

    @Test
    @DisplayName("resolveLongPollVersion: корректировка версии под границы [min, max]")
    void testResolveLongPollVersion() {
        // Ошибочные/пустые диапазоны
        assertNull(VkClient.resolveLongPollVersion(5, null, null));
        assertNull(VkClient.resolveLongPollVersion(5, 10, 4)); // min > max

        // Ограничение снизу и сверху
        assertEquals(3, VkClient.resolveLongPollVersion(1, 3, 10)); // current < min
        assertEquals(10, VkClient.resolveLongPollVersion(15, 3, 10)); // current > max

        // Версия попадает в диапазон
        assertEquals(5, VkClient.resolveLongPollVersion(5, 3, 10));
    }

    @Test
    @DisplayName("versionInt: парсинг чисел и строковых представлений из JSON")
    void testVersionInt() throws Exception {
        JsonNode node = mapper.readTree("{\"num\": 5, \"str\": \"12\", \"bad\": \"abc\", \"empty\": null}");

        assertNull(VkClient.versionInt(null, "num"));
        assertNull(VkClient.versionInt(node, "non_existent"));
        assertNull(VkClient.versionInt(node, "empty"));
        assertNull(VkClient.versionInt(node, "bad"));

        assertEquals(5, VkClient.versionInt(node, "num"));
        assertEquals(12, VkClient.versionInt(node, "str"));
    }

    @Test
    @DisplayName("LongPollServer: корректная работа геттеров")
    void testLongPollServerGetters() {
        VkClient.LongPollServer server = new VkClient.LongPollServer("https://server.vk", "key_abc", "42");
        assertEquals("https://server.vk", server.getServer());
        assertEquals("key_abc", server.getKey());
        assertEquals("42", server.getTs());
    }

    @Test
    @DisplayName("Конструкторы и геттер/сеттер версии Long Poll")
    void testLongPollVersionSettings() {
        assertEquals(VkClient.LONG_POLL_VERSION, client.getLongPollVersion());

        client.setLongPollVersion(10);
        assertEquals(10, client.getLongPollVersion());

        VkClient customClient = new VkClient("tok", 1L, 7);
        assertEquals(7, customClient.getLongPollVersion());
    }

    @Test
    @DisplayName("call: успешный ответ распаковывает узел 'response'")
    void testCallSuccess() {
        client.nextResponse = "{\"response\": {\"items\": [1, 2, 3]}}";

        JsonNode result = client.call("users.get", Map.of("user_ids", "1"));

        assertNotNull(result);
        assertTrue(result.has("items"));
        assertEquals(3, result.get("items").size());
        assertEquals("POST", client.lastRequest.method());
        assertTrue(client.lastRequest.url().toString().contains("access_token=test_token"));
    }

    @Test
    @DisplayName("call: ошибка VK API выбрасывает VkApiException с кодом и текстом")
    void testCallVkApiError() {
        client.nextResponse = "{\"error\": {\"error_code\": 5, \"error_msg\": \"User authorization failed: invalid access_token.\"}}";

        VkApiException ex = assertThrows(VkApiException.class, () ->
                client.call("messages.send", Map.of())
        );

        assertEquals(5, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("User authorization failed"));
    }

    @Test
    @DisplayName("call: поврежденный JSON выбрасывает UncheckedIOException")
    void testCallMalformedJson() {
        client.nextResponse = "{ невалидный json }";

        assertThrows(UncheckedIOException.class, () -> client.call("test.method", Map.of()));
    }

    @Test
    @DisplayName("sendMessage: отправляет параметры peer_id, message и random_id")
    void testSendMessage() throws IOException {
        client.nextResponse = "{\"response\": 12345678}";

        client.sendMessage(2000000001L, "Привет, мир!");

        assertNotNull(client.lastRequest);
        Buffer buffer = new Buffer();
        client.lastRequest.body().writeTo(buffer);
        String body = buffer.readUtf8();

        assertTrue(body.contains("peer_id=2000000001"));
        assertTrue(body.contains("message="));
        assertTrue(body.contains("random_id="));
    }

    @Test
    @DisplayName("markAsRead: не выбрасывает исключение даже при ошибке API")
    void testMarkAsReadSwallowsException() {
        // Подсовываем ошибку API, которую метод должен безопасно проигнорировать
        client.nextResponse = "{\"error\": {\"error_code\": 15, \"error_msg\": \"Access denied\"}}";

        assertDoesNotThrow(() -> client.markAsRead(100L));
    }

    @Test
    @DisplayName("getLongPollServer: парсит координаты сервера Long Poll")
    void testGetLongPollServerSuccess() {
        client.nextResponse = "{\"response\": {\"server\": \"https://lp.vk.com\", \"key\": \"qwerty\", \"ts\": \"999\"}}";

        VkClient.LongPollServer lp = client.getLongPollServer();

        assertEquals("https://lp.vk.com", lp.getServer());
        assertEquals("qwerty", lp.getKey());
        assertEquals("999", lp.getTs());
    }

    @Test
    @DisplayName("getLongPollServer: выбрасывает исключение, если поля отсутствуют")
    void testGetLongPollServerMissingFields() {
        client.nextResponse = "{\"response\": {\"server\": \"https://lp.vk.com\"}}"; // нет key и ts

        assertThrows(VkApiException.class, () -> client.getLongPollServer());
    }

    @Test
    @DisplayName("poll: выполняет GET-запрос и парсит ответ Long Poll")
    void testPoll() {
        client.nextResponse = "{\"ts\": \"1001\", \"updates\": []}";

        JsonNode response = client.poll("https://lp.vk.com", "key1", "1000");

        assertNotNull(response);
        assertEquals("1001", response.get("ts").asText());
        assertEquals("GET", client.lastRequest.method());
    }


    private static class TestableVkClient extends VkClient {
        Request lastRequest;
        String nextResponse = "{\"response\": 1}";

        public TestableVkClient(String token, long groupId) {
            super(token, groupId);
        }

        @Override
        String execute(Request request) {
            this.lastRequest = request;
            return nextResponse;
        }
    }
}
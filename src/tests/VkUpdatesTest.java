package tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import game.VkUpdates;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VkUpdatesTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonNode json(String raw) throws Exception {
        return MAPPER.readTree(raw);
    }

    @Test
    @DisplayName("Событие message_new версии 5 разбирается корректно")
    void parsesVersion5MessageNew() throws Exception {
        JsonNode update = json("{\"type\": \"message_new\", \"object\": {\"message\": {"
                + "\"text\": \"W\", \"peer_id\": 2000000001, \"from_id\": 42, \"id\": 7}}}");

        assertEquals("message_new", VkUpdates.type(update));
        assertEquals("W", VkUpdates.text(update));
        assertEquals(2000000001L, VkUpdates.peerId(update));
        assertEquals(42L, VkUpdates.fromId(update));
    }

    @Test
    @DisplayName("Старый формат массива разбирается корректно")
    void parsesLegacyArrayFormat() throws Exception {
        JsonNode update = json("[4, 2000000001, 12345, 100, \"S\", {\"title\": \" ... \"}]");

        assertEquals("message_new", VkUpdates.type(update));
        assertEquals("S", VkUpdates.text(update));
        assertEquals(2000000001L, VkUpdates.peerId(update));
    }

    @Test
    @DisplayName("Другие события игнорируются")
    void ignoresOtherEventTypes() throws Exception {
        JsonNode update = json("{\"type\": \"message_reply\", \"object\": {\"message\": "
                + "{\"text\": \"W\", \"peer_id\": 1, \"from_id\": 2}}}");

        assertNotEquals("message_new", VkUpdates.type(update));
    }

    @Test
    @DisplayName("Сообщение от самого сообщества распознаётся по отрицательному from_id")
    void detectsCommunityMessages() throws Exception {
        JsonNode update = json("{\"type\": \"message_new\", \"object\": {\"message\": "
                + "{\"text\": \"W\", \"peer_id\": 1, \"from_id\": -2000000001}}}");

        assertTrue(VkUpdates.fromId(update) < 0);
    }

    @Test
    @DisplayName("Пустое и некорректное событие не приводит к исключению")
    void handlesEmptyUpdate() throws Exception {
        JsonNode empty = json("{}");

        assertEquals("", VkUpdates.type(empty));
        assertNull(VkUpdates.text(empty));
        assertEquals(0L, VkUpdates.peerId(empty));
        assertEquals(0L, VkUpdates.fromId(empty));
    }
}

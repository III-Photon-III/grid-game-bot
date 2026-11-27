package vk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Parses the Long Poll update payloads of the VK Bots Long Poll API.
 *
 * <p>Version 3 delivers the event as an object:
 * <pre>{"type": "message_new", "object": {"message": {"text": "...", "peer_id": 1, "from_id": 2}}}</pre>
 * The legacy layout is an array whose first item is the numeric event code.
 */
public final class VkUpdates {

    /** Event type of a new incoming message. */
    public static final String EVENT_MESSAGE_NEW = "message_new";

    /** Numeric event code of the legacy layout. */
    private static final int LEGACY_CODE_MESSAGE_NEW = 4;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private VkUpdates() {
    }

    /**
     * Returns the event type of the update, or an empty string when the event
     * is not a known one.
     */
    public static String type(JsonNode update) {
        if (update == null) {
            return "";
        }
        if (update.isObject()) {
            return update.path("type").asText("");
        }
        if (update.isArray() && update.size() > 0) {
            int code = update.path(0).asInt(-1);
            return code == LEGACY_CODE_MESSAGE_NEW ? EVENT_MESSAGE_NEW : "";
        }
        return "";
    }

    /**
     * Extracts the message body, or {@code null} when the update carries no text.
     */
    public static String text(JsonNode update) {
        JsonNode object = object(update);
        if (object == null) {
            return null;
        }
        if (object.isArray()) {
            // Legacy layout: [code, peer_id, event_id, ts, text, attachments].
            JsonNode text = object.path(4);
            return text.isMissingNode() || text.isNull() ? null : text.asText(null);
        }
        JsonNode message = object.path("message");
        if (message.isObject()) {
            // При нажатии inline-кнопки VK присылает payload вместо обычного текста.
            // Сначала проверяем payload, иначе вернётся метка кнопки (например, "⬆️ ВВЕРХ").
            String payload = readPayload(message);
            String text = message.path("text").asText(null);

            if (payload != null && !payload.isEmpty()) {
                String cmd = extractCmd(payload);
                if (cmd != null && !cmd.isEmpty()) {
                    return cmd;
                }
            }

            // Если в text пришёл JSON вида {"cmd":"w"} — парсим его напрямую.
            if (text != null && !text.isEmpty() && text.startsWith("{\"cmd\":\"")) {
                return extractCmd(text);
            }

            if (text != null && !text.isEmpty()) {
                return text;
            }
            return object.path("text").asText(null);
        }
        return object.path("text").asText(null);
    }

    /**
     * Extracts the dialog id the message belongs to, or {@code 0} when unknown.
     */
    public static long peerId(JsonNode update) {
        JsonNode object = object(update);
        if (object == null) {
            return 0;
        }
        if (object.isArray()) {
            // Legacy layout: [code, peer_id, event_id, ts, text, attachments].
            return object.path(1).asLong(0);
        }
        JsonNode message = object.path("message");
        if (message.isObject()) {
            long peerId = message.path("peer_id").asLong(0);
            if (peerId != 0) {
                return peerId;
            }
        }
        return object.path("peer_id").asLong(0);
    }

    /**
     * Extracts the author id. Negative values mean the community itself.
     */
    public static long fromId(JsonNode update) {
        JsonNode object = object(update);
        if (object == null) {
            return 0;
        }
        JsonNode message = object.path("message");
        if (message.isObject()) {
            long fromId = message.path("from_id").asLong(0);
            if (fromId != 0) {
                return fromId;
            }
        }
        return object.path("from_id").asLong(0);
    }

    /**
     * Extracts the command from a button payload like {@code {"cmd":"w"}}.
     * Returns {@code "/restart"} / {@code "/quit"} for game commands so the
     * controller recognizes them.
     */
    private static String extractCmd(String payload) {
        if (payload == null) {
            return null;
        }
        String trimmed = payload.trim();
        if (trimmed.startsWith("{")) {
            try {
                JsonNode node = MAPPER.readTree(trimmed);
                JsonNode cmdNode = node.get("cmd");
                if (cmdNode != null && cmdNode.isTextual()) {
                    String cmd = cmdNode.asText();
                    return switch (cmd) {
                        case "restart" -> "/restart";
                        case "quit" -> "/quit";
                        default -> cmd;
                    };
                }
            } catch (Exception ignored) {
                // Если пришёл невалидный JSON, вернём исходный текст
            }
        }
        return payload;
    }

    /**
     * Читает поле {@code payload} из объекта сообщения VK.
     * Пробуем через get() и path(), т.к. Jackson иногда ведёт себя иначе.
     */
    private static String readPayload(JsonNode message) {
        JsonNode node = message.get("payload");
        if (node == null || node.isMissingNode() || node.isNull()) {
            node = message.path("payload");
        }
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.isObject() ? node.toString() : node.asText(null);
    }

    /**
     * Returns the payload of the event: the {@code object} field of the v3
     * layout or the whole array of the legacy layout.
     */
    private static JsonNode object(JsonNode update) {
        if (update == null) {
            return null;
        }
        if (update.isObject()) {
            JsonNode object = update.get("object");
            return object != null ? object : update;
        }
        return update;
    }
}

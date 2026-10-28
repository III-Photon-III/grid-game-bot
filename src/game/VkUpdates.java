package game;

import com.fasterxml.jackson.databind.JsonNode;

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
            String text = message.path("text").asText(null);
            if (text != null) {
                return text;
            }
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

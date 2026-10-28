package game;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;

/**
 * VK Bots Long Poll transport: talks to the VK Bots Long Poll API
 * (protocol version 5 by default) and exposes a {@link VkApi} implementation
 * for sending messages.
 *
 * <p>The Long Poll protocol version can be changed at runtime via
 * {@link #setLongPollVersion(int)}. On a {@code failed: 4} response from VK,
 * use {@link #resolveLongPollVersion(int, Integer, Integer)} together with
 * {@link #versionInt(JsonNode, String)} to pick a compatible version from the
 * server-reported range.
 */
public class VkClient implements VkApi {

    /** Default Bots Long Poll protocol version. */
    public static final int LONG_POLL_VERSION = 5;

    private static final String API_BASE = "https://api.vk.com/method/";

    /** Версия VK API для обычных вызовов методов (groups.getLongPollServer, messages.send и т.д.). */
    private static final String API_VERSION = "5.199";

    private final String token;
    private final long groupId;
    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;

    private int longPollVersion;

    public VkClient(String token, long groupId) {
        this(token, groupId, LONG_POLL_VERSION);
    }

    public VkClient(String token, long groupId, int longPollVersion) {
        this.token = token;
        this.groupId = groupId;
        this.longPollVersion = longPollVersion;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.mapper = new ObjectMapper();
    }

    public int getLongPollVersion() {
        return longPollVersion;
    }

    public void setLongPollVersion(int longPollVersion) {
        this.longPollVersion = longPollVersion;
    }

    /**
     * Builds the Long Poll request URL for the given server coordinates.
     *
     * @param server Long Poll server address (from {@link #getLongPollServer()})
     * @param key    Long Poll key
     * @param ts     current ts value
     * @param version protocol version to pass to VK
     */
    public static HttpUrl buildLongPollUrl(String server, String key, String ts, int version) {
        return HttpUrl.parse(server).newBuilder()
                .addQueryParameter("act", "a_check")
                .addQueryParameter("key", key)
                .addQueryParameter("ts", ts)
                .addQueryParameter("wait", "25")
                .addQueryParameter("mode", "2")
                .addQueryParameter("version", Integer.toString(version))
                .build();
    }

    /**
     * Executes an HTTP request and returns the response body as a string.
     *
     * @throws UncheckedIOException if the request fails
     */
    String execute(Request request) {
        try {
            return httpClient.newCall(request).execute().body().string();
        } catch (IOException e) {
            throw new UncheckedIOException("Network request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Resolves a compatible Long Poll protocol version on {@code failed: 4}.
     *
     * <p>Clamps {@code current} into the server-reported range {@code [min, max]}.
     * Returns {@code null} when the range is missing or invalid (e.g. {@code min > max}),
     * in which case the caller should fall back to refreshing server/key/ts.
     *
     * @param current the protocol version currently in use
     * @param min     {@code min_version} from the VK response, or {@code null}
     * @param max     {@code max_version} from the VK response, or {@code null}
     * @return a version inside the range, or {@code null} if the range is unusable
     */
    public static Integer resolveLongPollVersion(int current, Integer min, Integer max) {
        if (min == null && max == null) {
            return null;
        }
        if (min != null && max != null && min > max) {
            return null;
        }
        if (min != null && current < min) {
            return min;
        }
        if (max != null && current > max) {
            return max;
        }
        return current;
    }

    /**
     * Safely parses an integer version field (e.g. {@code min_version} /
     * {@code max_version}) from a JSON node. Accepts both numeric and textual
     * representations. Returns {@code null} when the field is absent or
     * cannot be parsed.
     */
    public static Integer versionInt(JsonNode node, String field) {
        if (node == null || node.isNull() || !node.has(field)) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.isMissingNode()) {
            return null;
        }
        if (value.isInt() || value.isLong()) {
            return value.asInt();
        }
        if (value.isTextual()) {
            try {
                return Integer.parseInt(value.asText().trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }


    @Override
    public void sendMessage(long peerId, String text) {
        System.out.println("DEBUG VkClient.sendMessage to peer " + peerId + ": " + text.substring(0, Math.min(text.length(), 80)));
        Map<String, String> params = new LinkedHashMap<>();
        params.put("peer_id", Long.toString(peerId));
        params.put("message", text);
        // random_id makes the request idempotent: VK deduplicates retries.
        // Must be truly random (not sequential) to avoid deduplication across bot restarts.
        params.put("random_id", Long.toString(ThreadLocalRandom.current().nextLong()));
        call("messages.send", params);
    }

    /**
     * Marks the incoming messages of the peer as read.
     * Failures are not fatal: the bot must keep working without the badge.
     */
    public void markAsRead(long peerId) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("peer_id", Long.toString(peerId));
        try {
            call("messages.markAsRead", params);
        } catch (RuntimeException ignored) {
            // messages.markAsRead не поддерживается для некоторых community-токенов;
            // это не критично — бот продолжает работать без отметки о прочтении.
        }
    }

    /**
     * Calls a VK API method and returns the {@code response} node.
     *
     * <p>Parameters are sent as a form-encoded POST body: the message text can
     * be long and must not end up in the URL.
     *
     * @throws VkApiException when VK answers with an error object
     */
    public JsonNode call(String method, Map<String, String> params) {
        HttpUrl url = HttpUrl.parse(API_BASE + method).newBuilder()
                .addQueryParameter("access_token", token)
                .addQueryParameter("v", API_VERSION)
                .build();

        FormBody.Builder body = new FormBody.Builder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            body.add(entry.getKey(), entry.getValue());
        }

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "grid-game-bot/1.0")
                .post(body.build())
                .build();
        String payload = execute(request);

        JsonNode root;
        try {
            root = mapper.readTree(payload);
        } catch (IOException e) {
            throw new UncheckedIOException("Malformed JSON response from " + method, e);
        }

        JsonNode error = root.get("error");
        if (error != null && !error.isNull()) {
            int code = error.path("error_code").asInt(0);
            String message = error.path("error_msg").asText("unknown error");
            throw new VkApiException(code, "VK API error " + code + ": " + message + " | raw=" + payload);
        }

        JsonNode response = root.get("response");
        return response != null ? response : root;
    }

    /**
     * Requests the Long Poll server coordinates for the community.
     * Requires a community token with the {@code manage} scope.
     */
    public LongPollServer getLongPollServer() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("group_id", Long.toString(groupId));

        JsonNode response = call("groups.getLongPollServer", params);
        String server = response.path("server").asText(null);
        String key = response.path("key").asText(null);
        String ts = response.path("ts").asText(null);
        if (server == null || key == null || ts == null) {
            throw new VkApiException(0, "groups.getLongPollServer returned no server/key/ts");
        }
        return new LongPollServer(server, key, ts);
    }

    /**
     * Performs one Long Poll request and returns the raw JSON payload
     * ({@code {"ts": "...", "updates": [...]}} or {@code {"failed": N}}).
     */
    public JsonNode poll(String server, String key, String ts) {
        HttpUrl url = buildLongPollUrl(server, key, ts, longPollVersion);

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "grid-game-bot/1.0")
                .get()
                .build();
        String body = execute(request);

        try {
            return mapper.readTree(body);
        } catch (IOException e) {
            throw new UncheckedIOException("Malformed Long Poll response", e);
        }
    }

    /**
     * Result of {@link #getLongPollServer()}: the Long Poll server address,
     * key and initial ts.
     */
    public static final class LongPollServer {
        private final String server;
        private final String key;
        private final String ts;

        public LongPollServer(String server, String key, String ts) {
            this.server = server;
            this.key = key;
            this.ts = ts;
        }

        public String getServer() {
            return server;
        }

        public String getKey() {
            return key;
        }

        public String getTs() {
            return ts;
        }
    }
}
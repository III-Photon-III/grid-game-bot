package vk;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Управляет сессиями игроков и распределяет входящие сообщения по диалогам.
 */
public final class SessionManager {

    private final VkClient client;
    private final Map<Long, VkSession> sessions = new ConcurrentHashMap<>();

    public SessionManager(VkClient client) {
        this.client = client;
    }

    public void handleMessage(long peerId, long fromId, String text) {
        if (text == null || text.isEmpty() || peerId == 0) {
            return;
        }
        if (fromId < 0) {
            // Сообщение отправлено сообществом: игнорируем собственное эхо
            return;
        }

        client.markAsRead(peerId);
        VkSession session = sessions.computeIfAbsent(peerId,
                id -> new VkSession(id, client, s -> sessions.remove(id, s)));
        session.offer(text);
    }

    public int getActiveSessionsCount() {
        return sessions.size();
    }
}
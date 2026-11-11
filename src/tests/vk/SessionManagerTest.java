package vk;

import okhttp3.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessionManagerTest {

    private DummyVkClient client;
    private SessionManager manager;

    @BeforeEach
    void setUp() {
        client = new DummyVkClient();
        manager = new SessionManager(client);
    }

    @Test
    @DisplayName("handleMessage: создает сессию и отмечает сообщение прочитанным")
    void testHandleMessageCreatesSession() {
        manager.handleMessage(1001L, 1001L, "/help");

        assertEquals(1, manager.getActiveSessionsCount(), "Должна создаться 1 сессия");
        assertEquals(1, client.markAsReadCount, "Должен быть вызван markAsRead");
        assertEquals(1001L, client.lastMarkedPeerId);

        // Отправляем команду завершения, чтобы освободить поток
        manager.handleMessage(1001L, 1001L, "q");
    }

    @Test
    @DisplayName("handleMessage: повторное сообщение тому же peerId использует существующую сессию")
    void testHandleMessageReusesSession() {
        manager.handleMessage(2001L, 2001L, "w");
        manager.handleMessage(2001L, 2001L, "s");

        assertEquals(1, manager.getActiveSessionsCount(), "Количество сессий не должно увеличиться");
        assertEquals(2, client.markAsReadCount);

        manager.handleMessage(2001L, 2001L, "q");
    }

    @Test
    @DisplayName("handleMessage: сообщения от разных пользователей создают разные сессии")
    void testHandleMessageMultipleUsers() {
        manager.handleMessage(3001L, 3001L, "w");
        manager.handleMessage(3002L, 3002L, "w");

        assertEquals(2, manager.getActiveSessionsCount(), "Должно быть создано 2 независимые сессии");

        manager.handleMessage(3001L, 3001L, "q");
        manager.handleMessage(3002L, 3002L, "q");
    }

    @Test
    @DisplayName("handleMessage: игнорирует сообщения от имени сообщества (fromId < 0)")
    void testHandleMessageIgnoresCommunityEcho() {
        // fromId отрицательный — эхо сообщения самой группы
        manager.handleMessage(1001L, -123456L, "Эхо бота");

        assertEquals(0, manager.getActiveSessionsCount(), "Сессия не должна создаваться для эхо");
        assertEquals(0, client.markAsReadCount);
    }

    @Test
    @DisplayName("handleMessage: игнорирует невалидный ввод (null, пустой текст, peerId = 0)")
    void testHandleMessageIgnoresInvalidInputs() {
        manager.handleMessage(0L, 123L, "hello");
        manager.handleMessage(1001L, 1001L, null);
        manager.handleMessage(1001L, 1001L, "");

        assertEquals(0, manager.getActiveSessionsCount());
        assertEquals(0, client.markAsReadCount);
    }

    private static class DummyVkClient extends VkClient {
        int markAsReadCount = 0;
        Long lastMarkedPeerId = null;

        DummyVkClient() {
            super("test_token", 12345L);
        }

        @Override
        String execute(Request request) {
            return "{\"response\": 1}";
        }

        @Override
        public void markAsRead(long peerId) {
            markAsReadCount++;
            lastMarkedPeerId = peerId;
        }

        @Override
        public void sendMessage(long peerId, String text) {
            // пустая реализация для тестов, чтобы не слать запросы
        }
    }
}
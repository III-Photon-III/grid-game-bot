package vk;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class VkSessionTest {

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("VkSession: корректно стартует поток и вызывает onFinished при завершении")
    void testSessionLifecycle() throws InterruptedException {
        CountDownLatch finishedLatch = new CountDownLatch(1);
        AtomicBoolean callbackCalled = new AtomicBoolean(false);

        VkApi dummyApi = (peerId, text) -> {
            // заглушка отправки сообщений
        };

        VkSession session = new VkSession(12345L, dummyApi, s -> {
            callbackCalled.set(true);
            finishedLatch.countDown();
        });

        // Отправляем команду 'q', чтобы контроллер вышел из игрового цикла
        session.offer("q");

        // Ждем сигнала о завершении потока
        boolean completed = finishedLatch.await(3, TimeUnit.SECONDS);

        assertTrue(completed, "Сессия должна была завершиться после получения команды выхода");
        assertTrue(callbackCalled.get(), "Коллбек onFinished должен быть вызван");
    }
}
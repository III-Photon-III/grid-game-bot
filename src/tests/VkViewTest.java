package tests;

import game.Game;
import game.GameController;
import game.Levels;
import game.Player;
import game.VkApi;
import game.VkView;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;

public class VkViewTest {

    /**
     * Заглушка VK API: записывает отправленные сообщения вместо
     * реального сетевого вызова.
     */
    private static final class RecordingApi implements VkApi {

        private final List<String> texts = new ArrayList<>();
        private final List<Long> peers = new ArrayList<>();

        @Override
        public void sendMessage(long peerId, String text) {
            peers.add(peerId);
            texts.add(text);
        }
    }

    @Test
    @DisplayName("Карта отправляется в правильный peer_id")
    void renderSendsGridToTheRightPeer() {
        RecordingApi api = new RecordingApi();
        BlockingQueue<String> input = new LinkedBlockingQueue<>();
        VkView view = new VkView(100L, api, input);
        Game game = new Game(new Levels(), Player.DEFAULT_MAX_HEALTH);

        view.render(game.getLevel(), game.getPlayer(), null);

        assertEquals(1, api.texts.size());
        assertEquals(100L, api.peers.get(0));
        String text = api.texts.get(0);
        assertTrue(text.contains("🟦"), text);
        assertTrue(text.contains("🟩"), text);
        assertTrue(text.contains("⬛"), text);
    }

    @Test
    @DisplayName("Спецсимволы в статус-сообщении не экранируются")
    void renderKeepsStatusMessageAsIs() {
        RecordingApi api = new RecordingApi();
        BlockingQueue<String> input = new LinkedBlockingQueue<>();
        VkView view = new VkView(101L, api, input);
        Game game = new Game(new Levels(), Player.DEFAULT_MAX_HEALTH);

        view.render(game.getLevel(), game.getPlayer(), "a < b & c > d");

        String text = api.texts.get(0);
        assertTrue(text.contains("a < b & c > d"), text);
        assertFalse(text.contains("&lt;"), text);
        assertFalse(text.contains("&amp;"), text);
    }

    @Test
    @DisplayName("Каждый диалог получает свои сообщения")
    void peersAreIsolated() {
        RecordingApi api = new RecordingApi();
        BlockingQueue<String> first = new LinkedBlockingQueue<>();
        BlockingQueue<String> second = new LinkedBlockingQueue<>();
        VkView firstView = new VkView(1L, api, first);
        VkView secondView = new VkView(2L, api, second);
        Game game = new Game(new Levels(), Player.DEFAULT_MAX_HEALTH);

        firstView.render(game.getLevel(), game.getPlayer(), null);
        secondView.render(game.getLevel(), game.getPlayer(), null);

        assertEquals(2, api.texts.size());
        assertEquals(1L, api.peers.get(0));
        assertEquals(2L, api.peers.get(1));
    }


    @Test
    @DisplayName("Ввод читается из очереди диалога")
    void readLineTakesInputFromQueue() {
        RecordingApi api = new RecordingApi();
        BlockingQueue<String> input = new LinkedBlockingQueue<>();
        VkView view = new VkView(3L, api, input);

        input.offer("W");
        assertEquals("W", view.readLine());
    }

    @Test
    @DisplayName("Две сессии в разных диалогах не мешают друг другу")
    void sessionsInDifferentPeersAreIndependent() throws Exception {
        RecordingApi api = new RecordingApi();
        BlockingQueue<String> first = new LinkedBlockingQueue<>();
        BlockingQueue<String> second = new LinkedBlockingQueue<>();
        VkView firstView = new VkView(10L, api, first);
        VkView secondView = new VkView(20L, api, second);

        GameController firstController = new GameController(
                new Game(new Levels(), Player.DEFAULT_MAX_HEALTH), firstView);
        GameController secondController = new GameController(
                new Game(new Levels(), Player.DEFAULT_MAX_HEALTH), secondView);

        Thread firstThread = new Thread(firstController::run);
        Thread secondThread = new Thread(secondController::run);
        firstThread.start();
        secondThread.start();

        // Ждём, пока оба диалога отрендерят стартовый экран.
        Thread.sleep(500);
        first.offer("Q");
        second.offer("Q");
        firstThread.join(5000);
        secondThread.join(5000);

        assertFalse(firstThread.isAlive(), "первый диалог должен завершиться");
        assertFalse(secondThread.isAlive(), "второй диалог должен завершиться");

        assertFalse(api.texts.isEmpty());
        long forFirst = api.peers.stream().filter(p -> p == 10L).count();
        long forSecond = api.peers.stream().filter(p -> p == 20L).count();
        assertTrue(forFirst > 0, "в первый диалог должны прийти сообщения");
        assertTrue(forSecond > 0, "во второй диалог должны прийти сообщения");
    }
}

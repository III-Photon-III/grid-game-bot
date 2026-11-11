package vk;

import model.Levels;
import model.Player;
import service.Game;
import service.GameController;
import view.VkView;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * Игровая сессия одного пользователя в отдельном фоновом потоке.
 */
public final class VkSession {

    private final BlockingQueue<String> input = new LinkedBlockingQueue<>();
    private final Thread thread;

    public VkSession(long peerId, VkApi api, Consumer<VkSession> onFinished) {
        this.thread = new Thread(() -> {
            try {
                VkView view = new VkView(peerId, api, input);
                GameController controller = new GameController(
                        new Game(new Levels(), Player.DEFAULT_MAX_HEALTH), view);
                controller.run();
            } finally {
                if (onFinished != null) {
                    onFinished.accept(this);
                }
            }
        }, "game-peer-" + peerId);
        this.thread.setDaemon(true);
        this.thread.start();
    }

    public void offer(String text) {
        input.offer(text);
    }
}
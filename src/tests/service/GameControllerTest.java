package service;

import model.Level;
import model.Levels;
import model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import view.GameView;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GameControllerTest {

    private Game game;
    private FakeGameView fakeView;

    @BeforeEach
    void setUp() {
        game = new Game(new Levels(42L), 100);
        fakeView = new FakeGameView();
    }

    @Test
    void testQuitCommands() {
        // Проверяем завершение по команде 'q'
        fakeView.setInputs("q");
        GameController controller = new GameController(game, fakeView);
        int score = controller.run();

        assertEquals(0, score);
        assertTrue(fakeView.bannerPrinted, "Баннер должен отрисовываться при старте");
        assertTrue(fakeView.exitPrinted, "Должно быть вызвано прощальное сообщение");
    }

    @Test
    void testSlashQuit() {
        fakeView.setInputs("/quit");
        GameController controller = new GameController(game, fakeView);
        int score = controller.run();

        assertEquals(0, score);
        assertTrue(fakeView.exitPrinted);
    }

    @Test
    void testMovementCommands() {
        fakeView.setInputs("d", "S", "a", "W", "q");
        GameController controller = new GameController(game, fakeView);

        assertDoesNotThrow(controller::run, "Команды перемещения не должны приводить к исключениям");
        assertTrue(fakeView.renderCount >= 5, "Рендер должен вызываться перед каждой командой");
    }

    @Test
    void testInvalidCommands() {
        fakeView.setInputs("", "   ", "xaxaxa", "/gav", "q");
        GameController controller = new GameController(game, fakeView);
        controller.run();

        assertTrue(fakeView.invalidCommandCount > 0, "Контроллер должен запрашивать сообщение о неверной команде");
    }

    @Test
    void testSeedCommand() {
        fakeView.setInputs("/seed 12345", "/seed blabllab", "/seed", "q");
        GameController controller = new GameController(game, fakeView);

        assertDoesNotThrow(controller::run);
    }

    @Test
    void testHelpCommand() {
        fakeView.setInputs("/help", "q");
        GameController controller = new GameController(game, fakeView);
        controller.run();

        assertTrue(fakeView.helpPrinted, "Должен вызываться printHelp");
    }

    @Test
    void testRestartCommand() {
        fakeView.setInputs("w", "/restart", "q");
        GameController controller = new GameController(game, fakeView);
        controller.run();

        assertEquals(100, game.getPlayer().getHealth());
    }

    private static class FakeGameView implements GameView {
        private final Queue<String> inputs = new ArrayDeque<>();

        boolean bannerPrinted = false;
        boolean exitPrinted = false;
        boolean defeatPrinted = false;
        boolean helpPrinted = false;
        int renderCount = 0;
        int invalidCommandCount = 0;

        void setInputs(String... lines) {
            inputs.clear();
            inputs.addAll(Arrays.asList(lines));
        }

        @Override
        public String readLine() {
            return inputs.poll(); // возвращает следующую команду или null, если очередь пуста
        }

        @Override
        public void printBanner() {
            bannerPrinted = true;
        }

        @Override
        public void render(Level level, Player player, String statusMessage) {
            renderCount++;
        }

        @Override
        public void printDefeat(Player player) {
            defeatPrinted = true;
        }

        @Override
        public void printExit(Player player) {
            exitPrinted = true;
        }

        @Override
        public void printHelp() {
            helpPrinted = true;
        }

        @Override
        public String getRestartMessage() {
            return "Restarted";
        }

        @Override
        public String getInvalidCommandMessage() {
            invalidCommandCount++;
            return "Invalid command";
        }

        @Override
        public String getLevelCompleteMessage(int levelsCompleted) {
            return "Level complete";
        }

        @Override
        public String getHealMessage(int health, int maxHealth) {
            return "Healed";
        }

        @Override
        public String getDamageMessage(int health, int maxHealth) {
            return "Damaged";
        }

        @Override
        public String getInvalidSeedMessage() {
            return "Invalid seed";
        }

        @Override
        public String getSeedMessage(long seed) {
            return "Seed set to " + seed;
        }

        @Override
        public String getUnknownCommandMessage(String cmd) {
            invalidCommandCount++;
            return "Unknown: " + cmd;
        }
    }
}
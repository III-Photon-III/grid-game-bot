package view;

import model.Levels;
import model.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import service.Game;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleViewTest {

    private PrintStream originalOut;
    private InputStream originalIn;
    private String originalOsName;
    private ByteArrayOutputStream outContent;

    @BeforeEach
    void setUp() {
        originalOut = System.out;
        originalIn = System.in;
        originalOsName = System.getProperty("os.name");

        // Избегаем запуска `cmd /c cls` на Windows во время тестов
        System.setProperty("os.name", "Linux");

        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setIn(originalIn);
        if (originalOsName != null) {
            System.setProperty("os.name", originalOsName);
        } else {
            System.clearProperty("os.name");
        }
    }

    private String output() {
        return outContent.toString(StandardCharsets.UTF_8);
    }

    private ConsoleView createViewWithInput(String input) {
        System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        return new ConsoleView();
    }

    @Test
    @DisplayName("printBanner выводит заголовок, управление и легенду карты")
    void printBannerOutputsTitleAndLegend() {
        ConsoleView view = createViewWithInput("");

        view.printBanner();

        String out = output();
        assertTrue(out.startsWith("\033[2J\033[H"));
        assertTrue(out.contains("ИГРА «ЛАБИРИНТ»"));
        assertTrue(out.contains("W — вверх, S — вниз, A — влево, D — вправо"));
        assertTrue(out.contains("# — стена"));
        assertTrue(out.contains("P — игрок"));
        assertTrue(out.contains("X — выход"));
        assertTrue(out.contains("M — монстр"));
        assertTrue(out.contains("+ — зелье"));
    }

    @Test
    @DisplayName("printHelp выводит справку по всем командам")
    void printHelpOutputsCommandsAndRules() {
        ConsoleView view = createViewWithInput("");

        view.printHelp();

        String out = output();
        assertTrue(out.contains("СПРАВКА И КОМАНДЫ"));
        assertTrue(out.contains("/help"));
        assertTrue(out.contains("/restart"));
        assertTrue(out.contains("/seed N"));
        assertTrue(out.contains("/quit"));
    }

    @Test
    @DisplayName("render выводит статистику игрока, карту и подсказку управления")
    void renderOutputsStatsAndGrid() {
        ConsoleView view = createViewWithInput("");
        Game game = new Game(new Levels(), Player.DEFAULT_MAX_HEALTH);
        Player player = game.getPlayer();

        view.render(game.getLevel(), player, null);

        String out = output();
        assertTrue(out.contains("Уровень: " + player.getCurrentLevelNumber()));
        assertTrue(out.contains("Пройдено уровней: " + player.getLevelsCompleted()));
        assertTrue(out.contains("Здоровье: " + player.getHealth() + " / " + player.getMaxHealth()));
        assertTrue(out.contains("P"));
        assertTrue(out.contains("X"));
        assertTrue(out.contains("#"));
        assertTrue(out.contains("W/A/S/D — движение"));
    }

    @Test
    @DisplayName("render выводит статус-сообщение, если оно не пустое")
    void renderOutputsStatusMessage() {
        ConsoleView view = createViewWithInput("");
        Game game = new Game(new Levels(), Player.DEFAULT_MAX_HEALTH);

        view.render(game.getLevel(), game.getPlayer(), "Тестовый статус");

        assertTrue(output().contains("Тестовый статус"));
    }

    @Test
    @DisplayName("render корректно обрабатывает пустое статус-сообщение")
    void renderHandlesEmptyStatusMessage() {
        ConsoleView view = createViewWithInput("");
        Game game = new Game(new Levels(), Player.DEFAULT_MAX_HEALTH);

        view.render(game.getLevel(), game.getPlayer(), "");

        assertTrue(output().contains("W/A/S/D — движение"));
    }

    @Test
    @DisplayName("printExit выводит экран выхода и количество пройденных уровней")
    void printExitOutputsSummary() {
        ConsoleView view = createViewWithInput("");
        Game game = new Game(new Levels(), Player.DEFAULT_MAX_HEALTH);

        view.printExit(game.getPlayer());

        String out = output();
        assertTrue(out.contains("ВЫ ВЫШЛИ ИЗ ИГРЫ"));
        assertTrue(out.contains("Пройдено уровней: 0"));
    }

    @Test
    @DisplayName("printDefeat выводит экран поражения, здоровье и варианты действий")
    void printDefeatOutputsSummary() {
        ConsoleView view = createViewWithInput("");
        Game game = new Game(new Levels(), Player.DEFAULT_MAX_HEALTH);
        Player player = game.getPlayer();

        view.printDefeat(player);

        String out = output();
        assertTrue(out.contains("ВЫ ПОГИБЛИ"));
        assertTrue(out.contains("Здоровье: " + Math.max(0, player.getHealth()) + " / " + player.getMaxHealth()));
        assertTrue(out.contains("Пройдено уровней: " + player.getLevelsCompleted()));
        assertTrue(out.contains("/restart"));
    }

    @Test
    @DisplayName("readLine читает строки из System.in и возвращает null при EOF")
    void readLineReadsLinesAndReturnsNullOnEof() {
        ConsoleView view = createViewWithInput("w\n/help\n");

        assertEquals("w", view.readLine());
        assertEquals("/help", view.readLine());
        assertNull(view.readLine());
    }

    @Test
    @DisplayName("Геттеры сообщений возвращают корректные тексты")
    void messageGettersReturnExpectedTexts() {
        ConsoleView view = createViewWithInput("");

        assertEquals(
                "Недопустимая команда. Введите W, A, S, D, Q или /help.",
                view.getInvalidCommandMessage()
        );
        assertEquals(
                "Неизвестная команда: /foo. Введите /help.",
                view.getUnknownCommandMessage("/foo")
        );
        assertEquals(
                "Новый забег: здоровье восстановлено, прогресс сброшен.",
                view.getRestartMessage()
        );
        assertEquals(
                "Новый забег с сидом 42.",
                view.getSeedMessage(42L)
        );
        assertEquals(
                "Использование: /seed <целое число>",
                view.getInvalidSeedMessage()
        );
        assertEquals(
                "Зелье! Здоровье: 8 / 10.",
                view.getHealMessage(8, 10)
        );
        assertEquals(
                "Монстр нанёс урон! Здоровье: 5 / 10.",
                view.getDamageMessage(5, 10)
        );
        assertEquals(
                "Лабиринт пройден! Пройдено уровней: 3. Загружаю следующий...",
                view.getLevelCompleteMessage(3)
        );
    }
}
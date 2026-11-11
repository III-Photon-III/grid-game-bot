package service;

import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameTest {
    private Levels levels;
    private Game game;
    private final int MAX_HEALTH = 100;

    @BeforeEach
    void setUp() {
        levels = new Levels(42);
        game = new Game(levels, MAX_HEALTH);
    }

    @Test
    void testInitialState() {
        assertNotNull(game.getLevel(), "Уровень должен быть создан");
        assertNotNull(game.getPlayer(), "Игрок должен быть создан");
        assertEquals(MAX_HEALTH, game.getPlayer().getHealth(), "Здоровье должно быть максимальным");
        assertEquals(game.getLevel().getStart(), game.getPlayer().getPosition(), "Игрок должен стоять на старте уровня");
    }

    @Test
    void testMoveDelegation() {
        Position startPos = game.getPlayer().getPosition();

        Level.MoveOutcome outcome = game.move(Level.Direction.LEFT);

        assertNotNull(outcome, "Результат хода не должен быть null");
        if (outcome != Level.MoveOutcome.STOPPED) {
            assertNotEquals(startPos, game.getPlayer().getPosition(),
                    "Персонаж должен был скользить до препятствия и сменить позицию");
        }
    }

    @Test
    void testCompleteLevel() {
        Level firstLevel = game.getLevel();

        game.completeLevel();

        Level nextLevel = game.getLevel();
        assertNotSame(firstLevel, nextLevel, "Должен загрузиться следующий уровень");
        assertEquals(nextLevel.getStart(), game.getPlayer().getPosition(),
                "На новом уровне позиция игрока должна сброситься на новый старт");
    }

    @Test
    void testRestart() {
        // Делаем ход и меняем состояние
        game.move(Level.Direction.LEFT);
        game.completeLevel();

        game.restart();

        assertEquals(MAX_HEALTH, game.getPlayer().getHealth(), "После рестарта HP должно восстановиться");
        assertEquals(game.getLevel().getStart(), game.getPlayer().getPosition(), "Позиция должна сброситься на старт 1-го уровня");
    }

    @Test
    void testSetSeed() {
        game.setSeed(12345L);

        assertNotNull(game.getLevel());
        assertEquals(game.getLevel().getStart(), game.getPlayer().getPosition());
        assertEquals(MAX_HEALTH, game.getPlayer().getHealth());
    }
}
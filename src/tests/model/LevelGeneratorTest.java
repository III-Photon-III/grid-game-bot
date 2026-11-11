package model;

import service.BalanceAnalyzer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LevelGeneratorTest {

    @Test
    void testLevelDimensions() {
        int width = 15;
        int height = 12;
        Level level = LevelGenerator.generate(width, height, 42L);

        assertNotNull(level, "Генератор не должен возвращать null");
        assertEquals(height, level.getHeight(), "Высота уровня не совпадает с запрошенной");
        assertEquals(width, level.getWidth(), "Ширина уровня не совпадает с запрошенной");
    }

    @Test
    void testOuterBoundsAreWalls() {
        Level level = LevelGenerator.generate(11, 11, 100L);

        for (int col = 0; col < level.getWidth(); col++) {
            assertTrue(level.isWall(new Position(0, col)), "Верхняя граница должна быть стеной");
            assertTrue(level.isWall(new Position(level.getHeight() - 1, col)), "Нижняя граница должна быть стеной");
        }

        for (int row = 0; row < level.getHeight(); row++) {
            assertTrue(level.isWall(new Position(row, 0)), "Левая граница должна быть стеной");
            assertTrue(level.isWall(new Position(row, level.getWidth() - 1)), "Правая граница должна быть стеной");
        }
    }

    @Test
    void testStartAndExitAreValidAndDistinct() {
        Level level = LevelGenerator.generate(10, 10, 999L);

        Position start = level.getStart();
        Position exit = level.getExit();

        assertNotNull(start, "Стартовая позиция не может быть null");
        assertNotNull(exit, "Позиция выхода не может быть null");
        assertNotEquals(start, exit, "Старт и выход не должны находиться в одной клетке");
        
        assertFalse(level.isWall(start), "Старт не может быть в стене");
        assertFalse(level.isWall(exit), "Выход не может быть в стене");
    }

    @Test
    void testLevelIsAlwaysSolvable() {
        long[] seeds = { 1L, 123L, 555L, 1024L, 9999L };
        
        for (long seed : seeds) {
            Level level = LevelGenerator.generate(12, 10, seed);
            assertTrue(BalanceAnalyzer.isSolvable(level), 
                "Сгенерированный уровень должен быть проходим (seed: " + seed + ")");
        }
    }

    @Test
    void testLevelContainsItems() {
        Level level = LevelGenerator.generate(12, 10, 1337L);

        int monsterCount = 0;
        int potionCount = 0;

        for (int row = 0; row < level.getHeight(); row++) {
            for (int col = 0; col < level.getWidth(); col++) {
                Position pos = new Position(row, col);
                if (level.isItem(pos)) {
                    Item item = level.itemAt(pos);
                    if (item instanceof Monster) {
                        monsterCount++;
                    } else if (item instanceof Potion) {
                        potionCount++;
                    }
                }
            }
        }

        assertTrue(monsterCount >= 1, "На уровне должен быть хотя бы один монстр");
        assertTrue(potionCount >= 1, "На уровне должно быть хотя бы одно зелье");
    }

    @Test
    void testStartAndExitHaveNoItemsPlacedOnThem() {
        Level level = LevelGenerator.generate(10, 10, 777L);
        
        assertFalse(level.isItem(level.getStart()), "На старте не должно быть предметов");
        assertFalse(level.isItem(level.getExit()), "На выходе не должно быть предметов");
    }
}
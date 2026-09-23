package tests;

import game.Level;
import game.Levels;
import game.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LevelsTest {

    @Test
    void testConstructorThrowsOnNullOrEmptyList() {
        assertThrows(IllegalArgumentException.class, () -> new Levels(null));
        assertThrows(IllegalArgumentException.class, () -> new Levels(Collections.emptyList()));
    }

    @Test
    void testBuiltInTemplates() {
        List<String[]> templates1 = Levels.builtInTemplates();
        assertNotNull(templates1);
        assertEquals(6, templates1.size(), "Должно быть ровно 6 встроенных лабиринтов");

        // Проверяем, что изменение полученного массива не портит шаблоны в классе
        templates1.get(0)[0] = "MODIFIED";
        List<String[]> templates2 = Levels.builtInTemplates();
        assertNotEquals("MODIFIED", templates2.get(0)[0], "Шаблоны должны возвращаться независимой копией");
    }

    @Test
    void testParseLevel() {
        String[] template = {
                "####",
                "#P.#",
                "#.X#",
                "####"
        };

        Level level = Levels.parse(template);

        assertNotNull(level);
        assertEquals(4, level.getHeight());
        assertEquals(4, level.getWidth());

        // Стены сохранились
        assertEquals('#', level.cellAt(new Position(0, 0)));
        assertEquals('#', level.cellAt(new Position(1, 3)));

        // Символы P и X заменились на Level.EMPTY
        assertEquals(Level.EMPTY, level.cellAt(new Position(1, 1)));
        assertEquals(Level.EMPTY, level.cellAt(new Position(2, 2)));

        // Выход определен верно
        assertTrue(level.isExit(new Position(2, 2)));
        assertFalse(level.isExit(new Position(1, 1)));
    }

    @Test
    void testParseEmpty() {
        Level level = Levels.parse();
        assertNotNull(level);
        assertEquals(0, level.getHeight());
        assertEquals(0, level.getWidth());
    }

    @Test
    void testNextSingleTemplate() {
        String[] single = {
                "###",
                "#PX",
                "###"
        };
        Levels levels = new Levels(List.<String[]>of(single));

        for (int i = 0; i < 5; i++) {
            Level level = levels.next();
            assertNotNull(level);
            assertEquals(3, level.getHeight());
            assertEquals(3, level.getWidth());
        }
    }

    @Test
    void testNextNeverRepeatsConsecutively() {
        String[] lvlA = {"###", "#PX", "###"};  // ширина 3
        String[] lvlB = {"####", "#P.X", "####"};  // ширина 4
        String[] lvlC = {"#####", "#P..X", "#####"};  // ширина 5

        Levels levels = new Levels(List.of(lvlA, lvlB, lvlC));

        int previousWidth = -1;
        for (int i = 0; i < 30; i++) {
            Level current = levels.next();
            int currentWidth = current.getWidth();

            assertNotEquals(previousWidth, currentWidth);
            previousWidth = currentWidth;
        }
    }

    @Test
    void testSeededRandomDeterminism() {
        Levels levels1 = new Levels(12345L);
        Levels levels2 = new Levels(12345L);

        for (int i = 0; i < 10; i++) {
            Level l1 = levels1.next();
            Level l2 = levels2.next();

            assertEquals(l1.getWidth(), l2.getWidth());
            assertEquals(l1.getHeight(), l2.getHeight());
        }
    }
}
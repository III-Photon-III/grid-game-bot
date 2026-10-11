package tests;

import game.Player;
import game.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PlayerTest {

    @Test
    void testInitialState() {
        Position start = new Position(1, 1);
        Player player = new Player(start);

        assertEquals(start, player.getPosition());
        assertEquals(0, player.getLevelsCompleted());
        assertEquals(1, player.getCurrentLevelNumber());
    }

    @Test
    void testSetPosition() {
        Player player = new Player(new Position(0, 0));
        Position newPos = new Position(3, 5);

        player.setPosition(newPos);

        assertEquals(newPos, player.getPosition());
    }

    @Test
    void testCompleteLevel() {
        Player player = new Player(new Position(1, 1));

        // Проходим 1-й уровень
        player.completeLevel();
        assertEquals(1, player.getLevelsCompleted());
        assertEquals(2, player.getCurrentLevelNumber());

        // Проходим 2-й и 3-й уровни
        player.completeLevel();
        player.completeLevel();
        assertEquals(3, player.getLevelsCompleted());
        assertEquals(4, player.getCurrentLevelNumber());
    }

    @Test
    void testInitialHealth() {
        Player player = new Player(new Position(1, 1));

        assertEquals(5, player.getHealth());
        assertEquals(5, player.getMaxHealth());
        assertFalse(player.isDefeated());
    }

    @Test
    void testCustomMaxHealth() {
        Player player = new Player(new Position(1, 1), 3);

        assertEquals(3, player.getHealth());
        assertEquals(3, player.getMaxHealth());
    }

    @Test
    void testInvalidMaxHealth() {
        assertThrows(IllegalArgumentException.class, () -> new Player(new Position(1, 1), 0));
        assertThrows(IllegalArgumentException.class, () -> new Player(new Position(1, 1), -1));
    }

    @Test
    void testTakeDamageKeepsPlayerAlive() {
        Player player = new Player(new Position(1, 1), 5);

        player.takeDamage(4);

        assertEquals(1, player.getHealth());
        assertFalse(player.isDefeated());
    }

    @Test
    void testDefeatedWhenHealthIsZero() {
        Player player = new Player(new Position(1, 1), 5);

        player.takeDamage(5);

        assertEquals(0, player.getHealth());
        assertTrue(player.isDefeated());
    }

    @Test
    void testDefeatedWhenHealthIsNegative() {
        Player player = new Player(new Position(1, 1), 5);

        player.takeDamage(7);

        assertEquals(-2, player.getHealth());
        assertTrue(player.isDefeated());
    }

    @Test
    void testNegativeDamageRejected() {
        Player player = new Player(new Position(1, 1));

        assertThrows(IllegalArgumentException.class, () -> player.takeDamage(-1));
    }

    @Test
    void testHealRestoresHealth() {
        Player player = new Player(new Position(1, 1), 5);
        player.takeDamage(3); // 2 / 5

        player.heal(2);

        assertEquals(4, player.getHealth());
        assertFalse(player.isDefeated());
    }

    @Test
    void testHealDoesNotExceedMaxHealth() {
        Player player = new Player(new Position(1, 1), 5);
        player.takeDamage(1); // 4 / 5

        player.heal(10);

        assertEquals(5, player.getHealth(), "Здоровье не должно превышать максимум");
    }

    @Test
    void testHealOnFullHealthKeepsValue() {
        Player player = new Player(new Position(1, 1), 5);

        player.heal(1);

        assertEquals(5, player.getHealth());
    }

    @Test
    void testNegativeHealRejected() {
        Player player = new Player(new Position(1, 1));

        assertThrows(IllegalArgumentException.class, () -> player.heal(-1));
    }

    @Test
    void testHealCapsAtMaxHealthAfterDamage() {
        Player player = new Player(new Position(1, 1), 5);
        player.takeDamage(1);

        player.heal(10);

        assertEquals(5, player.getHealth());
    }

    @Test
    void testHealthPersistsAcrossLevels() {
        Player player = new Player(new Position(1, 1), 5);
        player.takeDamage(2);

        player.completeLevel();

        assertEquals(3, player.getHealth());
        assertEquals(1, player.getLevelsCompleted());
    }
}
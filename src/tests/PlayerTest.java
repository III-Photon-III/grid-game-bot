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
}
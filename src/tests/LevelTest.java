package tests;

import game.Level;
import game.Levels;
import game.Player;
import game.Position;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LevelTest {

    private static final String[] SIMPLE_MAZE = {
            "#######",
            "#P....#",
            "#.###.#",
            "#....X#",
            "#######",
    };

    private static final String[] HORIZONTAL_CORRIDOR = {
            "#######",
            "#P....#",
            "#######",
    };

    private static final String[] CORRIDOR_WITH_EXIT = {
            "#######",
            "#P...X#",
            "#######",
    };

    private static final String[] VERTICAL_CORRIDOR = {
            "#####",
            "#...#",
            "#...#",
            "#.P.#",
            "#...#",
            "#...#",
            "#####",
    };

    @Test
    void testConstructorAndGetters() {
        char[][] cells = {
                {'#', '#', '#', '#'},
                {'#', '.', '.', '#'},
                {'#', '#', '#', '#'},
        };
        Position start = new Position(1, 1);
        Position exit = new Position(1, 2);

        Level level = new Level(cells, start, exit);

        assertEquals(3, level.getHeight());
        assertEquals(4, level.getWidth());
        assertEquals(start, level.getStart());
        assertEquals(exit, level.getExit());
    }

    @Test
    void testEmptyGrid() {
        Level level = new Level(new char[0][0], null, null);

        assertEquals(0, level.getHeight());
        assertEquals(0, level.getWidth());
        assertNull(level.getStart());
        assertNull(level.getExit());
    }

    @Test
    void testIsInside() {
        Level level = Levels.parse(SIMPLE_MAZE);

        assertTrue(level.isInside(new Position(0, 0)));
        assertTrue(level.isInside(new Position(4, 6)));
        assertTrue(level.isInside(new Position(2, 3)));

        assertFalse(level.isInside(new Position(-1, 0)));
        assertFalse(level.isInside(new Position(0, -1)));
        assertFalse(level.isInside(new Position(5, 0)));
        assertFalse(level.isInside(new Position(0, 7)));
        assertFalse(level.isInside(new Position(10, 10)));
    }

    @Test
    void testIsWall() {
        Level level = Levels.parse(SIMPLE_MAZE);

        assertTrue(level.isWall(new Position(0, 0)));
        assertTrue(level.isWall(new Position(2, 2)));

        assertFalse(level.isWall(new Position(1, 1)));
        assertFalse(level.isWall(new Position(2, 5)));

        assertTrue(level.isWall(new Position(-1, 1)));
        assertTrue(level.isWall(new Position(5, 1)));
        assertTrue(level.isWall(new Position(1, -1)));
        assertTrue(level.isWall(new Position(1, 7)));
    }

    @Test
    void testIsExit() {
        Level level = Levels.parse(SIMPLE_MAZE);

        assertTrue(level.isExit(new Position(3, 5)));
        assertFalse(level.isExit(new Position(1, 1)));
        assertFalse(level.isExit(new Position(0, 0)));

        Level withoutExit = Levels.parse(
                "#####",
                "#P..#",
                "#####");

        assertNull(withoutExit.getExit());
        assertFalse(withoutExit.isExit(new Position(1, 1)));
    }

    @Test
    void testCellAt() {
        Level level = Levels.parse(SIMPLE_MAZE);

        assertEquals(Level.WALL, level.cellAt(new Position(0, 0)));
        assertEquals(Level.WALL, level.cellAt(new Position(2, 2)));
        assertEquals(Level.EMPTY, level.cellAt(new Position(1, 1)));
        assertEquals(Level.EMPTY, level.cellAt(new Position(3, 5)));
        assertEquals(Level.EMPTY, level.cellAt(new Position(2, 1)));
    }

    @Test
    void testDirectionNext() {
        Position position = new Position(2, 3);

        assertEquals(new Position(1, 3), Level.Direction.UP.next(position));
        assertEquals(new Position(3, 3), Level.Direction.DOWN.next(position));
        assertEquals(new Position(2, 2), Level.Direction.LEFT.next(position));
        assertEquals(new Position(2, 4), Level.Direction.RIGHT.next(position));

        assertEquals(new Position(2, 3), position);
    }

    @Test
    void testMovePlayerSlidesUntilWall() {
        Level level = Levels.parse(HORIZONTAL_CORRIDOR);
        Player player = new Player(level.getStart());

        boolean reachedExit = level.movePlayer(player, Level.Direction.RIGHT);

        assertFalse(reachedExit);
        assertEquals(new Position(1, 5), player.getPosition());
        assertFalse(level.isWall(player.getPosition()));
    }

    @Test
    void testMovePlayerBlockedImmediately() {
        Level level = Levels.parse(HORIZONTAL_CORRIDOR);
        Player player = new Player(level.getStart());

        boolean reachedExit = level.movePlayer(player, Level.Direction.LEFT);

        assertFalse(reachedExit);
        assertEquals(new Position(1, 1), player.getPosition());
    }

    @Test
    void testMovePlayerSlidesVertically() {
        Level level = Levels.parse(VERTICAL_CORRIDOR);
        Player player = new Player(level.getStart());

        assertFalse(level.movePlayer(player, Level.Direction.UP));
        assertEquals(new Position(1, 2), player.getPosition());

        assertFalse(level.movePlayer(player, Level.Direction.DOWN));
        assertEquals(new Position(5, 2), player.getPosition());
    }

    @Test
    void testMovePlayerReachesExit() {
        Level level = Levels.parse(CORRIDOR_WITH_EXIT);
        Player player = new Player(level.getStart());

        boolean reachedExit = level.movePlayer(player, Level.Direction.RIGHT);

        assertTrue(reachedExit);
        assertEquals(new Position(1, 5), player.getPosition());
        assertTrue(level.isExit(player.getPosition()));
    }

    @Test
    void testMovePlayerTreatsBoundsAsWall() {
        Level level = Levels.parse("P....");
        Player player = new Player(level.getStart());

        assertFalse(level.movePlayer(player, Level.Direction.LEFT));
        assertEquals(new Position(0, 0), player.getPosition());

        assertFalse(level.movePlayer(player, Level.Direction.RIGHT));
        assertEquals(new Position(0, 4), player.getPosition());
    }

    @Test
    void testMovePlayerDoesNotMoveForBlockedDirection() {
        Level level = Levels.parse(HORIZONTAL_CORRIDOR);
        Player player = new Player(level.getStart());

        level.movePlayer(player, Level.Direction.RIGHT);
        Position afterFirstMove = player.getPosition();

        assertFalse(level.movePlayer(player, Level.Direction.DOWN));
        assertEquals(afterFirstMove, player.getPosition());
    }
}

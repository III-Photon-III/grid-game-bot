package tests;

import game.Level;
import game.Levels;
import game.Monster;
import game.Player;
import game.Potion;
import game.Position;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MonsterTest {

    private static final String[] CORRIDOR_WITH_MONSTER = {
            "########",
            "#P.M...#",
            "########",
    };

    private static final String[] CORRIDOR_WITH_TWO_MONSTERS = {
            "#########",
            "#P.M.M..#",
            "#########",
    };

    private static final String[] EXIT_AFTER_MONSTER = {
            "#####",
            "#PMX#",
            "#####",
    };

    @Test
    void testDefaultDamage() {
        assertEquals(1, new Monster().getDamage());
    }

    @Test
    void testCustomDamage() {
        assertEquals(4, new Monster(4).getDamage());
        assertEquals(0, new Monster(0).getDamage());
    }

    @Test
    void testNegativeDamageRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Monster(-1));
    }

    @Test
    void testApplyDamagesPlayer() {
        Monster monster = new Monster(2);
        Player player = new Player(new Position(0, 0), 5);

        monster.apply(player);

        assertEquals(3, player.getHealth());
    }

    @Test
    void testGetSymbol() {
        assertEquals('M', new Monster().getSymbol());
    }

    @Test
    void testMonsterCellIsPassable() {
        Level level = Levels.parse(CORRIDOR_WITH_MONSTER);
        Position monsterPosition = new Position(1, 3);

        assertTrue(level.isItem(monsterPosition));
        assertTrue(level.isMonster(monsterPosition));
        assertEquals(1, level.monsterAt(monsterPosition).getDamage());
        assertEquals('M', level.itemAt(monsterPosition).getSymbol());
        assertFalse(level.isWall(monsterPosition));
        assertEquals(Level.EMPTY, level.cellAt(monsterPosition));
    }

    @Test
    void testLevelWithoutMonsters() {
        Level level = Levels.parse(
                "####",
                "#PX#",
                "####");

        assertFalse(level.isMonster(new Position(1, 1)));
        assertNull(level.monsterAt(new Position(1, 1)));
    }

    @Test
    void testSlideThroughMonsterRemovesIt() {
        Level level = Levels.parse(CORRIDOR_WITH_MONSTER);
        Player player = new Player(level.getStart());

        Level.MoveOutcome outcome = level.movePlayer(player, Level.Direction.RIGHT);

        assertEquals(Level.MoveOutcome.STOPPED, outcome);
        assertEquals(new Position(1, 6), player.getPosition());
        assertEquals(4, player.getHealth());
        assertFalse(player.isDefeated());
        assertNull(level.itemAt(new Position(1, 3)));
        assertFalse(level.isItem(new Position(1, 3)));
        assertFalse(level.isMonster(new Position(1, 3)));
    }

    @Test
    void testTwoMonstersInOneSlide() {
        Level level = Levels.parse(CORRIDOR_WITH_TWO_MONSTERS);
        Player player = new Player(level.getStart());

        Level.MoveOutcome outcome = level.movePlayer(player, Level.Direction.RIGHT);

        assertEquals(Level.MoveOutcome.STOPPED, outcome);
        assertEquals(new Position(1, 7), player.getPosition());
        assertEquals(3, player.getHealth());
        assertFalse(level.isMonster(new Position(1, 3)));
        assertFalse(level.isMonster(new Position(1, 5)));
    }

    @Test
    void testDefeatOnExactZeroHealth() {
        Level level = Levels.parse(CORRIDOR_WITH_MONSTER);
        Player player = new Player(level.getStart(), 1);

        Level.MoveOutcome outcome = level.movePlayer(player, Level.Direction.RIGHT);

        assertEquals(Level.MoveOutcome.DEFEATED, outcome);
        assertEquals(new Position(1, 3), player.getPosition());
        assertEquals(0, player.getHealth());
        assertTrue(player.isDefeated());
        assertTrue(level.isMonster(new Position(1, 3)), "Монстр остаётся на клетке при поражении");
    }

    @Test
    void testDefeatWithStrongMonster() {
        char[][] cells = {
                {'#', '#', '#', '#', '#'},
                {'#', '.', '.', '.', '#'},
                {'#', '#', '#', '#', '#'},
        };
        Level level = new Level(cells, new Position(1, 1), null,
                Map.of(new Position(1, 2), (game.Item) new Monster(3)));
        Player player = new Player(level.getStart(), 2);

        Level.MoveOutcome outcome = level.movePlayer(player, Level.Direction.RIGHT);

        assertEquals(Level.MoveOutcome.DEFEATED, outcome);
        assertEquals(new Position(1, 2), player.getPosition());
        assertEquals(-1, player.getHealth());
        assertTrue(level.isMonster(new Position(1, 2)));
    }

    @Test
    void testExitReachedAfterMonsterCleared() {
        Level level = Levels.parse(EXIT_AFTER_MONSTER);
        Player player = new Player(level.getStart());

        Level.MoveOutcome outcome = level.movePlayer(player, Level.Direction.RIGHT);

        assertEquals(Level.MoveOutcome.EXIT_REACHED, outcome);
        assertEquals(level.getExit(), player.getPosition());
        assertEquals(4, player.getHealth());
        assertFalse(level.isMonster(new Position(1, 2)));
    }

    @Test
    void testDefeatTakesPrecedenceOverExit() {
        Level level = Levels.parse(EXIT_AFTER_MONSTER);
        Player player = new Player(level.getStart(), 1);

        Level.MoveOutcome outcome = level.movePlayer(player, Level.Direction.RIGHT);

        assertEquals(Level.MoveOutcome.DEFEATED, outcome);
        assertEquals(new Position(1, 2), player.getPosition());
        assertTrue(player.isDefeated());
    }
}

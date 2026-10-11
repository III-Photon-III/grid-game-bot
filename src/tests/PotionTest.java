package tests;

import game.Level;
import game.Levels;
import game.Player;
import game.Potion;
import game.Position;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PotionTest {

    private static final String[] CORRIDOR_WITH_POTION = {
            "########",
            "#P..+..#",
            "########",
    };

    private static final String[] MONSTER_THEN_POTION = {
            "#########",
            "#P.M..+#",
            "#########",
    };

    private static final String[] POTION_THEN_MONSTER = {
            "#########",
            "#P.+..M#",
            "#########",
    };

    @Test
    void testDefaultHealing() {
        assertEquals(1, new Potion().getHealing());
    }

    @Test
    void testCustomHealing() {
        assertEquals(3, new Potion(3).getHealing());
        assertEquals(0, new Potion(0).getHealing());
    }

    @Test
    void testNegativeHealingRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Potion(-1));
    }

    @Test
    void testGetSymbol() {
        assertEquals('+', new Potion().getSymbol());
    }

    @Test
    void testSlideThroughPotionHealsAndRemovesIt() {
        Level level = Levels.parse(CORRIDOR_WITH_POTION);
        Player player = new Player(level.getStart(), 5);
        player.takeDamage(2); // 3 / 5

        Level.MoveOutcome outcome = level.movePlayer(player, Level.Direction.RIGHT);

        assertEquals(Level.MoveOutcome.STOPPED, outcome);
        assertEquals(new Position(1, 6), player.getPosition());
        assertEquals(4, player.getHealth());
        assertNull(level.itemAt(new Position(1, 4)));
        assertFalse(level.isItem(new Position(1, 4)));
    }

    @Test
    void testPotionAtFullHealthIsConsumedWithoutEffect() {
        Level level = Levels.parse(CORRIDOR_WITH_POTION);
        Player player = new Player(level.getStart(), 5); // уже максимум

        Level.MoveOutcome outcome = level.movePlayer(player, Level.Direction.RIGHT);

        assertEquals(Level.MoveOutcome.STOPPED, outcome);
        assertEquals(5, player.getHealth(), "Здоровье не растёт выше максимума");
        assertNull(level.itemAt(new Position(1, 4)), "Зелье всё равно исчезает");
    }

    @Test
    void testPotionHealsAfterMonsterInSameSlide() {
        Level level = Levels.parse(MONSTER_THEN_POTION);
        Player player = new Player(level.getStart(), 5);

        Level.MoveOutcome outcome = level.movePlayer(player, Level.Direction.RIGHT);

        assertEquals(Level.MoveOutcome.STOPPED, outcome);
        assertEquals(5, player.getHealth(), "Урон 1 компенсирован зельем +1");
        assertFalse(level.isItem(new Position(1, 3)));
        assertFalse(level.isItem(new Position(1, 6)));
    }

    @Test
    void testPotionDoesNotSaveFromLethalMonsterBehindIt() {
        Level level = Levels.parse(POTION_THEN_MONSTER);
        Player player = new Player(level.getStart(), 1);

        Level.MoveOutcome outcome = level.movePlayer(player, Level.Direction.RIGHT);

        // Зельё нет спасения: игрок уже на максимуме питьё (1/1), зельё не вступает
        // в действие, а смертельный монстр бьёт игрока в 0.
        assertEquals(Level.MoveOutcome.DEFEATED, outcome);
        assertEquals(new Position(1, 6), player.getPosition());
        assertEquals(0, player.getHealth());
        assertTrue(player.isDefeated());
        assertTrue(level.isItem(new Position(1, 6)), "Монстр остаётся при поражении");
        assertFalse(level.isItem(new Position(1, 3)), "Зелье израсходовано");
    }

    @Test
    void testLethalMonsterNotRemovedOnDefeat() {
        Level level = Levels.parse(
                "#######",
                "#P.M..#",
                "#######");
        Player player = new Player(level.getStart(), 1);

        Level.MoveOutcome outcome = level.movePlayer(player, Level.Direction.RIGHT);

        assertEquals(Level.MoveOutcome.DEFEATED, outcome);
        assertTrue(level.isItem(new Position(1, 3)), "Монстр остаётся при поражении");
        assertTrue(level.isMonster(new Position(1, 3)));
        assertTrue(player.isDefeated());
    }

    @Test
    void testMonsterBehindPotionSavesPlayer() {
        Level level = Levels.parse(
                "#########",
                "#P.+..M.#",
                "#########");
        // Здоровье 1/5: зельё поднимает до 2, монстр снимает 1 -> выживает.
        Player player = new Player(level.getStart(), 5);
        player.takeDamage(4);

        Level.MoveOutcome outcome = level.movePlayer(player, Level.Direction.RIGHT);

        assertEquals(Level.MoveOutcome.STOPPED, outcome);
        assertEquals(new Position(1, 7), player.getPosition());
        assertEquals(1, player.getHealth());
        assertFalse(player.isDefeated());
    }

    @Test
    void testLethalMonsterEndsMoveBeforePotionBeyond() {
        Level level = Levels.parse(
                "#########",
                "#P.M.+.#",
                "#########");
        Player player = new Player(level.getStart(), 1);

        Level.MoveOutcome outcome = level.movePlayer(player, Level.Direction.RIGHT);

        // Смертельный удар обрывает движение: зельё позади монстра не достигнуто.
        assertEquals(Level.MoveOutcome.DEFEATED, outcome);
        assertEquals(new Position(1, 3), player.getPosition());
        assertTrue(level.isItem(new Position(1, 5)), "Зелье позади не израсходовано");
        assertTrue(level.isItem(new Position(1, 3)));
    }
}
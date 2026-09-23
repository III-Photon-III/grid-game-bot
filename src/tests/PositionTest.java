package tests;

import game.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PositionTest {

    @Test
    void testConstructorAndGetters() {
        Position pos = new Position(3, 7);

        assertEquals(3, pos.getRow());
        assertEquals(7, pos.getCol());
    }

    @Test
    void testTranslate() {
        Position original = new Position(2, 5);

        // Смещение в плюс
        Position movedPositive = original.translate(3, 4);
        assertEquals(5, movedPositive.getRow());
        assertEquals(9, movedPositive.getCol());

        // Смещение в минус
        Position movedNegative = original.translate(-5, -2);
        assertEquals(-3, movedNegative.getRow());
        assertEquals(3, movedNegative.getCol());

        // Исходный объект остался неизменным (иммутабельность)
        assertEquals(2, original.getRow());
        assertEquals(5, original.getCol());
    }

    @Test
    void testEqualsSameAndEqualObjects() {
        Position pos1 = new Position(1, 2);
        Position pos2 = new Position(1, 2);

        // Рефлексивность (ссылка на сам объект)
        assertEquals(pos1, pos1);

        // Одинаковые координаты у разных объектов
        assertEquals(pos1, pos2);
        assertEquals(pos2, pos1);
    }

    @Test
    void testEqualsDifferentCoordinates() {
        Position pos = new Position(1, 2);

        assertNotEquals(pos, new Position(0, 2)); // разный row
        assertNotEquals(pos, new Position(1, 0)); // разный col
        assertNotEquals(pos, new Position(2, 1)); // координаты перепутаны местами
    }

    @Test
    void testEqualsNullAndOtherTypes() {
        Position pos = new Position(1, 2);

        assertNotEquals(null, pos);
        assertNotEquals(pos, "Some string");
        assertNotEquals(pos, Integer.valueOf(42));
    }

    @Test
    void testHashCodeContract() {
        Position pos1 = new Position(4, 5);
        Position pos2 = new Position(4, 5);

        assertEquals(pos1, pos2);
        assertEquals(pos1.hashCode(), pos2.hashCode());
    }

    @Test
    void testToString() {
        Position pos = new Position(3, -5);
        assertEquals("(3, -5)", pos.toString());
    }
}
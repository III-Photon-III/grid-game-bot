package game;

import java.util.HashMap;
import java.util.Map;

public final class Level {

    public static final char WALL = '#';
    public static final char EMPTY = '.';

    public enum Direction {

        UP(-1, 0),
        DOWN(1, 0),
        LEFT(0, -1),
        RIGHT(0, 1);

        private final int deltaRow;
        private final int deltaCol;

        Direction(int deltaRow, int deltaCol) {
            this.deltaRow = deltaRow;
            this.deltaCol = deltaCol;
        }

        public Position next(Position current) {
            return current.translate(deltaRow, deltaCol);
        }
    }

    public enum MoveOutcome {

        STOPPED,
        EXIT_REACHED,
        DEFEATED
    }

    private final char[][] cells;
    private final Position start;
    private final Position exit;
    private final Map<Position, Item> items;

    public Level(char[][] cells, Position start, Position exit) {
        this(cells, start, exit, null);
    }

    public Level(char[][] cells, Position start, Position exit, Map<Position, Item> items) {
        this.cells = cells;
        this.start = start;
        this.exit = exit;
        this.items = items == null ? new HashMap<>() : new HashMap<>(items);
    }

    public int getHeight() {
        return cells.length;
    }

    public int getWidth() {
        return cells.length > 0 ? cells[0].length : 0;
    }

    public Position getStart() {
        return start;
    }

    public Position getExit() {
        return exit;
    }

    public boolean isInside(Position position) {
        return position.getRow() >= 0 && position.getRow() < getHeight()
                && position.getCol() >= 0 && position.getCol() < getWidth();
    }

    public boolean isWall(Position position) {
        return !isInside(position) || cells[position.getRow()][position.getCol()] == WALL;
    }

    public boolean isExit(Position position) {
        return exit != null && exit.equals(position);
    }

    public char cellAt(Position position) {
        return cells[position.getRow()][position.getCol()];
    }

    public Item itemAt(Position position) {
        return items.get(position);
    }

    public boolean isItem(Position position) {
        return items.containsKey(position);
    }

    public Monster monsterAt(Position position) {
        Item item = items.get(position);
        return item instanceof Monster ? (Monster) item : null;
    }

    public boolean isMonster(Position position) {
        return monsterAt(position) != null;
    }

    public MoveOutcome movePlayer(Player player, Direction direction) {
        while (true) {
            Position next = direction.next(player.getPosition());

            if (isWall(next)) {
                return MoveOutcome.STOPPED;
            }

            Item item = items.get(next);
            if (item != null) {
                item.apply(player);
                if (player.isDefeated()) {
                    // Поражение: игрок остаётся на клетке предмета, предмет выживает.
                    player.setPosition(next);
                    return MoveOutcome.DEFEATED;
                }
                items.remove(next);
            }

            player.setPosition(next);

            if (isExit(next)) {
                return MoveOutcome.EXIT_REACHED;
            }
        }
    }
}
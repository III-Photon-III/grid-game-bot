package game;

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

    private final char[][] cells;
    private final Position start;
    private final Position exit;

    public Level(char[][] cells, Position start, Position exit) {
        this.cells = cells;
        this.start = start;
        this.exit = exit;
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

    public boolean movePlayer(Player player, Direction direction) {
        while (true) {
            Position next = direction.next(player.getPosition());

            if (isWall(next)) {
                return false;
            }

            player.setPosition(next);

            if (isExit(next)) {
                return true;
            }
        }
    }
}
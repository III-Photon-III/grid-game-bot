package game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class Levels {
    private static final String[][] BUILT_IN = {
            {
                    "###########",
                    "#P....#..X#",
                    "#..###....#",
                    "#.........#",
                    "###########",
            },
            {
                    "##########",
                    "#P..#..#.#",
                    "#..##....#",
                    "#.#....#.#",
                    "#....##..#",
                    "#X.......#",
                    "##########",
            },
            {
                    "#########",
                    "#P..#..X#",
                    "#.#.#.#.#",
                    "#.#...#.#",
                    "#.#####.#",
                    "#...#...#",
                    "#########",
            },
            {
                    "##########",
                    "#P.#..#.X#",
                    "#..###...#",
                    "#.#......#",
                    "#...###..#",
                    "#........#",
                    "##########",
            },
            {
                    "############",
                    "#P.........#",
                    "#..#...#...#",
                    "#.....#....#",
                    "#.#...#...X#",
                    "#.....#....#",
                    "#..#.......#",
                    "############",
            },
            {
                    "############",
                    "#P..#..#..X#",
                    "#..........#",
                    "##...#...#.#",
                    "#..........#",
                    "##.#....#..#",
                    "############",
            },
    };

    private final List<String[]> templates;
    private final Random random;
    private int lastIndex = -1;

    public Levels() {
        this(builtInTemplates(), new Random());
    }

    public Levels(long seed) {
        this(builtInTemplates(), new Random(seed));
    }

    public Levels(List<String[]> templates) {
        this(templates, new Random());
    }

    private Levels(List<String[]> templates, Random random) {
        if (templates == null || templates.isEmpty()) {
            throw new IllegalArgumentException("Список лабиринтов не может быть пустым");
        }
        this.templates = List.copyOf(templates);
        this.random = random;
    }

    public Level next() {
        int index = pickIndex();
        lastIndex = index;
        return parse(templates.get(index));
    }

    private int pickIndex() {
        if (templates.size() == 1) {
            return 0;
        }
        int index = random.nextInt(templates.size() - 1);
        if (lastIndex >= 0 && index >= lastIndex) {
            index++;
        }
        return index;
    }

    public static Level parse(String... rows) {
        int height = rows.length;
        int width = height > 0 ? rows[0].length() : 0;
        char[][] cells = new char[height][width];
        Position start = null;
        Position exit = null;

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < rows[row].length(); col++) {
                char symbol = rows[row].charAt(col);
                if (symbol == 'P') {
                    start = new Position(row, col);
                    cells[row][col] = Level.EMPTY;
                } else if (symbol == 'X') {
                    exit = new Position(row, col);
                    cells[row][col] = Level.EMPTY;
                } else {
                    cells[row][col] = symbol;
                }
            }
        }
        return new Level(cells, start, exit);
    }

    public static List<String[]> builtInTemplates() {
        List<String[]> copy = new ArrayList<>();
        for (String[] rows : BUILT_IN) {
            copy.add(rows.clone());
        }
        return copy;
    }
}

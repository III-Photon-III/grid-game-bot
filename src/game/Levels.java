package game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class Levels {

    // Обучающие уровни: без обязательного урона (L = 0 при оптимальной игре),
    // учат скольжению, монстрам и зельям.
    private static final String[][] TRAINING = {
            {
                    "#########",
                    "#P......#",
                    "#..###..#",
                    "#......+#",
                    "#.#####.#",
                    "#X......#",
                    "#########",
            },
            {
                    "###########",
                    "#P...M...X#",
                    "#.#######.#",
                    "#.#.....#.#",
                    "#....+..#.#",
                    "#.#.....#.#",
                    "#.#######.#",
                    "#.........#",
                    "###########",
            },
            {
                    "#############",
                    "#P....+....X#",
                    "#.###.#####.#",
                    "#M..........#",
                    "#.#####.###.#",
                    "#...........#",
                    "#############",
            },
    };

    // Основной пул: 5 лабиринтов с неизбежным уроном (L = 1)
    // и 3 безопасных (L = 0) — средние потери 5/8 = 0.625.
    private static final String[][] MAIN = {
            // Прямой коридор: монстр на единственном маршруте к выходу.
            {
                    "###########",
                    "#P...M...X#",
                    "###########",
            },
            // Поворот вниз: спуск только через монстра.
            {
                    "#########",
                    "#P....###",
                    "#####.###",
                    "#####M###",
                    "#####.###",
                    "#####X###",
                    "#########",
            },
            // Ступенька вправо-вниз с монстром на повороте.
            {
                    "##########",
                    "#P....####",
                    "#####M####",
                    "#####.X###",
                    "##########",
            },
            // Зелье до монстра не спасает: 5 -> 5 -> 4 -> выход.
            {
                    "#############",
                    "#P..+..M...X#",
                    "#############",
            },
            // Два монстра с зельем между ними: 5 -> 4 -> 5 -> 4.
            {
                    "###",
                    "#P#",
                    "#M#",
                    "#+#",
                    "#M#",
                    "#X#",
                    "###",
            },
            // Безопасные лабиринты (L = 0): монстров можно обойти.
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
    };

    private final List<String[]> training;
    private final List<String[]> templates;
    private final Random random;
    private int trainingIndex;
    private int lastIndex = -1;

    public Levels() {
        this(trainingTemplates(), mainTemplates(), new Random());
    }

    public Levels(long seed) {
        this(trainingTemplates(), mainTemplates(), new Random(seed));
    }

    public Levels(List<String[]> templates) {
        this(List.of(), templates, new Random());
    }

    private Levels(List<String[]> training, List<String[]> templates, Random random) {
        if (templates == null || templates.isEmpty()) {
            throw new IllegalArgumentException("Список лабиринтов не может быть пустым");
        }
        this.training = List.copyOf(training);
        this.templates = List.copyOf(templates);
        this.random = random;
    }

    /** Сбрасывает забег: обучающие уровни и выбор без повторов — заново. */
    public void restart() {
        trainingIndex = 0;
        lastIndex = -1;
    }

    public Level next() {
        if (trainingIndex < training.size()) {
            return parse(training.get(trainingIndex++));
        }
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
        Map<Position, Item> items = new HashMap<>();

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < rows[row].length(); col++) {
                char symbol = rows[row].charAt(col);
                if (symbol == 'P') {
                    start = new Position(row, col);
                    cells[row][col] = Level.EMPTY;
                } else if (symbol == 'X') {
                    exit = new Position(row, col);
                    cells[row][col] = Level.EMPTY;
                } else if (symbol == 'M') {
                    items.put(new Position(row, col), new Monster());
                    cells[row][col] = Level.EMPTY;
                } else if (symbol == '+') {
                    items.put(new Position(row, col), new Potion());
                    cells[row][col] = Level.EMPTY;
                } else {
                    cells[row][col] = symbol;
                }
            }
        }
        return new Level(cells, start, exit, items);
    }

    public static List<String[]> trainingTemplates() {
        List<String[]> copy = new ArrayList<>();
        for (String[] rows : TRAINING) {
            copy.add(rows.clone());
        }
        return copy;
    }

    public static List<String[]> mainTemplates() {
        List<String[]> copy = new ArrayList<>();
        for (String[] rows : MAIN) {
            copy.add(rows.clone());
        }
        return copy;
    }

    public static List<String[]> builtInTemplates() {
        List<String[]> copy = trainingTemplates();
        copy.addAll(mainTemplates());
        return copy;
    }
}

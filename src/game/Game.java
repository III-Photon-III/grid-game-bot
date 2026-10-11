package game;

import java.util.Locale;
import java.util.Scanner;


public final class Game {

    private enum State {

        RUNNING,
        QUIT,
        DEFEATED
    }

    private static final String INVALID_COMMAND_MESSAGE =
            "Недопустимая команда. Введите W, A, S, D, Q или /help.";

    private Levels levels;
    private final Renderer renderer;
    private final Scanner scanner;

    private Level level;
    private Player player;
    private String statusMessage;
    private State state = State.RUNNING;
    private final int maxHealth;

    public Game(Levels levels, Renderer renderer) {
        this(levels, renderer, Player.DEFAULT_MAX_HEALTH);
    }

    public Game(Levels levels, Renderer renderer, int maxHealth) {
        this.levels = levels;
        this.renderer = renderer;
        this.scanner = new Scanner(System.in);
        this.maxHealth = maxHealth;
    }

    public static void main(String[] args) {
        new Game(new Levels(), new Renderer()).run();
    }

    public int run() {
        renderer.printBanner();
        startRun(null);

        while (true) {
            while (state == State.RUNNING) {
                renderer.render(level, player, statusMessage);
                readCommand();
            }

            if (state == State.DEFEATED) {
                renderer.printDefeat(player);
                if (offerRestart()) {
                    continue;
                }
                return player.getLevelsCompleted();
            }

            renderer.printExit(player);
            return player.getLevelsCompleted();
        }
    }

    private void startRun(String message) {
        level = levels.next();
        player = new Player(level.getStart(), maxHealth);
        statusMessage = message;
        state = State.RUNNING;
    }

    private boolean offerRestart() {
        String line = scanner.hasNextLine() ? scanner.nextLine().trim() : "";
        if (line.equalsIgnoreCase("/restart")) {
            levels.restart();
            startRun("Новый забег: здоровье восстановлено, прогресс сброшен.");
            return true;
        }
        return false;
    }

    private void readCommand() {
        String line = scanner.hasNextLine() ? scanner.nextLine().trim() : "Q";
        if (line.isEmpty()) {
            statusMessage = INVALID_COMMAND_MESSAGE;
            return;
        }
        if (line.charAt(0) == '/') {
            executeCommand(line);
            return;
        }
        switch (Character.toUpperCase(line.charAt(0))) {
            case 'W':
                move(Level.Direction.UP);
                break;
            case 'A':
                move(Level.Direction.LEFT);
                break;
            case 'S':
                move(Level.Direction.DOWN);
                break;
            case 'D':
                move(Level.Direction.RIGHT);
                break;
            case 'Q':
                state = State.QUIT;
                break;
            default:
                statusMessage = INVALID_COMMAND_MESSAGE;
        }
    }

    private void executeCommand(String line) {
        String[] parts = line.split("\\s+");
        String name = parts[0].toLowerCase(Locale.ROOT);
        switch (name) {
            case "/help":
            case "/?":
                showHelp();
                break;
            case "/restart":
                levels.restart();
                startRun("Новый забег: здоровье восстановлено, прогресс сброшен.");
                break;
            case "/seed":
                startSeededRun(parts);
                break;
            case "/quit":
                state = State.QUIT;
                break;
            default:
                statusMessage = "Неизвестная команда: " + parts[0] + ". Введите /help.";
        }
    }

    private void showHelp() {
        renderer.printHelp();
        if (scanner.hasNextLine()) {
            scanner.nextLine(); 
        }
        statusMessage = null;
    }

    private void startSeededRun(String[] parts) {
        if (parts.length != 2) {
            statusMessage = "Использование: /seed <целое число>";
            return;
        }
        try {
            long seed = Long.parseLong(parts[1]);
            levels = new Levels(seed);
            startRun("Новый забег с сидом " + seed + ".");
        } catch (NumberFormatException e) {
            statusMessage = "Использование: /seed <целое число>";
        }
    }

    private void move(Level.Direction direction) {
        int healthBefore = player.getHealth();

        switch (level.movePlayer(player, direction)) {
            case EXIT_REACHED:
                completeLevel();
                break;
            case DEFEATED:
                state = State.DEFEATED;
                break;
            case STOPPED:
            default:
                statusMessage = statusAfterMove(healthBefore);
                break;
        }
    }

    private String statusAfterMove(int healthBefore) {
        int health = player.getHealth();
        if (health > healthBefore) {
            return "Зелье! Здоровье: " + health + " / " + player.getMaxHealth() + ".";
        }
        if (health < healthBefore) {
            return "Монстр нанёс урон! Здоровье: " + health + " / " + player.getMaxHealth() + ".";
        }
        return null;
    }

    private void completeLevel() {
        player.completeLevel();
        statusMessage = "Лабиринт пройден! Пройдено уровней: "
                + player.getLevelsCompleted() + ". Загружаю следующий...";

        level = levels.next();
        player.setPosition(level.getStart());
    }

    public Level getLevel() {
        return level;
    }

    public Player getPlayer() {
        return player;
    }

    public int getMaxHealth() {
        return maxHealth;
    }
}

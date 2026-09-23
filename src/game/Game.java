package game;

import java.util.Scanner;


public final class Game {

    private static final String INVALID_COMMAND_MESSAGE =
            "Недопустимая команда. Введите W, A, S, D или Q.";

    private final Levels levels;
    private final Renderer renderer;
    private final Scanner scanner;

    private Level level;
    private Player player;
    private String statusMessage;
    private boolean quit;

    public Game(Levels levels, Renderer renderer) {
        this.levels = levels;
        this.renderer = renderer;
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        new Game(new Levels(), new Renderer()).run();
    }

    public int run() {
        renderer.printBanner();

        level = levels.next();
        player = new Player(level.getStart());

        while (!quit) {
            renderer.render(level, player, statusMessage);
            readCommand();
        }

        renderer.printExit(player);
        return player.getLevelsCompleted();
    }

    private void readCommand() {
        String line = scanner.hasNextLine() ? scanner.nextLine().trim() : "Q";
        if (line.isEmpty()) {
            statusMessage = INVALID_COMMAND_MESSAGE;
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
                quit = true;
                break;
            default:
                statusMessage = INVALID_COMMAND_MESSAGE;
        }
    }

    private void move(Level.Direction direction) {
        if (level.movePlayer(player, direction)) {
            completeLevel();
        } else {
            statusMessage = null;
        }
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
}

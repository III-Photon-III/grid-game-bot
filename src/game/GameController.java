package game;

import java.util.Locale;

public final class GameController {

    private enum State { RUNNING, QUIT, DEFEATED }

    private final Game game;
    private final GameView view;

    private State state = State.RUNNING;
    private String statusMessage;

    public GameController(Game game, GameView view) {
        this.game = game;
        this.view = view;
    }

    public int run() {
        view.printBanner();

        while (true) {
            while (state == State.RUNNING) {
                view.render(game.getLevel(), game.getPlayer(), statusMessage);
                readCommand();
            }

            if (state == State.DEFEATED) {
                view.printDefeat(game.getPlayer());
                if (offerRestart()) {
                    continue;
                }
                return game.getPlayer().getLevelsCompleted();
            }

            view.printExit(game.getPlayer());
            return game.getPlayer().getLevelsCompleted();
        }
    }

    private boolean offerRestart() {
        String line = view.readLine();
        if (line == null) {
            return false;
        }
        if (line.trim().equalsIgnoreCase("/restart")) {
            game.restart();
            statusMessage = view.getRestartMessage();
            state = State.RUNNING;
            return true;
        }
        return false;
    }

    private void readCommand() {
        String line = view.readLine();
        if (line == null) {
            state = State.QUIT;
            return;
        }
        line = line.trim();
        if (line.isEmpty()) {
            statusMessage = view.getInvalidCommandMessage();
            return;
        }
        if (line.charAt(0) == '/') {
            executeCommand(line);
            return;
        }

        switch (Character.toUpperCase(line.charAt(0))) {
            case 'W': processMove(Level.Direction.UP); break;
            case 'A': processMove(Level.Direction.LEFT); break;
            case 'S': processMove(Level.Direction.DOWN); break;
            case 'D': processMove(Level.Direction.RIGHT); break;
            case 'Q': state = State.QUIT; break;
            default: statusMessage = view.getInvalidCommandMessage();
        }
    }

    private void processMove(Level.Direction direction) {
        int healthBefore = game.getPlayer().getHealth();

        Level.MoveOutcome moveResult = game.move(direction);

        String resultStr = moveResult.toString();

        switch (moveResult) {
            case EXIT_REACHED:
                game.completeLevel();
                statusMessage = view.getLevelCompleteMessage(game.getPlayer().getLevelsCompleted());
                break;

            case DEFEATED:
                state = State.DEFEATED;
                break;

            case STOPPED:
            default:
                updateStatusAfterMove(healthBefore);
                break;
        }
    }

    private void updateStatusAfterMove(int healthBefore) {
        int health = game.getPlayer().getHealth();
        int max = game.getPlayer().getMaxHealth();

        if (health > healthBefore) {
            statusMessage = view.getHealMessage(health, max);
        } else if (health < healthBefore) {
            statusMessage = view.getDamageMessage(health, max);
        } else {
            statusMessage = null;
        }
    }

    private void executeCommand(String line) {
        String[] parts = line.split("\\s+");
        String name = parts[0].toLowerCase(Locale.ROOT);
        switch (name) {
            case "/help":
            case "/?":
                view.printHelp();
                view.readLine();
                statusMessage = null;
                break;
            case "/restart":
                game.restart();
                statusMessage = view.getRestartMessage();
                break;
            case "/seed":
                if (parts.length != 2) {
                    statusMessage = view.getInvalidSeedMessage();
                } else {
                    try {
                        long seed = Long.parseLong(parts[1]);
                        game.setSeed(seed);
                        statusMessage = view.getSeedMessage(seed);
                    } catch (NumberFormatException e) {
                        statusMessage = view.getInvalidSeedMessage();
                    }
                }
                break;
            case "/quit":
                state = State.QUIT;
                break;
            default:
                statusMessage = view.getUnknownCommandMessage(parts[0]);
        }
    }
}

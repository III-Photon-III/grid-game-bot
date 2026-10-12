package game;

public class Main {
    public static void main(String[] args) {

        Game game = new Game(new Levels(), Player.DEFAULT_MAX_HEALTH);

        GameView view = new ConsoleView();

        GameController controller = new GameController(game, view);

        controller.run();
    }
}
package game;

public final class Game {

    private Levels levels;
    private Level level;
    private Player player;
    private final int maxHealth;

    public Game(Levels levels, int maxHealth) {
        this.levels = levels;
        this.maxHealth = maxHealth;
        startRun();
    }

    public void startRun() {
        level = levels.next();
        player = new Player(level.getStart(), maxHealth);
    }

    public void restart() {
        levels.restart();
        startRun();
    }

    public void setSeed(long seed) {
        levels = new Levels(seed);
        startRun();
    }

    public Level.MoveOutcome move(Level.Direction direction) {
        return level.movePlayer(player, direction);
    }

    public void completeLevel() {
        player.completeLevel();
        level = levels.next();
        player.setPosition(level.getStart());
    }

    public Level getLevel() { return level; }
    public Player getPlayer() { return player; }
    public int getMaxHealth() { return maxHealth; }
}
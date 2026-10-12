package game;

public interface GameView {
    void printBanner();
    void printHelp();
    void render(Level level, Player player, String statusMessage);
    void printExit(Player player);
    void printDefeat(Player player);

    String getInvalidCommandMessage();
    String getUnknownCommandMessage(String command);
    String getRestartMessage();
    String getSeedMessage(long seed);
    String getInvalidSeedMessage();
    String getHealMessage(int health, int maxHealth);
    String getDamageMessage(int health, int maxHealth);
    String getLevelCompleteMessage(int levelsCompleted);
}
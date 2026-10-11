package game;

public final class Player {

    public static final int DEFAULT_MAX_HEALTH = 5;

    private final int maxHealth;

    private Position position;
    private int levelsCompleted;
    private int health;

    public Player(Position startPosition) {
        this(startPosition, DEFAULT_MAX_HEALTH);
    }

    public Player(Position startPosition, int maxHealth) {
        if (maxHealth <= 0) {
            throw new IllegalArgumentException("Максимальное здоровье должно быть больше нуля");
        }
        this.position = startPosition;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public int getLevelsCompleted() {
        return levelsCompleted;
    }

    public void completeLevel() {
        levelsCompleted++;
    }

    public int getCurrentLevelNumber() {
        return levelsCompleted + 1;
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public void takeDamage(int damage) {
        if (damage < 0) {
            throw new IllegalArgumentException("Урон не может быть отрицательным");
        }
        health -= damage;
    }

    public void heal(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Лечение не может быть отрицательным");
        }
        health = Math.min(maxHealth, health + amount);
    }

    public boolean isDefeated() {
        return health <= 0;
    }
}
package game;

public final class Potion implements Item {

    public static final int DEFAULT_HEALING = 1;

    private final int healing;

    public Potion() {
        this(DEFAULT_HEALING);
    }

    public Potion(int healing) {
        if (healing < 0) {
            throw new IllegalArgumentException("Сила зелья не может быть отрицательной");
        }
        this.healing = healing;
    }

    public int getHealing() {
        return healing;
    }

    @Override
    public void apply(Player player) {
        player.heal(healing);
    }

    @Override
    public char getSymbol() {
        return '+';
    }
}
package game;

public final class Monster implements Item {

    public static final int DEFAULT_DAMAGE = 1;

    private final int damage;

    public Monster() {
        this(DEFAULT_DAMAGE);
    }

    public Monster(int damage) {
        if (damage < 0) {
            throw new IllegalArgumentException("Урон монстра не может быть отрицательным");
        }
        this.damage = damage;
    }

    public int getDamage() {
        return damage;
    }

    @Override
    public void apply(Player player) {
        player.takeDamage(damage);
    }

    @Override
    public char getSymbol() {
        return 'M';
    }
}

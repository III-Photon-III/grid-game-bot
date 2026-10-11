package game;


public final class BalanceAnalyzer {

    public static final int DEFAULT_MAX_HEALTH = 5;

    private static record State(Position position, int health) {
    }

    private static record SlideResult(State state, boolean reachedExit) {
    }

    private BalanceAnalyzer() {
    }

    public static boolean isSolvable(Level level) {
        return bestFinalHealth(level, DEFAULT_MAX_HEALTH) >= 1;
    }

    public static boolean isSolvable(Level level, int maxHealth) {
        return bestFinalHealth(level, maxHealth) >= 1;
    }

    public static int minimumLoss(Level level) {
        return minimumLoss(level, DEFAULT_MAX_HEALTH);
    }

    public static int minimumLoss(Level level, int maxHealth) {
        return maxHealth - bestFinalHealth(level, maxHealth);
    }

    private static int bestFinalHealth(Level level, int maxHealth) {
        if (level.getStart() == null || level.getExit() == null) {
            return 0;
        }

        State initial = new State(level.getStart(), maxHealth);
        java.util.Queue<State> queue = new java.util.ArrayDeque<>();
        java.util.Set<State> visited = new java.util.HashSet<>();

        queue.add(initial);
        visited.add(initial);
        int best = 0;

        State current;
        while ((current = queue.poll()) != null) {
            for (Level.Direction direction : Level.Direction.values()) {
                SlideResult result = slide(level, current, direction);
                if (result == null) {
                    continue;
                }
                if (result.reachedExit) {
                    best = Math.max(best, result.state.health());
                    continue;
                }
                if (result.state.equals(current)) {
                    continue;
                }
                if (visited.add(result.state)) {
                    queue.add(result.state);
                }
            }
        }
        return best;
    }

    private static SlideResult slide(Level level, State from, Level.Direction direction) {
        int health = from.health();
        Position position = from.position();

        while (true) {
            Position next = direction.next(position);

            if (level.isWall(next)) {
                return new SlideResult(new State(position, health), false);
            }

            Item item = level.itemAt(next);
            if (item instanceof Monster monster) {
                health -= monster.getDamage();
                if (health <= 0) {
                    return null;
                }
            } else if (item instanceof Potion potion) {
                health = Math.min(Player.DEFAULT_MAX_HEALTH, health + potion.getHealing());
            }

            position = next;
            if (level.isExit(next)) {
                return new SlideResult(new State(position, health), true);
            }
        }
    }
}
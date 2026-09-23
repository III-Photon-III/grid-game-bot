package game;

public final class Renderer {
    private static final String CLEAR_SCREEN = "\033[2J\033[H";
    private static final int DEFAULT_WIDTH = 80;

    public void printBanner() {
        clearScreen();
        line("=================================");
        line("        ИГРА «ЛАБИРИНТ»");
        line("=================================");
        line("Скользите по клеткам до ближайшего");
        line("препятствия и находите выход X.");
        line("");
        line("Управление:");
        line("  W — вверх, S — вниз, A — влево, D — вправо");
        line("  Q — выйти из игры");
        line("");
        line("Обозначения карты:");
        line("  # — стена");
        line("  . — свободная клетка");
        line("  P — игрок");
        line("  X — выход");
        line("=================================");
    }

    public void render(Level level, Player player, String statusMessage) {
        clearScreen();
        line("Уровень: " + player.getCurrentLevelNumber());
        line("Пройдено уровней: " + player.getLevelsCompleted());
        line("");
        printGrid(level, player);
        line("");
        line("W — вверх");
        line("A — влево");
        line("S — вниз");
        line("D — вправо");
        line("Q — выйти");
        if (statusMessage != null && !statusMessage.isEmpty()) {
            line("");
            line(statusMessage);
        }
    }

    public void printExit(Player player) {
        clearScreen();
        line("=================================");
        line("        ВЫ ВЫШЛИ ИЗ ИГРЫ");
        line("=================================");
        line("Пройдено уровней: " + player.getLevelsCompleted());
    }

    private void printGrid(Level level, Player player) {
        Position playerPosition = player.getPosition();
        for (int row = 0; row < level.getHeight(); row++) {
            StringBuilder rowLine = new StringBuilder();
            for (int col = 0; col < level.getWidth(); col++) {
                Position position = new Position(row, col);
                if (position.equals(playerPosition)) {
                    rowLine.append('P');
                } else if (level.isExit(position)) {
                    rowLine.append('X');
                } else {
                    rowLine.append(level.cellAt(position));
                }
            }
            line(rowLine.toString());
        }
    }

    private void clearScreen() {
        System.out.print(CLEAR_SCREEN);
        System.out.flush();
    }

    private void line(String text) {
        if (text.isEmpty()) {
            System.out.println();
            return;
        }
        int padding = (terminalWidth() - text.length()) / 2;
        if (padding > 0) {
            System.out.println(" ".repeat(padding) + text);
        } else {
            System.out.println(text);
        }
    }

    private int terminalWidth() {
        String columns = System.getenv("COLUMNS");
        if (columns != null) {
            try {
                int width = Integer.parseInt(columns.trim());
                if (width > 0) {
                    return width;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return DEFAULT_WIDTH;
    }
}
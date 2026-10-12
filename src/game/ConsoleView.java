package game;

public final class ConsoleView implements GameView {
    private static final String CLEAR_SCREEN = "\033[2J\033[H";
    private static final int DEFAULT_WIDTH = 80;

    @Override
    public void printBanner() {
        clearScreen();
        line("=================================");
        line("        ИГРА «ЛАБИРИНТ»");
        line("=================================");
        line("Скользите по клеткам до ближайшего");
        line("препятствия и находите выход X.");
        line("Монстры M отнимают здоровье,");
        line("зелья + лечат (не выше максимума).");
        line("При нуле здоровья — поражение.");
        line("");
        line("Управление:");
        line("  W — вверх, S — вниз, A — влево, D — вправо");
        line("  Q — выйти из игры");
        line("  /help — справка по командам");
        line("");
        line("Обозначения карты:");
        line("  # — стена");
        line("  . — свободная клетка");
        line("  P — игрок");
        line("  X — выход");
        line("  M — монстр");
        line("  + — зелье");
        line("=================================");
    }

    @Override
    public void printHelp() {
        clearScreen();
        line("=================================");
        line("     СПРАВКА И КОМАНДЫ");
        line("=================================");
        line("Управление: W вверх, S вниз,");
        line("A влево, D вправо, Q выход.");
        line("");
        line("Команды:");
        line("  /help — эта справка");
        line("  /restart — новый забег с нуля:");
        line("    здоровье и прогресс сбрасываются,");
        line("    обучающие уровни начинаются заново");
        line("  /seed N — новый забег с сидом N");
        line("    (детерминированные лабиринты)");
        line("  /quit — выйти из игры");
        line("");
        line("Правила:");
        line("  Скользите до стены или выхода X.");
        line("  M — минус здоровье (монстр исчезает),");
        line("  + — плюс здоровье (не выше максимума).");
        line("  При 0 здоровья — поражение.");
        line("=================================");
        line("Нажмите Enter, чтобы вернуться в игру...");
    }

    @Override
    public void render(Level level, Player player, String statusMessage) {
        clearScreen();
        line("Уровень: " + player.getCurrentLevelNumber());
        line("Пройдено уровней: " + player.getLevelsCompleted());
        line("Здоровье: " + player.getHealth() + " / " + player.getMaxHealth());
        line("");
        line("");
        printGrid(level, player);
        line("");
        line("W/A/S/D — движение, Q — выход, /help — команды");
        line("");
        if (statusMessage != null && !statusMessage.isEmpty()) {
            line("");
            line(statusMessage);
        }
    }

    @Override
    public void printExit(Player player) {
        clearScreen();
        line("=================================");
        line("        ВЫ ВЫШЛИ ИЗ ИГРЫ");
        line("=================================");
        line("Пройдено уровней: " + player.getLevelsCompleted());
    }

    @Override
    public void printDefeat(Player player) {
        clearScreen();
        line("=================================");
        line("         ВЫ ПОГИБЛИ");
        line("=================================");
        line("Вас убил монстр. Здоровье: "
                + Math.max(0, player.getHealth()) + " / " + player.getMaxHealth());
        line("Пройдено уровней: " + player.getLevelsCompleted());
        line("");
        line("/restart — сыграть ещё раз, Enter — выйти");
    }

    @Override
    public String getInvalidCommandMessage() {
        return "Недопустимая команда. Введите W, A, S, D, Q или /help.";
    }

    @Override
    public String getUnknownCommandMessage(String command) {
        return "Неизвестная команда: " + command + ". Введите /help.";
    }

    @Override
    public String getRestartMessage() {
        return "Новый забег: здоровье восстановлено, прогресс сброшен.";
    }

    @Override
    public String getSeedMessage(long seed) {
        return "Новый забег с сидом " + seed + ".";
    }

    @Override
    public String getInvalidSeedMessage() {
        return "Использование: /seed <целое число>";
    }

    @Override
    public String getHealMessage(int health, int maxHealth) {
        return "Зелье! Здоровье: " + health + " / " + maxHealth + ".";
    }

    @Override
    public String getDamageMessage(int health, int maxHealth) {
        return "Монстр нанёс урон! Здоровье: " + health + " / " + maxHealth + ".";
    }

    @Override
    public String getLevelCompleteMessage(int levelsCompleted) {
        return "Лабиринт пройден! Пройдено уровней: " + levelsCompleted + ". Загружаю следующий...";
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
                } else if (level.isItem(position)) {
                    rowLine.append(level.itemAt(position).getSymbol());
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
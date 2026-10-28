package game;

import java.util.concurrent.BlockingQueue;


public final class VkView implements GameView {
    private static final String WALL = "⬛";
    private static final String FLOOR = "⬜";
    private static final String PLAYER = "🟦";
    private static final String EXIT = "🟩";
    private static final String MONSTER = "🟪";
    private static final String POTION = "🟨";

    private final long peerId;
    private final VkApi api;
    private final BlockingQueue<String> inputQueue;

    private final ConsoleView texts = new ConsoleView();

    public VkView(long peerId, VkApi api, BlockingQueue<String> inputQueue) {
        this.peerId = peerId;
        this.api = api;
        this.inputQueue = inputQueue;
    }

    @Override
    public String readLine() {
        try {
            return inputQueue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }


    @Override
    public void printBanner() {
        send("ИГРА «ЛАБИРИНТ»\n\n"
                + "Скользите по клеткам до ближайшего препятствия и находите выход 🟩.\n"
                + "Монстры 🟪 отнимают здоровье, зелья 🟨 лечат (не выше максимума).\n"
                + "При нуле здоровья — поражение.\n\n"
                + "Управление:\n"
                + "  W — вверх, S — вниз, A — влево, D — вправо, Q — выход\n"
                + "  /help — справка по командам\n\n"
                + "Обозначения карты:\n"
                + "  ⬛ — стена, ⬜ — свободная клетка, 🟦 — игрок, 🟩 — выход\n"
                + "  🟪 — монстр, 🟨 — зелье\n\n"
                + "Отправьте любое сообщение, чтобы начать…");
    }

    @Override
    public void printHelp() {
        send("СПРАВКА И КОМАНДЫ\n\n"
                + "Управление: W вверх, S вниз, A влево, D вправо, Q выход.\n\n"
                + "Команды:\n"
                + "  /help — эта справка\n"
                + "  /restart — новый забег с нуля:\n"
                + "    здоровье и прогресс сбрасываются,\n"
                + "    обучающие уровни начинаются заново\n"
                + "  /seed N — новый забег с сидом N\n"
                + "    (детерминированные лабиринты)\n"
                + "  /quit — выйти из игры\n\n"
                + "Правила:\n"
                + "  Скользите до стены или выхода 🟩.\n"
                + "  🟪 — минус здоровье (монстр исчезает),\n"
                + "  🟨 — плюс здоровье (не выше максимума).\n"
                + "  При 0 здоровья — поражение.\n\n"
                + "Отправьте любое сообщение, чтобы вернуться в игру…");
    }

    @Override
    public void render(Level level, Player player, String statusMessage) {
        StringBuilder text = new StringBuilder();
        text.append("Уровень: ").append(player.getCurrentLevelNumber())
                .append(" | Пройдено: ").append(player.getLevelsCompleted())
                .append(" | Здоровье: ").append(player.getHealth())
                .append(" / ").append(player.getMaxHealth());
        text.append("\n\n").append(grid(level, player));
        text.append("\n\nW/A/S/D — движение, Q — выход, /help — команды");
        if (statusMessage != null && !statusMessage.isEmpty()) {
            text.append("\n\n").append(statusMessage);
        }
        send(text.toString());
    }

    @Override
    public void printExit(Player player) {
        send("ВЫ ВЫШЛИ ИЗ ИГРЫ\n"
                + "Пройдено уровней: " + player.getLevelsCompleted()
                + "\n\nОтправьте любое сообщение, чтобы сыграть снова…");
    }

    @Override
    public void printDefeat(Player player) {
        send("ВЫ ПОГИБЛИ\n"
                + "Вас убил монстр. Здоровье: "
                + Math.max(0, player.getHealth()) + " / " + player.getMaxHealth()
                + "\nПройдено уровней: " + player.getLevelsCompleted()
                + "\n\n/restart — сыграть ещё раз, /quit — выйти");
    }

    @Override
    public String getInvalidCommandMessage() {
        return texts.getInvalidCommandMessage();
    }

    @Override
    public String getUnknownCommandMessage(String command) {
        return texts.getUnknownCommandMessage(command);
    }

    @Override
    public String getRestartMessage() {
        return texts.getRestartMessage();
    }

    @Override
    public String getSeedMessage(long seed) {
        return texts.getSeedMessage(seed);
    }

    @Override
    public String getInvalidSeedMessage() {
        return texts.getInvalidSeedMessage();
    }

    @Override
    public String getHealMessage(int health, int maxHealth) {
        return texts.getHealMessage(health, maxHealth);
    }

    @Override
    public String getDamageMessage(int health, int maxHealth) {
        return texts.getDamageMessage(health, maxHealth);
    }

    @Override
    public String getLevelCompleteMessage(int levelsCompleted) {
        return texts.getLevelCompleteMessage(levelsCompleted);
    }

    private String grid(Level level, Player player) {
        Position playerPosition = player.getPosition();
        StringBuilder builder = new StringBuilder();
        for (int row = 0; row < level.getHeight(); row++) {
            for (int col = 0; col < level.getWidth(); col++) {
                Position position = new Position(row, col);
                if (position.equals(playerPosition)) {
                    builder.append(PLAYER);
                } else if (level.isExit(position)) {
                    builder.append(EXIT);
                } else if (level.isItem(position)) {
                    builder.append(symbolOf(level.itemAt(position)));
                } else if (level.cellAt(position) == Level.WALL) {
                    builder.append(WALL);
                } else {
                    builder.append(FLOOR);
                }
            }
            builder.append('\n');
        }
        int length = builder.length();
        if (length > 0) {
            builder.setLength(length - 1);
        }
        return builder.toString();
    }

    private String symbolOf(Item item) {
        if (item instanceof Monster) {
            return MONSTER;
        }
        if (item instanceof Potion) {
            return POTION;
        }
        return FLOOR;
    }

    private void send(String text) {
        System.out.println("DEBUG send to peer " + peerId + ": " + text.substring(0, Math.min(text.length(), 80)));
        try {
            api.sendMessage(peerId, text);
        } catch (RuntimeException e) {
            String detail = e.getMessage();
            if (e instanceof VkApiException apiErr) {
                detail = "VK API error " + apiErr.getErrorCode() + ": " + e.getMessage();
            }
            System.err.println("Failed to send VK message to peer " + peerId + ": " + detail);
        }
    }
}

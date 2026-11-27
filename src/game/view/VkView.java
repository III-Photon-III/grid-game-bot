package view;

import exceptions.VkApiException;
import model.*;
import vk.VkApi;

import java.util.concurrent.BlockingQueue;

public final class VkView implements GameView {
    private enum Tile {
        WALL("⬛"),
        FLOOR("⬜"),
        PLAYER("🟦"),
        EXIT("🟩"),
        MONSTER("🟪"),
        POTION("🟨");

        private final String symbol;

        Tile(String symbol) {
            this.symbol = symbol;
        }

        String symbol() {
            return symbol;
        }
    }

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
        String text = "ИГРА «ЛАБИРИНТ»\n\n"
                + "Скользите по клеткам до ближайшего препятствия и находите выход " + Tile.EXIT.symbol() + ".\n"
                + "Монстры " + Tile.MONSTER.symbol() + " отнимают здоровье, зелья " + Tile.POTION.symbol() + " лечат (не выше максимума).\n"
                + "При нуле здоровья — поражение.\n\n"
                + "Управление:\n"
                + "  W — вверх, S — вниз, A — влево, D — вправо, Q — выход\n"
                + "  /help — справка по командам\n\n"
                + "Обозначения карты:\n"
                + "  " + Tile.WALL.symbol() + " — стена, " + Tile.FLOOR.symbol() + " — свободная клетка, "
                + Tile.PLAYER.symbol() + " — игрок, " + Tile.EXIT.symbol() + " — выход\n"
                + "  " + Tile.MONSTER.symbol() + " — монстр, " + Tile.POTION.symbol() + " — зелье\n\n"
                + "Нажмите кнопку ниже, чтобы начать…";
        send(text, startKeyboard());
    }

    private String startKeyboard() {
        return """
                {
                  "inline": true,
                  "buttons": [
                    [{"action": {"type": "text", "payload": "{\\\"cmd\\\":\\\"start\\\"}", "label": "Начать игру"}, "color": "primary"}]
                  ]
                }
                """;
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
                + "  Скользите до стены или выхода " + Tile.EXIT.symbol() + ".\n"
                + "  " + Tile.MONSTER.symbol() + " — минус здоровье (монстр исчезает),\n"
                + "  " + Tile.POTION.symbol() + " — плюс здоровье (не выше максимума).\n"
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
        send(text.toString(), directionKeyboard());
    }

    @Override
    public void printExit(Player player) {
        String text = "ВЫ ВЫШЛИ ИЗ ИГРЫ\n"
                + "Пройдено уровней: " + player.getLevelsCompleted()
                + "\n\n";
        send(text, endGameKeyboard());
    }

    @Override
    public void printDefeat(Player player) {
        String text = "ВЫ ПОГИБЛИ\n"
                + "Вас убил монстр. Здоровье: "
                + Math.max(0, player.getHealth()) + " / " + player.getMaxHealth()
                + "\nПройдено уровней: " + player.getLevelsCompleted()
                + "\n\n/restart — сыграть ещё раз, /quit — выйти";
        send(text, endGameKeyboard());
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
                builder.append(tileAt(level, playerPosition, new Position(row, col)).symbol());
            }
            builder.append('\n');
        }
        int length = builder.length();
        if (length > 0) {
            builder.setLength(length - 1);
        }
        return builder.toString();
    }

    private Tile tileAt(Level level, Position playerPosition, Position position) {
        if (position.equals(playerPosition)) {
            return Tile.PLAYER;
        }
        if (level.isExit(position)) {
            return Tile.EXIT;
        }
        if (level.isItem(position)) {
            return tileOf(level.itemAt(position));
        }
        if (level.cellAt(position) == Level.Cell.WALL) {
            return Tile.WALL;
        }
        return Tile.FLOOR;
    }

    private Tile tileOf(Item item) {
        if (item instanceof Monster) {
            return Tile.MONSTER;
        }
        if (item instanceof Potion) {
            return Tile.POTION;
        }
        return Tile.FLOOR;
    }

    private void send(String text) {
        send(text, null);
    }

    private void send(String text, String keyboard) {
        System.out.println("DEBUG send to peer " + peerId + ": "
                + text.substring(0, Math.min(text.length(), 80)));
        try {
            if (keyboard != null) {
                api.sendMessageWithKeyboard(peerId, text, keyboard);
            } else {
                api.sendMessage(peerId, text);
            }
        } catch (RuntimeException e) {
            String detail = e.getMessage();
            if (e instanceof VkApiException apiErr) {
                detail = "VK API error " + apiErr.getErrorCode() + ": " + e.getMessage();
            }
            System.err.println("Failed to send VK message to peer " + peerId + ": " + detail);
        }
    }

    private String directionKeyboard() {
        return """
                {
                  "inline": true,
                  "buttons": [
                    [{"action": {"type": "text", "payload": "{\\\"cmd\\\":\\\"w\\\"}", "label": "\u2B06 ВВЕРХ"}, "color": "primary"}],
                    [
                      {"action": {"type": "text", "payload": "{\\\"cmd\\\":\\\"a\\\"}", "label": "\u2B05 ВЛЕВО"}, "color": "secondary"},
                      {"action": {"type": "text", "payload": "{\\\"cmd\\\":\\\"d\\\"}", "label": "ВПРАВО \u27A1"}, "color": "secondary"}
                    ],
                    [{"action": {"type": "text", "payload": "{\\\"cmd\\\":\\\"s\\\"}", "label": "\u2B07 ВНИЗ"}, "color": "primary"}],
                    [{"action": {"type": "text", "payload": "{\\\"cmd\\\":\\\"q\\\"}", "label": "Выход"}, "color": "negative"}]
                  ]
                }
                """;
    }

    private String endGameKeyboard() {
        return """
                {
                  "inline": true,
                  "buttons": [
                    [{"action": {"type": "text", "payload": "{\\\"cmd\\\":\\\"restart\\\"}", "label": "Сыграть снова"}, "color": "primary"}],
                    [{"action": {"type": "text", "payload": "{\"cmd\":\"quit\"}", "label": "Выйти"}, "color": "negative"}]
                  ]
                }
                """;
    }
}
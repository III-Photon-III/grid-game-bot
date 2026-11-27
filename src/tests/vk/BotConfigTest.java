package vk;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BotConfigTest {

    @Test
    @DisplayName("Читает ключи и значения из .env, игнорирует комментарии и кавычки")
    void loadsValuesFromEnvFile(@TempDir Path tempDir) throws IOException {
        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, """
                # Комментарий
                TEST_VK_TOKEN = "secret_token_123"
                TEST_VK_GROUP_ID='987654'
                INVALID_LINE_WITHOUT_EQUALS
                EMPTY_VALUE=
                """);

        BotConfig config = BotConfig.load(envFile);

        assertEquals("secret_token_123", config.get("TEST_VK_TOKEN", null, 0));
        assertEquals("987654", config.get("TEST_VK_GROUP_ID", null, 1));
        assertNull(config.get("EMPTY_VALUE", null, 2));
        assertNull(config.get("MISSING_KEY", null, 3));
    }

    @Test
    @DisplayName("Аргументы командной строки имеют приоритет над файлом .env")
    void cliArgsOverrideEnvFile(@TempDir Path tempDir) throws IOException {
        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, "TEST_VK_TOKEN=from_file\n");

        BotConfig config = BotConfig.load(envFile);
        String[] args = {"from_cli"};

        assertEquals("from_cli", config.get("TEST_VK_TOKEN", args, 0));
    }

    @Test
    @DisplayName("Корректно работает, если файл .env отсутствует")
    void handlesMissingEnvFile(@TempDir Path tempDir) {
        Path missingFile = tempDir.resolve("does_not_exist.env");

        BotConfig config = BotConfig.load(missingFile);

        assertNull(config.get("TEST_VK_TOKEN", null, 0));
        assertEquals("cli_token", config.get("TEST_VK_TOKEN", new String[]{"cli_token"}, 0));
    }
}
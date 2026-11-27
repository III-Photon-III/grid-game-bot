package vk;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class BotConfig {

    private static final Path DEFAULT_ENV_FILE = Path.of(".env");

    private final Map<String, String> fileValues;

    private BotConfig(Map<String, String> fileValues) {
        this.fileValues = fileValues;
    }

    public static BotConfig load() {
        return load(DEFAULT_ENV_FILE);
    }

    public static BotConfig load(Path envFile) {
        return new BotConfig(parseEnvFile(envFile));
    }

    public String get(String name, String[] args, int argIndex) {
        if (args != null && args.length > argIndex && args[argIndex] != null
                && !args[argIndex].isBlank()) {
            return args[argIndex].trim();
        }

        String envValue = System.getenv(name);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        String fileValue = fileValues.get(name);
        if (fileValue != null && !fileValue.isBlank()) {
            return fileValue.trim();
        }

        return null;
    }

    private static Map<String, String> parseEnvFile(Path envFile) {
        Map<String, String> result = new HashMap<>();
        if (envFile == null || !Files.exists(envFile)) {
            return result;
        }

        try {
            for (String rawLine : Files.readAllLines(envFile, StandardCharsets.UTF_8)) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eqIndex = line.indexOf('=');
                if (eqIndex <= 0) {
                    continue;
                }
                String key = line.substring(0, eqIndex).trim();
                String value = stripQuotes(line.substring(eqIndex + 1).trim());
                if (!key.isEmpty() && !value.isEmpty()) {
                    result.put(key, value);
                }
            }
        } catch (IOException e) {
            System.err.println("Не удалось прочитать файл конфигурации " + envFile + ": " + e.getMessage());
        }
        return result;
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1).trim();
        }
        return value;
    }
}
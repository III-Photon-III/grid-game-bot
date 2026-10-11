package tests;

import game.BalanceAnalyzer;
import game.Level;
import game.Levels;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Баланс-тест: минимальные потери L_i каждой карты при оптимальной игре
 * (BFS по состояниям «позиция, здоровье») и ожидаемая длина забега.
 * Цель: забег ~10–15 уровней при идеальной игре, бесконечная игра невозможна.
 */
public class LevelsBalanceTest {

    private static final int MAX_HEALTH = 5;

    @Test
    void trainingMapsAreForgiving() {
        List<String[]> maps = Levels.trainingTemplates();

        assertFalse(maps.isEmpty(), "Пул обучения не должен быть пустым");
        for (String[] rows : maps) {
            Level level = Levels.parse(rows);
            String name = rows[0];
            assertTrue(BalanceAnalyzer.isSolvable(level), "Обучающая карта должна быть проходима: " + name);
            assertEquals(0, BalanceAnalyzer.minimumLoss(level),
                    "Обучающая карта не должна требовать обязательного урона: " + name);
        }
    }

    @Test
    void mainMapsMeetBalanceTargets() {
        List<String[]> maps = Levels.mainTemplates();
        assertFalse(maps.isEmpty(), "Основной пул не должен быть пустым");

        int zeroLossMaps = 0;
        int lossAtLeastOne = 0;
        int totalLoss = 0;

        for (String[] rows : maps) {
            Level level = Levels.parse(rows);
            assertTrue(BalanceAnalyzer.isSolvable(level), "Карта должна быть проходима: " + rows[0]);

            int loss = BalanceAnalyzer.minimumLoss(level);
            assertTrue(loss <= 3, "Слишком дорогая карта (L=" + loss + "): " + rows[0]);

            totalLoss += loss;
            if (loss >= 1) {
                lossAtLeastOne++;
            }
            if (loss == 0) {
                zeroLossMaps++;
            }
        }

        double averageLoss = (double) totalLoss / maps.size();
        assertTrue(averageLoss >= 0.4 && averageLoss <= 0.7,
                "Средние потери L̄=" + averageLoss + " вне цели [0.4; 0.7] — забег "
                        + "слишком длинный или слишком короткий");
        assertTrue(lossAtLeastOne * 100 / maps.size() >= 60,
                "Карт с обязательным уроном должно быть большинство: "
                        + lossAtLeastOne + " из " + maps.size());
        assertTrue(zeroLossMaps >= 1,
                "Нужна хотя бы одна карта с L=0 (награда за неочевидный маршрут)");
    }

    @Test
    void combinedRunLengthStaysInTargetRange() {
        int training = Levels.trainingTemplates().size();
        int main = Levels.mainTemplates().size();

        int totalLoss = 0;
        for (String[] rows : Levels.mainTemplates()) {
            totalLoss += BalanceAnalyzer.minimumLoss(Levels.parse(rows));
        }
        double averageMainLoss = (double) totalLoss / main;

        // Идеальная игра: обучающие уровни без потерь + ожидаемая длина
        // сложного пула ≈ maxHealth / средние потери.
        double expectedRun = training + MAX_HEALTH / averageMainLoss;

        assertTrue(expectedRun <= 15.5,
                "Ожидаемая длина забега при идеальной игре " + expectedRun
                        + " уровней — цель 10–15");
    }
}
package model;

import service.BalanceAnalyzer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class LevelGenerator {

    private LevelGenerator() {}

    public static Level generate(int width, int height, long seed) {
        Random rnd = new Random(seed);

        while (true) {
            char[][] cells = new char[height][width];
            
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    cells[i][j] = Level.WALL;
                }
            }

            int numRooms = 4 + rnd.nextInt(5);
            for (int i = 0; i < numRooms; i++) {
                int rw = 3 + rnd.nextInt(4); 
                int rh = 3 + rnd.nextInt(4); 
                
                int rx = 1 + rnd.nextInt(Math.max(1, width - rw - 1));
                int ry = 1 + rnd.nextInt(Math.max(1, height - rh - 1));

                for (int r = ry; r < ry + rh; r++) {
                    for (int c = rx; c < rx + rw; c++) {
                        if (r < height - 1 && c < width - 1) {
                            cells[r][c] = Level.EMPTY;
                        }
                    }
                }
            }

            int numPillars = 2 + rnd.nextInt(5);
            for (int i = 0; i < numPillars; i++) {
                int pr = 2 + rnd.nextInt(Math.max(1, height - 4));
                int pc = 2 + rnd.nextInt(Math.max(1, width - 4));
                cells[pr][pc] = Level.WALL;
            }

            List<Position> emptySpots = new ArrayList<>();
            for (int r = 1; r < height - 1; r++) {
                for (int c = 1; c < width - 1; c++) {
                    if (cells[r][c] == Level.EMPTY) {
                        emptySpots.add(new Position(r, c));
                    }
                }
            }

            if (emptySpots.size() < 15) continue;

            Collections.shuffle(emptySpots, rnd);
            
            Position start = emptySpots.get(0);
            Position exit = emptySpots.get(emptySpots.size() - 1); 

            Level baseLevel = new Level(cells, start, exit, null);
            if (!BalanceAnalyzer.isSolvable(baseLevel, 999)) {
                continue; 
            }

            Map<Position, Item> items = new HashMap<>();
            int numMonsters = 1 + rnd.nextInt(4); 
            int numPotions = 1 + rnd.nextInt(3);

            int spotIdx = 1;
            for (int i = 0; i < numMonsters && spotIdx < emptySpots.size() - 1; i++) {
                items.put(emptySpots.get(spotIdx++), new Monster());
            }
            for (int i = 0; i < numPotions && spotIdx < emptySpots.size() - 1; i++) {
                items.put(emptySpots.get(spotIdx++), new Potion());
            }

            Level finalLevel = new Level(cells, start, exit, items);
            
            if (BalanceAnalyzer.isSolvable(finalLevel)) {
                
                return finalLevel;
            }
        }
    }
}
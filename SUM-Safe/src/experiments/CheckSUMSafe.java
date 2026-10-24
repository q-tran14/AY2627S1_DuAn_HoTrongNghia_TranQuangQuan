package experiments;

import algorithms.sum.AlgoSUMSafe;
import java.nio.file.*;
import java.util.*;

public class CheckSUMSafe {
    private static int cases;
    private static final Path ROOT = Path.of("results/local/safe-checks");
    private static final AlgoSUMSafe REUSED = new AlgoSUMSafe();

    private static void check(List<String> lines, long threshold, int window) throws Exception {
        Files.write(ROOT.resolve("input.txt"), lines);
        Map<String, Long> expected = CheckSumCorrectness.oracle(lines, threshold, window);
        for (boolean pruning : new boolean[]{false, true}) {
            REUSED.runAlgorithm(ROOT.resolve("input.txt").toString(), ROOT.resolve("output.txt").toString(),
                    threshold, window, pruning);
            Map<String, Long> actual = CheckSumCorrectness.readOutput(ROOT.resolve("output.txt"));
            if (!expected.equals(actual)) throw new AssertionError(
                    "Case " + cases + " pruning=" + pruning + "\n" + lines + "\nexpected=" + expected + "\nactual=" + actual);
            if (REUSED.huiCount != actual.size()) throw new AssertionError("HUI count");
        }
        cases++;
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(ROOT);
        check(Files.readAllLines(Path.of("datasets/DB_Utility.txt")), 30, 10);
        check(List.of("1:10:10", "1:5:5"), 10, 10);
        check(List.of("1:10:10", "2:1:1", "2:1:1"), 10, 1);
        check(List.of("1 2 3:12:4 4 4"), 10, 10);
        check(List.of("1:10:10", "2:1:1"), 10, 1);
        check(List.of("1:5:5", "2:1:1", "2:1:1", "1:7:7"), 10, 1);
        check(List.of(), 10, 0);
        check(List.of("@metadata", "#comment", "", "1 2:0:0 0"), 1, 0);
        check(List.of("1 2:4000000000:2000000000 2000000000"), 3000000000L, 10);
        Random random = new Random(20261021);
        int prefixes = 0;
        for (int k = 0; k < 500; k++) {
            List<String> lines = new ArrayList<>();
            int count = 1 + random.nextInt(10);
            long threshold = 1 + random.nextInt(100);
            int window = random.nextInt(12);
            for (int t = 0; t < count; t++) {
                List<Integer> items = new ArrayList<>();
                for (int i = 1; i <= 7; i++) if (random.nextBoolean()) items.add(i);
                if (items.isEmpty()) items.add(1);
                Collections.shuffle(items, random);
                StringJoiner ids = new StringJoiner(" "), values = new StringJoiner(" ");
                long sum = 0;
                for (int item : items) {
                    int value = random.nextInt(16);
                    ids.add("" + item); values.add("" + value); sum += value;
                }
                lines.add(ids + ":" + sum + ":" + values);
                check(lines, threshold, window);
                prefixes++;
            }
        }
        int rejected = 0;
        for (String invalid : List.of("1 1:2:1 1", "1:5:4", "1:-1:-1", "1 2:4:4", "1:2")) {
            Files.writeString(ROOT.resolve("invalid.txt"), invalid);
            try {
                REUSED.runAlgorithm(ROOT.resolve("invalid.txt").toString(), ROOT.resolve("invalid.out").toString(), 1, 1, true);
                throw new AssertionError("Accepted invalid input: " + invalid);
            } catch (java.io.IOException expected) { rejected++; }
        }
        String report = "PASS: 9 fixed cases + " + prefixes + " prefixes from 500 random databases.\n"
                + "Both RU on/off match exhaustive oracle (itemsets AND utility).\n"
                + "Same miner instance reused across calls; invalid inputs rejected: " + rejected + ".\n"
                + "Seed=20261021. RC>=0; nonnegative full-history utility.\n"
                + "This section tests cold-start prefixes; retained-state updates are checked separately.\n";
        Files.writeString(ROOT.resolve("report.txt"), report);
        System.out.print(report);
    }
}

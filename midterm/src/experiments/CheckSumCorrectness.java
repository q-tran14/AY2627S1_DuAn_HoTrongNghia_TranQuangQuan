package experiments;

import algorithms.sum.AlgoScentedUtilityMiner;
import java.nio.file.*;
import java.util.*;

/** Small independent exhaustive oracle. No performance claims and no large datasets. */
public class CheckSumCorrectness {
    static Map<String, Long> oracle(List<String> lines, long threshold, int maxR) {
        List<Map<Integer, Long>> transactions = new ArrayList<>();
        Map<Integer, Integer> lastSeen = new TreeMap<>();
        for (String line : lines) {
            if (line.isBlank() || "#%@".indexOf(line.charAt(0)) >= 0) continue;
            String[] fields = line.split(":");
            String[] items = fields[0].trim().split("\\s+");
            String[] values = fields[2].trim().split("\\s+");
            Map<Integer, Long> transaction = new HashMap<>();
            for (int j = 0; j < items.length; j++) {
                int item = Integer.parseInt(items[j]);
                transaction.put(item, Long.parseLong(values[j]));
                lastSeen.put(item, transactions.size() + 1);
            }
            transactions.add(transaction);
        }
        List<Integer> active = new ArrayList<>();
        for (int item : lastSeen.keySet()) {
            // Match the current source boundary: RC >= 0, for every item in X.
            if (transactions.size() - lastSeen.get(item) <= maxR) active.add(item);
        }
        if (active.size() > 20) throw new IllegalArgumentException("Oracle limited to 20 active items");
        Map<String, Long> result = new TreeMap<>();
        for (int mask = 1; mask < (1 << active.size()); mask++) {
            List<Integer> selected = new ArrayList<>();
            for (int j = 0; j < active.size(); j++) if ((mask & (1 << j)) != 0) selected.add(active.get(j));
            long utility = 0;
            for (Map<Integer, Long> transaction : transactions) {
                if (transaction.keySet().containsAll(selected)) {
                    for (int item : selected) utility += transaction.get(item);
                }
            }
            if (utility >= threshold) result.put(selected.toString(), utility);
        }
        return result;
    }

    static Map<String, Long> readOutput(Path output) throws Exception {
        Map<String, Long> result = new TreeMap<>();
        for (String line : Files.readAllLines(output)) {
            if (line.isBlank() || line.startsWith("@")) continue;
            String[] fields = line.split("#UTIL:");
            List<Integer> items = new ArrayList<>();
            for (String item : fields[0].trim().split("\\s+")) items.add(Integer.parseInt(item));
            Collections.sort(items);
            if (result.put(items.toString(), Long.parseLong(fields[1].trim())) != null) {
                throw new IllegalStateException("Duplicate output itemset: " + items);
            }
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of("results/local/checks");
        Files.createDirectories(root);
        String[] names = {"spmf-example", "singleton-final-utility", "expired-singleton",
                "triple-only-high", "counter-zero", "reinduction"};
        List<List<String>> inputs = List.of(
                Files.readAllLines(Path.of("datasets/DB_Utility.txt")),
                List.of("1:10:10", "1:5:5"),
                List.of("1:10:10", "2:1:1", "2:1:1"),
                List.of("1 2 3:12:4 4 4"),
                List.of("1:10:10", "2:1:1"),
                List.of("1:5:5", "2:1:1", "2:1:1", "1:7:7"));
        long[] thresholds = {30, 10, 10, 10, 10, 10};
        int[] windows = {10, 10, 1, 10, 1, 1};
        int failures = 0;
        StringBuilder report = new StringBuilder(
                "Oracle: full-history utility; every item satisfies RC >= 0 at final transaction.\n");
        for (int i = 0; i < names.length; i++) {
            Path input = root.resolve(names[i] + ".txt");
            Path output = root.resolve(names[i] + ".out");
            Files.write(input, inputs.get(i));
            AlgoScentedUtilityMiner sum = new AlgoScentedUtilityMiner();
            sum.runAlgorithm(input.toString(), output.toString(), thresholds[i], windows[i]);
            if (sum.endTimestamp == 0) throw new IllegalStateException("SUM did not finish: " + names[i]);
            Map<String, Long> expected = oracle(inputs.get(i), thresholds[i], windows[i]);
            Map<String, Long> actual = readOutput(output);
            boolean pass = actual.equals(expected);
            if (!pass) failures++;
            report.append(pass ? "PASS " : "FAIL ").append(names[i])
                    .append("\n  expected: ").append(expected)
                    .append("\n  actual:   ").append(actual).append('\n');
        }
        report.append("Cases: ").append(names.length).append(", mismatches: ").append(failures).append('\n');
        Files.writeString(root.resolve("report.txt"), report);
        System.out.print(report);
        if (failures > 0) System.exit(1);
    }
}

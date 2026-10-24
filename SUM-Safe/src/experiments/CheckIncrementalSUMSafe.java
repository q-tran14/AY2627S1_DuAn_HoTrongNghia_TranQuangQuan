package experiments;

import algorithms.sum.AlgoSUMSafe;
import java.nio.file.*;
import java.util.*;

/** Oracle recomputes full prefixes; the miner reads only a new batch, then inputs are deleted. */
public final class CheckIncrementalSUMSafe {
    private static final Path ROOT = Path.of("results/local/incremental-checks");
    private static long snapshots;

    private static long minimum(List<String> lines) {
        Map<Integer, Long> total = new HashMap<>();
        for (String line : lines) {
            if (line.isBlank() || "#%@".indexOf(line.charAt(0)) >= 0) continue;
            String[] parts = line.split(":"), items = parts[0].trim().split("\\s+"), values = parts[2].trim().split("\\s+");
            for (int i = 0; i < items.length; i++) {
                int item = Integer.parseInt(items[i]);
                total.put(item, total.getOrDefault(item, 0L) + Long.parseLong(values[i]));
            }
        }
        return total.values().stream().mapToLong(x -> x).min().orElse(0);
    }

    private static void sequence(List<List<String>> batches, long initial, int window) throws Exception {
        for (AlgoSUMSafe.ThresholdPolicy policy : AlgoSUMSafe.ThresholdPolicy.values()) {
            for (boolean pruning : new boolean[]{false, true}) {
                AlgoSUMSafe miner = new AlgoSUMSafe();
                miner.initialize(initial, window, pruning, policy);
                List<String> all = new ArrayList<>();
                long expectedThreshold = initial;
                int nonempty = 0, total = 0;
                for (List<String> batch : batches) {
                    long size = batch.stream().filter(s -> !s.isBlank() && "#%@".indexOf(s.charAt(0)) < 0).count();
                    if (size > 0 && nonempty > 0 && policy == AlgoSUMSafe.ThresholdPolicy.PAPER_MIN_ALL_SINGLETONS)
                        expectedThreshold += minimum(all);
                    Path input = ROOT.resolve("new-batch.txt");
                    Files.write(input, batch);
                    if (miner.appendBatch(input.toString()) != size) throw new AssertionError("Append count");
                    Files.delete(input); // Future mining MUST NOT reopen any previous input.
                    if (size > 0) nonempty++;
                    total += size;
                    all.addAll(batch);
                    if (miner.getCurrentMinUtility() != expectedThreshold || miner.transactionCount != total
                            || miner.getBatchCount() != nonempty) throw new AssertionError("State / threshold mismatch");
                    Path output = ROOT.resolve("incremental.txt");
                    miner.mine(output.toString());
                    Map<String, Long> expected = CheckSumCorrectness.oracle(all, expectedThreshold, window);
                    if (!expected.equals(CheckSumCorrectness.readOutput(output)))
                        throw new AssertionError("Incremental output mismatch, policy=" + policy + " pruning=" + pruning + " input=" + all);
                    if (miner.huiCount != expected.size()) throw new AssertionError("HUI count");
                    // Cold-start result using exactly the same threshold, tested separately from state updates.
                    Files.write(ROOT.resolve("prefix.txt"), all);
                    AlgoSUMSafe cold = new AlgoSUMSafe();
                    cold.runAlgorithm(ROOT.resolve("prefix.txt").toString(), ROOT.resolve("cold.txt").toString(),
                            expectedThreshold, window, pruning);
                    if (!expected.equals(CheckSumCorrectness.readOutput(ROOT.resolve("cold.txt"))))
                        throw new AssertionError("Cold result mismatch");
                    miner.mine(ROOT.resolve("repeat.txt").toString());
                    if (miner.getCurrentMinUtility() != expectedThreshold
                            || !expected.equals(CheckSumCorrectness.readOutput(ROOT.resolve("repeat.txt"))))
                        throw new AssertionError("Repeated mine must not advance threshold");
                    snapshots++;
                }
            }
        }
    }

    private static void failedUpdateChecks() throws Exception {
        AlgoSUMSafe miner = new AlgoSUMSafe();
        miner.initialize(10, 1, true, AlgoSUMSafe.ThresholdPolicy.PAPER_MIN_ALL_SINGLETONS);
        Path input = ROOT.resolve("atomic.txt"), output = ROOT.resolve("atomic-out.txt");
        Files.writeString(input, "1:10:10\n");
        miner.appendBatch(input.toString());
        miner.mine(output.toString());
        Files.writeString(input, "2:1:1\n3:99:1\n");
        try { miner.appendBatch(input.toString()); throw new AssertionError("Accepted malformed batch"); }
        catch (java.io.IOException expected) { /* prior state must survive */ }
        miner.mine(output.toString());
        if (miner.transactionCount != 1 || miner.getCurrentMinUtility() != 10
                || !CheckSumCorrectness.readOutput(output).equals(Map.of("[1]", 10L)))
            throw new AssertionError("Malformed append changed prior state");
        Files.writeString(input, "2:1:1\n");
        miner.appendBatch(input.toString());
        try { miner.appendBatch(input.toString()); throw new AssertionError("Dynamic update without prior mine"); }
        catch (IllegalStateException expected) { }
        if (miner.transactionCount != 2 || miner.getCurrentMinUtility() != 20) throw new AssertionError("Pending state changed");
        // Output failure must leave pending dynamic update retriable.
        try { miner.mine(input.toString()); throw new AssertionError("Input overwritten"); }
        catch (IllegalArgumentException expected) { }
        miner.mine(output.toString());
        miner.initialize(1, 10, true, AlgoSUMSafe.ThresholdPolicy.PAPER_MIN_ALL_SINGLETONS);
        Files.writeString(input, "1:9223372036854775807:9223372036854775807\n");
        miner.appendBatch(input.toString());
        miner.mine(output.toString());
        Files.writeString(input, "2:1:1\n");
        try { miner.appendBatch(input.toString()); throw new AssertionError("Threshold overflow accepted"); }
        catch (java.io.IOException expected) { }
        if (miner.transactionCount != 1 || miner.getCurrentMinUtility() != 1) throw new AssertionError("Overflow changed state");
        // TWU/total overflow while merging with existing singleton, in fixed mode.
        miner.initialize(1, 10, true, AlgoSUMSafe.ThresholdPolicy.FIXED);
        Files.writeString(input, "1:9223372036854775807:9223372036854775807\n");
        miner.appendBatch(input.toString());
        Files.writeString(input, "1:1:1\n");
        try { miner.appendBatch(input.toString()); throw new AssertionError("History overflow accepted"); }
        catch (java.io.IOException expected) { }
        if (miner.transactionCount != 1) throw new AssertionError("Overflow appended partial state");
        miner.initialize(10, 1, true, AlgoSUMSafe.ThresholdPolicy.FIXED);
        if (miner.transactionCount != 0 || miner.getMinimumSingletonUtility().isPresent()) throw new AssertionError("Reset failed");
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(ROOT);
        snapshots = 0;
        sequence(List.of(List.of("1 2 3:12:4 4 4"), List.of("1 2 3:6:2 2 2"),
                List.of("1 2 3 4:16:2 2 2 10")), 10, 1);
        sequence(List.of(List.of("1:10:10"), List.of("2:1:1"), List.of("2:1:1"), List.of("1:7:7")), 10, 1);
        sequence(List.of(List.of(), List.of("@metadata", "#empty"), List.of("1 2:0:0 0"), List.of(), List.of("1:5:5")), 1, 0);
        sequence(List.of(List.of("1:1:1", "2:50:50"), List.of("2:50:50")), 10, 0);
        Random random = new Random(20261023);
        for (int n = 0; n < 500; n++) {
            List<List<String>> batches = new ArrayList<>();
            for (int b = 0, size = 2 + random.nextInt(4); b < size; b++) {
                List<String> batch = new ArrayList<>();
                for (int t = 0, rows = 1 + random.nextInt(3); t < rows; t++) {
                    List<Integer> items = new ArrayList<>();
                    for (int i = 1; i <= 7; i++) if (random.nextBoolean()) items.add(i);
                    if (items.isEmpty()) items.add(1);
                    Collections.shuffle(items, random);
                    StringJoiner ids = new StringJoiner(" "), values = new StringJoiner(" ");
                    int sum = 0;
                    for (int item : items) {
                        int value = random.nextInt(16);
                        ids.add("" + item); values.add("" + value); sum += value;
                    }
                    batch.add(ids + ":" + sum + ":" + values);
                }
                batches.add(batch);
            }
            sequence(batches, 1 + random.nextInt(100), random.nextInt(12));
        }
        failedUpdateChecks();
        String report = "PASS: " + snapshots + " retained-state snapshots across fixed/paper and RU off/on.\n"
                + "500 random multi-batch sequences + 4 fixed sequences; seed=20261023.\n"
                + "Every snapshot matches exhaustive oracle AND cold-start mining at the same threshold.\n"
                + "Batch input deleted after append; mine succeeds without reopening prior input.\n"
                + "Threshold checked from independent cumulative singleton totals.\n"
                + "Empty updates, expiry/reinduction, new items, repeat mine, malformed append, pending update, overflow and reset checked.\n"
                + "In-memory state within one Java process; mining projections are rebuilt each query.\n";
        Files.writeString(ROOT.resolve("report.txt"), report);
        System.out.print(report);
    }
}

package algorithms.sum;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Experimental SUM-style vertical miner, independently implemented.
 * Keeps TID -> utility maps and BitSet intersections; replaces the original
 * traversal with canonical DFS and a remaining-utility upper bound.
 * Semantics: nonnegative full-history utility, every item active at final TID
 * (RC >= 0). Retains singleton histories across appendBatch calls in one object.
 * Mining projections / joined maps are rebuilt at each query, never cached stale.
 */
public final class AlgoSUMSafe {
    public long huiCount, joinAttempts, visitedNodes, prunedBranches;
    public long loadNanos, prepareNanos, mineNanos;
    public int transactionCount, activeItemCount;

    public enum ThresholdPolicy { FIXED, PAPER_MIN_ALL_SINGLETONS }

    private static final class History {
        final Map<Integer, Long> utilities = new HashMap<>();
        long total, twu;
        int lastTid;
    }

    private final Map<Integer, History> histories = new TreeMap<>();
    private final Set<Path> inputs = new HashSet<>();
    private int maxR, batchCount, lastBatchSize;
    private boolean initialized, pendingBatch;
    private ThresholdPolicy policy;
    private Long previousMinimum;

    /** Start a fresh in-memory session. This explicitly clears prior history. */
    public void initialize(long minUtility, int maxReinduction, boolean useRemainingUtility,
                           ThresholdPolicy thresholdPolicy) {
        if (minUtility <= 0 || maxReinduction < 0 || thresholdPolicy == null)
            throw new IllegalArgumentException("Invalid threshold, maxR or policy");
        histories.clear();
        inputs.clear();
        threshold = minUtility;
        maxR = maxReinduction;
        pruning = useRemainingUtility;
        policy = thresholdPolicy;
        previousMinimum = null;
        pendingBatch = false;
        batchCount = lastBatchSize = transactionCount = activeItemCount = 0;
        huiCount = joinAttempts = visitedNodes = prunedBranches = 0;
        loadNanos = prepareNanos = mineNanos = 0;
        order = null;
        singletons = null;
        initialized = true;
    }

    private void requireSession() {
        if (!initialized) throw new IllegalStateException("Call initialize before appendBatch or mine");
    }

    public long getCurrentMinUtility() { requireSession(); return threshold; }
    public int getBatchCount() { return batchCount; }
    public int getLastBatchSize() { return lastBatchSize; }
    /** Includes inactive / TWU-filtered items; histories are kept for reinduction. */
    public OptionalLong getMinimumSingletonUtility() {
        return histories.values().stream().mapToLong(h -> h.total).min();
    }

    /**
     * Read ONLY the new file. Validate and check arithmetic before committing.
     * All item histories (even expired/unpromising) are retained.
     * Empty batches are no-ops. Dynamic mode requires a completed mine per update.
     */
    public int appendBatch(String input) throws IOException {
        requireSession();
        long start = System.nanoTime();
        Map<Integer, History> delta = new TreeMap<>();
        int newCount = 0;
        Path path = Path.of(input).toAbsolutePath().normalize();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty() || "#%@".indexOf(line.charAt(0)) >= 0) continue;
                try {
                    String[] fields = line.split(":", -1);
                    if (fields.length != 3) throw new IllegalArgumentException("Expected three fields");
                    String[] items = fields[0].trim().split("\\s+");
                    String[] values = fields[2].trim().split("\\s+");
                    if (items.length != values.length) throw new IllegalArgumentException("Item/utility count mismatch");
                    long declared = Long.parseLong(fields[1].trim()), sum = 0;
                    Map<Integer, Long> row = new HashMap<>();
                    for (int i = 0; i < items.length; i++) {
                        int item = Integer.parseInt(items[i]);
                        long value = Long.parseLong(values[i]);
                        if (item <= 0 || value < 0 || row.put(item, value) != null)
                            throw new IllegalArgumentException("Require distinct positive IDs and nonnegative utility");
                        sum = Math.addExact(sum, value);
                    }
                    if (sum != declared) throw new IllegalArgumentException("TU differs from sum of utilities");
                    newCount = Math.addExact(newCount, 1);
                    int tid = Math.addExact(transactionCount, newCount);
                    for (Map.Entry<Integer, Long> item : row.entrySet()) {
                        History h = delta.computeIfAbsent(item.getKey(), key -> new History());
                        h.utilities.put(tid, item.getValue());
                        h.total = Math.addExact(h.total, item.getValue());
                        h.twu = Math.addExact(h.twu, sum);
                        h.lastTid = tid;
                    }
                } catch (IllegalArgumentException | ArithmeticException e) {
                    throw new IOException("Invalid data at line " + lineNumber + ": " + e.getMessage(), e);
                }
            }
        }
        if (newCount == 0) { lastBatchSize = 0; loadNanos = System.nanoTime() - start; return 0; }
        if (policy == ThresholdPolicy.PAPER_MIN_ALL_SINGLETONS && pendingBatch)
            throw new IllegalStateException("Dynamic policy: mine the pending batch before appending another");
        long nextThreshold = threshold;
        try {
            if (policy == ThresholdPolicy.PAPER_MIN_ALL_SINGLETONS && previousMinimum != null)
                nextThreshold = Math.addExact(threshold, previousMinimum);
            for (Map.Entry<Integer, History> item : delta.entrySet()) {
                History old = histories.get(item.getKey());
                if (old != null) {
                    Math.addExact(old.total, item.getValue().total);
                    Math.addExact(old.twu, item.getValue().twu);
                }
            }
        } catch (ArithmeticException e) {
            throw new IOException("Update would overflow; state unchanged", e);
        }
        // Commit only after the complete batch and all additions are validated.
        for (Map.Entry<Integer, History> item : delta.entrySet()) {
            History old = histories.get(item.getKey()), add = item.getValue();
            if (old == null) histories.put(item.getKey(), add);
            else {
                old.utilities.putAll(add.utilities);
                old.total += add.total;
                old.twu += add.twu;
                old.lastTid = add.lastTid;
            }
        }
        transactionCount += newCount;
        threshold = nextThreshold;
        lastBatchSize = newCount;
        batchCount++;
        pendingBatch = true;
        inputs.add(path);
        loadNanos = System.nanoTime() - start;
        return newCount;
    }

    private static final class Vertical {
        final BitSet tids = new BitSet();
        final Map<Integer, Long> utility = new HashMap<>();
        final Map<Integer, Long> remaining = new HashMap<>();
        long total, bound;

        void add(int tid, long value, long rest) {
            tids.set(tid);
            utility.put(tid, value);
            remaining.put(tid, rest);
            total = Math.addExact(total, value);
            bound = Math.addExact(bound, Math.addExact(value, rest));
        }
    }

    private List<Integer> order;
    private List<Vertical> singletons;
    private BufferedWriter writer;
    private long threshold;
    private boolean pruning;

    public void runAlgorithm(String input, String output, long minUtility, int maxR,
                             boolean useRemainingUtility) throws IOException {
        if (minUtility <= 0 || maxR < 0) throw new IllegalArgumentException("Invalid threshold or maxR");
        Path in = Path.of(input).toAbsolutePath().normalize();
        Path out = Path.of(output).toAbsolutePath().normalize();
        if (in.equals(out) || (Files.exists(out) && Files.isSameFile(in, out)))
            throw new IllegalArgumentException("Input and output must differ");
        initialize(minUtility, maxR, useRemainingUtility, ThresholdPolicy.FIXED);
        appendBatch(input);
        mine(output);
    }

    /** Mine a fresh output snapshot without reopening any previously appended input. */
    public void mine(String output) throws IOException {
        requireSession();
        Path out = Path.of(output).toAbsolutePath().normalize();
        for (Path input : inputs) {
            if (input.equals(out) || (Files.exists(input) && Files.exists(out) && Files.isSameFile(input, out)))
                throw new IllegalArgumentException("Output must differ from all appended inputs");
        }
        huiCount = joinAttempts = visitedNodes = prunedBranches = 0;
        long start = System.nanoTime();
        order = new ArrayList<>();
        for (int item : histories.keySet()) {
            History h = histories.get(item);
            if (transactionCount - h.lastTid <= maxR && h.twu >= threshold)
                order.add(item);
        }
        activeItemCount = order.size();
        singletons = new ArrayList<>();
        for (int item : order) singletons.add(new Vertical());
        Map<Integer, Long> suffix = new HashMap<>();
        for (int j = order.size() - 1; j >= 0; j--) {
            for (Map.Entry<Integer, Long> value : histories.get(order.get(j)).utilities.entrySet()) {
                int tid = value.getKey();
                long rest = suffix.getOrDefault(tid, 0L);
                singletons.get(j).add(tid, value.getValue(), rest);
                suffix.put(tid, Math.addExact(rest, value.getValue()));
            }
        }
        prepareNanos = System.nanoTime() - start;
        Files.createDirectories(out.getParent());
        start = System.nanoTime();
        try (BufferedWriter outputWriter = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            writer = outputWriter;
            explore(new ArrayList<>(), null, 0);
        } finally {
            writer = null;
            mineNanos = System.nanoTime() - start;
        }
        OptionalLong minimum = getMinimumSingletonUtility();
        previousMinimum = minimum.isPresent() ? minimum.getAsLong() : null;
        pendingBatch = false;
    }

    private void explore(List<Integer> prefix, Vertical parent, int start) throws IOException {
        for (int j = start; j < order.size(); j++) {
            if (Thread.currentThread().isInterrupted()) throw new IOException("Mining interrupted");
            Vertical child = singletons.get(j);
            if (parent != null) {
                joinAttempts++;
                BitSet common = (BitSet) parent.tids.clone();
                common.and(child.tids);
                if (common.isEmpty()) continue;
                Vertical joined = new Vertical();
                for (int tid = common.nextSetBit(0); tid >= 0; tid = common.nextSetBit(tid + 1))
                    joined.add(tid, Math.addExact(parent.utility.get(tid), child.utility.get(tid)),
                            child.remaining.get(tid));
                child = joined;
            }
            visitedNodes++;
            prefix.add(order.get(j));
            // Output is independent of the decision to extend this prefix.
            if (child.total >= threshold) {
                for (int item : prefix) writer.write(item + " ");
                writer.write("#UTIL: " + child.total);
                writer.newLine();
                huiCount++;
            }
            if (j + 1 < order.size()) {
                if (!pruning || child.bound >= threshold) explore(prefix, child, j + 1);
                else prunedBranches++;
            }
            prefix.remove(prefix.size() - 1);
        }
    }
}

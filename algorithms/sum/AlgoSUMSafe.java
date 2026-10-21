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
 * (RC >= 0). This is a batch mining kernel, not a persistent incremental API.
 */
public final class AlgoSUMSafe {
    public long huiCount, joinAttempts, visitedNodes, prunedBranches;
    public long loadNanos, prepareNanos, mineNanos;
    public int transactionCount, activeItemCount;

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
        huiCount = joinAttempts = visitedNodes = prunedBranches = 0;
        transactionCount = activeItemCount = 0;
        loadNanos = prepareNanos = mineNanos = 0;
        threshold = minUtility;
        pruning = useRemainingUtility;
        long start = System.nanoTime();
        List<Map<Integer, Long>> rows = new ArrayList<>();
        Map<Integer, Long> twu = new TreeMap<>();
        Map<Integer, Integer> lastSeen = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(in, StandardCharsets.UTF_8)) {
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
                    rows.add(row);
                    for (int item : row.keySet()) {
                        twu.put(item, Math.addExact(twu.getOrDefault(item, 0L), sum));
                        lastSeen.put(item, rows.size());
                    }
                } catch (IllegalArgumentException | ArithmeticException e) {
                    throw new IOException("Invalid data at line " + lineNumber + ": " + e.getMessage(), e);
                }
            }
        }
        transactionCount = rows.size();
        loadNanos = System.nanoTime() - start;
        start = System.nanoTime();
        order = new ArrayList<>();
        for (int item : twu.keySet()) {
            // Same RC boundary as the original source. Lazy counter representation.
            if (transactionCount - lastSeen.get(item) <= maxR && twu.get(item) >= threshold)
                order.add(item);
        }
        activeItemCount = order.size();
        singletons = new ArrayList<>();
        for (int item : order) singletons.add(new Vertical());
        for (int tid = 0; tid < rows.size(); tid++) {
            Map<Integer, Long> row = rows.get(tid);
            long suffix = 0;
            for (int j = order.size() - 1; j >= 0; j--) {
                Long value = row.get(order.get(j));
                if (value != null) {
                    singletons.get(j).add(tid, value, suffix);
                    suffix = Math.addExact(suffix, value);
                }
            }
        }
        rows.clear();
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

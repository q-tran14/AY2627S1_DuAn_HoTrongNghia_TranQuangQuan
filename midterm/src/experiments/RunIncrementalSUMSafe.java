package experiments;

import algorithms.sum.AlgoSUMSafe;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.*;
import java.util.*;

/** All batches in a command run in one retained in-memory mining session. */
public final class RunIncrementalSUMSafe {
    /** Command syntax; the pruning flag must precede the ordered batch paths. */
    private static final String USAGE =
            "incremental fixed|paper outputDir initialMin maxR [--pruning true|false] batch1 [batch2 ...]";

    /**
     * Quote a value as a single CSV field.
     * @param value field value, possibly containing quotation marks
     * @return value enclosed in quotes with internal quotes escaped
     */
    private static String csv(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }

    /**
     * Mine an ordered sequence of inserted batches using one retained session.
     * @param args policy, output directory, initial threshold, maxR, optional
     *             pruning flag, and one or more paths containing new transactions
     * @throws Exception if arguments, inputs, or output operations fail
     */
    public static void main(String[] args) throws Exception {
        // Step 1: parse configuration without changing the backward-compatible defaults.
        if (args.length < 5) throw new IllegalArgumentException(USAGE);
        AlgoSUMSafe.ThresholdPolicy policy;
        if (args[0].equals("fixed")) policy = AlgoSUMSafe.ThresholdPolicy.FIXED;
        else if (args[0].equals("paper")) policy = AlgoSUMSafe.ThresholdPolicy.PAPER_MIN_ALL_SINGLETONS;
        else throw new IllegalArgumentException("Policy must be fixed or paper");
        boolean pruning = true;
        int firstBatch = 4;
        if (args[4].equals("--pruning")) {
            if (args.length < 7) throw new IllegalArgumentException(USAGE);
            if (!args[5].equals("true") && !args[5].equals("false"))
                throw new IllegalArgumentException("Pruning must be true or false");
            pruning = Boolean.parseBoolean(args[5]);
            firstBatch = 6;
        }
        Path root = Path.of(args[1]).toAbsolutePath().normalize();
        // Step 2: validate every path before creating output files.
        for (int i = firstBatch; i < args.length; i++) {
            if (args[i].startsWith("--"))
                throw new IllegalArgumentException("Unknown or misplaced option: " + args[i] + ". " + USAGE);
            Path input = Path.of(args[i]).toAbsolutePath().normalize();
            if (input.startsWith(root)) throw new IllegalArgumentException("Output directory must not contain batch inputs");
            if (!Files.isRegularFile(input)) throw new IOException("Missing batch: " + input);
        }
        // Step 3: initialize once; each later file appends only its new transactions.
        AlgoSUMSafe miner = new AlgoSUMSafe();
        miner.initialize(Long.parseLong(args[2]), Integer.parseInt(args[3]), pruning, policy);
        Files.createDirectories(root);
        System.out.println("Incremental SUM-Safe, policy=" + args[0] + "; RU pruning=" + pruning
                + "; one retained object, global TIDs");
        // Step 4: write a complete HUI snapshot and configuration row after each append.
        try (BufferedWriter report = Files.newBufferedWriter(root.resolve("batches.csv"), StandardCharsets.UTF_8)) {
            report.write("batch,input,new_transactions,total_transactions,minUtility,minimum_singleton_utility,HUI,joins,update_ms,prepare_ms,mine_ms,pruning\n");
            for (int i = firstBatch; i < args.length; i++) {
                int number = i - firstBatch + 1;
                int added = miner.appendBatch(args[i]);
                Path output = root.resolve(String.format(Locale.ROOT, "batch-%02d.txt", number));
                miner.mine(output.toString());
                OptionalLong minimum = miner.getMinimumSingletonUtility();
                String min = minimum.isPresent() ? Long.toString(minimum.getAsLong()) : "";
                String row = String.format(Locale.ROOT, "%d,%s,%d,%d,%d,%s,%d,%d,%.3f,%.3f,%.3f,%b%n",
                        number, csv(args[i]), added, miner.transactionCount, miner.getCurrentMinUtility(), min,
                        miner.huiCount, miner.joinAttempts, miner.loadNanos / 1e6, miner.prepareNanos / 1e6,
                        miner.mineNanos / 1e6, pruning);
                report.write(row);
                report.flush();
                System.out.printf(Locale.ROOT, "Batch %d: new=%d total=%d minUtility=%d minSingleton=%s HUI=%d -> %s%n",
                        number, added, miner.transactionCount, miner.getCurrentMinUtility(), min, miner.huiCount, output);
            }
        }
    }
}

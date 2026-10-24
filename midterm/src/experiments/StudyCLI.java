package experiments;

import algorithms.sum.AlgoSUMSafe;
import ca.pfv.spmf.algorithms.frequentpatterns.rminer_sminer.AlgoScentedUtilityMiner;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Portable entry point shared by CMD and PowerShell through run.cmd. */
public final class StudyCLI {
    /**
     * Run the command and report configuration/input errors without a stack trace.
     * @param args command name followed by command-specific arguments
     * @throws Exception if an unexpected execution failure occurs
     */
    public static void main(String[] args) throws Exception {
        try {
            execute(args);
        } catch (IllegalArgumentException | IOException error) {
            System.err.println("ERROR: " + error.getMessage());
            System.err.println("Use run.cmd help for command syntax.");
            System.exit(2);
        }
    }

    /**
     * Dispatch a CLI invocation to the corresponding experiment client.
     * @param args command and its parameters; empty arguments display help
     * @throws Exception if command validation or execution fails
     */
    private static void execute(String[] args) throws Exception {
        String mode = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        if (mode.equals("--help") || mode.equals("-h")) mode = "help";
        switch (mode) {
            case "demo":
                if (args.length > 2) throw new IllegalArgumentException("demo [minUtility]");
                demo(args.length == 2 ? Long.parseLong(args[1]) : 10);
                break;
            case "check":
                requireLength(args, 1);
                CheckSUMSafe.main(new String[0]);
                CheckIncrementalSUMSafe.main(new String[0]);
                break;
            case "original-check":
                requireLength(args, 1);
                CheckSumCorrectness.main(new String[0]);
                break;
            case "incremental":
                RunIncrementalSUMSafe.main(Arrays.copyOfRange(args, 1, args.length));
                break;
            case "incremental-check":
                requireLength(args, 1);
                CheckIncrementalSUMSafe.main(new String[0]);
                break;
            case "incremental-demo":
                requireLength(args, 1);
                RunIncrementalSUMSafe.main(new String[]{"paper", "results/local/incremental-demo", "10", "1",
                        "datasets/incremental/batch-01.txt", "datasets/incremental/batch-02.txt", "datasets/incremental/batch-03.txt"});
                List<String> cumulative = new ArrayList<>();
                long[] demoThresholds = {10, 14, 20};
                for (int number = 1; number <= 3; number++) {
                    cumulative.addAll(Files.readAllLines(Path.of(String.format(Locale.ROOT, "datasets/incremental/batch-%02d.txt", number))));
                    Map<String, Long> actualBatch = CheckSumCorrectness.readOutput(
                            Path.of(String.format(Locale.ROOT, "results/local/incremental-demo/batch-%02d.txt", number)));
                    if (!actualBatch.equals(CheckSumCorrectness.oracle(cumulative, demoThresholds[number - 1], 1)))
                        throw new AssertionError("Incremental demo mismatch at batch " + number);
                    System.out.println("Batch " + number + " output: " + actualBatch + " | oracle PASS");
                }
                break;
            case "run":
                if (args.length != 6 && args.length != 7)
                    throw new IllegalArgumentException("run safe|original input output minUtility maxR [true|false]");
                String version = args[1].toLowerCase(Locale.ROOT);
                if (!version.equals("safe") && !version.equals("original"))
                    throw new IllegalArgumentException("Choose safe or original");
                if (version.equals("original") && args.length == 7)
                    throw new IllegalArgumentException("Pruning option belongs to safe only");
                if (args.length == 7 && !args[6].equals("true") && !args[6].equals("false"))
                    throw new IllegalArgumentException("Pruning must be true or false");
                long threshold = Long.parseLong(args[4]);
                int window = Integer.parseInt(args[5]);
                validate(args[2], args[3], threshold, window);
                if (version.equals("safe")) {
                    RunSUMSafe.main(new String[]{args[2], args[3], args[4], args[5], args.length == 7 ? args[6] : "true"});
                } else {
                    RunOfficialSum.main(new String[]{args[2], args[3], args[4], args[5]});
                }
                break;
            case "help":
                if (args.length > 1) throw new IllegalArgumentException("help takes no parameters");
                System.out.println("run.cmd demo [minUtility]              Compare official SUM / SUM-Safe / oracle");
                System.out.println("run.cmd check                          Check batch AND retained-state SUM-Safe");
                System.out.println("run.cmd original-check                 Reproduce known mismatches (exit 1)");
                System.out.println("run.cmd pack                           Create a clean submission ZIP");
                System.out.println("run.cmd incremental-demo               Three retained batches with dynamic thresholds");
                System.out.println("run.cmd incremental-check              Verify retained state and threshold schedule");
                System.out.println("run.cmd incremental fixed|paper outputDir initialMin maxR [--pruning true|false] batch1 [batch2 ...]");
                System.out.println("run.cmd run safe input output minUtility maxR [true|false]");
                System.out.println("run.cmd run original input output minUtility maxR");
                System.out.println("Paths are relative to the midterm folder, unless absolute.");
                System.out.println("minUtility/initialMin: positive integer; maxR: nonnegative integer.");
                System.out.println("RU pruning defaults to true. Each incremental file contains NEW transactions only.");
                System.out.println("See README.md for parameters and examples; --help and -h also display this help.");
                break;
            case "pack":
                requireLength(args, 1);
                PackageSubmission.main(new String[0]);
                break;
            default:
                throw new IllegalArgumentException("Unknown command: " + mode + ". Use run.cmd help");
        }
    }

    /**
     * Check that a fixed-arity command received exactly the expected arguments.
     * @param args actual arguments including the command name
     * @param length required argument count
     */
    private static void requireLength(String[] args, int length) {
        if (args.length != length) throw new IllegalArgumentException("Unexpected arguments");
    }

    /**
     * Validate single-run parameters and keep output separate from input.
     * @param input existing input file
     * @param output destination for the mined itemsets
     * @param threshold positive minimum utility
     * @param window nonnegative maximum reinduction count
     * @throws Exception if the configuration or filesystem operation is invalid
     */
    private static void validate(String input, String output, long threshold, int window) throws Exception {
        if (threshold <= 0 || window < 0) throw new IllegalArgumentException("Invalid threshold or maxR");
        Path in = Path.of(input).toAbsolutePath().normalize();
        Path out = Path.of(output).toAbsolutePath().normalize();
        if (!Files.isRegularFile(in)) throw new IllegalArgumentException("Input not found: " + in);
        if (in.equals(out) || (Files.exists(out) && Files.isSameFile(in, out)))
            throw new IllegalArgumentException("Input and output must differ");
        Files.createDirectories(out.getParent());
    }

    /**
     * Compare the preserved SUM baseline and SUM-Safe with exhaustive enumeration.
     * @param threshold positive utility threshold for the three-item example
     * @throws Exception if the input cannot be read or SUM-Safe disagrees with the oracle
     */
    private static void demo(long threshold) throws Exception {
        Path input = Path.of("datasets/counterexamples/triple-only-high.txt");
        Path original = Path.of("results/local/demo/original.txt");
        Path safe = Path.of("results/local/demo/safe.txt");
        validate(input.toString(), original.toString(), threshold, 10);
        List<String> lines = Files.readAllLines(input);
        Map<String, Long> expected = CheckSumCorrectness.oracle(lines, threshold, 10);
        System.out.println("Input: " + input + " | minUtility=" + threshold + " | maxR=10");
        for (String line : lines) System.out.println(line);
        System.out.println("Expected (independent exhaustive oracle): " + expected);
        System.out.println("\nOFFICIAL SPMF SUM (unmodified mining source):");
        AlgoScentedUtilityMiner baseline = new AlgoScentedUtilityMiner();
        baseline.runAlgorithm(input.toString(), original.toString(), threshold, 10);
        if (baseline.endTimestamp == 0) throw new IllegalStateException("Official SUM incomplete");
        baseline.printStats();
        Map<String, Long> actual = CheckSumCorrectness.readOutput(original);
        System.out.println("Actual: " + actual);
        System.out.println(expected.equals(actual) ? "MATCH for this input" : "MISMATCH: original SUM differs from oracle");
        System.out.println("\nSUM-SAFE (experimental correction):");
        AlgoSUMSafe corrected = new AlgoSUMSafe();
        corrected.runAlgorithm(input.toString(), safe.toString(), threshold, 10, true);
        Map<String, Long> fixed = CheckSumCorrectness.readOutput(safe);
        System.out.println("Actual: " + fixed);
        if (!expected.equals(fixed)) throw new AssertionError("SUM-Safe mismatch");
        System.out.println("PASS: SUM-Safe matches oracle (itemsets AND utility)");
        String report = "Input: " + String.join("; ", lines) + "\nminUtility=" + threshold + ", maxR=10\n"
                + "Expected: " + expected + "\nOfficial SUM: " + actual + "\nSUM-Safe: " + fixed
                + "\nSUM-Safe matches: " + expected.equals(fixed) + "\n";
        Files.writeString(Path.of("results/local/demo/report.txt"), report);
        System.out.println("Outputs: " + original + " | " + safe);
    }
}

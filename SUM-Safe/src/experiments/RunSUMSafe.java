package experiments;

import algorithms.sum.AlgoSUMSafe;
import java.util.Locale;

public class RunSUMSafe {
    public static void main(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("input output minUtility maxR pruning");
        AlgoSUMSafe algorithm = new AlgoSUMSafe();
        algorithm.runAlgorithm(args[0], args[1], Long.parseLong(args[2]),
                Integer.parseInt(args[3]), Boolean.parseBoolean(args[4]));
        System.out.println("SUM-Safe (experimental), RU pruning=" + args[4]);
        System.out.printf(Locale.ROOT,
                "HUI=%d joins=%d nodes=%d pruned=%d load_ms=%.3f prepare_ms=%.3f mine_ms=%.3f%n",
                algorithm.huiCount, algorithm.joinAttempts, algorithm.visitedNodes, algorithm.prunedBranches,
                algorithm.loadNanos / 1e6, algorithm.prepareNanos / 1e6, algorithm.mineNanos / 1e6);
    }
}

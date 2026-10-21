package experiments;

import ca.pfv.spmf.algorithms.frequentpatterns.rminer_sminer.AlgoScentedUtilityMiner;

/** Thin launcher for the unmodified official source used by the audit. */
public class RunOfficialSum {
    public static void main(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("Expected input output minUtility maxR");
        AlgoScentedUtilityMiner algorithm = new AlgoScentedUtilityMiner();
        algorithm.runAlgorithm(args[0], args[1], Long.parseLong(args[2]), Integer.parseInt(args[3]));
        if (algorithm.endTimestamp == 0) throw new IllegalStateException("SUM did not complete");
        algorithm.printStats();
    }
}

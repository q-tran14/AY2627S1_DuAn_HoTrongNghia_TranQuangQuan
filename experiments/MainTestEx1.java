package experiments;

import ca.pfv.spmf.algorithms.frequentpatterns.rminer_sminer.AlgoScentedUtilityMiner;
import java.io.IOException;
import java.util.concurrent.*;

public class MainTestEx1 {

    public static void main(String[] args) {
        
        String input = "datasets/DB_Utility.txt";
        String output = "output.txt";
        int minUtility = 30;
        int maxReinductionCount = 10;
        
        // --- DEADLINE CONFIGURATION (IN SECONDS) ---
        long timeLimitInSeconds = 30; 
        
        if (args.length >= 4) {
            input = args[0];
            output = args[1];
            try {
                minUtility = Integer.parseInt(args[2]);
                maxReinductionCount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                System.out.println("[WARNING] Parameters must be integers.");
            }
        } 
        
        System.out.println("============= ALGORITHM EXECUTION PARAMETERS =============");
        System.out.println("Input file: " + input);
        System.out.println("Output file: " + output);
        System.out.println("Min Utility threshold: " + minUtility);
        System.out.println("Max Reinduction Count: " + maxReinductionCount);
        System.out.println("Deadline: " + timeLimitInSeconds + " seconds");
        System.out.println("==========================================================");
        
        final String finalInput = input;
        final String finalOutput = output;
        final int finalMinUtility = minUtility;
        final int finalMaxReinductionCount = maxReinductionCount;
        
        // Initialize the algorithm outside the thread to retrieve stats if interrupted
        final AlgoScentedUtilityMiner algorithm = new AlgoScentedUtilityMiner();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        // Start timing
        long startTime = System.currentTimeMillis();
        
        Future<?> future = executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    algorithm.runAlgorithm(finalInput, finalOutput, finalMinUtility, finalMaxReinductionCount);
                } catch (IOException e) {
                    System.err.println("[ERROR] An error occurred during execution: " + e.getMessage());
                }
            }
        });
        
        try {
            System.out.println("[INFO] Running algorithm...");
            // Wait for the algorithm to complete within the time limit
            future.get(timeLimitInSeconds, TimeUnit.SECONDS);
            
            // If completed before the deadline
            long endTime = System.currentTimeMillis();
            System.out.println("\n[SUCCESS] Algorithm completed before the deadline!");
            System.out.println(">> Actual execution time: " + (endTime - startTime) + " ms");
            
            // Print standard statistics
            algorithm.printStats();
            
        } catch (TimeoutException e) {
            // If the time limit (Deadline) is exceeded
            long endTime = System.currentTimeMillis();
            System.out.println("\n[TIMEOUT] Algorithm exceeded the time limit (" + timeLimitInSeconds + "s)!");
            System.out.println(">> Execution time before interruption: " + (endTime - startTime) + " ms");
            
            // Cancel the running thread
            future.cancel(true);
            
            System.out.println("\n--- STATISTICS (PARTIAL EXECUTION) ---");
            
            // DÒNG THÊM MỚI: Lấy bộ nhớ tối đa đã ghi nhận được tính đến lúc bị ngắt
            algorithm.totalMemory = ca.pfv.spmf.tools.MemoryLogger.getInstance().getMaxMemory();
            
            // Call printStats() to output results obtained up to the point of interruption
            algorithm.printStats();
            
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("[ERROR] An unexpected error occurred: " + e.getMessage());
            
        } finally {
            // Clean up Executor
            executor.shutdownNow();
            System.out.println("[FINISHED] Program terminated.");
        }
    }
}
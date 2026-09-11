package experiments;

import algorithms.AlgoScentedUtilityMiner;
import java.io.IOException;
import java.util.concurrent.*;

public class MainTestEx1 {

    public static void main(String[] args) {
        String input = "datasets/DB_Utility.txt";
        String output = "output.txt";
        int minUtility = 30;
        int maxReinductionCount = 10;
        long timeLimitInSeconds = 30; // Deadline tính bằng giây

        if (args.length >= 4) {
            input = args[0];
            output = args[1];
            minUtility = Integer.parseInt(args[2]);
            maxReinductionCount = Integer.parseInt(args[3]);
        }

        final AlgoScentedUtilityMiner algorithm = new AlgoScentedUtilityMiner();
        final String finalInput = input;
        final String finalOutput = output;
        final int finalMinUtil = minUtility;
        final int finalMaxR = maxReinductionCount;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        long startTime = System.currentTimeMillis();

        Future<?> future = executor.submit(() -> {
            try {
                algorithm.runAlgorithm(finalInput, finalOutput, finalMinUtil, finalMaxR);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        try {
            // Chờ thuật toán hoàn thành trong khoảng thời gian deadline
            future.get(timeLimitInSeconds, TimeUnit.SECONDS);
            System.out.println("[SUCCESS] Completed within deadline!");
            algorithm.printStats();
        } catch (TimeoutException e) {
            long endTime = System.currentTimeMillis();
            System.out.println("\n[TIMEOUT] Algorithm exceeded the time limit (" + timeLimitInSeconds + "s)!");
            System.out.println(">> Execution time before interruption: " + (endTime - startTime) + " ms");
            
            // 1. Chốt thời gian kết thúc để tránh lỗi âm thời gian
            algorithm.endTimestamp = endTime;

            // 2. ÉP BỘ ĐO TÍNH TOÁN RAM HIỆN TẠI TRƯỚC KHI HỦY LUỒNG
            tools.MemoryLogger.getInstance().checkMemory();
            algorithm.totalMemory = tools.MemoryLogger.getInstance().getMaxMemory();

            // 3. Hủy luồng chạy nền
            future.cancel(true);
            
            System.out.println("\n--- STATISTICS (PARTIAL EXECUTION) ---");
            algorithm.printStats();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            executor.shutdownNow();
        }
    }
}
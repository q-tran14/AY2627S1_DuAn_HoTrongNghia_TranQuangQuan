package experiments;

import algorithms.sum.AlgoScentedUtilityMiner;
import algorithms.ulb.AlgoULBMiner;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import tools.MemoryLogger;

public class MainCompareAllDatasets {

    static class DatasetConfig {
        String key;
        String fileName;
        int[] thresholds;
        long timeLimitInSeconds; // Deadline riêng cho từng dataset

        DatasetConfig(String key, String fileName, int[] thresholds, long timeLimitInSeconds) {
            this.key = key;
            this.fileName = fileName;
            this.thresholds = thresholds;
            this.timeLimitInSeconds = timeLimitInSeconds;
        }
    }

    static class Result {
        long time = 0;
        double memory = 0.0;
        int huiCount = 0;
        String status = "SUCCESS";
    }

    public static void main(String[] args) {
        String outputPath = "results/output.txt";
        String csvResultPath = "results/comparison_results_all.csv";
        int maxReinductionCount = 10; // Window size mặc định theo paper

        // Cấu hình threshold và deadline cụ thể cho từng dataset theo paper (Table 1 & Fig 16)
        DatasetConfig[] datasetConfigs = new DatasetConfig[]{
            new DatasetConfig("foodmart", "foodmart.txt", 
                new int[]{500000, 600000, 700000, 800000, 900000, 1000000}, 10), // 10s deadline
            new DatasetConfig("mushroom", "mushroom.txt", 
                new int[]{400000, 500000, 600000, 700000, 800000, 900000, 1000000}, 30), // 30s deadline
            new DatasetConfig("chess", "chess.txt", 
                new int[]{400000, 500000, 600000, 700000, 800000, 900000, 1000000}, 60), // 60s deadline
            new DatasetConfig("connect", "connect.txt", 
                new int[]{16000000, 17000000, 18000000, 19000000, 20000000}, 180) // 180s deadline
        };

        String[] algorithms = {"SUM", "ULB"};
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try (BufferedWriter csv = new BufferedWriter(new FileWriter(csvResultPath))) {
            csv.write("Dataset,MinUtility,Algorithm,ExecutionTime_ms,Memory_MB,HUICount,Status\n");

            for (int d = 0; d < datasetConfigs.length; d++) {
                DatasetConfig config = datasetConfigs[d];
                String datasetPath = "datasets/" + config.fileName;

                System.out.println("======================================================================");
                System.out.printf(">>> PHIÊN %d/4: DATASET [%s] (Deadline: %ds) <<<\n", 
                    (d + 1), config.key.toUpperCase(), config.timeLimitInSeconds);
                System.out.println("======================================================================");

                for (int minUtil : config.thresholds) {
                    for (String algoName : algorithms) {
                        System.out.println("--------------------------------------------------");
                        System.out.printf("[%s] Dataset: %s | Threshold: %d\n", algoName, config.key, minUtil);

                        Result res = runSingleExperiment(executor, algoName, datasetPath, outputPath, minUtil, maxReinductionCount, config.timeLimitInSeconds);

                        System.out.printf("[DONE] %s -> Time: %d ms | RAM: %.2f MB | HUIs: %d | Status: %s\n",
                                algoName, res.time, res.memory, res.huiCount, res.status);

                        csv.write(String.format("%s,%d,%s,%d,%.2f,%d,%s\n",
                                config.key, minUtil, algoName, res.time, res.memory, res.huiCount, res.status));
                        csv.flush();

                        // Dọn dẹp Garbage Collector giữa các lượt
                        System.gc();
                        Thread.sleep(600);
                    }
                }
            }

            System.out.println("\n[XONG THỰC NGHIỆM] Đang tiến hành tạo các bảng Pivot...");
            generatePivotTables(csvResultPath, datasetConfigs, algorithms);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executor.shutdownNow();
        }
    }

    private static Result runSingleExperiment(ExecutorService executor, String algoName, String input, String output,
                                              int minUtility, int maxR, long timeLimitSeconds) {
        Result result = new Result();
        MemoryLogger.getInstance().reset();

        final AlgoScentedUtilityMiner sumAlgo = "SUM".equalsIgnoreCase(algoName) ? new AlgoScentedUtilityMiner() : null;
        final AlgoULBMiner ulbAlgo = "ULB".equalsIgnoreCase(algoName) ? new AlgoULBMiner() : null;

        Callable<Void> task = () -> {
            if (sumAlgo != null) {
                sumAlgo.runAlgorithm(input, output, minUtility, maxR);
                result.memory = sumAlgo.totalMemory;
                result.huiCount = sumAlgo.huiCount;
            } else if (ulbAlgo != null) {
                ulbAlgo.runAlgorithm(input, output, minUtility);
                result.memory = MemoryLogger.getInstance().getMaxMemory();
                result.huiCount = ulbAlgo.huiCount;
            }
            return null;
        };

        long startTime = System.currentTimeMillis();
        Future<Void> future = executor.submit(task);

        try {
            future.get(timeLimitSeconds, TimeUnit.SECONDS);
            result.time = System.currentTimeMillis() - startTime;
        } catch (TimeoutException e) {
            result.time = System.currentTimeMillis() - startTime;
            result.status = "TIMEOUT";

            // Chốt lại RAM trước khi hủy luồng để không bị = 0.0
            MemoryLogger.getInstance().checkMemory();
            result.memory = MemoryLogger.getInstance().getMaxMemory();
            if (sumAlgo != null) result.huiCount = sumAlgo.huiCount;
            if (ulbAlgo != null) result.huiCount = ulbAlgo.huiCount;

            future.cancel(true);
        } catch (Exception e) {
            result.status = "ERROR";
            result.time = System.currentTimeMillis() - startTime;
        }

        return result;
    }

    // Tự động phân tích CSV thô thành 8 bảng CSV riêng biệt tương ứng Fig. 16 & 17
    private static void generatePivotTables(String csvPath, DatasetConfig[] configs, String[] algorithms) {
        Map<String, Map<Integer, Map<String, Long>>> timeData = new HashMap<>();
        Map<String, Map<Integer, Map<String, Double>>> memData = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath))) {
            String line = reader.readLine(); // Header
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 5) continue;
                String dataset = parts[0];
                int minUtil = Integer.parseInt(parts[1]);
                String algo = parts[2];
                long time = Long.parseLong(parts[3]);
                double mem = Double.parseDouble(parts[4]);

                timeData.computeIfAbsent(dataset, k -> new TreeMap<>())
                        .computeIfAbsent(minUtil, k -> new HashMap<>()).put(algo, time);
                memData.computeIfAbsent(dataset, k -> new TreeMap<>())
                        .computeIfAbsent(minUtil, k -> new HashMap<>()).put(algo, mem);
            }

            for (DatasetConfig config : configs) {
                String ds = config.key;
                
                // Xuất bảng Thời gian (ms)
                String timeFile = "table_time_" + ds + ".csv";
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(timeFile))) {
                    bw.write("MinUtility,SUM,ULB\n");
                    Map<Integer, Map<String, Long>> dsTime = timeData.getOrDefault(ds, Collections.emptyMap());
                    for (int threshold : config.thresholds) {
                        Map<String, Long> algoMap = dsTime.getOrDefault(threshold, Collections.emptyMap());
                        long sumT = algoMap.getOrDefault("SUM", 0L);
                        long ulbT = algoMap.getOrDefault("ULB", 0L);
                        bw.write(threshold + "," + sumT + "," + ulbT + "\n");
                    }
                }

                // Xuất bảng Bộ nhớ (MB)
                String memFile = "table_memory_" + ds + ".csv";
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(memFile))) {
                    bw.write("MinUtility,SUM,ULB\n");
                    Map<Integer, Map<String, Double>> dsMem = memData.getOrDefault(ds, Collections.emptyMap());
                    for (int threshold : config.thresholds) {
                        Map<String, Double> algoMap = dsMem.getOrDefault(threshold, Collections.emptyMap());
                        double sumM = algoMap.getOrDefault("SUM", 0.0);
                        double ulbM = algoMap.getOrDefault("ULB", 0.0);
                        bw.write(String.format("%d,%.2f,%.2f\n", threshold, sumM, ulbM));
                    }
                }
                System.out.printf("=> Đã tạo bảng cho [%s]: %s | %s\n", ds.toUpperCase(), timeFile, memFile);
            }
        } catch (IOException e) {
            System.err.println("[LỖI] Lỗi khi tạo bảng: " + e.getMessage());
        }
    }
}
package algorithms.sum;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Example of how to use the AlgoScentedUtilityMiner algorithm 
 * from the source code.
 * This class demonstrates how to set up and run the
 * high utility itemset mining algorithm using the 
 * AlgoScentedUtilityMiner class.
 */
public class MainTestAlgoScentedUtilityMiner {

    /**
     * Main method to run the AlgoScentedUtilityMiner algorithm.
     * @param args Command line arguments (not used).
     * @throws IOException If there is an error reading or writing files.
     */
    public static void main(String[] args) throws IOException {
        
        // Path to the input file (change the filename as needed)
        if (args.length != 0 && args.length != 4) {
            throw new IllegalArgumentException(
                    "Usage: MainTestAlgoScentedUtilityMiner [input output minUtility maxR]");
        }
        String input = args.length == 4 ? args[0] : "datasets/DB_Utility.txt";
        
        // Path to the output file where results will be written
        String output = args.length == 4 ? args[1] : "results/local/sum-example.txt";
        
        // Uncomment and use the following line if you want to set a different utility threshold
         long minUtility = args.length == 4 ? Long.parseLong(args[2]) : 30;
        
        // Maximum reinduction count (set as needed)
        int maxReinductionCount = args.length == 4 ? Integer.parseInt(args[3]) : 10;
        if (minUtility <= 0 || maxReinductionCount < 0) {
            throw new IllegalArgumentException("minUtility must be positive and maxR nonnegative");
        }
        Path inputPath = Path.of(input).toAbsolutePath().normalize();
        Path outputPath = Path.of(output).toAbsolutePath().normalize();
        if (!Files.isRegularFile(inputPath)) {
            throw new IOException("Input file not found: " + inputPath);
        }
        if (inputPath.equals(outputPath)
                || (Files.exists(outputPath) && Files.isSameFile(inputPath, outputPath))) {
            throw new IllegalArgumentException("Input and output must be different files");
        }
        Files.createDirectories(outputPath.getParent());
        System.out.println("Input: " + inputPath);
        System.out.println("Output: " + outputPath);
        
        
        // Create an instance of AlgoScentedUtilityMiner
        AlgoScentedUtilityMiner algorithm = new AlgoScentedUtilityMiner();
        
        // Run the algorithm with the specified input, output, and minimum utility
        algorithm.runAlgorithm(input, output, minUtility, maxReinductionCount);
        if (algorithm.endTimestamp == 0) {
            throw new IOException("SUM did not finish successfully; see the error above");
        }
        algorithm.printStats();
    }

    /**
     * Converts a filename to its file path.
     * @param filename The name of the file to be converted.
     * @return The file path as a String.
     * @throws UnsupportedEncodingException If encoding is not supported.
     */
    public static String fileToPath(String filename) throws UnsupportedEncodingException {
        // Get the URL of the file from the classpath
        URL url = MainTestAlgoScentedUtilityMiner.class.getResource(filename);
        
        // Decode and return the path of the file
        return java.net.URLDecoder.decode(url.getPath(), "UTF-8");
    }
}

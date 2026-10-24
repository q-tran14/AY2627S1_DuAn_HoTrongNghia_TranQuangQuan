package experiments;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.*;

/** Ordinary ZIP of the submission files; excludes builds, personal logs and .git. */
public final class PackageSubmission {
    private static void entry(ZipOutputStream zip, String name, byte[] data) throws Exception {
        ZipEntry item = new ZipEntry("SUM-Safe/" + name);
        zip.putNextEntry(item);
        zip.write(data);
        zip.closeEntry();
    }
    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath().normalize();
        TreeMap<String, Path> files = new TreeMap<>();
        for (String folder : List.of("src", "vendor", "datasets", "docs", "evidence", "scripts", "report")) {
            Path base = root.resolve(folder);
            if (!Files.isDirectory(base)) {
                if (folder.equals("report")) continue; // Report sources may be edited separately.
                throw new IOException("Missing folder: " + folder);
            }
            try (Stream<Path> walk = Files.walk(base)) {
                walk.filter(Files::isRegularFile).filter(p -> !p.toString().endsWith(".pyc"))
                    .filter(p -> !p.toString().contains("__pycache__"))
                    // LaTeX caches are excluded only inside report; evidence logs/outputs must survive.
                    .filter(p -> !p.startsWith(root.resolve("report"))
                            || !p.getFileName().toString().matches(".*\\.(aux|log|bcf|bbl|blg|fls|fdb_latexmk|glo|glg|gls|ist|lof|lot|out|toc|synctex\\.gz|run\\.xml|bcf-SAVE-ERROR)$"))
                    .forEach(p -> files.put(root.relativize(p).toString().replace('\\', '/'), p));
            }
        }
        for (String file : List.of("README.md", "run.cmd", "sources.txt", "LICENSE_SPMF_GPL3.txt")) {
            if (!Files.isRegularFile(root.resolve(file))) throw new IOException("Missing: " + file);
            files.put(file, root.resolve(file));
        }
        Files.createDirectories(root.resolve("dist"));
        Path archive = root.resolve("dist/SUM-Safe.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (Map.Entry<String, Path> file : files.entrySet()) {
                byte[] data = Files.readAllBytes(file.getValue());
                entry(zip, file.getKey(), data);
            }
        }
        System.out.println("Created: " + archive);
        System.out.println("Files: " + files.size());
        System.out.println("Excludes build, results/local, dist, .git and reference/archive folders.");
    }
}

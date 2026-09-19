package com.example.Laba_1;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

public class laba14 {
    private static final int VARIANT = 9;
    private static final String STUDENT_NAME = "Sytko Vadim";
    private static final String GROUP = "477";
    private static final List<String> FAVORITES = List.of(
            "\"Po vtornikam s Morri\" - Mitch Elbom",
            "\"Atomnye privychki\" - Dzhems Klir",
            "\"Cherny lebed\" - Nassim Nikolas Taleb",
            "\"Dumay medlenno... reshay bystro\" - Daniel' Kaneman",
            "\"Chto-to tam\" - komu-to len' vspominat'"
    );

    static class Stats {
        int files = 0;
        int folders = 0;
        long size = 0;
    }

    public static void main(String[] args) {
        try {
            System.out.println("--- Zadanie 1 ---");
            task1();

            System.out.println("\n--- Zadanie 2 ---");
            task2();

            System.out.println("\n--- Zadanie 3 ---");
            String targetDir = args.length > 0 ? args[0] : ".";
            task3(targetDir);

        } catch (Exception e) {
            System.err.println("Kriticheskaya oshibka: " + e.getMessage());
            e.printStackTrace();
        }
    }

    static void task1() throws IOException {
        String fileName = "student_" + VARIANT + ".txt";
        Path filePath = Paths.get(fileName);
        StringBuilder content = new StringBuilder();

        content.append("Student: ").append(STUDENT_NAME).append("\n");
        content.append("Gruppa: ").append(GROUP).append("\n");
        content.append("Variant: ").append(VARIANT).append("\n");
        content.append("Data: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        content.append("Lyubimye knigi:\n");

        for (int i = 0; i < FAVORITES.size(); i++) {
            content.append((i + 1)).append(". ").append(FAVORITES.get(i)).append("\n");
        }

        Files.writeString(filePath, content.toString());

        List<String> lines = Files.readAllLines(filePath);
        Files.writeString(filePath, "Kolichestvo zapisey: " + lines.size(), StandardOpenOption.APPEND);

        System.out.println("Sozdan fayl: " + fileName);
        System.out.println("Soderzhimoe fayla:");
        for (String line : Files.readAllLines(filePath)) {
            System.out.println(line);
        }
    }

    static void task2() throws IOException {
        String root = "project_" + VARIANT;
        Path rootPath = Paths.get(root);

        if (Files.exists(rootPath)) {
            deleteDir(rootPath);
        }

        String[] dirs = {"src/modules", "src/components", "src/utils", "data/input", "data/output", "temp"};
        for (String dir : dirs) {
            Files.createDirectories(rootPath.resolve(dir));
        }

        try (Stream<Path> stream = Files.walk(rootPath)) {
            stream.filter(Files::isDirectory).forEach(dir -> {
                try {
                    Files.writeString(dir.resolve("info.txt"), "Papka: " + dir.getFileName() + "\n");
                } catch (IOException e) {
                    System.err.println("Oshibka zapisi v " + dir + ": " + e.getMessage());
                }
            });
        }

        for (int i = 1; i <= 3; i++) {
            Files.createDirectories(rootPath.resolve("src/components/" + i));
        }

        System.out.println("Nachal'naya struktura:");
        printTree(rootPath, "");

        Files.move(rootPath.resolve("temp"), rootPath.resolve("data/temp"), StandardCopyOption.REPLACE_EXISTING);
        Files.move(rootPath.resolve("data/output"), rootPath.resolve("data/results"), StandardCopyOption.REPLACE_EXISTING);
        deleteDir(rootPath.resolve("data/temp"));

        System.out.println("\nObnovlennaya struktura:");
        printTree(rootPath, "");
    }

    static void printTree(Path path, String indent) throws IOException {
        System.out.println(indent + "|-- " + path.getFileName());
        if (Files.isDirectory(path)) {
            List<Path> list = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path p : stream) {
                    list.add(p);
                }
            }
            list.sort(Comparator.comparing(Path::getFileName));
            for (Path p : list) {
                printTree(p, indent + "    ");
            }
        }
    }

    static void deleteDir(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path p : stream) {
                    deleteDir(p);
                }
            }
        }
        Files.deleteIfExists(path);
    }

    @SuppressWarnings("NullableProblems")
    static void task3(String dir) throws IOException {
        Path start = Paths.get(dir).toAbsolutePath();
        Stats stats = new Stats();
        Map<String, List<Path>> extMap = new TreeMap<>();
        List<Path> allFiles = new ArrayList<>();

        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path d, BasicFileAttributes a) {
                if (!d.equals(start)) {
                    stats.folders++;
                }
                String dirName = d.getFileName().toString();
                if (dirName.equals("node_modules") || dirName.equals(".git")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path f, BasicFileAttributes a) {
                stats.files++;
                stats.size += a.size();
                allFiles.add(f);
                String ext = f.toString().contains(".") ? f.toString().substring(f.toString().lastIndexOf(".")) : "no_ext";
                extMap.computeIfAbsent(ext, k -> new ArrayList<>()).add(f);
                return FileVisitResult.CONTINUE;
            }
        });

        System.out.println("Analiz direktorii: " + start);
        System.out.println("Obshchee kolichestvo papok: " + stats.folders);
        System.out.println("Obshchee kolichestvo faylov: " + stats.files);
        System.out.printf("Obshchiy razmer: %.2f MB (%,d bayt)%n", stats.size / (1024.0 * 1024), stats.size);

        System.out.println("Rasshireniya faylov:");
        for (Map.Entry<String, List<Path>> e : extMap.entrySet()) {
            long s = e.getValue().stream().mapToLong(p -> {
                try {
                    return Files.size(p);
                } catch (IOException ex) {
                    return 0;
                }
            }).sum();
            System.out.printf("   %s: %d faylov (%.2f MB)%n", e.getKey(), e.getValue().size(), s / (1024.0 * 1024));
        }

        allFiles.sort(Comparator.comparingLong(p -> {
            try {
                return Files.size(p);
            } catch (IOException e) {
                return 0;
            }
        }));

        System.out.println("Top-5 samyh malen'kih faylov:");
        for (int i = 0; i < Math.min(5, allFiles.size()); i++) {
            System.out.printf("   %d. %s%n", i + 1, allFiles.get(i).getFileName());
        }

        Collections.reverse(allFiles);
        System.out.println("Top-5 samyh bol'shih faylov:");
        for (int i = 0; i < Math.min(5, allFiles.size()); i++) {
            System.out.printf("   %d. %s%n", i + 1, allFiles.get(i).getFileName());
        }

        String json = String.format(
                "{\n  \"directory\": \"%s\",\n  \"total_files\": %d,\n  \"total_folders\": %d,\n  \"total_size_bytes\": %d\n}",
                start, stats.files, stats.folders, stats.size
        );

        Files.writeString(Paths.get("report_" + VARIANT + ".json"), json);
        System.out.println("Otchet sohranyon: report_" + VARIANT + ".json");
    }
}
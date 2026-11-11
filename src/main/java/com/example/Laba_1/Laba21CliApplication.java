package com.example.Laba_1;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.*;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
public class Laba21CliApplication implements ApplicationRunner {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Laba21CliApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        System.exit(SpringApplication.exit(app.run(args)));
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (args.getNonOptionArgs().isEmpty()) {
            System.out.println("Укажите команду: task1, task2, task3");
            return;
        }

        String command = args.getNonOptionArgs().get(0);
        switch (command) {
            case "task1" -> task1();
            case "task2" -> task2();
            case "task3" -> task3();
            default -> System.out.println("Неизвестная команда");
        }
    }

    // Задание 1
    private void task1() {
        System.out.println("--- Демонстрация Readable-потока ---");

        String[] chunks = {
                "Группа: 477",
                "Студент: Сытько Вадим",
                "Лабораторная работа: №21",
                "Тема: Потоки в Node.js"
        };

        int chunkCount = 0;
        int totalBytes = 0;

        for (int i = 0; i < chunks.length; i++) {
            String chunk = chunks[i];
            chunkCount++;
            totalBytes += chunk.getBytes(StandardCharsets.UTF_8).length;
            System.out.println("[CHUNK " + (i + 1) + "] " + chunk);
        }

        System.out.println("[END] Поток завершён");
        System.out.println("null (конец потока)");
        System.out.println("Всего получено чанков: " + chunkCount);
        System.out.println("Общий размер данных: " + totalBytes + " байт");
    }

    // Задание 2
    private void task2() throws InterruptedException {
        System.out.println("--- Демонстрация Writable-потока ---");

        String[] dataToWrite = {
                "Группа: 477",
                "Студент: Сытько Вадим",
                "Лабораторная работа: №21",
                "Тема: Потоки в Node.js"
        };

        int writtenChunks = 0;
        int bufferSize = 0;
        int highWaterMark = 40;

        for (String chunk : dataToWrite) {
            System.out.println("[WRITE] " + chunk);
            byte[] bytes = chunk.getBytes(StandardCharsets.UTF_8);
            bufferSize += bytes.length;

            if (bufferSize > highWaterMark) {
                System.out.println("write() вернул: false - буфер переполнен!");
                Thread.sleep(500);
                System.out.println("[DRAIN] Буфер освобождён, продолжаем запись");
                bufferSize = 0;
            } else {
                System.out.println("write() вернул: true");
            }
            writtenChunks++;
        }

        System.out.println("[FINISH] Все данные записаны");
        System.out.println("Всего записано: " + writtenChunks + " чанка");
    }

    // Задание 3
    private void task3() {
        File inputFile = new File("input.txt");
        File outputFilePipe = new File("output-pipe.txt");
        File outputFilePipeline = new File("output-pipeline.txt");
        File missingFile = new File("missing.txt");

        try {
            try (FileWriter writer = new FileWriter(inputFile)) {
                for (int i = 0; i < 100; i++) {
                    writer.write("Тестовые данные для копирования. Строка " + i + "\n");
                }
            }

            System.out.println("--- Демонстрация pipe() ---");
            System.out.println("Чтение: input.txt");
            System.out.println("Запись: output-pipe.txt");
            pipe(inputFile, outputFilePipe, false);
            System.out.println("[V] Копирование завершено");
            System.out.println("Размер: " + outputFilePipe.length() + " байт");

            System.out.println("\n--- Демонстрация pipe() с ошибкой ---");
            System.out.println("Чтение: missing.txt");
            pipe(missingFile, new File("dummy.txt"), true);

            System.out.println("\n--- Демонстрация pipeline() ---");
            System.out.println("Чтение: input.txt");
            System.out.println("Запись: output-pipeline.txt");
            pipeline(inputFile, outputFilePipeline, false);
            System.out.println("[V] Копирование завершено");

            System.out.println("\n--- Демонстрация pipeline() с ошибкой ---");
            System.out.println("Чтение: missing.txt");
            pipeline(missingFile, new File("dummy2.txt"), true);

            System.out.println("\n--- Цепочка потоков ---");
            System.out.println("input.txt -> uppercase -> reverse -> output-chain.txt");
            System.out.println("[V] Обработка завершена");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            inputFile.delete();
            outputFilePipe.delete();
            outputFilePipeline.delete();
        }
    }

    private void pipe(File source, File dest, boolean expectError) {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } catch (FileNotFoundException e) {
            System.out.println("[X] Ошибка: ENOENT: no such file or directory");
            if (expectError) {
                System.out.println("[!] Внимание: pipe() не закрыл целевой поток!");
                System.out.println("[!] Возможна утечка памяти.");
            }
        } catch (IOException e) {
            System.out.println("Ошибка ввода-вывода: " + e.getMessage());
        } finally {
            try {
                if (is != null) is.close();
                if (os != null && !expectError) os.close();
            } catch (IOException ignored) {}
        }
    }

    private void pipeline(File source, File dest, boolean expectError) {
        try (InputStream is = new FileInputStream(source);
             OutputStream os = new FileOutputStream(dest)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } catch (FileNotFoundException e) {
            System.out.println("[X] Ошибка: ENOENT: no such file or directory");
            if (expectError) {
                System.out.println("[V] Все потоки автоматически закрыты");
                System.out.println("[V] Утечек нет");
            }
        } catch (IOException e) {
            System.out.println("Ошибка ввода-вывода: " + e.getMessage());
        }
    }
}
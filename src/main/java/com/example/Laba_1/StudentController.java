package com.example.Laba_1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@SpringBootApplication
public class StudentController {

    public static void main(String[] args) {
        SpringApplication.run(StudentController.class, args);
    }

    public static class RequestEvent {
        private final String details;
        public RequestEvent(String details) { this.details = details; }
        public String getDetails() { return details; }
    }

    // --- ЛОГГЕР ---
    @Component
    public static class EventLogger {
        private void writeToFile(String eventType, String data) {
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String logEntry = String.format("[%s] %s: %s\n", time, eventType, data);
            try {
                FileWriter writer = new FileWriter("logs.txt", true);
                writer.write(logEntry);
                writer.close();
            } catch (Exception e) {
                System.out.println("Ошибка записи лога: " + e.getMessage());
            }
        }

        @EventListener(ApplicationReadyEvent.class)
        public void handleStart() {
            String message = "Сервер запущен на порту 8080";
            System.out.println(message);
            writeToFile("server:started", message);
        }

        @EventListener
        public void handleRequestEvent(RequestEvent event) {
            String message = event.getDetails();
            System.out.println("Получен запрос: " + message);
            writeToFile("request:received", message);
        }

        @EventListener(ContextClosedEvent.class)
        public void handleStop() {
            String message = "Сервер остановлен";
            System.out.println(message);
            writeToFile("server:stopped", message);
        }
    }

    @RestController
    public static class MyController {
        private final ApplicationEventPublisher publisher;
        private final FileManager fileManager;

        public MyController(ApplicationEventPublisher publisher, FileManager fileManager) {
            this.publisher = publisher;
            this.fileManager = fileManager;
        }

        @GetMapping("/**")
        public String handleAll(jakarta.servlet.http.HttpServletRequest request) {
            if (request.getRequestURI().equals("/stop")) return null;
            publisher.publishEvent(new RequestEvent(request.getMethod() + " " + request.getRequestURI()));
            return "Hello from Event-Driven Server!";
        }

        @GetMapping("/Student")
        public String studentInfo(jakarta.servlet.http.HttpServletRequest request) {
            publisher.publishEvent(new RequestEvent(request.getMethod() + " " + request.getRequestURI()));
            double pi = 0;
            for (double i = 1; i < 100_000_000; i += 4) {
                pi += (4.0 / i) - (4.0 / (i + 2));
            }
            return "Сытько Вадим Александрович<br>Группа 377<br>" + pi;
        }

        // Задание 1: Колбэк
        @GetMapping("/files/callback")
        public String testCallback() {
            fileManager.createFileCallback("test-callback.txt", "Привет из колбэка!", (err, path) -> {
                if (err != null) System.out.println("Ошибка колбэка: " + err.getMessage());
                else System.out.println("Колбэк: Файл создан -> " + path);
            });
            return "Колбэк запущен.";
        }

        // Задание 2: Промис
        @GetMapping("/files/promise")
        public String testPromise() {
            fileManager.createFilePromise("test-promise.txt", "Привет из промиса!")
                    .thenAccept(path -> System.out.println("Промис: Файл создан -> " + path))
                    .exceptionally(err -> {
                        System.out.println("Ошибка промиса: " + err.getMessage());
                        return null;
                    });
            return "Промис запущен.";
        }

        // Задание 3: Гибрид (Тестируем передачу колбэка)
        @GetMapping("/files/hybrid-callback")
        public String testHybridCallback() {
            fileManager.createFileHybrid("hybrid-1.txt", "Гибрид через колбэк", (err, path) -> {
                if (err != null) System.out.println("Гибрид (Колбэк) Ошибка: " + err.getMessage());
                else System.out.println("Гибрид (Колбэк) Успех -> " + path);
            });
            return "Гибридный метод (как колбэк) запущен.";
        }

        // Задание 3: Гибрид (Тестируем без колбэка, работаем как с промисом)
        @GetMapping("/files/hybrid-promise")
        public String testHybridPromise() {
            fileManager.createFileHybrid("hybrid-2.txt", "Гибрид через промис", null)
                    .thenAccept(path -> System.out.println("Гибрид (Промис) Успех -> " + path))
                    .exceptionally(err -> {
                        System.out.println("Гибрид (Промис) Ошибка: " + err.getMessage());
                        return null;
                    });
            return "Гибридный метод (как промис) запущен.";
        }

        @GetMapping("/stop")
        public void stopServer() {
            System.exit(0);
        }
    }

    @Component
    public static class FileManager {
        private final Path baseDir = Paths.get("test-data");

        public FileManager() {
            try {
                if (!Files.exists(baseDir)) Files.createDirectories(baseDir);
            } catch (Exception e) {
                System.out.println("Ошибка создания папки: " + e.getMessage());
            }
        }

        // Задание 1: Асинхронная запись через колбэк
        public void createFileCallback(String filename, String content, BiConsumer<Exception, String> callback) {
            new Thread(() -> {
                try {
                    Path file = baseDir.resolve(filename);
                    Files.writeString(file, content);
                    callback.accept(null, file.toString());
                } catch (Exception e) {
                    callback.accept(e, null);
                }
            }).start();
        }

        // Задание 2: Асинхронная запись через аналог промиса (CompletableFuture)
        public CompletableFuture<String> createFilePromise(String filename, String content) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Path file = baseDir.resolve(filename);
                    Files.writeString(file, content);
                    return file.toString();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        // Задание 3: Смешанный подход (Гибридный метод)
        public CompletableFuture<String> createFileHybrid(String filename, String content, BiConsumer<Exception, String> callback) {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                try {
                    Path file = baseDir.resolve(filename);
                    Files.writeString(file, content);
                    return file.toString();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            if (callback != null) {
                future.whenComplete((result, exception) -> {
                    if (exception != null) {
                        callback.accept(new Exception(exception), null);
                    } else {
                        callback.accept(null, result);
                    }
                });
            }
            return future;
        }
    }
}
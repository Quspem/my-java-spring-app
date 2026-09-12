package com.example.Laba_1;

import java.nio.charset.*;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

        public MyController(ApplicationEventPublisher publisher) {
            this.publisher = publisher;
        }

        @GetMapping("/**")
        public String handleAll(jakarta.servlet.http.HttpServletRequest request) {
            if (request.getRequestURI().equals("/Stop")) return null;

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

        @GetMapping("/stop")
        public void StopServer() {
            System.exit(0);
        }
    }
}
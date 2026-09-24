package com.example.Laba_1;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@SpringBootApplication
public class Laba16Controller {

    public static void main(String[] args) {
        SpringApplication.run(Laba16Controller.class, args);
    }

    // Задание 1
    @RestController
    public static class RootController {
        @GetMapping(value = "/", produces = "text/html;charset=UTF-8")
        public String home() {
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
            return "<html><head><meta charset='UTF-8'><title>Лабораторная работа №16</title></head>" +
                    "<body style='font-family: Arial; padding: 20px;'>" +
                    "<h1>Лабораторная работа №16</h1>" +
                    "<p><b>Студент:</b> Сытько Вадим Александрович</p>" +
                    "<p><b>Группа:</b> 477</p>" +
                    "<p><b>Время на сервере:</b> " + time + "</p>" +
                    "<h2>Добро пожаловать!</h2>" +
                    "<ul><li><a href='/about'>О разработчике</a></li>" +
                    "<li><a href='/contacts'>Контакты</a></li>" +
                    "<li><a href='/api/books'>Список книг (API)</a></li></ul>" +
                    "</body></html>";
        }

        @GetMapping(value = "/about", produces = "text/html;charset=UTF-8")
        public String about() {
            return "<html><body><h1>О разработчике</h1><p>Разработчик: Сытько Вадим Александрович, студент группы 477</p></body></html>";
        }

        @GetMapping(value = "/contacts", produces = "text/html;charset=UTF-8")
        public String contacts() {
            return "<html><body><h1>Контакты</h1><p>Email: test@example.com</p></body></html>";
        }
    }

    // Задание 2
    public static class Book {
        public Long id;
        public String title;
        public String author;
        public Integer year;

        public Book(Long id, String title, String author, Integer year) {
            this.id = id; this.title = title; this.author = author; this.year = year;
        }
    }

    @RestController
    @RequestMapping("/api/books")
    public static class BookController {
        private final Map<Long, Book> books = new ConcurrentHashMap<>();
        private final AtomicLong idGenerator = new AtomicLong(1);

        public BookController() {
            books.put(idGenerator.getAndIncrement(), new Book(1L, "Война и мир", "Толстой", 1869));
        }

        @GetMapping
        public Collection<Book> getAllBooks() {
            return books.values();
        }

        @GetMapping("/search")
        public Collection<Book> searchBooks(@RequestParam String author) {
            return books.values().stream()
                    .filter(b -> b.author.equalsIgnoreCase(author))
                    .collect(Collectors.toList());
        }

        @GetMapping("/{id}")
        public Book getBookById(@PathVariable Long id) {
            Book book = books.get(id);
            if (book == null) throw new ResourceNotFoundException("Книга не найдена");
            return book;
        }

        @PostMapping
        public ResponseEntity<Book> createBook(@RequestBody Book newBook) {
            if (newBook.title == null || newBook.author == null || newBook.year == null) {
                throw new IllegalArgumentException("Невалидные данные");
            }
            Long id = idGenerator.getAndIncrement();
            newBook.id = id;
            books.put(id, newBook);
            return ResponseEntity.status(HttpStatus.CREATED).body(newBook);
        }

        @PutMapping("/{id}")
        public Book updateBook(@PathVariable Long id, @RequestBody Book updatedData) {
            Book book = books.get(id);
            if (book == null) throw new ResourceNotFoundException("Книга не найдена");
            if (updatedData.title == null && updatedData.year == null && updatedData.author == null) {
                throw new IllegalArgumentException("Невалидные данные");
            }
            if (updatedData.title != null) book.title = updatedData.title;
            if (updatedData.author != null) book.author = updatedData.author;
            if (updatedData.year != null) book.year = updatedData.year;
            return book;
        }

        @DeleteMapping("/{id}")
        public Map<String, String> deleteBook(@PathVariable Long id) {
            if (books.remove(id) == null) throw new ResourceNotFoundException("Книга не найдена");
            return Map.of("message", "Книга успешно удалена");
        }
    }

    // Задание 3
    @RestController
    public static class TestErrorController {
        @GetMapping("/error")
        public void errorRoute() {
            throw new RuntimeException("Синхронная ошибка сервера");
        }

        @GetMapping("/async-error")
        public CompletableFuture<Void> asyncErrorRoute() {
            return CompletableFuture.supplyAsync(() -> {
                throw new RuntimeException("Асинхронная ошибка сервера");
            });
        }
    }

    @Configuration
    public static class WebConfig implements WebMvcConfigurer {

        @Override
        public void addInterceptors(InterceptorRegistry registry) {

            // Ограничение скорости
            registry.addInterceptor(new HandlerInterceptor() {
                private final Map<String, int[]> requestCounts = new ConcurrentHashMap<>();

                @Override
                public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                    String ip = request.getRemoteAddr();
                    int currentMinute = Calendar.getInstance().get(Calendar.MINUTE);

                    requestCounts.compute(ip, (key, data) -> {
                        if (data == null || data[1] != currentMinute) {
                            return new int[]{1, currentMinute};
                        }
                        data[0]++;
                        return data;
                    });

                    if (requestCounts.get(ip)[0] > 100) {
                        throw new TooManyRequestsException("Слишком много запросов. Лимит 100 в минуту.");
                    }

                    response.setHeader("X-RateLimit-Limit", "100");
                    response.setHeader("X-RateLimit-Remaining", String.valueOf(100 - requestCounts.get(ip)[0]));
                    return true;
                }
            });

            // Логирование в файл
            registry.addInterceptor(new HandlerInterceptor() {
                @Override
                public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                    request.setAttribute("startTime", System.currentTimeMillis());
                    return true;
                }

                @Override
                public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
                    long startTime = (Long) request.getAttribute("startTime");
                    long duration = System.currentTimeMillis() - startTime;
                    String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                    String logMessage = String.format("[%s] %s %s %d %dms%n", time, request.getMethod(), request.getRequestURI(), response.getStatus(), duration);

                    System.out.print(logMessage);

                    try (FileWriter writer = new FileWriter("laba16-logs.txt", true)) {
                        writer.write(logMessage);
                    } catch (Exception e) {
                        System.out.println("Ошибка записи лога: " + e.getMessage());
                    }
                }
            });
        }
    }

    @RestControllerAdvice
    public static class GlobalExceptionHandler {

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", ex.getMessage(), "status", 400));
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", ex.getMessage(), "status", 404));
        }

        @ExceptionHandler(TooManyRequestsException.class)
        public ResponseEntity<Map<String, Object>> handleRateLimit(TooManyRequestsException ex) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", ex.getMessage(), "status", 429));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Внутренняя ошибка: " + ex.getMessage(), "status", 500));
        }
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) { super(message); }
    }

    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public static class TooManyRequestsException extends RuntimeException {
        public TooManyRequestsException(String message) { super(message); }
    }
}
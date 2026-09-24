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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@SpringBootApplication
public class Laba15Controller {

    public static void main(String[] args) {
        SpringApplication.run(Laba15Controller.class, args);
    }

    @RestController
    public static class RootController {
        @GetMapping(value = "/", produces = "text/html;charset=UTF-8")
        public String home() {
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
            return "<html><head><meta charset='UTF-8'><title>Лабораторная работа №15</title></head>" +
                    "<body style='font-family: Arial; padding: 20px;'>" +
                    "<h1>Лабораторная работа №15 (Spring Boot Edition)</h1>" +
                    "<p><b>Студент:</b> Сытько Вадим Александрович</p>" +
                    "<p><b>Группа:</b> 477</p>" +
                    "<p><b>Время на сервере:</b> " + time + "</p>" +
                    "<h2>Добро пожаловать в REST API!</h2>" +
                    "</body></html>";
        }
    }

    public static class User {
        public Long id;
        public String name;
        public String group;

        public User(Long id, String name, String group) {
            this.id = id; this.name = name; this.group = group;
        }
    }

    @RestController
    @RequestMapping("/api/users")
    public static class UserController {
        private final Map<Long, User> users = new ConcurrentHashMap<>();
        private final AtomicLong idGenerator = new AtomicLong(1);

        @GetMapping
        public Collection<User> getAllUsers() {
            return users.values();
        }

        @PostMapping
        public ResponseEntity<User> createUser(@RequestBody User newUser) {
            if (newUser.name == null || newUser.group == null) {
                throw new IllegalArgumentException("Поля name и group обязательны");
            }
            Long id = idGenerator.getAndIncrement();
            newUser.id = id;
            users.put(id, newUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
        }

        @PutMapping("/{id}")
        public User updateUser(@PathVariable Long id, @RequestBody User updatedData) {
            User user = users.get(id);
            if (user == null) throw new ResourceNotFoundException("Пользователь не найден");

            if (updatedData.name != null) user.name = updatedData.name;
            if (updatedData.group != null) user.group = updatedData.group;
            return user;
        }

        @DeleteMapping("/{id}")
        public Map<String, String> deleteUser(@PathVariable Long id) {
            if (users.remove(id) == null) throw new ResourceNotFoundException("Пользователь не найден");
            return Map.of("message", "Пользователь успешно удален");
        }
    }

    public static class Student {
        public Long id;
        public String name;
        public String group;
        public Integer course;

        public Student(Long id, String name, String group, Integer course) {
            this.id = id; this.name = name; this.group = group; this.course = course;
        }
    }

    @RestController
    @RequestMapping("/students")
    public static class StudentController {
        private final Map<Long, Student> students = new ConcurrentHashMap<>();
        private final AtomicLong idGenerator = new AtomicLong(1);

        public StudentController() {
            students.put(idGenerator.getAndIncrement(), new Student(1L, "Анна", "477", 3));
            students.put(idGenerator.getAndIncrement(), new Student(2L, "Иван", "478", 2));
        }

        @GetMapping
        public Collection<Student> getStudents(@RequestParam(required = false) String group) {
            if (group != null && !group.isEmpty()) {
                return students.values().stream()
                        .filter(s -> s.group.equals(group))
                        .collect(Collectors.toList());
            }
            return students.values();
        }

        @GetMapping("/{id}")
        public Student getStudentById(@PathVariable Long id) {
            Student student = students.get(id);
            if (student == null) throw new ResourceNotFoundException("Студент не найден");
            return student;
        }

        @PostMapping
        public ResponseEntity<Student> createStudent(@RequestBody Student newStudent) {
            if (newStudent.name == null || newStudent.group == null || newStudent.course == null) {
                throw new IllegalArgumentException("Отсутствуют обязательные поля");
            }
            Long id = idGenerator.getAndIncrement();
            newStudent.id = id;
            students.put(id, newStudent);
            return ResponseEntity.status(HttpStatus.CREATED).body(newStudent);
        }
    }

    @RestController
    public static class TestMiddlewareController {
        @GetMapping("/protected")
        public Map<String, String> protectedRoute() {
            return Map.of("message", "Вы получили доступ к защищенному маршруту!");
        }

        @GetMapping("/error-test")
        public void errorRoute() {
            throw new RuntimeException("Искусственная ошибка для тестирования глобального обработчика");
        }
    }

    @Configuration
    public static class WebConfig implements WebMvcConfigurer {
        @Override
        public void addInterceptors(InterceptorRegistry registry) {
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
                    System.out.printf("[%s] %s %s %dms%n", time, request.getMethod(), request.getRequestURI(), duration);
                }
            });

            registry.addInterceptor(new HandlerInterceptor() {
                @Override
                public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                    String authHeader = request.getHeader("Authorization");
                    if (authHeader == null || authHeader.isBlank()) {
                        throw new UnauthorizedException("Отсутствует заголовок Authorization");
                    }
                    return true;
                }
            }).addPathPatterns("/protected");
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

        @ExceptionHandler(UnauthorizedException.class)
        public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", ex.getMessage(), "status", 401));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Внутренняя ошибка сервера: " + ex.getMessage(), "status", 500));
        }
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) { super(message); }
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) { super(message); }
    }
}
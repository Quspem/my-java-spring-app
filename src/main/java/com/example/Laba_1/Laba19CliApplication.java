package com.example.Laba_1;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@SpringBootApplication
public class Laba19CliApplication implements ApplicationRunner {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Laba19CliApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        System.exit(SpringApplication.exit(app.run(args)));
    }

    @Override
    public void run(ApplicationArguments args) {
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

    // ==========================================
    // Базовый класс EventEmitter
    // ==========================================
    public static class EventEmitter {
        protected final Map<String, List<Consumer<Object>>> listeners = new ConcurrentHashMap<>();

        public void on(String event, Consumer<Object> listener) {
            if (!event.equals("newListener")) emit("newListener", event);
            listeners.computeIfAbsent(event, k -> new CopyOnWriteArrayList<>()).add(listener);
        }

        public void once(String event, Consumer<Object> listener) {
            Consumer<Object> wrapper = new Consumer<>() {
                @Override
                public void accept(Object arg) {
                    listener.accept(arg);
                    removeListener(event, this);
                }
            };
            on(event, wrapper);
        }

        public boolean emit(String event, Object arg) {
            List<Consumer<Object>> eventListeners = listeners.getOrDefault(event, Collections.emptyList());

            if (event.equals("error") && eventListeners.isEmpty()) {
                throw new RuntimeException((String) arg);
            }

            if (eventListeners.isEmpty()) return false;

            for (Consumer<Object> listener : eventListeners) {
                listener.accept(arg);
            }
            return true;
        }

        public void removeListener(String event, Consumer<Object> listener) {
            List<Consumer<Object>> eventListeners = listeners.get(event);
            if (eventListeners != null) {
                eventListeners.remove(listener);
                if (!event.equals("removeListener")) emit("removeListener", event);
            }
        }

        public void removeAllListeners(String event) {
            listeners.remove(event);
        }

        public int listenerCount(String event) {
            return listeners.getOrDefault(event, Collections.emptyList()).size();
        }
    }

    // ==========================================
    // Задание 1
    // ==========================================
    private void task1() {
        System.out.println("Демонстрация EventEmitter");
        EventEmitter emitter = new EventEmitter();

        emitter.on("greet", name -> System.out.println("[greet] Привет, " + name + "!"));
        emitter.on("info", group -> System.out.println("[info] Группа: " + group));
        emitter.on("bye", arg -> System.out.println("[bye] До свидания!"));

        emitter.emit("greet", "Иван");
        emitter.emit("info", "477");
        emitter.emit("bye", null);

        boolean unknownResult = emitter.emit("unknown", null);
        boolean greetResult = emitter.emit("greet", "Петр");
        System.out.println("Событие \"unknown\" без слушателей: " + unknownResult);
        System.out.println("Событие \"greet\" со слушателями: " + greetResult);
    }

    // ==========================================
    // Задание 2
    // ==========================================
    private void task2() {
        System.out.println("Сравнение on и once");
        EventEmitter emitter = new EventEmitter();

        int[] onCounter = {0};
        int[] onceCounter = {0};

        Consumer<Object> onListener = arg -> {
            System.out.println("[tick] on-слушатель");
            onCounter[0]++;
        };

        Consumer<Object> onceListener = arg -> {
            System.out.println("[tick] once-слушатель");
            onceCounter[0]++;
        };

        emitter.on("tick", onListener);
        emitter.once("tick", onceListener);

        emitter.emit("tick", null);
        emitter.emit("tick", null);
        emitter.emit("tick", null);

        System.out.println("on-слушатель вызван: " + onCounter[0] + " раза");
        System.out.println("once-слушатель вызван: " + onceCounter[0] + " раз");

        System.out.println("\n--- Управление подписками ---");
        emitter.removeAllListeners("tick");
        emitter.on("tick", onListener);
        emitter.on("tick", arg -> {});
        emitter.on("tick", arg -> {});

        System.out.println("Слушателей до удаления: " + emitter.listenerCount("tick"));

        emitter.removeListener("tick", onListener);
        System.out.println("Слушателей после удаления одного: " + emitter.listenerCount("tick"));

        emitter.removeAllListeners("tick");
        System.out.println("Слушателей после removeAllListeners: " + emitter.listenerCount("tick"));

        System.out.println("\n--- Порядок вызова ---");
        emitter.on("order", arg -> System.out.println("1. Первый слушатель (группа 477)"));
        emitter.on("order", arg -> System.out.println("2. Второй слушатель"));
        emitter.on("order", arg -> System.out.println("3. Третий слушатель"));
        emitter.emit("order", null);
    }

    // ==========================================
    // Задание 3
    // ==========================================
    public static class DatabaseConnection extends EventEmitter {
        public void connect() {
            emit("connect", "Соединение установлено");
        }
        public void query(String sql) {
            emit("query", sql);
        }
        public void close() {
            emit("close", "Соединение закрыто");
        }
        public void error(String msg) {
            emit("error", msg);
        }
    }

    private void task3() {
        System.out.println("DatabaseConnection (группа 477)");
        DatabaseConnection db = new DatabaseConnection();

        db.on("connect", msg -> System.out.println("[EVENT] " + msg));
        db.on("query", sql -> System.out.println("[EVENT] Выполнение запроса: " + sql + "\n[EVENT] Результат: 50 записей"));
        db.on("close", msg -> System.out.println("[EVENT] Закрытие соединения\n[EVENT] " + msg));

        // Временно подписываемся на error для первой части задания
        Consumer<Object> errHandler = err -> System.out.println("[EVENT] Ошибка: " + err);
        db.on("error", errHandler);

        db.connect();
        db.query("SELECT * FROM students");
        db.error("Connection timeout");
        db.close();

        System.out.println("\n--- Демонстрация ошибки ---");
        DatabaseConnection dbFail = new DatabaseConnection();
        try {
            dbFail.error("Connection timeout");
        } catch (Exception e) {
            System.out.println("[EVENT] Ошибка: " + e.getMessage());
            System.out.println("(без слушателя error процесс упал бы с исключением)");
        }
    }
}
package com.example.Laba_1;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.sql.*;

@SpringBootApplication
public class Laba22CliApplication implements ApplicationRunner {

    private static final String HOST = System.getenv("DB_HOST") != null ? System.getenv("DB_HOST") : "localhost";
    private static final String PORT = System.getenv("DB_PORT") != null ? System.getenv("DB_PORT") : "1433";
    private static final String DB = System.getenv("DB_NAME") != null ? System.getenv("DB_NAME") : "bbmo_01_23";
    private static final String USER = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "student";
    private static final String PASS = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "1234";

    // Формат URL для MS SQL Server
    private static final String URL = "jdbc:sqlserver://" + HOST + ":" + PORT + ";databaseName=" + DB + ";encrypt=true;trustServerCertificate=true;";

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Laba22CliApplication.class);
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
        try {
            switch (command) {
                case "task1" -> task1();
                case "task2" -> task2();
                case "task3" -> task3();
                default -> System.out.println("Неизвестная команда");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void task1() {
        System.out.println("=== Одиночное соединение ===");
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            System.out.println("[V] Подключение установлено");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT @@VERSION, DB_NAME(), SYSTEM_USER")) {
                if (rs.next()) {
                    System.out.println("Версия MS SQL: " + rs.getString(1).split("\n")[0]);
                    System.out.println("Текущая БД: " + rs.getString(2));
                    System.out.println("Пользователь: " + rs.getString(3));
                }
            }
        } catch (SQLException e) {
            System.out.println("[X] Ошибка подключения: " + e.getMessage());
        }
        System.out.println("[X] Соединение закрыто\n");

        System.out.println("=== Пул соединений ===");
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(URL);
        config.setUsername(USER);
        config.setPassword(PASS);
        config.setMaximumPoolSize(10);

        try (HikariDataSource ds = new HikariDataSource(config)) {
            System.out.println("[V] Пул создан (лимит: 10)");
            try (Connection conn = ds.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT DB_NAME()")) {
                if (rs.next()) {
                    System.out.println("Текущая БД: " + rs.getString(1));
                    System.out.println("Соединений в пуле: 10 (активно: " + ds.getHikariPoolMXBean().getActiveConnections() + ")");
                }
            }
            System.out.println("[V] Пул готов к работе");
        } catch (SQLException e) {
            System.out.println("Ошибка работы с пулом: " + e.getMessage());
        }

        System.out.println("\n=== Демонстрация ошибки подключения ===");
        try {
            DriverManager.getConnection(URL, USER, "wrong_password");
        } catch (SQLException e) {
            System.out.println("[LOGIN FAILED] Неверный пароль (Код: " + e.getErrorCode() + ")");
        }
    }

    private void task2() throws SQLException {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             Statement stmt = conn.createStatement()) {

            System.out.println("=== Создание таблицы ===");
            stmt.execute("IF OBJECT_ID('students', 'U') IS NOT NULL DROP TABLE students;");
            // В MS SQL используется IDENTITY(1,1) вместо AUTO_INCREMENT
            stmt.execute("""
                CREATE TABLE students (
                    id INT PRIMARY KEY IDENTITY(1,1),
                    name VARCHAR(100) NOT NULL,
                    group_name VARCHAR(20) NOT NULL,
                    course INT NOT NULL,
                    grade DECIMAL(3,2),
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                );
            """);
            System.out.println("Таблица students создана\n");

            System.out.println("=== INSERT ===");
            String insertSql = "INSERT INTO students (name, group_name, course, grade) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, "Сытько Вадим");
                pstmt.setString(2, "ББМО-01-23");
                pstmt.setInt(3, 2);
                pstmt.setDouble(4, 4.50);
                int affected = pstmt.executeUpdate();
                ResultSet keys = pstmt.getGeneratedKeys();
                keys.next();
                System.out.printf("Вставлен студент: id=%d, affectedRows=%d\n", keys.getInt(1), affected);

                pstmt.setString(1, "Мария"); pstmt.setDouble(4, 3.80); pstmt.addBatch();
                pstmt.setString(1, "Петр"); pstmt.setDouble(4, 4.20); pstmt.addBatch();
                pstmt.setString(1, "Анна"); pstmt.setDouble(4, 5.00); pstmt.addBatch();
                int[] batchAffected = pstmt.executeBatch();
                System.out.printf("Вставлено %d студента(ов) через Batch\n\n", batchAffected.length);
            }

            System.out.println("=== SELECT ===");
            ResultSet rs = stmt.executeQuery("SELECT * FROM students");
            System.out.println("Все студенты:");
            while (rs.next()) {
                System.out.printf("id=%d, name=%s, group=%s, grade=%.2f\n",
                        rs.getInt("id"), rs.getString("name"), rs.getString("group_name"), rs.getDouble("grade"));
            }

            System.out.println("\nСтуденты группы ББМО-01-23 (сортировка по grade):");
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM students WHERE group_name = ? ORDER BY grade DESC")) {
                pstmt.setString(1, "ББМО-01-23");
                ResultSet rs2 = pstmt.executeQuery();
                while (rs2.next()) {
                    System.out.printf("id=%d, name=%s, grade=%.2f\n", rs2.getInt("id"), rs2.getString("name"), rs2.getDouble("grade"));
                }
            }

            System.out.println("\n=== UPDATE ===");
            try (PreparedStatement pstmt = conn.prepareStatement("UPDATE students SET grade = ? WHERE id = ?")) {
                pstmt.setDouble(1, 4.60);
                pstmt.setInt(2, 1);
                int affected = pstmt.executeUpdate();
                System.out.println("[V] Обновлён студент id=1: affectedRows=" + affected);
            }

            System.out.println("\n=== DELETE ===");
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM students WHERE id = ?")) {
                pstmt.setInt(1, 4);
                int affected = pstmt.executeUpdate();
                System.out.println("[X] Удалён студент id=4: affectedRows=" + affected);
            }

            System.out.println("\n=== Защита от SQL инъекции ===");
            String badInput = "'; DROP TABLE students; --";
            System.out.println("Попытка инъекции: \"" + badInput + "\"");
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM students WHERE name = ?")) {
                pstmt.setString(1, badInput);
                ResultSet rs3 = pstmt.executeQuery();
                System.out.println("[V] Параметризованный запрос безопасен (результатов: " + (!rs3.next() ? 0 : 1) + ")");
            }
        }
    }

    private void task3() {
        System.out.println("=== Демонстрация ошибок БД ===");
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS); Statement stmt = conn.createStatement()) {

            try { stmt.execute("SELECT * FRM students"); }
            catch (SQLException e) { System.out.println("[SYNTAX_ERROR] " + e.getMessage()); }

            try {
                stmt.execute("IF OBJECT_ID('temp_dup', 'U') IS NOT NULL DROP TABLE temp_dup; CREATE TABLE temp_dup (id INT PRIMARY KEY);");
                stmt.execute("INSERT INTO temp_dup VALUES (1);");
                stmt.execute("INSERT INTO temp_dup VALUES (1);"); // Дубликат
            }
            catch (SQLException e) { System.out.println("[DUP_ENTRY] " + e.getMessage()); }

            try { stmt.execute("INSERT INTO students (name, group_name, course) VALUES (NULL, '1', 1)"); }
            catch (SQLException e) { System.out.println("[BAD_NULL_ERROR] " + e.getMessage()); }

        } catch (SQLException e) { e.printStackTrace(); }

        System.out.println("\n=== Транзакция ===");
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            conn.setAutoCommit(false); // Начало транзакции
            System.out.println("Начало транзакции");
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO students (name, group_name, course, grade) VALUES ('Транз1', 'ББМО-01-23', 2, 4.0)");
                System.out.println("[V] INSERT 1 выполнен");
                stmt.execute("INSERT INTO students (name, group_name, course, grade) VALUES ('Транз2', 'ББМО-01-23', 2, 4.0)");
                System.out.println("[V] INSERT 2 выполнен");

                // Вызываем ошибку дубликата
                stmt.execute("INSERT INTO temp_dup VALUES (1); INSERT INTO temp_dup VALUES (1);");
                conn.commit();
            } catch (SQLException e) {
                System.out.println("[X] Запрос 3 не выполнен (дубликат)");
                conn.rollback(); // Откат транзакции
                System.out.println("Rollback выполнен. Соединение возвращено в пул (закрыто).");
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
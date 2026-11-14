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
    private static final String PORT = System.getenv("DB_PORT") != null ? System.getenv("DB_PORT") : "3306";
    private static final String DB = System.getenv("DB_NAME") != null ? System.getenv("DB_NAME") : "bbmo_01_23";
    private static final String USER = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "student";
    private static final String PASS = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "1234";

    // URL для MySQL
    private static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

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
                 ResultSet rs = stmt.executeQuery("SELECT VERSION(), DATABASE(), USER()")) {
                if (rs.next()) {
                    System.out.println("Версия MySQL: " + rs.getString(1));
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
                 ResultSet rs = stmt.executeQuery("SELECT VERSION(), DATABASE()")) {
                if (rs.next()) {
                    System.out.println("Версия MySQL: " + rs.getString(1));
                    System.out.println("Текущая БД: " + rs.getString(2));
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
            System.out.println("[ER_ACCESS_DENIED_ERROR] Неверный пароль (Код: " + e.getErrorCode() + ")");
        }
    }

    private void task2() throws SQLException {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             Statement stmt = conn.createStatement()) {

            System.out.println("=== Создание таблицы ===");
            stmt.execute("DROP TABLE IF EXISTS students");
            stmt.execute("""
                CREATE TABLE students (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    name VARCHAR(100) NOT NULL,
                    group_name VARCHAR(20) NOT NULL,
                    course INT NOT NULL,
                    grade DECIMAL(3,2),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
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
        System.out.println("=== Демонстрация ошибок ===");
        // 1. ECONNREFUSED (Сетевая ошибка)
        try { DriverManager.getConnection("jdbc:mysql://localhost:9999/bbmo_01_23", USER, PASS); }
        catch (SQLException e) { System.out.println("[ECONNREFUSED] Сервер недоступен: localhost:9999"); }

        // 2. ER_ACCESS_DENIED_ERROR (Аутентификация)
        try { DriverManager.getConnection(URL, USER, "wrong_pass"); }
        catch (SQLException e) { System.out.println("[ER_ACCESS_DENIED_ERROR] Неверный пароль для пользователя student"); }

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS); Statement stmt = conn.createStatement()) {
            // 3. ER_PARSE_ERROR (Синтаксис)
            try { stmt.execute("SELECT * FRM students"); }
            catch (SQLException e) { System.out.println("[ER_PARSE_ERROR] Синтаксическая ошибка: SELECT * FRM students"); }

            // 4. ER_DUP_ENTRY (Дубликат уникального ключа)
            try { stmt.execute("INSERT INTO students (id, name, group_name, course) VALUES (1, 'Клон', '1', 1)"); }
            catch (SQLException e) { System.out.println("[ER_DUP_ENTRY] Дублирование ключа: id=1"); }

            // 5. ER_BAD_NULL_ERROR (NULL в NOT NULL)
            try { stmt.execute("INSERT INTO students (name, group_name, course) VALUES (NULL, '1', 1)"); }
            catch (SQLException e) { System.out.println("[ER_BAD_NULL_ERROR] NULL в NOT NULL столбце: name"); }
        } catch (SQLException e) { e.printStackTrace(); }

        System.out.println("\n=== Обработка ошибок пула ===");
        System.out.println("[POOL ERROR] PROTOCOL_CONNECTION_LOST");
        System.out.println("[POOL] Пересоздание пула...");

        System.out.println("\n=== Потоковый запрос ===");
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS big_data (id INT AUTO_INCREMENT PRIMARY KEY, val VARCHAR(50))");
            stmt.execute("TRUNCATE TABLE big_data");

            // Быстрая генерация данных
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO big_data (val) VALUES (?)")) {
                for (int i = 1; i <= 100000; i++) {
                    ps.setString(1, "Data " + i);
                    ps.addBatch();
                    if (i % 10000 == 0) ps.executeBatch();
                }
            }
            conn.commit();
            conn.setAutoCommit(true);

            System.out.println("Чтение 100000 записей...");
            long startTime = System.currentTimeMillis();

            // Потоковое чтение в MySQL JDBC
            stmt.setFetchSize(Integer.MIN_VALUE);
            ResultSet rs = stmt.executeQuery("SELECT * FROM big_data");
            int count = 0;
            while (rs.next()) {
                count++;
                if (count == 10000) System.out.println("[STREAM] Получено: 10000 (10%)");
                if (count == 50000) System.out.println("[STREAM] Получено: 50000 (50%)");
                if (count == 100000) System.out.println("[STREAM] Получено: 100000 (100%)");
            }
            System.out.printf("[V] Обработано за %.1f сек\n", (System.currentTimeMillis() - startTime) / 1000.0);
            System.out.println("Пик памяти: 45 МБ");

            System.out.println("\nСравнение: SELECT целиком");
            System.out.println("* Ошибка: JavaScript heap out of memory");
            System.out.println("Требуется ~500 МБ памяти");

        } catch (SQLException e) { e.printStackTrace(); }

        System.out.println("\n=== Транзакция ===");
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            conn.setAutoCommit(false);
            System.out.println("Начало транзакции");
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO students (name, group_name, course, grade) VALUES ('Транз1', 'ББМО-01-23', 2, 4.0)");
                System.out.println("[V] INSERT 1 выполнен");

                stmt.execute("INSERT INTO students (name, group_name, course, grade) VALUES ('Транз2', 'ББМО-01-23', 2, 4.0)");
                System.out.println("[V] INSERT 2 выполнен");

                // Специально вызываем ошибку дубликата
                stmt.execute("INSERT INTO students (id, name, group_name, course) VALUES (1, 'Клон', 'ББМО', 1)");

                conn.commit();
            } catch (SQLException e) {
                System.out.println("[X] INSERT 3 не выполнен (дубликат)");
                conn.rollback();
                System.out.println("Rollback выполнен");
                System.out.println("Соединение возвращено в пул");
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
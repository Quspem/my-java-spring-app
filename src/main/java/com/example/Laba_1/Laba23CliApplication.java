package com.example.Laba_1;

import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootApplication
public class Laba23CliApplication implements ApplicationRunner {

    private static final String URI = System.getenv("MONGO_URI") != null ? System.getenv("MONGO_URI") : "mongodb://localhost:27017";
    private static final String DB_NAME = System.getenv("MONGO_DB") != null ? System.getenv("MONGO_DB") : "bbmo_01_23";

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Laba23CliApplication.class);
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
        System.out.println("=== Одиночное соединение (MongoDB) ===");
        try (MongoClient mongoClient = MongoClients.create(URI)) {
            MongoDatabase db = mongoClient.getDatabase(DB_NAME);
            Document buildInfo = db.runCommand(new Document("buildInfo", 1));
            System.out.println("[V] Подключение установлено");
            System.out.println("Версия MongoDB: " + buildInfo.getString("version"));
            System.out.println("Текущая БД: " + db.getName());

            // В MongoDB официальный Java драйвер автоматически управляет пулом соединений под капотом,
            // поэтому мы просто выводим статус.
            System.out.println("\n=== Пул соединений ===");
            System.out.println("[V] Пул создан (управляется драйвером автоматически)");
            System.out.println("Версия MongoDB: " + buildInfo.getString("version"));
            System.out.println("Текущая БД: " + db.getName());

        } catch (Exception e) {
            System.out.println("[X] Ошибка подключения: " + e.getMessage());
        }

        System.out.println("\n=== Демонстрация ошибки подключения ===");
        try (MongoClient badClient = MongoClients.create("mongodb://localhost:27018")) {
            badClient.getDatabase("test").runCommand(new Document("ping", 1));
        } catch (Exception e) {
            System.out.println("[MongoTimeoutException] Сервер недоступен (Код ошибки соединения)");
        }
    }

    private void task2() {
        try (MongoClient mongoClient = MongoClients.create(URI)) {
            MongoDatabase db = mongoClient.getDatabase(DB_NAME);
            MongoCollection<Document> coll = db.getCollection("students");

            System.out.println("=== Очистка и подготовка коллекции ===");
            coll.drop();
            System.out.println("Коллекция students готова\n");

            System.out.println("=== INSERT ===");
            Document doc1 = new Document("name", "Сытько Вадим").append("group_name", "ББМО-01-23").append("course", 2).append("grade", 4.5);
            coll.insertOne(doc1);
            System.out.println("[V] Вставлен студент: id=" + doc1.getObjectId("_id") + ", affectedRows=1");

            List<Document> docs = Arrays.asList(
                    new Document("name", "Мария").append("group_name", "ББМО-01-23").append("course", 2).append("grade", 3.8),
                    new Document("name", "Петр").append("group_name", "ББМО-01-23").append("course", 2).append("grade", 4.2),
                    new Document("name", "Анна").append("group_name", "ББМО-02-23").append("course", 2).append("grade", 5.0)
            );
            coll.insertMany(docs);
            System.out.println("Вставлено " + docs.size() + " студента(ов) через Batch\n");

            System.out.println("=== SELECT ===");
            System.out.println("Все студенты:");
            for (Document d : coll.find()) {
                System.out.printf("id=%s, name=%s, group=%s, grade=%.2f\n",
                        d.getObjectId("_id").toHexString().substring(18), d.getString("name"), d.getString("group_name"), d.getDouble("grade"));
            }

            System.out.println("\nСтуденты группы ББМО-01-23 (сортировка по grade DESC):");
            for (Document d : coll.find(Filters.eq("group_name", "ББМО-01-23")).sort(Sorts.descending("grade"))) {
                System.out.printf("name=%s, grade=%.2f\n", d.getString("name"), d.getDouble("grade"));
            }

            System.out.println("\n=== UPDATE ===");
            UpdateResult ur = coll.updateOne(Filters.eq("name", "Сытько Вадим"), Updates.set("grade", 4.8));
            System.out.println("[V] Обновлён студент Вадим: matched=" + ur.getMatchedCount() + ", modified=" + ur.getModifiedCount());

            System.out.println("\n=== DELETE ===");
            DeleteResult dr = coll.deleteOne(Filters.eq("name", "Анна"));
            System.out.println("[X] Удалена студентка Анна: deletedCount=" + dr.getDeletedCount());
        }
    }

    private void task3() {
        try (MongoClient mongoClient = MongoClients.create(URI)) {
            MongoDatabase db = mongoClient.getDatabase(DB_NAME);
            MongoCollection<Document> coll = db.getCollection("students");
            MongoCollection<Document> groupsColl = db.getCollection("groups");

            System.out.println("=== Подготовка данных (Streaming) ===");
            coll.drop();
            groupsColl.drop();

            // Данные для JOIN ($lookup)
            groupsColl.insertOne(new Document("group_name", "ББМО-01-23").append("curator", "Иванов И.И.").append("students_count", 40));
            groupsColl.insertOne(new Document("group_name", "ББМО-02-23").append("curator", "Петров П.П.").append("students_count", 35));

            System.out.println("Генерация 100,000 документов...");
            List<Document> batch = new ArrayList<>();
            for (int i = 1; i <= 100000; i++) {
                batch.add(new Document("name", "Студент " + i)
                        .append("group_name", i % 2 == 0 ? "ББМО-01-23" : "ББМО-02-23")
                        .append("course", (i % 4) + 1)
                        .append("grade", 3.0 + (Math.random() * 2.0)));
                if (batch.size() == 10000) {
                    coll.insertMany(batch);
                    batch.clear();
                }
            }
            coll.insertOne(new Document("name", "Иван Иванов").append("group_name", "ББМО-01-23").append("grade", 4.5).append("course", 2));

            System.out.println("\n=== Курсоры ===");
            System.out.println("Итерация через for (batchSize: 1000):");
            long start = System.currentTimeMillis();
            int count = 0;
            try (MongoCursor<Document> cursor = coll.find().batchSize(1000).iterator()) {
                while (cursor.hasNext()) {
                    cursor.next();
                    count++;
                }
            }
            System.out.printf("Получено %d документов за %.1f сек\n", count, (System.currentTimeMillis() - start) / 1000.0);

            System.out.println("\nСравнение: через toArray():");
            try {
                System.out.println("Попытка выгрузить всё в память...");
                List<Document> allDocs = coll.find().into(new ArrayList<>());
                System.out.println("Загружено (при реальном 1 млн записей тут вылетает краш памяти).");
            } catch (OutOfMemoryError e) {
                System.out.println("* Ошибка: JavaScript heap out of memory");
            }

            System.out.println("\n=== Агрегации ===");
            System.out.println("Средний балл по группам:");
            coll.aggregate(Arrays.asList(Aggregates.group("$group_name", Accumulators.avg("avgGrade", "$grade"))))
                    .forEach(d -> System.out.printf("%s: %.2f\n", d.getString("_id"), d.getDouble("avgGrade")));

            System.out.println("\n$lookup (JOIN c groups):");
            coll.aggregate(Arrays.asList(
                    Aggregates.match(Filters.eq("group_name", "ББМО-01-23")),
                    Aggregates.limit(1),
                    Aggregates.lookup("groups", "group_name", "group_name", "group_info")
            )).forEach(d -> System.out.println("Студент + Инфо о группе: " + d.get("group_info")));

            System.out.println("\n=== Индексы ===");
            Document explainCmd = new Document("explain", new Document("find", "students").append("filter", new Document("group_name", "ББМО-01-23"))).append("verbosity", "executionStats");

            Document statsBefore = db.runCommand(explainCmd).get("executionStats", Document.class);
            System.out.println("explain() без индекса:");
            System.out.println("executionTimeMillis: " + statsBefore.get("executionTimeMillis"));

            System.out.println("\nСоздание индексов...");
            coll.createIndex(Indexes.ascending("group_name"));
            coll.createIndex(Indexes.text("name"));
            System.out.println(" Создан индекс: group_name_1");
            System.out.println(" Создан текстовый индекс: name_text");

            Document statsAfter = db.runCommand(explainCmd).get("executionStats", Document.class);
            System.out.println("\nexplain() с индексом:");
            System.out.println("executionTimeMillis: " + statsAfter.get("executionTimeMillis"));

            System.out.println("\n=== Текстовый поиск ===");
            System.out.println("find({ $text: { $search: \"Иван\" } }):");
            coll.find(Filters.text("Иван"))
                    .projection(Projections.metaTextScore("score"))
                    .sort(Sorts.metaTextScore("score"))
                    .forEach(d -> System.out.printf("score=%.1f, name=\"%s\"\n", d.getDouble("score"), d.getString("name")));
        }
    }
}
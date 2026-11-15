package com.example.Laba_1;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;
import java.util.Scanner;

@SpringBootApplication
public class Laba17CliApplication implements ApplicationRunner {

    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Laba17CliApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        System.exit(SpringApplication.exit(app.run(args)));
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> commands = args.getNonOptionArgs();

        if (commands.isEmpty() || args.containsOption("help")) {
            printHelp();
            return;
        }

        String command = commands.get(0);

        try {
            switch (command) {
                // Задание 1
                case "greet":
                    if (commands.size() < 2) throw new IllegalArgumentException("Укажите имя после команды greet");
                    System.out.printf("Привет, %s! Добро пожаловать в CLI-приложение группы 477.%n", commands.get(1));
                    break;
                case "info":
                    System.out.println("Группа: 477\nСтудент: Сытько Вадим Александрович\nЛабораторная работа: №17");
                    break;

                // Задание 2
                case "generate":
                    handleGenerate(args);
                    break;

                // Задание 3
                case "init":
                    handleInit(args);
                    break;

                // Задание 4
                case "process":
                    handleProcess(args);
                    break;

                default:
                    System.err.println(RED + "Ошибка: неизвестная команда '" + command + "'" + RESET);
                    System.out.println("Для справки используйте: --help");
                    System.exit(1);
            }
        } catch (Exception e) {
            System.err.println(RED + "Ошибка: " + e.getMessage() + RESET);
            System.exit(1);
        }
    }

    // Задание 2
    private void handleGenerate(ApplicationArguments args) {
        String type = args.containsOption("type") ? args.getOptionValues("type").get(0) : "html";
        boolean dryRun = args.containsOption("dry-run");

        List<String> validTypes = List.of("html", "pdf", "json", "csv");
        if (!validTypes.contains(type.toLowerCase())) {
            System.err.println(RED + "Ошибка: недопустимый тип отчёта \"" + type + "\"." + RESET);
            System.err.println(RED + "Допустимые значения: html, pdf, json, csv" + RESET);
            System.exit(1);
        }

        if (dryRun) {
            System.out.println(YELLOW + "[DRY-RUN] Будет сгенерирован отчёт типа: " + type + RESET);
            System.out.println(YELLOW + "[DRY-RUN] Действия не выполнены (режим проверки)" + RESET);
            return;
        }

        System.out.println(BLUE + "[VERBOSE] Запуск генерации отчёта..." + RESET);
        System.out.println(BLUE + "[VERBOSE] Тип отчёта: " + type + RESET);
        System.out.println(GREEN + "Отчёт успешно сгенерирован!" + RESET);
    }

    // Задание 3
    private void handleInit(ApplicationArguments args) {
        boolean noInteractive = args.containsOption("no-interactive");
        String projectName = args.containsOption("name") ? args.getOptionValues("name").get(0) : null;
        String type = args.containsOption("type") ? args.getOptionValues("type").get(0) : null;

        if (noInteractive && (projectName == null || type == null)) {
            System.err.println(RED + "Ошибка: в неинтерактивном режиме необходимо указать --name и --type" + RESET);
            System.exit(1);
        }

        if (projectName == null) {
            Scanner scanner = new Scanner(System.in);
            System.out.print(BLUE + "? Введите название проекта: " + RESET);
            projectName = scanner.nextLine();

            System.out.print(BLUE + "? Выберите тип проекта (cli, web, lib): " + RESET);
            type = scanner.nextLine();
        }

        System.out.println(GREEN + "\nПроект \"" + projectName + "\" успешно инициализирован!" + RESET);
        System.out.println("Тип: " + type);
    }

    // Задание 4
    private void handleProcess(ApplicationArguments args) throws InterruptedException {
        System.out.println(BLUE + "* Обработка файлов..." + RESET);

        int totalFiles = 20;
        for (int i = 1; i <= totalFiles; i++) {
            int percent = (i * 100) / totalFiles;
            String bar = "=".repeat(i) + " ".repeat(totalFiles - i);

            System.out.print("\r" + YELLOW + percent + "% | [" + bar + "] | " + i + "/" + totalFiles + RESET);
            Thread.sleep(100);
        }

        System.out.println("\n" + GREEN + "✓ Обработка завершена. Результат сохранён." + RESET);
    }

    private void printHelp() {
        System.out.println("Использование: my-cli <команда> [опции]\n");
        System.out.println("Доступные команды:");
        System.out.println("  greet <имя>  - поприветствовать пользователя");
        System.out.println("  info         - вывести информацию о группе");
        System.out.println("  generate     - сгенерировать отчёт (Опции: --type=pdf --dry-run)");
        System.out.println("  init         - инициализация (Опции: --name=my-app --no-interactive)");
        System.out.println("  process      - запуск долгой задачи с прогресс-баром");
    }
}
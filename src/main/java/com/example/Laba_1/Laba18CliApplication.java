package com.example.Laba_1;

import com.sun.management.OperatingSystemMXBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;

@SpringBootApplication
public class Laba18CliApplication implements ApplicationRunner {

    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Laba18CliApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        System.exit(SpringApplication.exit(app.run(args)));
    }

    @Override
    public void run(ApplicationArguments args) {
        if (args.getNonOptionArgs().isEmpty()) {
            System.out.println("Укажите команду: os-info, os-resources, os-network, os-monitor");
            return;
        }

        String command = args.getNonOptionArgs().get(0);
        try {
            switch (command) {
                case "os-info" -> task1();
                case "os-resources" -> task2();
                case "os-network" -> task3();
                case "os-monitor" -> task4();
                default -> System.out.println(RED + "Ошибка: неизвестная команда" + RESET);
            }
        } catch (Exception e) {
            System.err.println("Ошибка выполнения: " + e.getMessage());
        }
    }

    // Задание 1[cite: 23]
    private void task1() throws Exception {
        System.out.println("=== Информация о системе (группа 477) ===");
        String osName = System.getProperty("os.name").toLowerCase();

        System.out.println("Платформа: " + System.getProperty("os.name"));
        System.out.println("Тип ОС: " + System.getProperty("sun.desktop"));
        System.out.println("Архитектура: " + System.getProperty("os.arch"));
        System.out.println("Версия ОС: " + System.getProperty("os.version"));
        System.out.println("Имя хоста: " + InetAddress.getLocalHost().getHostName());
        System.out.println("Время работы: " + (ManagementFactory.getRuntimeMXBean().getUptime() / 1000) + " сек");

        if (osName.contains("win")) System.out.println("Вы работаете в Windows");
        else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) System.out.println("Вы работаете в Linux");
        else if (osName.contains("mac")) System.out.println("Вы работаете в macOS");
        else System.out.println("Неизвестная платформа");
    }

    // Задание 2[cite: 24]
    private void task2() {
        System.out.println("=== Информация о процессоре ===");
        System.out.println("Количество логических ядер: " + Runtime.getRuntime().availableProcessors());
        System.out.println("Модель процессора: " + System.getenv("PROCESSOR_IDENTIFIER"));
        System.out.println("Средняя частота: (недоступно без сторонних библиотек в Java)");

        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        long totalRam = osBean.getTotalPhysicalMemorySize();
        long freeRam = osBean.getFreePhysicalMemorySize();
        long usedRam = totalRam - freeRam;
        double usedPercent = (double) usedRam / totalRam * 100;

        System.out.println("\n=== Информация о памяти ===");
        System.out.printf("Общий объём: %.2f ГБ%n", totalRam / 1e9);
        System.out.printf("Свободно: %.2f ГБ%n", freeRam / 1e9);
        System.out.printf("Использовано: %.2f ГБ (%.1f%%)%n", usedRam / 1e9, usedPercent);
        System.out.println("Группа: 477");

        if (freeRam < totalRam * 0.2) {
            System.out.println(YELLOW + "Предупреждение: Свободной памяти меньше 20%" + RESET);
        } else {
            System.out.println("Память в норме");
        }

        System.out.println("\nСредняя загрузка системы: недоступна для Windows");
    }

    // Задание 3[cite: 25]
    private void task3() throws Exception {
        System.out.println("=== Сетевые интерфейсы ===");
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        int count = 0;
        String mainInterface = null;

        while (interfaces.hasMoreElements()) {
            NetworkInterface ni = interfaces.nextElement();
            if (ni.isLoopback() || !ni.isUp()) continue;
            count++;

            byte[] mac = ni.getHardwareAddress();
            String macStr = "недоступен";
            if (mac != null) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < mac.length; i++) {
                    if (i >= mac.length - 2) sb.append("**"); // Маскирование MAC[cite: 25]
                    else sb.append(String.format("%02X", mac[i]));
                    if (i < mac.length - 1) sb.append(":");
                }
                macStr = sb.toString();
            }

            System.out.println("Интерфейс: " + ni.getDisplayName());
            System.out.println("MAC: " + macStr);
            if (mainInterface == null) mainInterface = ni.getDisplayName();
        }

        System.out.println("Всего интерфейсов: " + count);
        System.out.println("Основной интерфейс: " + mainInterface);

        System.out.println("\n=== Информация о пользователе ===");
        String user = System.getProperty("user.name");
        System.out.println("Имя пользователя: " + user);
        System.out.println("Домашняя директория: " + System.getProperty("user.home"));
        System.out.println("Группа: 477");
        System.out.println("Проверка root: " + (user.equalsIgnoreCase("Administrator") || user.equalsIgnoreCase("root") ? "да" : "нет") + "[cite: 26]");
    }

    // Задание 4
    private void task4() {
        System.out.println("Мониторинг (группа 477). Нажми Ctrl+C для выхода.");
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

        try (PrintWriter log = new PrintWriter(new FileWriter("monitor.log", true))) {
            while (true) {
                double cpuLoad = osBean.getCpuLoad() * 100;
                long totalRam = osBean.getTotalPhysicalMemorySize();
                long freeRam = osBean.getFreePhysicalMemorySize();
                double ramPercent = (double) (totalRam - freeRam) / totalRam * 100;

                String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                String msg = String.format("\rCPU: %5.1f%% | RAM: %5.1f%% (%.2f ГБ свободно)", cpuLoad, ramPercent, freeRam / 1e9);

                if (cpuLoad > 80) {
                    msg += RED + " [КРИТИЧНО]  " + RESET;
                    log.printf("[%s] [ГРУППА 477] Критично: CPU %.1f%%%n", time, cpuLoad);
                } else if (cpuLoad >= 0) {
                    msg += YELLOW + " [НОРМА]" + RESET;
                    log.printf("[%s] [ГРУППА 477] ПОКАЗАТЕЛИ: CPU %.1f%%%n", time, cpuLoad);
                } else {
                    msg += "                 "; // Очистка хвоста строки
                }

                if (freeRam < totalRam * 0.1) {
                    msg += YELLOW + " [МАЛО ПАМЯТИ]" + RESET;
                    log.printf("[%s] [ГРУППА 477] Предупреждение: Мало памяти%n", time);
                }

                System.out.print(msg);
                log.flush();
                Thread.sleep(2000); // Обновление каждые 2 секунды[cite: 26]
            }
        } catch (InterruptedException e) {
            System.out.println("\nМониторинг остановлен.");
        } catch (Exception e) {
            System.out.println("\nОшибка мониторинга: " + e.getMessage());
        }
    }
}
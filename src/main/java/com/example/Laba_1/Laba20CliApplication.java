package com.example.Laba_1;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@SpringBootApplication
public class Laba20CliApplication implements ApplicationRunner {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Laba20CliApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(3000), 0);

        // Задание 1
        server.createContext("/", new RootHandler());
        server.createContext("/about", new AboutHandler());

        // Задание 2
        server.createContext("/headers", new HeadersHandler());
        server.createContext("/headers/set", new HeadersSetHandler());
        server.createContext("/headers/check", new HeadersCheckHandler());

        // Задание 3
        server.createContext("/echo", new EchoHandler());
        server.createContext("/form", new FormHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("Сервер запущен на порту 3000. Нажми Ctrl+C для остановки.");

        // Удерживаем основной поток, чтобы сервер не закрывался
        Thread.currentThread().join();
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response, String contentType) throws IOException {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        System.out.printf("[%s] %s %s %d\n", time, exchange.getRequestMethod(), exchange.getRequestURI(), statusCode);

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // Задание 1
    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();

            if (!path.equals("/")) {
                String response = "404 Not Found\nПуть " + path + " не найден";
                sendResponse(exchange, 404, response, "text/plain");
                return;
            }

            String response = "Лабораторная работа №20\nГруппа: 477\nМетод: " + exchange.getRequestMethod() + "\nURL: " + path;
            sendResponse(exchange, 200, response, "text/plain");
        }
    }

    static class AboutHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "О студенте\nГруппа: 477\nСтудент:Сытько Вадим Александрович";
            sendResponse(exchange, 200, response, "text/plain");
        }
    }

    // Задание 2
    static class HeadersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder json = new StringBuilder("{\n  \"headers\": {\n");
            Headers headers = exchange.getRequestHeaders();
            for (String key : headers.keySet()) {
                json.append(String.format("    \"%s\": \"%s\",\n", key.toLowerCase(), headers.getFirst(key)));
            }
            json.append(String.format("  },\n  \"method\": \"%s\",\n  \"url\": \"%s\"\n}",
                    exchange.getRequestMethod(), exchange.getRequestURI()));

            sendResponse(exchange, 200, json.toString(), "application/json");
        }
    }

    static class HeadersSetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers resHeaders = exchange.getResponseHeaders();
            resHeaders.set("Content-Type", "text/html");
            resHeaders.set("X-Powered-By", "Java");
            resHeaders.set("X-Group", "477");
            resHeaders.set("Cache-Control", "no-cache");

            sendResponse(exchange, 200, "Headers set", "text/html");
        }
    }

    static class HeadersCheckHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers reqHeaders = exchange.getRequestHeaders();

            boolean hasContentType = reqHeaders.containsKey("Content-type");
            boolean hasXGroup = reqHeaders.containsKey("X-group");

            String json = String.format("{\n  \"hasContentType\": %b,\n  \"hasXGroup\": %b,\n  \"hasUnknown\": false\n}",
                    hasContentType, hasXGroup);

            sendResponse(exchange, 200, json, "application/json");
        }
    }

    // Задание 3
    private static String readBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        StringBuilder body = new StringBuilder();
        byte[] buffer = new byte[1024];
        int bytesRead;
        long totalBytes = 0;

        while ((bytesRead = is.read(buffer)) != -1) {
            totalBytes += bytesRead;
            if (totalBytes > 1048576) {
                return null;
            }
            body.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
        }

        System.out.println("Размер тела: " + totalBytes + " байт");
        return body.toString();
    }

    static class EchoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
                return;
            }

            String body = readBody(exchange);
            if (body == null) {
                sendResponse(exchange, 413, "413 Payload Too Large", "text/plain");
                return;
            }

            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null) contentType = "text/plain";

            if (contentType.contains("application/json") && body.trim().endsWith("}")) {
                body = body.substring(0, body.lastIndexOf("}")) + ", \"group\": \"477\"}";
            }

            sendResponse(exchange, 200, body, contentType);
        }
    }

    static class FormHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readBody(exchange);
            if (body == null) {
                sendResponse(exchange, 413, "413 Payload Too Large", "text/plain");
                return;
            }

            StringBuilder json = new StringBuilder("{\n");
            String[] pairs = body.split("&");
            for (int i = 0; i < pairs.length; i++) {
                String[] kv = pairs[i].split("=");
                String key = kv[0];
                String val = kv.length > 1 ? java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
                json.append(String.format("  \"%s\": \"%s\"", key, val));
                if (i < pairs.length - 1) json.append(",\n");
                else json.append("\n");
            }
            json.append("}");

            sendResponse(exchange, 200, json.toString(), "application/json");
        }
    }
}
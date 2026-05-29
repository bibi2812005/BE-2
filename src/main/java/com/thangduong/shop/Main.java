package com.thangduong.shop;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) throws IOException {
        int port = getPort();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/v1/user", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json; charset=UTF-8", "{\"code\":405,\"message\":\"Method Not Allowed\"}");
                return;
            }

            String response = "{"
                    + "\"code\":1000,"
                    + "\"result\":["
                    + "{\"id\":\"012\",\"username\":\"trongphat012\",\"fullname\":\"trongphat\",\"email\":\"trongphat@gmail.com\",\"roles\":[{\"name\":\"USER\",\"description\":\"The user is using the system\"}]},"
                    + "{\"id\":\"123\",\"username\":\"Biz7\",\"fullname\":\"TaCanh\",\"email\":\"tacanh@gmail.com\",\"roles\":[{\"name\":\"ADMIN\",\"description\":\"The admin is managing the system\"}]}"
                    + "]"
                    + "}";
            sendResponse(exchange, 200, "application/json; charset=UTF-8", response);
        });
        server.createContext("/users", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "text/plain; charset=UTF-8", "Method Not Allowed");
                return;
            }

            String response = "[{\"id\":1,\"name\":\"Thang Duong\"},{\"id\":2,\"name\":\"Anna Nguyen\"},{\"id\":3,\"name\":\"Minh Tran\"}]";
            sendResponse(exchange, 200, "application/json; charset=UTF-8", response);
        });
        server.createContext("/", exchange -> sendResponse(
                exchange,
                200,
                "text/plain; charset=UTF-8",
                "Hello from Render Java!"
        ));
        server.setExecutor(null);
        server.start();

        System.out.println("Server started on port " + port);
    }

    private static int getPort() {
        String portValue = System.getenv("PORT");
        if (portValue != null) {
            try {
                return Integer.parseInt(portValue);
            } catch (NumberFormatException ignored) {
            }
        }
        return 10000;
    }

    private static void sendResponse(com.sun.net.httpserver.HttpExchange exchange, int statusCode, String contentType, String response) throws IOException {
        byte[] body = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }
}

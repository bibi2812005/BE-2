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
        server.createContext("/v3/api-docs", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json; charset=UTF-8", "{\"code\":405,\"message\":\"Method Not Allowed\"}");
                return;
            }

            String host = exchange.getRequestHeaders().getFirst("Host");
            if (host == null || host.isBlank()) {
                host = "localhost:" + port;
            }
            String openApi = "{"
                    + "\"openapi\":\"3.0.3\","
                    + "\"info\":{\"title\":\"BE-2 API\",\"version\":\"1.0.0\"},"
                    + "\"servers\":[{\"url\":\"https://" + host + "\"}],"
                    + "\"paths\":{"
                    + "\"/\":{\"get\":{\"summary\":\"Health message\",\"responses\":{\"200\":{\"description\":\"OK\"}}}},"
                    + "\"/users\":{\"get\":{\"summary\":\"Simple users list\",\"responses\":{\"200\":{\"description\":\"OK\"}}}},"
                    + "\"/api/v1/user\":{\"get\":{\"summary\":\"Get my info users\",\"responses\":{\"200\":{\"description\":\"OK\"}}}}"
                    + "}"
                    + "}";
            sendResponse(exchange, 200, "application/json; charset=UTF-8", openApi);
        });
        server.createContext("/swagger-ui", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "text/plain; charset=UTF-8", "Method Not Allowed");
                return;
            }
            String host = exchange.getRequestHeaders().getFirst("Host");
            if (host == null || host.isBlank()) {
                host = "localhost:" + port;
            }
            String url = "https://" + host + "/v3/api-docs";
            String html = "<!doctype html><html><head><meta charset=\"UTF-8\"><title>Swagger UI</title>"
                    + "<link rel=\"stylesheet\" href=\"https://unpkg.com/swagger-ui-dist@5/swagger-ui.css\" />"
                    + "</head><body><div id=\"swagger-ui\"></div>"
                    + "<script src=\"https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js\"></script>"
                    + "<script>window.ui=SwaggerUIBundle({url:'" + url + "',dom_id:'#swagger-ui'});</script>"
                    + "</body></html>";
            sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
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

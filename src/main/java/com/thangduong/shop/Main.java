package com.thangduong.shop;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        int port = getPort();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/v1/user", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json; charset=UTF-8", "{\"code\":405,\"message\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String response = "{\"code\":1000,\"result\":" + fetchUsersJson() + "}";
                sendResponse(exchange, 200, "application/json; charset=UTF-8", response);
            } catch (Exception ex) {
                String error = "{\"code\":500,\"message\":\"" + escapeJson(ex.getMessage()) + "\"}";
                sendResponse(exchange, 500, "application/json; charset=UTF-8", error);
            }
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

    private static String fetchUsersJson() throws SQLException, URISyntaxException {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            throw new IllegalStateException("DATABASE_URL is missing");
        }

        ConnectionInfo connectionInfo = parseDatabaseUrl(databaseUrl);
        List<String> rows = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(connectionInfo.jdbcUrl, connectionInfo.username, connectionInfo.password);
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT * FROM users LIMIT 100")) {

            ResultSetMetaData md = rs.getMetaData();
            int columns = md.getColumnCount();
            List<String> columnNames = new ArrayList<>();
            for (int i = 1; i <= columns; i++) {
                columnNames.add(md.getColumnLabel(i).toLowerCase());
            }

            while (rs.next()) {
                String id = valueFrom(rs, columnNames, "id");
                String username = valueFrom(rs, columnNames, "username");
                String fullname = valueFrom(rs, columnNames, "fullname", "full_name", "name");
                String email = valueFrom(rs, columnNames, "email");

                String row = "{"
                        + "\"id\":\"" + escapeJson(id) + "\","
                        + "\"username\":\"" + escapeJson(username) + "\","
                        + "\"fullname\":\"" + escapeJson(fullname) + "\","
                        + "\"email\":\"" + escapeJson(email) + "\","
                        + "\"roles\":[]"
                        + "}";
                rows.add(row);
            }
        }

        return "[" + String.join(",", rows) + "]";
    }

    private static String valueFrom(ResultSet rs, List<String> columns, String... candidates) throws SQLException {
        for (String candidate : candidates) {
            for (String column : columns) {
                if (column.equalsIgnoreCase(candidate)) {
                    String value = rs.getString(column);
                    return value == null ? "" : value;
                }
            }
        }
        return "";
    }

    private static ConnectionInfo parseDatabaseUrl(String databaseUrl) throws URISyntaxException {
        URI uri = new URI(databaseUrl);
        String[] userInfo = uri.getUserInfo().split(":", 2);
        String username = userInfo[0];
        String password = userInfo.length > 1 ? userInfo[1] : "";
        String host = uri.getHost();
        int port = uri.getPort();
        String database = uri.getPath().replaceFirst("/", "");
        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        return new ConnectionInfo(jdbcUrl, username, password);
    }

    private static String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static class ConnectionInfo {
        private final String jdbcUrl;
        private final String username;
        private final String password;

        private ConnectionInfo(String jdbcUrl, String username, String password) {
            this.jdbcUrl = jdbcUrl;
            this.username = username;
            this.password = password;
        }
    }
}

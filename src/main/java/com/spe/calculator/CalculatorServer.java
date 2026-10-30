package com.spe.calculator;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple REST server for the calculator, using the JDK's built-in HttpServer (no frameworks).
 *
 * Endpoints:
 *   GET /api/v1/add?a=&b=
 *   GET /api/v1/subtract?a=&b=
 *   GET /api/v1/multiply?a=&b=
 *   GET /api/v1/divide?a=&b=
 *   GET /actuator/health
 */
public class CalculatorServer {

    private final Calculator calculator = new Calculator();
    private final HttpServer server;

    public CalculatorServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/v1/add", ex -> handle(ex, "add"));
        server.createContext("/api/v1/subtract", ex -> handle(ex, "subtract"));
        server.createContext("/api/v1/multiply", ex -> handle(ex, "multiply"));
        server.createContext("/api/v1/divide", ex -> handle(ex, "divide"));
        server.createContext("/actuator/health", ex -> send(ex, 200, "{\"status\":\"UP\"}"));
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    private void handle(HttpExchange ex, String operation) throws IOException {
        if (!"GET".equals(ex.getRequestMethod())) {
            send(ex, 405, error("Only GET is supported"));
            return;
        }
        Map<String, String> params = parseQuery(ex.getRequestURI().getRawQuery());
        double a;
        double b;
        try {
            a = parseNumber(params, "a");
            b = parseNumber(params, "b");
        } catch (IllegalArgumentException e) {
            send(ex, 400, error(e.getMessage()));
            return;
        }

        double result;
        try {
            switch (operation) {
                case "add" -> result = calculator.add(a, b);
                case "subtract" -> result = calculator.subtract(a, b);
                case "multiply" -> result = calculator.multiply(a, b);
                default -> result = calculator.divide(a, b);
            }
        } catch (ArithmeticException e) {
            send(ex, 400, error(e.getMessage()));
            return;
        }

        String json = "{\"operation\":\"" + operation + "\",\"a\":" + format(a)
                + ",\"b\":" + format(b) + ",\"result\":" + format(result) + "}";
        send(ex, 200, json);
    }

    private static double parseNumber(Map<String, String> params, String name) {
        String value = params.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing parameter '" + name + "'");
        }
        try {
            double d = Double.parseDouble(value);
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                throw new NumberFormatException();
            }
            return d;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter '" + name + "' must be a number");
        }
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null) {
            return params;
        }
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String value = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            params.put(key, value);
        }
        return params;
    }

    // Prints 5 instead of 5.0 for whole numbers
    private static String format(double d) {
        if (d == Math.rint(d) && Math.abs(d) < 1e15) {
            return String.valueOf((long) d);
        }
        return String.valueOf(d);
    }

    private static String error(String message) {
        return "{\"status\":400,\"error\":\"Bad Request\",\"message\":\"" + message + "\"}";
    }

    private static void send(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        CalculatorServer app = new CalculatorServer(port);
        app.start();
        System.out.println("Calculator API running on port " + port);
    }
}

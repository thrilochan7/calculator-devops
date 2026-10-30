package com.spe.calculator;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Integration test: starts the real server on a random port and calls the endpoints over HTTP. */
class CalculatorServerTest {

    private static CalculatorServer server;
    private static final HttpClient client = HttpClient.newHttpClient();

    @BeforeAll
    static void startServer() throws Exception {
        server = new CalculatorServer(0);
        server.start();
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                URI.create("http://localhost:" + server.getPort() + path)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void addEndpoint() throws Exception {
        HttpResponse<String> res = get("/api/v1/add?a=2&b=3");
        assertEquals(200, res.statusCode());
        assertEquals("{\"operation\":\"add\",\"a\":2,\"b\":3,\"result\":5}", res.body());
    }

    @Test
    void divideEndpoint() throws Exception {
        HttpResponse<String> res = get("/api/v1/divide?a=5&b=2");
        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("\"result\":2.5"));
    }

    @Test
    void divideByZeroReturns400() throws Exception {
        HttpResponse<String> res = get("/api/v1/divide?a=5&b=0");
        assertEquals(400, res.statusCode());
        assertTrue(res.body().contains("Division by zero"));
    }

    @Test
    void missingParameterReturns400() throws Exception {
        HttpResponse<String> res = get("/api/v1/add?a=5");
        assertEquals(400, res.statusCode());
        assertTrue(res.body().contains("Missing parameter 'b'"));
    }

    @Test
    void invalidNumberReturns400() throws Exception {
        HttpResponse<String> res = get("/api/v1/multiply?a=abc&b=2");
        assertEquals(400, res.statusCode());
        assertTrue(res.body().contains("must be a number"));
    }

    @Test
    void healthEndpoint() throws Exception {
        HttpResponse<String> res = get("/actuator/health");
        assertEquals(200, res.statusCode());
        assertEquals("{\"status\":\"UP\"}", res.body());
    }
}

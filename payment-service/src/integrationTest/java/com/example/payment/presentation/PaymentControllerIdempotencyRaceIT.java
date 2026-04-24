package com.example.payment.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.payment.AbstractIntegrationTest;
import com.example.payment.domain.port.EventPublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;

class PaymentControllerIdempotencyRaceIT extends AbstractIntegrationTest {

  @LocalServerPort private int port;

  @MockBean private EventPublisher eventPublisher;

  @BeforeEach
  void setUpRestAssured() {
    RestAssured.port = port;
    RestAssured.basePath = "";
  }

  @Test
  void shouldHandleConcurrentPostsWithSameIdempotencyKey() throws Exception {
    UUID idempotencyKey = UUID.randomUUID();
    UUID payerId = UUID.randomUUID();
    UUID payeeId = UUID.randomUUID();
    String jwt = JwtTestHelper.generateToken(UUID.randomUUID());
    String body =
        """
        {"amount":"100.00","currency":"BRL","payerId":"%s","payeeId":"%s"}
        """
            .formatted(payerId, payeeId);

    int threads = 10;
    ExecutorService exec = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Response>> futures = new ArrayList<>();

    for (int i = 0; i < threads; i++) {
      futures.add(
          exec.submit(
              () -> {
                start.await();
                return RestAssured.given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + jwt)
                    .header("Idempotency-Key", idempotencyKey.toString())
                    .body(body)
                    .post("/api/v1/payments");
              }));
    }

    start.countDown();
    exec.shutdown();
    exec.awaitTermination(30, TimeUnit.SECONDS);

    ObjectMapper mapper = new ObjectMapper();
    Set<String> uniqueIds = new HashSet<>();
    int successCount = 0;
    int errorCount = 0;

    for (Future<Response> f : futures) {
      Response r = f.get();
      int status = r.getStatusCode();
      if (status == 200 || status == 201) {
        successCount++;
        JsonNode json = mapper.readTree(r.getBody().asString());
        uniqueIds.add(json.get("id").asText());
      } else {
        errorCount++;
      }
    }

    assertThat(uniqueIds)
        .as("concurrent posts should produce exactly 1 unique payment id")
        .hasSize(1);
    assertThat(successCount + errorCount).isEqualTo(threads);
    assertThat(errorCount).as("no 5xx expected (idempotency should be race-safe)").isZero();
  }
}

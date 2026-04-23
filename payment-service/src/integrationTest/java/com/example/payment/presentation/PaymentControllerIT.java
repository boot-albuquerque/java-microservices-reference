package com.example.payment.presentation;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.example.payment.AbstractIntegrationTest;
import com.example.payment.domain.port.EventPublisher;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;

class PaymentControllerIT extends AbstractIntegrationTest {

  @LocalServerPort private int port;

  @MockBean private EventPublisher eventPublisher;

  @BeforeEach
  void setUpRestAssured() {
    RestAssured.port = port;
    RestAssured.basePath = "";
  }

  private static final UUID TEST_USER = UUID.randomUUID();

  private String validPayload() {
    return """
        {
          "amount": "100.00",
          "currency": "BRL",
          "payerId": "%s",
          "payeeId": "%s"
        }
        """
        .formatted(UUID.randomUUID(), UUID.randomUUID());
  }

  @Test
  void shouldReturn201WhenCreatingPayment() {
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", JwtTestHelper.bearerToken(TEST_USER))
        .header("Idempotency-Key", UUID.randomUUID().toString())
        .body(validPayload())
        .when()
        .post("/api/v1/payments")
        .then()
        .statusCode(201)
        .body("id", notNullValue())
        .body("status", equalTo("PENDING"));
  }

  @Test
  void shouldReturn200OnIdempotentRepeat() {
    String idempotencyKey = UUID.randomUUID().toString();
    String payload = validPayload();

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", JwtTestHelper.bearerToken(TEST_USER))
        .header("Idempotency-Key", idempotencyKey)
        .body(payload)
        .when()
        .post("/api/v1/payments")
        .then()
        .statusCode(201);

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", JwtTestHelper.bearerToken(TEST_USER))
        .header("Idempotency-Key", idempotencyKey)
        .body(payload)
        .when()
        .post("/api/v1/payments")
        .then()
        .statusCode(200)
        .body("id", notNullValue());
  }

  @Test
  void shouldReturn401WhenNoToken() {
    given()
        .contentType(ContentType.JSON)
        .header("Idempotency-Key", UUID.randomUUID().toString())
        .body(validPayload())
        .when()
        .post("/api/v1/payments")
        .then()
        .statusCode(401);
  }

  @Test
  void shouldReturn400WhenIdempotencyKeyMissing() {
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", JwtTestHelper.bearerToken(TEST_USER))
        .body(validPayload())
        .when()
        .post("/api/v1/payments")
        .then()
        .statusCode(400);
  }

  @Test
  void shouldReturn400WhenIdempotencyKeyNotUUID() {
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", JwtTestHelper.bearerToken(TEST_USER))
        .header("Idempotency-Key", "not-a-uuid")
        .body(validPayload())
        .when()
        .post("/api/v1/payments")
        .then()
        .statusCode(400);
  }

  @Test
  void shouldReturn400WhenBodyInvalid() {
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", JwtTestHelper.bearerToken(TEST_USER))
        .header("Idempotency-Key", UUID.randomUUID().toString())
        .body("{\"currency\": \"BRL\"}")
        .when()
        .post("/api/v1/payments")
        .then()
        .statusCode(400);
  }

  @Test
  void shouldReturn200WhenGettingExistingPayment() {
    String idempotencyKey = UUID.randomUUID().toString();

    String paymentId =
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", JwtTestHelper.bearerToken(TEST_USER))
            .header("Idempotency-Key", idempotencyKey)
            .body(validPayload())
            .when()
            .post("/api/v1/payments")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    given()
        .header("Authorization", JwtTestHelper.bearerToken(TEST_USER))
        .when()
        .get("/api/v1/payments/" + paymentId)
        .then()
        .statusCode(200)
        .body("id", equalTo(paymentId));
  }

  @Test
  void shouldReturn404WhenPaymentNotFound() {
    given()
        .header("Authorization", JwtTestHelper.bearerToken(TEST_USER))
        .when()
        .get("/api/v1/payments/" + UUID.randomUUID())
        .then()
        .statusCode(404);
  }
}

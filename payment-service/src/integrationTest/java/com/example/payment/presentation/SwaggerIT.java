package com.example.payment.presentation;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import com.example.payment.AbstractIntegrationTest;
import com.example.payment.domain.port.EventPublisher;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;

class SwaggerIT extends AbstractIntegrationTest {

  @LocalServerPort private int port;

  @MockBean private EventPublisher eventPublisher;

  @BeforeEach
  void setUp() {
    RestAssured.port = port;
    RestAssured.basePath = "";
  }

  @Test
  void shouldReturnOpenApiJsonWithoutAuthentication() {
    given()
        .when()
        .get("/v3/api-docs")
        .then()
        .statusCode(200)
        .contentType(containsString("application/json"))
        .body("openapi", containsString("3."));
  }

  @Test
  void shouldReturnOpenApiJsonWithAuthentication() {
    given()
        .header("Authorization", JwtTestHelper.bearerToken(java.util.UUID.randomUUID()))
        .when()
        .get("/v3/api-docs")
        .then()
        .statusCode(200)
        .body("info.title", equalTo("Payment Service API"));
  }

  @Test
  void shouldRejectWithoutJwt() {
    given().when().post("/api/v1/payments").then().statusCode(401);
  }
}

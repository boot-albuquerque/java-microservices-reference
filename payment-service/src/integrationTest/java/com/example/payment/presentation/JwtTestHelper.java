package com.example.payment.presentation;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;

public class JwtTestHelper {

  public static final String TEST_SECRET = "test-secret-for-integration-tests-min-32-bytes-xxxx";

  private static final SecretKey SIGNING_KEY =
      Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));

  public static String generateToken(UUID userId) {
    return Jwts.builder()
        .claim("userId", userId.toString())
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + 3_600_000))
        .signWith(SIGNING_KEY)
        .compact();
  }

  public static String bearerToken(UUID userId) {
    return "Bearer " + generateToken(userId);
  }
}

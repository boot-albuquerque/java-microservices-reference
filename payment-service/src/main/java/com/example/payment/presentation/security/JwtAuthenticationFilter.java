package com.example.payment.presentation.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

  private final SecretKey signingKey;

  public JwtAuthenticationFilter(String secret) {
    byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
    this.signingKey = Keys.hmacShaKeyFor(keyBytes);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = authHeader.substring(7);

    Claims claims;
    try {
      claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    } catch (JwtException e) {
      log.warn("JWT validation failed: {}", e.getMessage());
      SecurityContextHolder.clearContext();
      filterChain.doFilter(request, response);
      return;
    }

    UUID userId;
    try {
      userId = UUID.fromString(claims.get("userId", String.class));
    } catch (IllegalArgumentException | NullPointerException e) {
      log.warn("JWT 'userId' claim missing or not a valid UUID: {}", e.getMessage());
      SecurityContextHolder.clearContext();
      filterChain.doFilter(request, response);
      return;
    }

    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(
            userId, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(authentication);

    filterChain.doFilter(request, response);
  }
}

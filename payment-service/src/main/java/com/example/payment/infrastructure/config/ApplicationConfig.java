package com.example.payment.infrastructure.config;

import com.example.payment.application.usecase.CreatePaymentUseCase;
import com.example.payment.application.usecase.GetPaymentUseCase;
import com.example.payment.application.usecase.ProcessPaymentUseCase;
import com.example.payment.domain.port.EventPublisher;
import com.example.payment.domain.port.IdempotencyStore;
import com.example.payment.domain.port.PaymentRepository;
import com.example.payment.domain.port.RateLimiter;
import com.example.payment.presentation.filter.RateLimitFilter;
import java.time.Clock;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  public CreatePaymentUseCase createPaymentUseCase(
      PaymentRepository repository,
      EventPublisher eventPublisher,
      IdempotencyStore idempotencyStore,
      Clock clock) {
    return new CreatePaymentUseCase(repository, eventPublisher, idempotencyStore, clock);
  }

  @Bean
  public ProcessPaymentUseCase processPaymentUseCase(
      PaymentRepository repository, EventPublisher eventPublisher, Clock clock) {
    return new ProcessPaymentUseCase(repository, eventPublisher, clock);
  }

  @Bean
  public GetPaymentUseCase getPaymentUseCase(PaymentRepository repository) {
    return new GetPaymentUseCase(repository);
  }

  @Bean
  public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(
      RateLimiter rateLimiter) {
    FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(new RateLimitFilter(rateLimiter));
    registration.addUrlPatterns("/api/*");
    registration.setOrder(20);
    return registration;
  }
}

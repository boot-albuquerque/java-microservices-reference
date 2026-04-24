package com.example.payment.infrastructure.kafka;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.example.payment.AbstractIntegrationTest;
import com.example.payment.domain.event.PaymentCreated;
import com.example.payment.domain.port.EventPublisher;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.apache.avro.specific.SpecificRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;

class KafkaCircuitBreakerIT extends AbstractIntegrationTest {

  @MockBean private KafkaTemplate<String, SpecificRecord> avroKafkaTemplate;

  @Autowired private EventPublisher eventPublisher;

  @BeforeEach
  void setupKafkaFailure() {
    when(avroKafkaTemplate.send(anyString(), anyString(), any(SpecificRecord.class)))
        .thenReturn(
            CompletableFuture.failedFuture(new RuntimeException("kafka broker unavailable")));
  }

  @Test
  void shouldFallbackWhenKafkaFails() {
    PaymentCreated event =
        new PaymentCreated(
            UUID.randomUUID(),
            new BigDecimal("50.00"),
            "BRL",
            UUID.randomUUID(),
            UUID.randomUUID(),
            null,
            Instant.now());

    assertThatCode(() -> eventPublisher.publishCreated(event)).doesNotThrowAnyException();
  }
}

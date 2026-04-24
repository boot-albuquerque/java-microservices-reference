package com.example.payment.infrastructure.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.payment.AbstractIntegrationTest;
import com.example.payment.domain.event.PaymentCreated;
import com.example.payment.domain.event.PaymentProcessed;
import com.example.payment.domain.model.PaymentStatus;
import com.example.payment.domain.port.EventPublisher;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.apache.avro.specific.SpecificRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;

class KafkaPaymentEventPublisherIT extends AbstractIntegrationTest {

  @MockBean private KafkaTemplate<String, SpecificRecord> avroKafkaTemplate;

  @Autowired private EventPublisher eventPublisher;

  @Test
  void shouldPublishAvroEvent() {
    when(avroKafkaTemplate.send(anyString(), anyString(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(CompletableFuture.completedFuture(null));

    UUID paymentId = UUID.randomUUID();
    assertThatNoException()
        .isThrownBy(
            () ->
                eventPublisher.publishCreated(
                    new PaymentCreated(
                        paymentId,
                        new BigDecimal("100.00"),
                        "BRL",
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        Instant.now())));

    ArgumentCaptor<SpecificRecord> captor = ArgumentCaptor.forClass(SpecificRecord.class);
    verify(avroKafkaTemplate).send(anyString(), eq(paymentId.toString()), captor.capture());
    assertThat(captor.getValue()).isInstanceOf(com.example.payment.avro.PaymentCreated.class);
  }

  @Test
  void shouldPublishPaymentProcessedEvent() {
    when(avroKafkaTemplate.send(anyString(), anyString(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(CompletableFuture.completedFuture(null));

    UUID paymentId = UUID.randomUUID();
    assertThatNoException()
        .isThrownBy(
            () ->
                eventPublisher.publishProcessed(
                    new PaymentProcessed(paymentId, PaymentStatus.COMPLETED, Instant.now())));

    ArgumentCaptor<SpecificRecord> captor = ArgumentCaptor.forClass(SpecificRecord.class);
    verify(avroKafkaTemplate).send(anyString(), eq(paymentId.toString()), captor.capture());
    com.example.payment.avro.PaymentProcessed avroEvent =
        (com.example.payment.avro.PaymentProcessed) captor.getValue();
    assertThat(avroEvent.getStatus()).isEqualTo(com.example.payment.avro.PaymentStatus.COMPLETED);
  }
}

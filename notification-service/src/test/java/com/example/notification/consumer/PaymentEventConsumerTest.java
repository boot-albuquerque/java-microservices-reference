package com.example.notification.consumer;

import static org.mockito.Mockito.verify;

import com.example.notification.metrics.NotificationMetrics;
import com.example.payment.avro.PaymentCreated;
import com.example.payment.avro.PaymentProcessed;
import com.example.payment.avro.PaymentStatus;
import java.time.Instant;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentEventConsumerTest {

  @Mock private NotificationMetrics metrics;

  @InjectMocks private PaymentEventConsumer consumer;

  @Test
  void shouldIncrementMetricWhenPaymentCreatedReceived() {
    var event =
        PaymentCreated.newBuilder()
            .setPaymentId(UUID.randomUUID())
            .setAmount("100.00")
            .setCurrency("BRL")
            .setPayerId(UUID.randomUUID())
            .setPayeeId(UUID.randomUUID())
            .setTimestamp(Instant.now())
            .build();

    var record =
        new ConsumerRecord<String, org.apache.avro.specific.SpecificRecord>(
            "payment.events", 0, 0L, "key", event);

    consumer.handle(record);

    verify(metrics).recordReceived("PaymentCreated");
  }

  @Test
  void shouldIncrementMetricWhenPaymentProcessedReceived() {
    var event =
        PaymentProcessed.newBuilder()
            .setPaymentId(UUID.randomUUID())
            .setStatus(PaymentStatus.COMPLETED)
            .setTimestamp(Instant.now())
            .build();

    var record =
        new ConsumerRecord<String, org.apache.avro.specific.SpecificRecord>(
            "payment.events", 0, 0L, "key", event);

    consumer.handle(record);

    verify(metrics).recordReceived("PaymentProcessed");
  }

  @Test
  void shouldClearMdcAfterHandling() {
    var event =
        PaymentCreated.newBuilder()
            .setPaymentId(UUID.randomUUID())
            .setAmount("50.00")
            .setCurrency("USD")
            .setPayerId(UUID.randomUUID())
            .setPayeeId(UUID.randomUUID())
            .setTimestamp(Instant.now())
            .build();

    var record =
        new ConsumerRecord<String, org.apache.avro.specific.SpecificRecord>(
            "payment.events", 0, 0L, "key", event);

    consumer.handle(record);

    org.junit.jupiter.api.Assertions.assertNull(org.slf4j.MDC.get("paymentId"));
    org.junit.jupiter.api.Assertions.assertNull(org.slf4j.MDC.get("eventType"));
  }
}

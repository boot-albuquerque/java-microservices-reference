package com.example.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.notification.consumer.PaymentEventConsumer;
import com.example.notification.metrics.NotificationMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
      "spring.kafka.consumer.properties.schema.registry.url=mock://test"
    })
@EmbeddedKafka(
    partitions = 1,
    topics = {"payment.events", "payment.events.DLT"})
class NotificationContextIT {

  @Autowired private PaymentEventConsumer paymentEventConsumer;

  @Autowired private NotificationMetrics notificationMetrics;

  @Autowired private MeterRegistry meterRegistry;

  @Test
  void contextLoads() {
    assertThat(paymentEventConsumer).isNotNull();
    assertThat(notificationMetrics).isNotNull();
  }

  @Test
  void metricsCounterIsRegisteredAfterInvocation() {
    notificationMetrics.recordReceived("PaymentCreated");

    var counter =
        meterRegistry.find("notifications.received").tag("type", "PaymentCreated").counter();
    assertThat(counter).isNotNull();
    assertThat(counter.count()).isGreaterThan(0.0);
  }
}

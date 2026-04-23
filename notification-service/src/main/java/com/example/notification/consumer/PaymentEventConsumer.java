package com.example.notification.consumer;

import com.example.notification.metrics.NotificationMetrics;
import com.example.payment.avro.PaymentCreated;
import com.example.payment.avro.PaymentProcessed;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {

  private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

  private final NotificationMetrics metrics;

  public PaymentEventConsumer(NotificationMetrics metrics) {
    this.metrics = metrics;
  }

  @KafkaListener(
      topics = "payment.events",
      groupId = "notification-group",
      containerFactory = "kafkaListenerContainerFactory")
  public void handle(ConsumerRecord<String, SpecificRecord> record) {
    SpecificRecord event = record.value();
    try {
      MDC.put("paymentId", extractPaymentId(event));
      MDC.put("eventType", event.getClass().getSimpleName());
      MDC.put(
          "traceId",
          Optional.ofNullable(record.headers().lastHeader("traceparent"))
              .map(h -> new String(h.value(), StandardCharsets.UTF_8))
              .orElse(""));
      log.info("Received payment event");
      metrics.recordReceived(event.getClass().getSimpleName());
    } finally {
      MDC.clear();
    }
  }

  private String extractPaymentId(SpecificRecord event) {
    if (event instanceof PaymentCreated e) return e.getPaymentId().toString();
    if (event instanceof PaymentProcessed e) return e.getPaymentId().toString();
    return "unknown";
  }
}

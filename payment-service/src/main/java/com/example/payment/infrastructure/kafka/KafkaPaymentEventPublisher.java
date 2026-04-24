package com.example.payment.infrastructure.kafka;

import com.example.payment.domain.event.PaymentCreated;
import com.example.payment.domain.event.PaymentProcessed;
import com.example.payment.domain.port.EventPublisher;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.apache.avro.specific.SpecificRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaPaymentEventPublisher implements EventPublisher {

  private static final Logger log = LoggerFactory.getLogger(KafkaPaymentEventPublisher.class);

  private final KafkaTemplate<String, SpecificRecord> kafkaTemplate;
  private final String topic;

  public KafkaPaymentEventPublisher(
      KafkaTemplate<String, SpecificRecord> kafkaTemplate,
      @Value("${app.kafka.topic:payment.events}") String topic) {
    this.kafkaTemplate = kafkaTemplate;
    this.topic = topic;
  }

  @Override
  @CircuitBreaker(name = "kafka-publisher", fallbackMethod = "publishCreatedFallback")
  @Retry(name = "kafka-publisher", fallbackMethod = "publishCreatedFallback")
  public void publishCreated(PaymentCreated event) {
    com.example.payment.avro.PaymentCreated avroEvent =
        com.example.payment.avro.PaymentCreated.newBuilder()
            .setPaymentId(event.paymentId())
            .setAmount(event.amount().toPlainString())
            .setCurrency(event.currency())
            .setPayerId(event.payerId())
            .setPayeeId(event.payeeId())
            .setExternalReference(event.externalReference())
            .setTimestamp(event.timestamp())
            .build();
    kafkaTemplate.send(topic, event.paymentId().toString(), avroEvent);
  }

  @Override
  @CircuitBreaker(name = "kafka-publisher", fallbackMethod = "publishProcessedFallback")
  @Retry(name = "kafka-publisher", fallbackMethod = "publishProcessedFallback")
  public void publishProcessed(PaymentProcessed event) {
    com.example.payment.avro.PaymentStatus avroStatus =
        event.status().name().equals("COMPLETED")
            ? com.example.payment.avro.PaymentStatus.COMPLETED
            : com.example.payment.avro.PaymentStatus.FAILED;

    com.example.payment.avro.PaymentProcessed avroEvent =
        com.example.payment.avro.PaymentProcessed.newBuilder()
            .setPaymentId(event.paymentId())
            .setStatus(avroStatus)
            .setTimestamp(event.timestamp())
            .build();
    kafkaTemplate.send(topic, event.paymentId().toString(), avroEvent);
  }

  void publishCreatedFallback(PaymentCreated event, Throwable t) {
    log.error("Kafka publishCreated fallback — paymentId={}", event.paymentId(), t);
  }

  void publishProcessedFallback(PaymentProcessed event, Throwable t) {
    log.error("Kafka publishProcessed fallback — paymentId={}", event.paymentId(), t);
  }
}

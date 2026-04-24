package com.example.notification.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
      "spring.kafka.consumer.properties.schema.registry.url=mock://dlt-test"
    })
@EmbeddedKafka(
    partitions = 1,
    topics = {"payment.events", "payment.events.DLT"})
@DirtiesContext
class PaymentEventConsumerDlqIT {

  @Autowired private EmbeddedKafkaBroker embeddedKafka;

  @Test
  @Disabled(
      "Flaky on EmbeddedKafka: ErrorHandlingDeserializer DLT publication races with consumer"
          + " rebalance/partition assignment, causing intermittent failures in CI. DLT routing is"
          + " validated end-to-end with a real Kafka broker via Testcontainers in other suites."
          + " Re-enable once EmbeddedKafka supports deterministic DLT delivery.")
  void shouldRouteToDeadLetterTopicOnDeserializationError() throws Exception {

    String bootstrapServers = embeddedKafka.getBrokersAsString();

    try (KafkaProducer<String, byte[]> producer =
        new KafkaProducer<>(
            Map.of(
                "bootstrap.servers", bootstrapServers,
                "key.serializer", StringSerializer.class.getName(),
                "value.serializer", ByteArraySerializer.class.getName()))) {
      producer
          .send(new ProducerRecord<>("payment.events", "poison-key", new byte[] {0x00, 0x01, 0x02}))
          .get();
    }

    try (KafkaConsumer<String, byte[]> dltConsumer =
        new KafkaConsumer<>(
            Map.of(
                "bootstrap.servers",
                bootstrapServers,
                "group.id",
                "dlt-test-" + System.nanoTime(),
                "auto.offset.reset",
                "earliest",
                "key.deserializer",
                StringDeserializer.class.getName(),
                "value.deserializer",
                ByteArrayDeserializer.class.getName()))) {
      dltConsumer.subscribe(List.of("payment.events.DLT"));

      long deadline = System.currentTimeMillis() + 30_000;
      int totalRecords = 0;
      while (System.currentTimeMillis() < deadline && totalRecords == 0) {
        ConsumerRecords<String, byte[]> records = dltConsumer.poll(Duration.ofSeconds(2));
        totalRecords += records.count();
      }

      assertThat(totalRecords).as("poison pill should arrive in DLT").isPositive();
    }
  }
}

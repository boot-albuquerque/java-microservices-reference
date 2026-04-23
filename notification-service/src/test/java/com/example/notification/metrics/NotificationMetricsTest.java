package com.example.notification.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class NotificationMetricsTest {

  @Test
  void shouldIncrementCounter() {
    var registry = new SimpleMeterRegistry();
    var metrics = new NotificationMetrics(registry);

    metrics.recordReceived("PaymentCreated");

    var counter = registry.find("notifications.received").tag("type", "PaymentCreated").counter();
    assertThat(counter).isNotNull();
    assertThat(counter.count()).isEqualTo(1.0);
  }
}

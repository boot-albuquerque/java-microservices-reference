package com.example.notification.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class NotificationMetrics {

  private final MeterRegistry registry;

  public NotificationMetrics(MeterRegistry registry) {
    this.registry = registry;
  }

  public void recordReceived(String eventType) {
    registry.counter("notifications.received", "type", eventType).increment();
  }
}

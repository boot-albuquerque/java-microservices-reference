package com.example.payment.infrastructure.metrics;

import com.example.payment.application.port.MetricsRecorder;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MicrometerMetricsRecorder implements MetricsRecorder {

  private final MeterRegistry registry;

  public MicrometerMetricsRecorder(MeterRegistry registry) {
    this.registry = registry;
  }

  @Override
  public void recordPaymentCreated() {
    registry.counter("payments.created").increment();
  }

  @Override
  public void recordPaymentProcessed(String status) {
    registry.counter("payments.processed", "status", status).increment();
  }
}

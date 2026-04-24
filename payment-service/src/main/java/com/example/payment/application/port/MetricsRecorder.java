package com.example.payment.application.port;

public interface MetricsRecorder {
  void recordPaymentCreated();

  void recordPaymentProcessed(String status);
}

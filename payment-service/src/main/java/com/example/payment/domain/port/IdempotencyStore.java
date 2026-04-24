package com.example.payment.domain.port;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyStore {

  Optional<UUID> find(UUID idempotencyKey);

  void store(UUID idempotencyKey, UUID paymentId);

  /** Atomically stores only if absent. Returns true if stored, false if key already existed. */
  boolean storeIfAbsent(UUID idempotencyKey, UUID paymentId);
}

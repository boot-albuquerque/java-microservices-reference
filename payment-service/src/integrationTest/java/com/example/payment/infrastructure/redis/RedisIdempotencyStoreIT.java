package com.example.payment.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.payment.AbstractIntegrationTest;
import com.example.payment.domain.port.IdempotencyStore;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RedisIdempotencyStoreIT extends AbstractIntegrationTest {

  @Autowired private IdempotencyStore idempotencyStore;

  @Test
  void shouldReturnEmptyForUnknownKey() {
    Optional<UUID> result = idempotencyStore.find(UUID.randomUUID());
    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnExisting() {
    UUID idempotencyKey = UUID.randomUUID();
    UUID paymentId = UUID.randomUUID();

    idempotencyStore.store(idempotencyKey, paymentId);

    Optional<UUID> found = idempotencyStore.find(idempotencyKey);
    assertThat(found).contains(paymentId);
  }

  @Test
  void shouldStoreAndRetrieveMultipleKeys() {
    UUID key1 = UUID.randomUUID();
    UUID key2 = UUID.randomUUID();
    UUID pid1 = UUID.randomUUID();
    UUID pid2 = UUID.randomUUID();

    idempotencyStore.store(key1, pid1);
    idempotencyStore.store(key2, pid2);

    assertThat(idempotencyStore.find(key1)).contains(pid1);
    assertThat(idempotencyStore.find(key2)).contains(pid2);
  }
}

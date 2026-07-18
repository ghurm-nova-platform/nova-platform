package ai.nova.platform.modelgateway.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import ai.nova.platform.modelgateway.config.ModelGatewayProperties;

class ProviderConcurrencyManagerTest {

    @Test
    void limitsConcurrentPermits() throws Exception {
        ModelGatewayProperties properties = new ModelGatewayProperties();
        properties.setMaxConcurrentRequestsPerProvider(1);
        ProviderConcurrencyManager manager = new ProviderConcurrencyManager(properties);
        UUID providerId = UUID.randomUUID();

        AtomicInteger concurrent = new AtomicInteger(0);
        AtomicInteger maxSeen = new AtomicInteger(0);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        Thread holder = new Thread(() -> {
            try (ProviderConcurrencyManager.Permit permit = manager.acquire(providerId, 5)) {
                started.countDown();
                release.await(5, TimeUnit.SECONDS);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        holder.start();
        started.await(5, TimeUnit.SECONDS);

        Thread challenger = new Thread(() -> {
            try {
                if (manager.tryAcquire(providerId, 5, 200)) {
                    int value = concurrent.incrementAndGet();
                    maxSeen.updateAndGet(current -> Math.max(current, value));
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
        challenger.start();
        challenger.join(1000);
        assertThat(maxSeen.get()).isLessThanOrEqualTo(1);

        release.countDown();
        holder.join(5000);
    }
}

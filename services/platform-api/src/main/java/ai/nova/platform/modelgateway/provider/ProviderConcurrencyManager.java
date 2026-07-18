package ai.nova.platform.modelgateway.provider;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import ai.nova.platform.modelgateway.config.ModelGatewayProperties;

@Component
public class ProviderConcurrencyManager {

    private final ModelGatewayProperties properties;
    private final Map<UUID, Semaphore> semaphores = new ConcurrentHashMap<>();

    public ProviderConcurrencyManager(ModelGatewayProperties properties) {
        this.properties = properties;
    }

    public Permit acquire(UUID providerId, int providerLimit) throws InterruptedException {
        int limit = Math.min(providerLimit, properties.getMaxConcurrentRequestsPerProvider());
        Semaphore semaphore = semaphores.computeIfAbsent(providerId, ignored -> new Semaphore(limit, true));
        semaphore.acquire();
        return new Permit(semaphore);
    }

    public boolean tryAcquire(UUID providerId, int providerLimit, long timeoutMs) throws InterruptedException {
        int limit = Math.min(providerLimit, properties.getMaxConcurrentRequestsPerProvider());
        Semaphore semaphore = semaphores.computeIfAbsent(providerId, ignored -> new Semaphore(limit, true));
        if (semaphore.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)) {
            return true;
        }
        return false;
    }

    public static final class Permit implements AutoCloseable {
        private final Semaphore semaphore;
        private boolean released;

        private Permit(Semaphore semaphore) {
            this.semaphore = semaphore;
        }

        @Override
        public void close() {
            if (!released) {
                semaphore.release();
                released = true;
            }
        }
    }
}

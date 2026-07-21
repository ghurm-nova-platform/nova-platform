package ai.nova.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import ai.nova.platform.auth.AuthDtos.TokenResponse;
import ai.nova.platform.identity.service.AuthenticationService;

/**
 * Concurrent login throughput smoke tests.
 *
 * <p>Default CI runs 25 concurrent logins. Scaled variants (100/500/1000) are the same pattern:
 * raise {@code concurrency} and optionally gate with {@code @Tag("performance")} /
 * {@code @EnabledIfSystemProperty(named = "identity.perf", matches = "true")}.
 */
@SpringBootTest
class IdentityPerformanceTest {

    private static final String EMAIL = "admin@nova.local";
    private static final String PASSWORD = "ChangeMe123!";
    private static final long TIMEOUT_SECONDS = 120;
    private static final int MAX_ATTEMPTS = 40;

    @Autowired
    private AuthenticationService authenticationService;

    @DynamicPropertySource
    static void enlargePool(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "40");
        registry.add("nova.audit.capture-security-events", () -> "false");
    }

    @Test
    void concurrentLoginsDefaultCi() throws Exception {
        assertConcurrentLoginsSucceed(25);
    }

    @Test
    @Tag("slow")
    @Tag("performance")
    @EnabledIfSystemProperty(named = "identity.perf", matches = "true")
    void concurrentLoginsScaled100() throws Exception {
        // Scaled variant of concurrentLoginsDefaultCi — same assertion, higher concurrency.
        assertConcurrentLoginsSucceed(100);
    }

    private void assertConcurrentLoginsSucceed(int concurrency) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>(concurrency);

        try {
            for (int i = 0; i < concurrency; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    TokenResponse tokens = loginWithRetry();
                    assertThat(tokens.accessToken()).isNotBlank();
                    successes.incrementAndGet();
                    return null;
                }));
            }

            start.countDown();
            for (Future<?> future : futures) {
                future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
            assertThat(successes.get()).isEqualTo(concurrency);
        } finally {
            executor.shutdownNow();
        }
    }

    private TokenResponse loginWithRetry() throws InterruptedException {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return authenticationService.login(
                        EMAIL, PASSWORD, null, "127.0.0.1", "IdentityPerformanceTest");
            } catch (RuntimeException ex) {
                if (!isRetryable(ex)) {
                    throw ex;
                }
                last = ex;
                Thread.sleep(Math.min(200L, 10L * attempt));
            }
        }
        throw last != null ? last : new IllegalStateException("login retries exhausted");
    }

    private static boolean isRetryable(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof DataAccessException) {
                return true;
            }
            String name = current.getClass().getName();
            if (name.contains("OptimisticLock")
                    || name.contains("StaleObject")
                    || name.contains("ConstraintViolation")
                    || (current.getMessage() != null
                            && current.getMessage().contains("Connection is not available"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}

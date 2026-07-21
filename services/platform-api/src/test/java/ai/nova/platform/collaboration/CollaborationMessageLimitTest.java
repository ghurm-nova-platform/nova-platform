package ai.nova.platform.collaboration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.collaboration.dto.CollaborationDtos.SessionDetail;
import ai.nova.platform.collaboration.service.CollaborationService;
import ai.nova.platform.collaboration.support.CollaborationTestFixture;
import ai.nova.platform.web.error.ApiException;

@SpringBootTest(
        properties = {
            "nova.collaboration.enabled=true",
            "nova.audit.enabled=true",
            "nova.collaboration.max-messages=2"
        })
class CollaborationMessageLimitTest {

    @Autowired
    private CollaborationService collaborationService;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
    }

    @Test
    void messagesBelowLimitAreAccepted() {
        SessionDetail session = createSession();

        collaborationService.sendMessage(
                session.id(), CollaborationTestFixture.infoMessage("first"), CollaborationTestFixture.collaborationWriteUser());

        SessionDetail updated = collaborationService.get(session.id(), CollaborationTestFixture.collaborationReadUser());
        assertThat(updated.messages()).hasSize(1);
    }

    @Test
    void finalAllowedMessageIsAccepted() {
        SessionDetail session = createSession();

        collaborationService.sendMessage(
                session.id(), CollaborationTestFixture.infoMessage("first"), CollaborationTestFixture.collaborationWriteUser());
        collaborationService.sendMessage(
                session.id(), CollaborationTestFixture.infoMessage("second"), CollaborationTestFixture.collaborationWriteUser());

        SessionDetail updated = collaborationService.get(session.id(), CollaborationTestFixture.collaborationReadUser());
        assertThat(updated.messages()).hasSize(2);
    }

    @Test
    void nextMessageIsRejectedWhenLimitReached() {
        SessionDetail session = createSession();
        collaborationService.sendMessage(
                session.id(), CollaborationTestFixture.infoMessage("first"), CollaborationTestFixture.collaborationWriteUser());
        collaborationService.sendMessage(
                session.id(), CollaborationTestFixture.infoMessage("second"), CollaborationTestFixture.collaborationWriteUser());

        assertThatThrownBy(() -> collaborationService.sendMessage(
                        session.id(),
                        CollaborationTestFixture.infoMessage("third"),
                        CollaborationTestFixture.collaborationWriteUser()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(api.getCode()).isEqualTo("COLLABORATION_MESSAGE_LIMIT_REACHED");
                });
    }

    @Test
    void messageCountsAreIsolatedPerSession() {
        SessionDetail sessionA = createSession();
        SessionDetail sessionB = createSession();

        collaborationService.sendMessage(
                sessionA.id(), CollaborationTestFixture.infoMessage("a1"), CollaborationTestFixture.collaborationWriteUser());
        collaborationService.sendMessage(
                sessionA.id(), CollaborationTestFixture.infoMessage("a2"), CollaborationTestFixture.collaborationWriteUser());
        collaborationService.sendMessage(
                sessionB.id(), CollaborationTestFixture.infoMessage("b1"), CollaborationTestFixture.collaborationWriteUser());

        SessionDetail updatedB = collaborationService.get(sessionB.id(), CollaborationTestFixture.collaborationReadUser());
        assertThat(updatedB.messages()).hasSize(1);
    }

    @Test
    void concurrentMessageInsertsDoNotExceedLimitSignificantly() throws Exception {
        SessionDetail session = createSession();
        collaborationService.sendMessage(
                session.id(), CollaborationTestFixture.infoMessage("seed"), CollaborationTestFixture.collaborationWriteUser());

        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(3);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService pool = Executors.newFixedThreadPool(3);
        try {
            Future<?>[] futures = new Future[3];
            for (int i = 0; i < 3; i++) {
                final int index = i;
                futures[i] = pool.submit(() -> sendConcurrent(session.id(), "msg-" + index, ready, start, accepted, rejected));
            }
            ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(accepted.get()).isLessThanOrEqualTo(1);
        assertThat(rejected.get()).isGreaterThanOrEqualTo(2);

        SessionDetail updated = collaborationService.get(session.id(), CollaborationTestFixture.collaborationReadUser());
        assertThat(updated.messages()).hasSizeLessThanOrEqualTo(2);
    }

    private void sendConcurrent(
            UUID sessionId,
            String content,
            CountDownLatch ready,
            CountDownLatch start,
            AtomicInteger accepted,
            AtomicInteger rejected) {
        ready.countDown();
        try {
            start.await(5, TimeUnit.SECONDS);
            collaborationService.sendMessage(
                    sessionId, CollaborationTestFixture.infoMessage(content), CollaborationTestFixture.collaborationWriteUser());
            accepted.incrementAndGet();
        } catch (ApiException ex) {
            if ("COLLABORATION_MESSAGE_LIMIT_REACHED".equals(ex.getCode())) {
                rejected.incrementAndGet();
            } else {
                throw ex;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ex);
        }
    }

    private SessionDetail createSession() {
        return collaborationService.create(
                CollaborationTestFixture.sampleCreateSessionRequest(CollaborationTestFixture.uniqueName("messages")),
                CollaborationTestFixture.collaborationWriteUser());
    }
}

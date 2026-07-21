package ai.nova.platform.collaboration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import ai.nova.platform.collaboration.dto.CollaborationDtos.AssignTaskRequest;
import ai.nova.platform.collaboration.dto.CollaborationDtos.RecordDecisionRequest;
import ai.nova.platform.collaboration.dto.CollaborationDtos.SessionDetail;
import ai.nova.platform.collaboration.dto.CollaborationDtos.TaskAction;
import ai.nova.platform.collaboration.entity.CollaborationDecisionType;
import ai.nova.platform.collaboration.entity.CollaborationSessionStatus;
import ai.nova.platform.collaboration.entity.CollaborationTaskStatus;
import ai.nova.platform.collaboration.service.CollaborationPersistenceSupport;
import ai.nova.platform.collaboration.service.CollaborationService;
import ai.nova.platform.collaboration.support.CollaborationTestFixture;
import ai.nova.platform.web.error.ApiException;

@SpringBootTest(properties = {"nova.collaboration.enabled=true", "nova.audit.enabled=true"})
class CollaborationConcurrencyTest {

    @Autowired
    private CollaborationService collaborationService;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
    }

    @Test
    void concurrentAssignmentOfSameTaskAllowsOnlyOneWinner() throws Exception {
        SessionDetail session = createSession();
        UUID taskId = session.tasks().getFirst().id();
        UUID codingParticipant = participantId(session, "CODING");
        UUID reviewParticipant = participantId(session, "REVIEW");

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = pool.submit(() -> runAssign(
                    session.id(), taskId, codingParticipant, ready, start, successCount, conflictCount));
            Future<?> second = pool.submit(() -> runAssign(
                    session.id(), taskId, reviewParticipant, ready, start, successCount, conflictCount));

            ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            first.get(30, TimeUnit.SECONDS);
            second.get(30, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);

        SessionDetail finalState =
                collaborationService.get(session.id(), CollaborationTestFixture.collaborationReadUser());
        assertThat(finalState.tasks().stream().filter(t -> t.id().equals(taskId)).findFirst().orElseThrow().status())
                .isEqualTo(CollaborationTaskStatus.ASSIGNED);
    }

    @Test
    void concurrentCompleteAndReassignProducesSingleFinalTaskState() throws Exception {
        SessionDetail session = createSession();
        UUID taskId = session.tasks().getFirst().id();
        UUID codingParticipant = participantId(session, "CODING");
        UUID reviewParticipant = participantId(session, "REVIEW");

        assign(session.id(), taskId, codingParticipant);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> complete = pool.submit(() -> runAction(
                    session.id(),
                    new AssignTaskRequest(taskId, TaskAction.COMPLETE, codingParticipant, null, null, null, null, null),
                    ready,
                    start,
                    successCount,
                    conflictCount));
            Future<?> reassign = pool.submit(() -> runAction(
                    session.id(),
                    new AssignTaskRequest(
                            taskId, TaskAction.REASSIGN, null, null, reviewParticipant, null, null, null),
                    ready,
                    start,
                    successCount,
                    conflictCount));

            ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            complete.get(30, TimeUnit.SECONDS);
            reassign.get(30, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);

        SessionDetail finalState =
                collaborationService.get(session.id(), CollaborationTestFixture.collaborationReadUser());
        CollaborationTaskStatus status = finalState.tasks().stream()
                .filter(t -> t.id().equals(taskId))
                .findFirst()
                .orElseThrow()
                .status();
        assertThat(status).isIn(CollaborationTaskStatus.COMPLETED, CollaborationTaskStatus.ASSIGNED);
    }

    @Test
    void concurrentPauseAndCancelLeavesTerminalSessionState() throws Exception {
        SessionDetail session = createSession();
        UUID taskId = session.tasks().getFirst().id();
        UUID codingParticipant = participantId(session, "CODING");
        assign(session.id(), taskId, codingParticipant);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> pause = pool.submit(() -> runAdmin(session.id(), "pause", ready, start, successCount, conflictCount));
            Future<?> cancel = pool.submit(() -> runAdmin(session.id(), "cancel", ready, start, successCount, conflictCount));

            ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            pause.get(30, TimeUnit.SECONDS);
            cancel.get(30, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);

        SessionDetail finalState =
                collaborationService.get(session.id(), CollaborationTestFixture.collaborationReadUser());
        assertThat(finalState.status()).isIn(CollaborationSessionStatus.WAITING, CollaborationSessionStatus.CANCELLED);
    }

    @Test
    void concurrentConflictResolutionAllowsSingleSuccessfulTransition() throws Exception {
        SessionDetail session = createConflictSession();
        UUID taskA = taskId(session, "conflict-a");
        UUID taskB = taskId(session, "conflict-b");
        UUID codingParticipant = participantId(session, "CODING");
        UUID reviewParticipant = participantId(session, "REVIEW");

        assign(session.id(), taskA, codingParticipant, "shared/file.java");
        assign(session.id(), taskB, reviewParticipant, "shared/file.java");

        SessionDetail conflicted =
                collaborationService.get(session.id(), CollaborationTestFixture.collaborationReadUser());
        assertThat(conflicted.conflictDetected()).isTrue();

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> resolve = pool.submit(() -> runDecision(
                    session.id(),
                    new RecordDecisionRequest(CollaborationDecisionType.RESOLVE_CONFLICT, "Resolved", null, null),
                    ready,
                    start,
                    successCount,
                    conflictCount));
            Future<?> pause = pool.submit(() -> runAdmin(session.id(), "pause", ready, start, successCount, conflictCount));

            ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            resolve.get(30, TimeUnit.SECONDS);
            pause.get(30, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);
    }

    private void runAssign(
            UUID sessionId,
            UUID taskId,
            UUID participantId,
            CountDownLatch ready,
            CountDownLatch start,
            AtomicInteger successCount,
            AtomicInteger conflictCount) {
        runAction(
                sessionId,
                new AssignTaskRequest(taskId, TaskAction.ASSIGN, participantId, null, null, null, null, null),
                ready,
                start,
                successCount,
                conflictCount);
    }

    private void runAction(
            UUID sessionId,
            AssignTaskRequest request,
            CountDownLatch ready,
            CountDownLatch start,
            AtomicInteger successCount,
            AtomicInteger conflictCount) {
        ready.countDown();
        try {
            start.await(5, TimeUnit.SECONDS);
            collaborationService.assign(sessionId, request, CollaborationTestFixture.collaborationWriteUser());
            successCount.incrementAndGet();
        } catch (ApiException ex) {
            if (HttpStatus.CONFLICT.equals(ex.getStatus())
                    && (CollaborationPersistenceSupport.CONCURRENT_MODIFICATION_CODE.equals(ex.getCode())
                            || "COLLABORATION_TASK_INVALID_STATUS".equals(ex.getCode())
                            || "COLLABORATION_PARTICIPANT_BUSY".equals(ex.getCode()))) {
                conflictCount.incrementAndGet();
            } else {
                throw ex;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ex);
        }
    }

    private void runAdmin(
            UUID sessionId,
            String action,
            CountDownLatch ready,
            CountDownLatch start,
            AtomicInteger successCount,
            AtomicInteger conflictCount) {
        ready.countDown();
        try {
            start.await(5, TimeUnit.SECONDS);
            if ("pause".equals(action)) {
                collaborationService.pause(sessionId, CollaborationTestFixture.collaborationAdminUser());
            } else {
                collaborationService.cancel(sessionId, CollaborationTestFixture.collaborationAdminUser());
            }
            successCount.incrementAndGet();
        } catch (ApiException ex) {
            if (HttpStatus.CONFLICT.equals(ex.getStatus())
                    && (CollaborationPersistenceSupport.CONCURRENT_MODIFICATION_CODE.equals(ex.getCode())
                            || "COLLABORATION_SESSION_INVALID_STATUS".equals(ex.getCode()))) {
                conflictCount.incrementAndGet();
            } else {
                throw ex;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ex);
        }
    }

    private void runDecision(
            UUID sessionId,
            RecordDecisionRequest request,
            CountDownLatch ready,
            CountDownLatch start,
            AtomicInteger successCount,
            AtomicInteger conflictCount) {
        ready.countDown();
        try {
            start.await(5, TimeUnit.SECONDS);
            collaborationService.recordDecision(sessionId, request, CollaborationTestFixture.collaborationWriteUser());
            successCount.incrementAndGet();
        } catch (ApiException ex) {
            if (HttpStatus.CONFLICT.equals(ex.getStatus())
                    && CollaborationPersistenceSupport.CONCURRENT_MODIFICATION_CODE.equals(ex.getCode())) {
                conflictCount.incrementAndGet();
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
                CollaborationTestFixture.sampleCreateSessionRequest(CollaborationTestFixture.uniqueName("concurrency")),
                CollaborationTestFixture.collaborationWriteUser());
    }

    private SessionDetail createConflictSession() {
        return collaborationService.create(
                CollaborationTestFixture.createSessionWithConflictTasks(CollaborationTestFixture.uniqueName("conflict")),
                CollaborationTestFixture.collaborationWriteUser());
    }

    private void assign(UUID sessionId, UUID taskId, UUID participantId) {
        collaborationService.assign(
                sessionId,
                new AssignTaskRequest(taskId, TaskAction.ASSIGN, participantId, null, null, null, null, null),
                CollaborationTestFixture.collaborationWriteUser());
    }

    private void assign(UUID sessionId, UUID taskId, UUID participantId, String artifactRef) {
        collaborationService.assign(
                sessionId,
                new AssignTaskRequest(taskId, TaskAction.ASSIGN, participantId, null, null, artifactRef, null, null),
                CollaborationTestFixture.collaborationWriteUser());
    }

    private UUID participantId(SessionDetail session, String role) {
        return session.participants().stream()
                .filter(p -> p.participantRole().name().equals(role))
                .findFirst()
                .orElseThrow()
                .id();
    }

    private UUID taskId(SessionDetail session, String taskKey) {
        return session.tasks().stream()
                .filter(t -> t.taskKey().equals(taskKey))
                .findFirst()
                .orElseThrow()
                .id();
    }
}

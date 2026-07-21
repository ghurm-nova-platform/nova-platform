package ai.nova.platform.collaboration.service;

import java.util.function.Supplier;

import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import ai.nova.platform.collaboration.entity.CollaborationParticipantEntity;
import ai.nova.platform.collaboration.entity.CollaborationSessionEntity;
import ai.nova.platform.collaboration.entity.CollaborationTaskEntity;
import ai.nova.platform.collaboration.repository.CollaborationParticipantRepository;
import ai.nova.platform.collaboration.repository.CollaborationSessionRepository;
import ai.nova.platform.collaboration.repository.CollaborationTaskRepository;
import ai.nova.platform.web.error.ApiException;

@Component
public class CollaborationPersistenceSupport {

    public static final String CONCURRENT_MODIFICATION_CODE = "COLLABORATION_CONCURRENT_MODIFICATION";
    public static final String CONCURRENT_MODIFICATION_MESSAGE =
            "Collaboration record was modified by another request. Reload and retry.";

    private final CollaborationSessionRepository sessionRepository;
    private final CollaborationParticipantRepository participantRepository;
    private final CollaborationTaskRepository taskRepository;

    public CollaborationPersistenceSupport(
            CollaborationSessionRepository sessionRepository,
            CollaborationParticipantRepository participantRepository,
            CollaborationTaskRepository taskRepository) {
        this.sessionRepository = sessionRepository;
        this.participantRepository = participantRepository;
        this.taskRepository = taskRepository;
    }

    public CollaborationSessionEntity saveSession(CollaborationSessionEntity session) {
        return save(() -> sessionRepository.saveAndFlush(session));
    }

    public CollaborationParticipantEntity saveParticipant(CollaborationParticipantEntity participant) {
        return save(() -> participantRepository.saveAndFlush(participant));
    }

    public CollaborationTaskEntity saveTask(CollaborationTaskEntity task) {
        return save(() -> taskRepository.saveAndFlush(task));
    }

    public void translateOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        throw concurrentModification();
    }

    public ApiException concurrentModification() {
        return new ApiException(HttpStatus.CONFLICT, CONCURRENT_MODIFICATION_CODE, CONCURRENT_MODIFICATION_MESSAGE);
    }

    private <T> T save(Supplier<T> saver) {
        try {
            return saver.get();
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw concurrentModification();
        }
    }
}

package ai.nova.platform.collaboration.service;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import ai.nova.platform.collaboration.entity.CollaborationParticipantEntity;
import ai.nova.platform.collaboration.entity.CollaborationSessionEntity;
import ai.nova.platform.collaboration.entity.CollaborationTaskEntity;
import ai.nova.platform.collaboration.repository.CollaborationMessageRepository;
import ai.nova.platform.collaboration.repository.CollaborationDecisionRepository;
import ai.nova.platform.collaboration.repository.CollaborationParticipantRepository;
import ai.nova.platform.collaboration.repository.CollaborationTaskRepository;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Component
public class CollaborationReferenceValidator {

    private final CollaborationParticipantRepository participantRepository;
    private final CollaborationTaskRepository taskRepository;
    private final CollaborationMessageRepository messageRepository;
    private final CollaborationDecisionRepository decisionRepository;

    public CollaborationReferenceValidator(
            CollaborationParticipantRepository participantRepository,
            CollaborationTaskRepository taskRepository,
            CollaborationMessageRepository messageRepository,
            CollaborationDecisionRepository decisionRepository) {
        this.participantRepository = participantRepository;
        this.taskRepository = taskRepository;
        this.messageRepository = messageRepository;
        this.decisionRepository = decisionRepository;
    }

    public CollaborationParticipantEntity requireParticipant(
            CollaborationSessionEntity session, UUID participantId, UUID organizationId) {
        CollaborationParticipantEntity participant = participantRepository
                .findByIdAndOrganizationId(participantId, organizationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "COLLABORATION_PARTICIPANT_NOT_FOUND", "Participant not found"));
        requireSameSession(session, participant.getSessionId(), "COLLABORATION_PARTICIPANT_MISMATCH", "Participant");
        return participant;
    }

    public CollaborationTaskEntity requireTask(
            CollaborationSessionEntity session, UUID taskId, UUID organizationId) {
        CollaborationTaskEntity task = taskRepository
                .findByIdAndOrganizationId(taskId, organizationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "COLLABORATION_TASK_NOT_FOUND", "Task not found"));
        requireSameSession(session, task.getSessionId(), "COLLABORATION_TASK_MISMATCH", "Task");
        return task;
    }

    public CollaborationTaskEntity requireDependencyTask(
            CollaborationSessionEntity session, UUID dependencyTaskId, UUID organizationId) {
        if (dependencyTaskId == null) {
            return null;
        }
        CollaborationTaskEntity dependency = requireTask(session, dependencyTaskId, organizationId);
        if (!dependency.getOrganizationId().equals(organizationId)) {
            throw tenantMismatch("Task");
        }
        return dependency;
    }

    public void requireMessageReference(CollaborationSessionEntity session, UUID messageId, UUID organizationId) {
        if (messageId == null) {
            return;
        }
        messageRepository
                .findByIdAndOrganizationId(messageId, organizationId)
                .filter(message -> message.getSessionId().equals(session.getId()))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "COLLABORATION_MESSAGE_NOT_FOUND", "Message not found"));
    }

    public void requireDecisionReference(CollaborationSessionEntity session, UUID decisionId, UUID organizationId) {
        if (decisionId == null) {
            return;
        }
        decisionRepository
                .findByIdAndOrganizationId(decisionId, organizationId)
                .filter(decision -> decision.getSessionId().equals(session.getId()))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "COLLABORATION_DECISION_NOT_FOUND", "Decision not found"));
    }

    public void requireTaskReferenceForMutation(
            CollaborationSessionEntity session, UUID taskId, UUID organizationId) {
        if (taskId == null) {
            return;
        }
        requireTask(session, taskId, organizationId);
    }

    public void requireOrganizationAccess(AuthenticatedUser user, UUID organizationId) {
        if (!user.getOrganizationId().equals(organizationId)) {
            throw tenantMismatch("Organization");
        }
    }

    private void requireSameSession(
            CollaborationSessionEntity session, UUID referencedSessionId, String code, String entityName) {
        if (!session.getId().equals(referencedSessionId)) {
            throw new ApiException(
                    HttpStatus.CONFLICT, code, entityName + " does not belong to session");
        }
    }

    private ApiException tenantMismatch(String entityName) {
        return new ApiException(
                HttpStatus.FORBIDDEN, "COLLABORATION_TENANT_MISMATCH", entityName + " belongs to another organization");
    }
}

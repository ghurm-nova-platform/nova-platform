package ai.nova.platform.collaboration.support;

import java.util.List;
import java.util.UUID;

import ai.nova.platform.audit.support.AuditTestFixture;
import ai.nova.platform.collaboration.dto.CollaborationDtos.CreateSessionRequest;
import ai.nova.platform.collaboration.dto.CollaborationDtos.InitialTaskRequest;
import ai.nova.platform.collaboration.dto.CollaborationDtos.SharedContext;
import ai.nova.platform.collaboration.dto.CollaborationDtos.SendMessageRequest;
import ai.nova.platform.collaboration.entity.CollaborationMessageType;
import ai.nova.platform.collaboration.entity.CollaborationParticipantRole;
import ai.nova.platform.collaboration.security.CollaborationAuthorizationService;
import ai.nova.platform.security.AuthenticatedUser;

public final class CollaborationTestFixture {

    public static final UUID ORG_ID = AuditTestFixture.ORG_ID;
    public static final UUID USER_ID = AuditTestFixture.USER_ID;
    public static final UUID PROJECT_ID = AuditTestFixture.PROJECT_ID;
    public static final UUID OTHER_ORG_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    public static final UUID OTHER_USER_ID = UUID.fromString("99999999-9999-9999-9999-999999999998");
    public static final UUID OTHER_PROJECT_ID = UUID.fromString("99999999-9999-9999-9999-999999999997");

    private CollaborationTestFixture() {
    }

    public static AuthenticatedUser collaborationReadUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "collaboration-reader@nova.local",
                "Collaboration Reader",
                List.of("USER"),
                List.of(CollaborationAuthorizationService.COLLABORATION_READ),
                true);
    }

    public static AuthenticatedUser collaborationWriteUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "collaboration-writer@nova.local",
                "Collaboration Writer",
                List.of("USER"),
                List.of(
                        CollaborationAuthorizationService.COLLABORATION_READ,
                        CollaborationAuthorizationService.COLLABORATION_WRITE),
                true);
    }

    public static AuthenticatedUser collaborationAdminUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "collaboration-admin@nova.local",
                "Collaboration Admin",
                List.of("USER"),
                List.of(
                        CollaborationAuthorizationService.COLLABORATION_READ,
                        CollaborationAuthorizationService.COLLABORATION_WRITE,
                        CollaborationAuthorizationService.COLLABORATION_ADMIN),
                true);
    }

    public static AuthenticatedUser collaborationOtherOrgWriteUser() {
        return new AuthenticatedUser(
                OTHER_USER_ID,
                OTHER_ORG_ID,
                "collaboration-writer-b@nova.local",
                "Collaboration Writer B",
                List.of("USER"),
                List.of(
                        CollaborationAuthorizationService.COLLABORATION_READ,
                        CollaborationAuthorizationService.COLLABORATION_WRITE),
                true);
    }

    public static SendMessageRequest infoMessage(String content) {
        return new SendMessageRequest(CollaborationParticipantRole.CODING, CollaborationMessageType.INFO, content, null);
    }

    public static SendMessageRequest infoMessageForTask(String content, UUID taskId) {
        return new SendMessageRequest(CollaborationParticipantRole.CODING, CollaborationMessageType.INFO, content, taskId);
    }

    public static AuthenticatedUser collaborationNoPermissionUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "collaboration-noperm@nova.local",
                "No Collaboration Perm",
                List.of("USER"),
                List.of("ENVIRONMENT_READ"),
                true);
    }

    public static String uniqueName(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    public static CreateSessionRequest sampleCreateSessionRequest(String name) {
        return new CreateSessionRequest(
                PROJECT_ID,
                null,
                name,
                new SharedContext(PROJECT_ID, null, "main", null, null, null, List.of()),
                List.of(CollaborationParticipantRole.CODING, CollaborationParticipantRole.REVIEW),
                List.of(
                        new InitialTaskRequest("task-1", "Implement feature", null, null, null),
                        new InitialTaskRequest("task-2", "Review changes", null, null, null)));
    }

    public static CreateSessionRequest createSessionWithParallelTasks(String name, String parallelGroup) {
        return new CreateSessionRequest(
                PROJECT_ID,
                null,
                name,
                new SharedContext(PROJECT_ID, null, "main", null, null, null, List.of()),
                List.of(CollaborationParticipantRole.CODING, CollaborationParticipantRole.REVIEW),
                List.of(
                        new InitialTaskRequest("parallel-a", "Parallel task A", null, parallelGroup, null),
                        new InitialTaskRequest("parallel-b", "Parallel task B", null, parallelGroup, null)));
    }

    public static CreateSessionRequest createSessionWithConflictTasks(String name) {
        return new CreateSessionRequest(
                PROJECT_ID,
                null,
                name,
                new SharedContext(PROJECT_ID, null, "main", null, null, null, List.of()),
                List.of(CollaborationParticipantRole.CODING, CollaborationParticipantRole.REVIEW),
                List.of(
                        new InitialTaskRequest("conflict-a", "Edit artifact A", null, null, null),
                        new InitialTaskRequest("conflict-b", "Edit artifact B", null, null, null)));
    }

    public static String createSessionBody(String name) {
        return """
                {
                  "projectId":"%s",
                  "name":"%s",
                  "sharedContext":{"projectId":"%s","branch":"main"},
                  "participantRoles":["CODING","REVIEW"],
                  "initialTasks":[
                    {"taskKey":"task-1","title":"Implement feature"},
                    {"taskKey":"task-2","title":"Review changes"}
                  ]
                }
                """.formatted(PROJECT_ID, name, PROJECT_ID);
    }

    public static String assignTaskBody(UUID taskId, UUID participantId, String artifactRef) {
        String artifactJson = artifactRef == null ? "null" : "\"" + artifactRef + "\"";
        return """
                {
                  "taskId":"%s",
                  "action":"ASSIGN",
                  "participantId":"%s",
                  "artifactRef":%s
                }
                """.formatted(taskId, participantId, artifactJson);
    }

    public static String completeTaskBody(UUID taskId) {
        return """
                {
                  "taskId":"%s",
                  "action":"COMPLETE"
                }
                """.formatted(taskId);
    }

    public static String sendMessageBody() {
        return """
                {
                  "senderRole":"CODING",
                  "messageType":"INFO",
                  "content":"Progress update from controller test"
                }
                """;
    }
}

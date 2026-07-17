package ai.nova.platform.conversation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.conversation.entity.Conversation;
import ai.nova.platform.conversation.entity.ConversationAuditAction;
import ai.nova.platform.conversation.entity.ConversationAuditLog;
import ai.nova.platform.conversation.entity.ConversationMessage;
import ai.nova.platform.conversation.entity.ConversationMessageRole;
import ai.nova.platform.conversation.entity.ConversationStatus;
import ai.nova.platform.conversation.repository.ConversationAuditLogRepository;
import ai.nova.platform.conversation.repository.ConversationMessageRepository;
import ai.nova.platform.conversation.repository.ConversationRepository;
import ai.nova.platform.conversation.service.ConversationMemoryService;
import ai.nova.platform.conversation.service.ConversationMemoryService.AssembledContext;
import ai.nova.platform.conversation.validation.ConversationProperties;
import ai.nova.platform.security.AuthenticatedUser;

@SpringBootTest
class ConversationMemoryServiceTest {

    private static final UUID ORG_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PROJECT_ID = UUID.fromString("55555555-5555-5555-5555-555555555501");
    private static final UUID AGENT_ID = UUID.fromString("66666666-6666-6666-6666-666666666601");
    private static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444401");

    @Autowired
    private ConversationMemoryService memoryService;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationMessageRepository messageRepository;

    @Autowired
    private ConversationAuditLogRepository auditLogRepository;

    private ConversationProperties props;
    private AuthenticatedUser user;

    @BeforeEach
    void setUp() {
        props = new ConversationProperties();
        props.setMaxContextMessages(3);
        props.setMaxContextCharacters(50);
        props.setMaxMessageCharacters(10000);
        user = new AuthenticatedUser(
                USER_ID, ORG_ID, "admin@nova.local", "Admin", java.util.List.of("ORG_ADMIN"), java.util.List.of(), true);
    }

    @Test
    @Transactional
    void respectsMessageLimitAndAlwaysIncludesCurrentUser() {
        Conversation conversation = createConversation();
        addMessage(conversation, ConversationMessageRole.USER, "msg1", 1);
        addMessage(conversation, ConversationMessageRole.ASSISTANT, "reply1", 2);
        addMessage(conversation, ConversationMessageRole.USER, "msg2", 3);
        addMessage(conversation, ConversationMessageRole.ASSISTANT, "reply2", 4);

        AssembledContext context = memoryService.assemble(
                conversation.getId(), PROJECT_ID, ORG_ID, "current question", props, user);

        assertThat(context.messages()).hasSize(3);
        assertThat(context.messages().getLast().content()).isEqualTo("current question");
        assertThat(context.messages().getLast().role()).isEqualTo("USER");
        assertThat(context.droppedMessageCount()).isEqualTo(2);
        assertThat(context.truncated()).isTrue();
    }

    @Test
    @Transactional
    void respectsCharacterLimit() {
        Conversation conversation = createConversation();
        addMessage(conversation, ConversationMessageRole.USER, "1234567890", 1);
        addMessage(conversation, ConversationMessageRole.ASSISTANT, "1234567890", 2);

        props.setMaxContextMessages(20);
        props.setMaxContextCharacters(24);

        AssembledContext context = memoryService.assemble(
                conversation.getId(), PROJECT_ID, ORG_ID, "12345", props, user);

        assertThat(context.messages()).hasSize(2);
        assertThat(context.messages().getFirst().content()).isEqualTo("1234567890");
        assertThat(context.messages().getLast().content()).isEqualTo("12345");
        assertThat(context.droppedMessageCount()).isEqualTo(1);
    }

    @Test
    @Transactional
    void truncationAuditContainsNoMessageContent() {
        Conversation conversation = createConversation();
        for (int i = 1; i <= 5; i++) {
            addMessage(conversation, ConversationMessageRole.USER, "secret-content-" + i, i);
        }

        memoryService.assemble(conversation.getId(), PROJECT_ID, ORG_ID, "current", props, user);

        java.util.List<ConversationAuditLog> audits = auditLogRepository.findAll().stream()
                .filter(a -> a.getConversationId().equals(conversation.getId())
                        && a.getAction() == ConversationAuditAction.MEMORY_TRUNCATED)
                .toList();

        assertThat(audits).isNotEmpty();
        for (ConversationAuditLog audit : audits) {
            assertThat(audit.getMetadata()).doesNotContain("secret-content");
            assertThat(audit.getMetadata()).contains("droppedCount");
        }
    }

    private Conversation createConversation() {
        Instant now = Instant.now();
        Conversation conversation = new Conversation(
                UUID.randomUUID(), ORG_ID, PROJECT_ID, AGENT_ID, "Memory test", ConversationStatus.ACTIVE, USER_ID, now);
        return conversationRepository.saveAndFlush(conversation);
    }

    private void addMessage(Conversation conversation, ConversationMessageRole role, String content, int sequence) {
        messageRepository.saveAndFlush(new ConversationMessage(
                UUID.randomUUID(),
                conversation.getId(),
                ORG_ID,
                PROJECT_ID,
                AGENT_ID,
                null,
                role,
                content,
                sequence,
                USER_ID,
                Instant.now(),
                null));
        conversationRepository.findForUpdate(conversation.getId(), PROJECT_ID, ORG_ID).ifPresent(locked -> {
            locked.setMessageCount(sequence);
            conversationRepository.saveAndFlush(locked);
        });
    }
}

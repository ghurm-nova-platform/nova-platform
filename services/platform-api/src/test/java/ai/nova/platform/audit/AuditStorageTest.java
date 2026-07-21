package ai.nova.platform.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import ai.nova.platform.audit.service.AuditStorageService;
import ai.nova.platform.audit.support.AuditTestFixture;
import ai.nova.platform.web.error.ApiException;

@SpringBootTest(properties = "nova.audit.enabled=true")
class AuditStorageTest {

    @Autowired
    private AuditStorageService storageService;

    @Test
    void appendPersistsEventAndRejectsMutation() {
        var saved = storageService.append(
                AuditTestFixture.sampleEvent(UUID.randomUUID(), ai.nova.platform.audit.entity.AuditAction.CREATE));
        assertThat(saved.id()).isNotNull();
        assertThatThrownBy(() -> storageService.rejectMutation())
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus(), ex -> ((ApiException) ex).getCode())
                .containsExactly(HttpStatus.CONFLICT, "AUDIT_IMMUTABLE");
    }
}

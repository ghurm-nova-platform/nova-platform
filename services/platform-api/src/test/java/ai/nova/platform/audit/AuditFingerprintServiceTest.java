package ai.nova.platform.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.audit.dto.AuditDtos.RecordAuditEventRequest;
import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditEntityType;
import ai.nova.platform.audit.entity.AuditResult;
import ai.nova.platform.audit.entity.AuditSeverity;
import ai.nova.platform.audit.entity.AuditSource;
import ai.nova.platform.audit.service.AuditFingerprintService;
import ai.nova.platform.audit.support.AuditTestFixture;

class AuditFingerprintServiceTest {

    private final AuditFingerprintService fingerprintService = new AuditFingerprintService(new ObjectMapper());

    @Test
    void sameDetailsDifferentMapInsertionOrderProducesSameFingerprint() {
        UUID entityId = UUID.randomUUID();
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("alpha", "1");
        first.put("beta", "2");

        Map<String, Object> second = new LinkedHashMap<>();
        second.put("beta", "2");
        second.put("alpha", "1");

        String requestId = "req-" + UUID.randomUUID();
        String firstFingerprint = fingerprint(entityId, requestId, first);
        String secondFingerprint = fingerprint(entityId, requestId, second);

        assertThat(secondFingerprint).isEqualTo(firstFingerprint);
    }

    @Test
    void differentDetailsProduceDifferentFingerprint() {
        UUID entityId = UUID.randomUUID();
        String requestId = "req-" + UUID.randomUUID();

        String first = fingerprint(entityId, requestId, Map.of("note", "alpha"));
        String second = fingerprint(entityId, requestId, Map.of("note", "beta"));

        assertThat(second).isNotEqualTo(first);
    }

    @Test
    void differentRequestIdsProduceDifferentFingerprint() {
        UUID entityId = UUID.randomUUID();
        Map<String, Object> details = Map.of("note", "fixture");

        String first = fingerprint(entityId, "req-a", details);
        String second = fingerprint(entityId, "req-b", details);

        assertThat(second).isNotEqualTo(first);
    }

    private String fingerprint(UUID entityId, String requestId, Map<String, Object> details) {
        String correlationId = "corr-fixed";
        return fingerprintService.fingerprint(new RecordAuditEventRequest(
                AuditTestFixture.ORG_ID,
                AuditTestFixture.PROJECT_ID,
                AuditTestFixture.USER_ID,
                "Nova Admin",
                null,
                AuditEntityType.ENVIRONMENT,
                entityId,
                "test-env",
                AuditAction.CREATE,
                AuditResult.SUCCESS,
                AuditSeverity.MEDIUM,
                AuditSource.ENVIRONMENT_MANAGEMENT,
                correlationId,
                requestId,
                null,
                null,
                details));
    }
}

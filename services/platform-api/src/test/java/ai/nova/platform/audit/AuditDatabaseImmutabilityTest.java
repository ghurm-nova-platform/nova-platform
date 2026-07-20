package ai.nova.platform.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import ai.nova.platform.audit.dto.AuditDtos.AuditEvent;
import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.repository.AuditEventRepository;
import ai.nova.platform.audit.service.AuditService;
import ai.nova.platform.audit.support.AuditTestFixture;

@SpringBootTest(properties = {"nova.audit.enabled=true", "nova.audit.immutable=true"})
class AuditDatabaseImmutabilityTest {

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditEventRepository eventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void updateDeleteBlockedOnImmutableTables() {
        UUID entityId = UUID.randomUUID();
        AuditEvent saved = auditService.record(AuditTestFixture.sampleEvent(entityId, AuditAction.CREATE));
        UUID eventId = saved.id();

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE audit_events SET username = 'x' WHERE id = ?", eventId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("AUDIT_IMMUTABLE");

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM audit_events WHERE id = ?", eventId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("AUDIT_IMMUTABLE");

        UUID correlationId = jdbcTemplate.queryForObject(
                "SELECT id FROM audit_correlation WHERE audit_event_id = ?", UUID.class, eventId);
        assertThat(correlationId).isNotNull();

        assertThatThrownBy(() -> jdbcTemplate.update(
                        "UPDATE audit_correlation SET chain_sequence = 99 WHERE id = ?", correlationId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("AUDIT_IMMUTABLE");

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM audit_correlation WHERE id = ?", correlationId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("AUDIT_IMMUTABLE");

        UUID indexId = jdbcTemplate.queryForObject(
                "SELECT id FROM audit_indexes WHERE audit_event_id = ? LIMIT 1", UUID.class, eventId);
        if (indexId != null) {
            assertThatThrownBy(() -> jdbcTemplate.update("UPDATE audit_indexes SET index_value = 'x' WHERE id = ?", indexId))
                    .isInstanceOf(DataAccessException.class)
                    .hasMessageContaining("AUDIT_IMMUTABLE");

            assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM audit_indexes WHERE id = ?", indexId))
                    .isInstanceOf(DataAccessException.class)
                    .hasMessageContaining("AUDIT_IMMUTABLE");
        }

        assertThat(eventRepository.findById(eventId)).isPresent();
    }

    @Test
    void insertStillAllowedAfterImmutabilityTriggers() {
        UUID entityId = UUID.randomUUID();
        AuditEvent saved = auditService.record(AuditTestFixture.sampleEvent(entityId, AuditAction.UPDATE));
        assertThat(saved.id()).isNotNull();
    }
}

package ai.nova.platform.knowledge.engine;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import ai.nova.platform.knowledge.engine.security.KnowledgeEngineAuthorizationService;
import ai.nova.platform.knowledge.engine.support.KnowledgeEngineTestFixture;
import ai.nova.platform.web.error.ApiException;

@SpringBootTest(properties = {"nova.knowledge.engine.enabled=true", "nova.audit.enabled=true"})
class KnowledgePermissionTest {

    @Autowired
    private KnowledgeEngineAuthorizationService authorizationService;

    @Test
    void readWriteAndAdminPermissions() {
        authorizationService.requireRead(KnowledgeEngineTestFixture.knowledgeReadUser());
        authorizationService.requireWrite(KnowledgeEngineTestFixture.knowledgeWriteUser());
        authorizationService.requireAdmin(KnowledgeEngineTestFixture.knowledgeAdminUser());

        assertThatThrownBy(() -> authorizationService.requireRead(KnowledgeEngineTestFixture.knowledgeNoPermissionUser()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);

        assertThatThrownBy(() -> authorizationService.requireWrite(KnowledgeEngineTestFixture.knowledgeReadUser()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);

        assertThatThrownBy(() -> authorizationService.requireAdmin(KnowledgeEngineTestFixture.knowledgeWriteUser()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}

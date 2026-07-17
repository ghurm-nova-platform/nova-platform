package ai.nova.platform.prompt.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.nova.platform.prompt.entity.PromptVariable;

public interface PromptVariableRepository extends JpaRepository<PromptVariable, UUID> {

    List<PromptVariable> findByPromptVersionIdOrderByNameAsc(UUID promptVersionId);

    @Modifying(clearAutomatically = true, flushAutomatically = false)
    @Query("DELETE FROM PromptVariable v WHERE v.promptVersionId = :promptVersionId")
    void deleteByPromptVersionId(@Param("promptVersionId") UUID promptVersionId);
}

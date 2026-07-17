package ai.nova.platform.prompt.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.nova.platform.prompt.entity.PromptTag;

public interface PromptTagRepository extends JpaRepository<PromptTag, UUID> {

    List<PromptTag> findByPromptIdOrderByTagNameAsc(UUID promptId);

    @Modifying(clearAutomatically = true, flushAutomatically = false)
    @Query("DELETE FROM PromptTag t WHERE t.promptId = :promptId")
    void deleteByPromptId(@Param("promptId") UUID promptId);
}

package ai.nova.platform.modelgateway.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.nova.platform.modelgateway.entity.ModelInvocation;

import jakarta.persistence.LockModeType;

public interface ModelInvocationRepository extends JpaRepository<ModelInvocation, UUID> {

    Optional<ModelInvocation> findByExecutionIdAndAttemptNumber(UUID executionId, Integer attemptNumber);

    List<ModelInvocation> findByExecutionIdOrderByAttemptNumberAsc(UUID executionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM ModelInvocation i WHERE i.id = :id")
    Optional<ModelInvocation> findByIdForUpdate(@Param("id") UUID id);

    int countByExecutionId(UUID executionId);
}

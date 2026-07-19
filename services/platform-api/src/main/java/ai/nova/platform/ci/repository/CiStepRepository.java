package ai.nova.platform.ci.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.ci.entity.CiStepEntity;

public interface CiStepRepository extends JpaRepository<CiStepEntity, UUID> {

    List<CiStepEntity> findByCiJobIdOrderByStepNumberAsc(UUID ciJobId);
}

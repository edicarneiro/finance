package com.financepulse.engine.adapters.out.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAccountJpaRepository extends JpaRepository<AccountJpaEntity, String> {

    Optional<AccountJpaEntity> findByIdAndUserId(String id, String userId);

    List<AccountJpaEntity> findAllByUserId(String userId);
}

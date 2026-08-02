package com.financepulse.engine.adapters.out.persistence;

import com.financepulse.engine.application.ports.NotificationPreferenceRepository;
import com.financepulse.engine.domain.notification.NotificationPreference;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaNotificationPreferenceRepositoryAdapter implements NotificationPreferenceRepository {

    private final SpringDataNotificationPreferenceJpaRepository jpaRepository;

    public JpaNotificationPreferenceRepositoryAdapter(SpringDataNotificationPreferenceJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<NotificationPreference> findAllByUserId(String userId) {
        return jpaRepository.findAllByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void saveOrUpdate(NotificationPreference preference) {
        String id = jpaRepository
                .findByUserIdAndAlertTypeAndChannel(preference.getUserId(), preference.getAlertType(), preference.getChannel())
                .map(NotificationPreferenceJpaEntity::getId)
                .orElse(preference.getId());

        jpaRepository.save(new NotificationPreferenceJpaEntity(id, preference.getUserId(), preference.getAlertType(), preference.getChannel(),
                preference.isEnabled()));
    }

    private NotificationPreference toDomain(NotificationPreferenceJpaEntity entity) {
        return NotificationPreference.reconstitute(entity.getId(), entity.getUserId(), entity.getAlertType(), entity.getChannel(), entity.isEnabled());
    }
}

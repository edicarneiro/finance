package com.financepulse.engine.adapters.out.persistence;

import com.financepulse.engine.application.ports.UserRepository;
import com.financepulse.engine.domain.user.Email;
import com.financepulse.engine.domain.user.User;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaUserRepositoryAdapter implements UserRepository {

    private final SpringDataUserJpaRepository jpaRepository;

    public JpaUserRepositoryAdapter(SpringDataUserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpaRepository.findByEmail(email.toString()).map(this::toDomain);
    }

    @Override
    public Optional<User> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public void save(User user) {
        jpaRepository.save(new UserJpaEntity(
                user.getId(),
                user.getEmail().toString(),
                user.getPasswordHash(),
                user.getName(),
                user.getCreatedAt()));
    }

    private User toDomain(UserJpaEntity entity) {
        return User.reconstitute(
                entity.getId(),
                Email.create(entity.getEmail()),
                entity.getPasswordHash(),
                entity.getName(),
                entity.getCreatedAt());
    }
}

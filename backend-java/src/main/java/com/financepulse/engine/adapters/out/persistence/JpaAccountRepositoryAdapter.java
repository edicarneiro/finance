package com.financepulse.engine.adapters.out.persistence;

import com.financepulse.engine.application.ports.AccountRepository;
import com.financepulse.engine.domain.account.Account;
import com.financepulse.engine.domain.account.Currency;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAccountRepositoryAdapter implements AccountRepository {

    private final SpringDataAccountJpaRepository jpaRepository;

    public JpaAccountRepositoryAdapter(SpringDataAccountJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Account> findByIdAndUserId(String id, String userId) {
        return jpaRepository.findByIdAndUserId(id, userId).map(this::toDomain);
    }

    @Override
    public List<Account> findAllByUserId(String userId) {
        return jpaRepository.findAllByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public void save(Account account) {
        jpaRepository.save(toEntity(account));
    }

    @Override
    public void update(Account account) {
        jpaRepository.save(toEntity(account));
    }

    private AccountJpaEntity toEntity(Account account) {
        return new AccountJpaEntity(
                account.getId(),
                account.getUserId(),
                account.getType(),
                account.getName(),
                account.getCurrency().toString(),
                account.getBalance(),
                account.isArchived(),
                account.getCreatedAt());
    }

    private Account toDomain(AccountJpaEntity entity) {
        return Account.reconstitute(
                entity.getId(),
                entity.getUserId(),
                entity.getType(),
                entity.getName(),
                Currency.create(entity.getCurrency()),
                entity.getBalance(),
                entity.isArchived(),
                entity.getCreatedAt());
    }
}

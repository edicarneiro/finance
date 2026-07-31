package com.financepulse.engine.testsupport;

import com.financepulse.engine.application.ports.AccountRepository;
import com.financepulse.engine.domain.account.Account;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryAccountRepository implements AccountRepository {

    private final Map<String, Account> accountsById = new LinkedHashMap<>();

    @Override
    public Optional<Account> findByIdAndUserId(String id, String userId) {
        return Optional.ofNullable(accountsById.get(id)).filter(account -> account.getUserId().equals(userId));
    }

    @Override
    public List<Account> findAllByUserId(String userId) {
        return accountsById.values().stream()
                .filter(account -> account.getUserId().equals(userId))
                .toList();
    }

    @Override
    public void save(Account account) {
        accountsById.put(account.getId(), account);
    }

    @Override
    public void update(Account account) {
        accountsById.put(account.getId(), account);
    }
}

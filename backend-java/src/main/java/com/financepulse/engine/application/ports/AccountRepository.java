package com.financepulse.engine.application.ports;

import com.financepulse.engine.domain.account.Account;
import java.util.List;
import java.util.Optional;

/**
 * Toda leitura/escrita de uma conta específica é escopada por userId na
 * própria assinatura — reforço estrutural do isolamento multi-tenant
 * (RF-047, rules.md § 4), não dependente de disciplina em camada superior.
 */
public interface AccountRepository {

    Optional<Account> findByIdAndUserId(String id, String userId);

    List<Account> findAllByUserId(String userId);

    void save(Account account);

    void update(Account account);
}

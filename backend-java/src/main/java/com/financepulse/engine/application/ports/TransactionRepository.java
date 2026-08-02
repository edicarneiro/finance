package com.financepulse.engine.application.ports;

import com.financepulse.engine.domain.transaction.Transaction;
import java.util.List;
import java.util.Optional;

/** Toda leitura/escrita é escopada por userId na própria assinatura (RF-047, rules.md § 4). */
public interface TransactionRepository {

    Optional<Transaction> findByIdAndUserId(String id, String userId);

    /** Todas as transações do usuário através de todas as contas — usada por agregações (ex.: dashboard, ver ADR-0020). */
    List<Transaction> findAllByUserId(String userId);

    List<Transaction> findAllByAccountIdAndUserId(String accountId, String userId);

    List<Transaction> findAllByCategoryIdAndUserId(String categoryId, String userId);

    void save(Transaction transaction);

    void update(Transaction transaction);

    void deleteByIdAndUserId(String id, String userId);

    boolean existsByCategoryIdAndUserId(String categoryId, String userId);
}

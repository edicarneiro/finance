package com.financepulse.engine.application.usecases.transaction;

import com.financepulse.engine.application.ports.TransactionRepository;
import com.financepulse.engine.domain.transaction.errors.TransactionNotFoundException;

/** RF-015: exclusão definitiva — ao contrário de contas (RF-013), transações não têm arquivamento. */
public class DeleteTransactionUseCase {

    private final TransactionRepository transactionRepository;

    public DeleteTransactionUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public void execute(Input input) {
        transactionRepository
                .findByIdAndUserId(input.transactionId(), input.userId())
                .orElseThrow(TransactionNotFoundException::new);

        transactionRepository.deleteByIdAndUserId(input.transactionId(), input.userId());
    }

    public record Input(String userId, String transactionId) {
    }
}

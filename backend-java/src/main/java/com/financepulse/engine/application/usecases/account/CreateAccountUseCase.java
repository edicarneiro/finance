package com.financepulse.engine.application.usecases.account;

import com.financepulse.engine.application.ports.AccountRepository;
import com.financepulse.engine.application.ports.IdGenerator;
import com.financepulse.engine.domain.account.Account;
import com.financepulse.engine.domain.account.AccountType;
import com.financepulse.engine.domain.account.Currency;
import java.math.BigDecimal;

public class CreateAccountUseCase {

    private final AccountRepository accountRepository;
    private final IdGenerator idGenerator;

    public CreateAccountUseCase(AccountRepository accountRepository, IdGenerator idGenerator) {
        this.accountRepository = accountRepository;
        this.idGenerator = idGenerator;
    }

    public Output execute(Input input) {
        Currency currency = Currency.create(input.currency());
        Account account = Account.create(
                idGenerator.generate(), input.userId(), input.type(), input.name(), currency, input.initialBalance());

        accountRepository.save(account);

        return new Output(account.getId());
    }

    public record Input(String userId, AccountType type, String name, String currency, BigDecimal initialBalance) {
    }

    public record Output(String accountId) {
    }
}

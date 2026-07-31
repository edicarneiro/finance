package com.financepulse.engine.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.financepulse.engine.domain.account.Account;
import com.financepulse.engine.domain.account.AccountType;
import com.financepulse.engine.domain.account.Currency;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/** Exercita o adaptador contra H2 real, mesma disciplina do JpaUserRepositoryAdapterTest (rules.md § 3). */
@DataJpaTest
class JpaAccountRepositoryAdapterTest {

    @Autowired
    private SpringDataAccountJpaRepository jpaRepository;

    @Test
    void savesAndReloadsAnAccountScopedToItsOwner() {
        JpaAccountRepositoryAdapter adapter = new JpaAccountRepositoryAdapter(jpaRepository);
        Account account = Account.create(
                "account-1", "user-1", AccountType.CHECKING, "Conta Corrente", Currency.create("BRL"), new BigDecimal("100.00"));

        adapter.save(account);

        Optional<Account> found = adapter.findByIdAndUserId("account-1", "user-1");
        assertThat(found).isPresent();
        assertThat(found.get().getBalance()).isEqualByComparingTo("100.00");
        assertThat(found.get().getType()).isEqualTo(AccountType.CHECKING);
    }

    @Test
    void doesNotFindAnAccountBelongingToAnotherUser() {
        JpaAccountRepositoryAdapter adapter = new JpaAccountRepositoryAdapter(jpaRepository);
        adapter.save(Account.create(
                "account-1", "user-1", AccountType.CASH, "Carteira", Currency.create("BRL"), BigDecimal.ZERO));

        assertThat(adapter.findByIdAndUserId("account-1", "another-user")).isEmpty();
    }

    @Test
    void listsAllAccountsForAUser() {
        JpaAccountRepositoryAdapter adapter = new JpaAccountRepositoryAdapter(jpaRepository);
        adapter.save(Account.create("account-1", "user-1", AccountType.CASH, "Carteira", Currency.create("BRL"), BigDecimal.ZERO));
        adapter.save(Account.create("account-2", "user-1", AccountType.SAVINGS, "Poupança", Currency.create("BRL"), BigDecimal.TEN));
        adapter.save(Account.create("account-3", "user-2", AccountType.CASH, "Outra", Currency.create("BRL"), BigDecimal.ZERO));

        List<Account> accounts = adapter.findAllByUserId("user-1");

        assertThat(accounts).hasSize(2).extracting(Account::getId).containsExactlyInAnyOrder("account-1", "account-2");
    }

    @Test
    void persistsAnUpdateToAnExistingAccount() {
        JpaAccountRepositoryAdapter adapter = new JpaAccountRepositoryAdapter(jpaRepository);
        Account account = Account.create(
                "account-1", "user-1", AccountType.CASH, "Carteira", Currency.create("BRL"), BigDecimal.ZERO);
        adapter.save(account);

        adapter.update(account.withName("Carteira Renomeada").archive());

        Account reloaded = adapter.findByIdAndUserId("account-1", "user-1").orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Carteira Renomeada");
        assertThat(reloaded.isArchived()).isTrue();
    }
}

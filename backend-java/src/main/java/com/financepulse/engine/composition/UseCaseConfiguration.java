package com.financepulse.engine.composition;

import com.financepulse.engine.application.ports.AccountRepository;
import com.financepulse.engine.application.ports.IdGenerator;
import com.financepulse.engine.application.ports.PasswordHasher;
import com.financepulse.engine.application.ports.TokenService;
import com.financepulse.engine.application.ports.UserRepository;
import com.financepulse.engine.application.usecases.AuthenticateUserUseCase;
import com.financepulse.engine.application.usecases.RegisterUserUseCase;
import com.financepulse.engine.application.usecases.account.ArchiveAccountUseCase;
import com.financepulse.engine.application.usecases.account.CreateAccountUseCase;
import com.financepulse.engine.application.usecases.account.GetConsolidatedBalanceUseCase;
import com.financepulse.engine.application.usecases.account.ListAccountsUseCase;
import com.financepulse.engine.application.usecases.account.UpdateAccountUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Raiz de composição da camada de aplicação (equivalente ao container.ts do
 * backend TypeScript, revisado por ADR-0013): os casos de uso permanecem
 * classes Java puras, sem anotação de framework (regra de dependência da
 * Arquitetura Hexagonal), então precisam ser instanciados explicitamente
 * aqui em vez de descobertos via @Component/@Service.
 */
@Configuration
public class UseCaseConfiguration {

    @Bean
    public RegisterUserUseCase registerUserUseCase(
            UserRepository userRepository, PasswordHasher passwordHasher, IdGenerator idGenerator) {
        return new RegisterUserUseCase(userRepository, passwordHasher, idGenerator);
    }

    @Bean
    public AuthenticateUserUseCase authenticateUserUseCase(
            UserRepository userRepository, PasswordHasher passwordHasher, TokenService tokenService) {
        return new AuthenticateUserUseCase(userRepository, passwordHasher, tokenService);
    }

    @Bean
    public CreateAccountUseCase createAccountUseCase(AccountRepository accountRepository, IdGenerator idGenerator) {
        return new CreateAccountUseCase(accountRepository, idGenerator);
    }

    @Bean
    public UpdateAccountUseCase updateAccountUseCase(AccountRepository accountRepository) {
        return new UpdateAccountUseCase(accountRepository);
    }

    @Bean
    public ArchiveAccountUseCase archiveAccountUseCase(AccountRepository accountRepository) {
        return new ArchiveAccountUseCase(accountRepository);
    }

    @Bean
    public ListAccountsUseCase listAccountsUseCase(AccountRepository accountRepository) {
        return new ListAccountsUseCase(accountRepository);
    }

    @Bean
    public GetConsolidatedBalanceUseCase getConsolidatedBalanceUseCase(AccountRepository accountRepository) {
        return new GetConsolidatedBalanceUseCase(accountRepository);
    }
}

package com.financepulse.engine.application.usecases.category;

import com.financepulse.engine.application.ports.CategoryRepository;
import com.financepulse.engine.application.ports.TransactionRepository;
import com.financepulse.engine.domain.category.errors.CategoryHasSubcategoriesException;
import com.financepulse.engine.domain.category.errors.CategoryHasTransactionsException;
import com.financepulse.engine.domain.category.errors.CategoryNotFoundException;

/** RF-023: exclusão definitiva, bloqueada se a categoria tiver subcategorias ou transações associadas (RN-002, ver ADR-0017). */
public class DeleteCategoryUseCase {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public DeleteCategoryUseCase(CategoryRepository categoryRepository, TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    public void execute(Input input) {
        categoryRepository.findByIdAndUserId(input.categoryId(), input.userId()).orElseThrow(CategoryNotFoundException::new);

        if (categoryRepository.existsByParentCategoryIdAndUserId(input.categoryId(), input.userId())) {
            throw new CategoryHasSubcategoriesException();
        }
        if (transactionRepository.existsByCategoryIdAndUserId(input.categoryId(), input.userId())) {
            throw new CategoryHasTransactionsException();
        }

        categoryRepository.deleteByIdAndUserId(input.categoryId(), input.userId());
    }

    public record Input(String userId, String categoryId) {
    }
}

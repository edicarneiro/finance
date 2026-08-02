package com.financepulse.engine.adapters.in.web;

import com.financepulse.engine.adapters.in.web.dto.CategoryResponse;
import com.financepulse.engine.adapters.in.web.dto.CreateCategoryRequest;
import com.financepulse.engine.adapters.in.web.dto.CreateCategoryResponse;
import com.financepulse.engine.adapters.in.web.dto.UpdateCategoryRequest;
import com.financepulse.engine.application.usecases.category.CreateCategoryUseCase;
import com.financepulse.engine.application.usecases.category.DeleteCategoryUseCase;
import com.financepulse.engine.application.usecases.category.ListCategoriesUseCase;
import com.financepulse.engine.application.usecases.category.UpdateCategoryUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rotas protegidas por {@link AuthenticationInterceptor}. RF-023 (CRUD completo + subcategorias, ver ADR-0017). */
@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final ListCategoriesUseCase listCategoriesUseCase;
    private final CreateCategoryUseCase createCategoryUseCase;
    private final UpdateCategoryUseCase updateCategoryUseCase;
    private final DeleteCategoryUseCase deleteCategoryUseCase;

    public CategoryController(
            ListCategoriesUseCase listCategoriesUseCase,
            CreateCategoryUseCase createCategoryUseCase,
            UpdateCategoryUseCase updateCategoryUseCase,
            DeleteCategoryUseCase deleteCategoryUseCase) {
        this.listCategoriesUseCase = listCategoriesUseCase;
        this.createCategoryUseCase = createCategoryUseCase;
        this.updateCategoryUseCase = updateCategoryUseCase;
        this.deleteCategoryUseCase = deleteCategoryUseCase;
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> list(HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        ListCategoriesUseCase.Output output = listCategoriesUseCase.execute(new ListCategoriesUseCase.Input(userId));

        return ResponseEntity.ok(output.categories().stream().map(CategoryResponse::from).toList());
    }

    @PostMapping
    public ResponseEntity<CreateCategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request, HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        CreateCategoryUseCase.Output output =
                createCategoryUseCase.execute(new CreateCategoryUseCase.Input(userId, request.name(), request.parentCategoryId()));

        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateCategoryResponse(output.categoryId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(
            @PathVariable String id, @Valid @RequestBody UpdateCategoryRequest request, HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        updateCategoryUseCase.execute(new UpdateCategoryUseCase.Input(userId, id, request.name()));

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, HttpServletRequest httpRequest) {
        String userId = AuthenticationInterceptor.getAuthenticatedUserId(httpRequest);

        deleteCategoryUseCase.execute(new DeleteCategoryUseCase.Input(userId, id));

        return ResponseEntity.noContent().build();
    }
}

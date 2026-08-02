package com.financepulse.engine.domain.category;

import java.time.Instant;
import java.util.Optional;

/**
 * RF-023 (ver ADR-0017): CRUD completo com subcategorias. Hierarquia
 * limitada a 2 níveis — {@code parentCategoryId} nulo indica categoria de
 * nível superior; não nulo indica subcategoria. Uma subcategoria não pode
 * ter subcategorias próprias (validado no caso de uso, que tem acesso ao
 * repositório para verificar o nível do pai).
 */
public final class Category {

    private final String id;
    private final String userId;
    private final String name;
    private final String parentCategoryId;
    private final Instant createdAt;

    private Category(String id, String userId, String name, String parentCategoryId, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.parentCategoryId = parentCategoryId;
        this.createdAt = createdAt;
    }

    public static Category create(String id, String userId, String name, String parentCategoryId) {
        String validatedName = CategoryPolicy.assertValidName(name);
        return new Category(id, userId, validatedName, parentCategoryId, Instant.now());
    }

    public static Category reconstitute(String id, String userId, String name, String parentCategoryId, Instant createdAt) {
        return new Category(id, userId, name, parentCategoryId, createdAt);
    }

    /** RF-023: apenas o nome é editável — {@code parentCategoryId} é imutável após a criação (ver ADR-0017). */
    public Category withName(String newName) {
        String validatedName = CategoryPolicy.assertValidName(newName);
        return new Category(id, userId, validatedName, parentCategoryId, createdAt);
    }

    public boolean isSubcategory() {
        return parentCategoryId != null;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public Optional<String> getParentCategoryId() {
        return Optional.ofNullable(parentCategoryId);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

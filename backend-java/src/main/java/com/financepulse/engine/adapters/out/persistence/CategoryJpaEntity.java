package com.financepulse.engine.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "categories")
public class CategoryJpaEntity {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String name;

    @Column(name = "parent_category_id")
    private String parentCategoryId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CategoryJpaEntity() {
        // exigido pelo JPA/Hibernate
    }

    public CategoryJpaEntity(String id, String userId, String name, String parentCategoryId, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.parentCategoryId = parentCategoryId;
        this.createdAt = createdAt;
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

    public String getParentCategoryId() {
        return parentCategoryId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

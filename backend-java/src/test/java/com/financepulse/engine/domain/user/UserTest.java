package com.financepulse.engine.domain.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void aFreshlyRegisteredUserIsNotDeleted() {
        User user = User.register("user-1", Email.create("user@example.com"), "hashed-value");

        assertThat(user.isDeleted()).isFalse();
        assertThat(user.getDeletedAt()).isEmpty();
    }

    @Test
    void anonymizeReplacesEmailPasswordHashAndNameAndRecordsDeletedAt() {
        User user = User.reconstitute("user-1", Email.create("user@example.com"), "hashed-value", "Maria", Instant.now(), null, Role.CUSTOMER, null);
        Instant deletedAt = Instant.now();

        User anonymized = user.anonymize(Email.create("deleted-user-1@anonymized.financepulse.internal"), "unusable-hash", deletedAt);

        assertThat(anonymized.getEmail().toString()).isEqualTo("deleted-user-1@anonymized.financepulse.internal");
        assertThat(anonymized.getPasswordHash()).isEqualTo("unusable-hash");
        assertThat(anonymized.getName()).isNull();
        assertThat(anonymized.isDeleted()).isTrue();
        assertThat(anonymized.getDeletedAt()).contains(deletedAt);
    }

    @Test
    void anonymizePreservesIdAndCreatedAt() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        User user = User.reconstitute("user-1", Email.create("user@example.com"), "hashed-value", "Maria", createdAt, null, Role.CUSTOMER, null);

        User anonymized = user.anonymize(Email.create("deleted-user-1@anonymized.financepulse.internal"), "unusable-hash", Instant.now());

        assertThat(anonymized.getId()).isEqualTo("user-1");
        assertThat(anonymized.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void aFreshlyRegisteredUserIsACustomerAndIsNotSuspended() {
        User user = User.register("user-1", Email.create("user@example.com"), "hashed-value");

        assertThat(user.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(user.isSupportOperator()).isFalse();
        assertThat(user.isSuspended()).isFalse();
        assertThat(user.getSuspendedAt()).isEmpty();
    }

    @Test
    void suspendSetsSuspendedAtAndReactivateClearsIt() {
        User user = User.register("user-1", Email.create("user@example.com"), "hashed-value");
        Instant suspendedAt = Instant.now();

        User suspended = user.suspend(suspendedAt);
        assertThat(suspended.isSuspended()).isTrue();
        assertThat(suspended.getSuspendedAt()).contains(suspendedAt);

        User reactivated = suspended.reactivate();
        assertThat(reactivated.isSuspended()).isFalse();
        assertThat(reactivated.getSuspendedAt()).isEmpty();
    }

    @Test
    void suspensionDoesNotChangeEmailNameOrPasswordHash() {
        User user = User.reconstitute(
                "user-1", Email.create("user@example.com"), "hashed-value", "Maria", Instant.now(), null, Role.CUSTOMER, null);

        User suspended = user.suspend(Instant.now());

        assertThat(suspended.getEmail()).isEqualTo(user.getEmail());
        assertThat(suspended.getName()).isEqualTo(user.getName());
        assertThat(suspended.getPasswordHash()).isEqualTo(user.getPasswordHash());
    }

    @Test
    void anonymizePreservesRoleAndSuspensionState() {
        User user = User.reconstitute(
                "user-1", Email.create("user@example.com"), "hashed-value", "Maria", Instant.now(), null, Role.SUPPORT_OPERATOR, null);

        User anonymized = user.anonymize(Email.create("deleted-user-1@anonymized.financepulse.internal"), "unusable-hash", Instant.now());

        assertThat(anonymized.getRole()).isEqualTo(Role.SUPPORT_OPERATOR);
    }
}

package com.financepulse.engine.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.domain.user.errors.InvalidConsentVersionException;
import org.junit.jupiter.api.Test;

class ConsentRecordTest {

    @Test
    void createsAConsentRecordWithTheGivenVersion() {
        ConsentRecord record = ConsentRecord.create("consent-1", "user-1", "2026-08-01");

        assertThat(record.getVersion()).isEqualTo("2026-08-01");
        assertThat(record.getUserId()).isEqualTo("user-1");
        assertThat(record.getAcceptedAt()).isNotNull();
    }

    @Test
    void trimsTheVersionString() {
        ConsentRecord record = ConsentRecord.create("consent-1", "user-1", "  2026-08-01  ");

        assertThat(record.getVersion()).isEqualTo("2026-08-01");
    }

    @Test
    void rejectsABlankVersion() {
        assertThatThrownBy(() -> ConsentRecord.create("consent-1", "user-1", "   ")).isInstanceOf(InvalidConsentVersionException.class);
    }

    @Test
    void rejectsANullVersion() {
        assertThatThrownBy(() -> ConsentRecord.create("consent-1", "user-1", null)).isInstanceOf(InvalidConsentVersionException.class);
    }
}

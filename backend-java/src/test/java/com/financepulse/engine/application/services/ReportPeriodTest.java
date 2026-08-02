package com.financepulse.engine.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.financepulse.engine.domain.report.errors.InvalidReportPeriodException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ReportPeriodTest {

    @Test
    void acceptsAStartDateBeforeTheEndDate() {
        ReportPeriod period = new ReportPeriod(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertThat(period.contains(LocalDate.of(2026, 7, 15))).isTrue();
    }

    @Test
    void acceptsAStartDateEqualToTheEndDate() {
        ReportPeriod period = new ReportPeriod(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1));

        assertThat(period.contains(LocalDate.of(2026, 7, 1))).isTrue();
    }

    @Test
    void rejectsAStartDateAfterTheEndDate() {
        assertThatThrownBy(() -> new ReportPeriod(LocalDate.of(2026, 7, 31), LocalDate.of(2026, 7, 1)))
                .isInstanceOf(InvalidReportPeriodException.class);
    }

    @Test
    void excludesDatesOutsideTheRange() {
        ReportPeriod period = new ReportPeriod(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertThat(period.contains(LocalDate.of(2026, 6, 30))).isFalse();
        assertThat(period.contains(LocalDate.of(2026, 8, 1))).isFalse();
    }
}

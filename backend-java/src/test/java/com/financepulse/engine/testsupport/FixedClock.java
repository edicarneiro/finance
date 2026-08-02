package com.financepulse.engine.testsupport;

import com.financepulse.engine.application.ports.Clock;
import java.time.LocalDate;

public class FixedClock implements Clock {

    private final LocalDate fixedToday;

    public FixedClock(LocalDate fixedToday) {
        this.fixedToday = fixedToday;
    }

    @Override
    public LocalDate today() {
        return fixedToday;
    }
}

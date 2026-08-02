package com.financepulse.engine.adapters.out.time;

import com.financepulse.engine.application.ports.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class SystemClock implements Clock {

    @Override
    public LocalDate today() {
        return LocalDate.now();
    }
}

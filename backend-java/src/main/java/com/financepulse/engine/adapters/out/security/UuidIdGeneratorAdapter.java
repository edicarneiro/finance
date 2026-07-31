package com.financepulse.engine.adapters.out.security;

import com.financepulse.engine.application.ports.IdGenerator;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UuidIdGeneratorAdapter implements IdGenerator {

    @Override
    public String generate() {
        return UUID.randomUUID().toString();
    }
}

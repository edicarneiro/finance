package com.financepulse.engine.testsupport;

import com.financepulse.engine.application.ports.IdGenerator;

public class SequentialIdGenerator implements IdGenerator {

    private final String prefix;
    private int counter = 0;

    public SequentialIdGenerator() {
        this("id");
    }

    public SequentialIdGenerator(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public String generate() {
        counter += 1;
        return prefix + "-" + counter;
    }
}

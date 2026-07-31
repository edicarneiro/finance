package com.financepulse.engine.adapters.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidIdGeneratorAdapterTest {

    @Test
    void generatesAValidRandomUuidOnEachCall() {
        UuidIdGeneratorAdapter generator = new UuidIdGeneratorAdapter();

        String first = generator.generate();
        String second = generator.generate();

        assertThat(UUID.fromString(first)).isNotNull();
        assertThat(first).isNotEqualTo(second);
    }
}

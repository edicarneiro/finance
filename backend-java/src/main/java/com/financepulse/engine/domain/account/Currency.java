package com.financepulse.engine.domain.account;

import com.financepulse.engine.domain.account.errors.InvalidCurrencyException;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Código de moeda ISO 4217 (três letras). Sem suporte a conversão/consolidação
 * multi-moeda nesta fase — vision.md assume BRL como padrão do MVP (Seção 11);
 * multi-moeda é Pós-MVP (Seção 15). Ver ADR-0014.
 */
public final class Currency {

    private static final Pattern ISO_4217_PATTERN = Pattern.compile("^[A-Z]{3}$");

    private final String code;

    private Currency(String code) {
        this.code = code;
    }

    public static Currency create(String rawValue) {
        String normalized = rawValue == null ? "" : rawValue.trim().toUpperCase();

        if (!ISO_4217_PATTERN.matcher(normalized).matches()) {
            throw new InvalidCurrencyException(rawValue);
        }

        return new Currency(normalized);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Currency)) {
            return false;
        }
        return code.equals(((Currency) other).code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return code;
    }
}

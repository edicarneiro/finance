package com.financepulse.engine.domain.user;

import com.financepulse.engine.domain.user.errors.InvalidEmailException;
import java.util.Objects;
import java.util.regex.Pattern;

public final class Email {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final String value;

    private Email(String value) {
        this.value = value;
    }

    public static Email create(String rawValue) {
        String normalized = rawValue == null ? "" : rawValue.trim().toLowerCase();

        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new InvalidEmailException(rawValue);
        }

        return new Email(normalized);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Email)) {
            return false;
        }
        return value.equals(((Email) other).value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

package com.financepulse.engine.testsupport;

import com.financepulse.engine.application.ports.TokenService;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FakeTokenService implements TokenService {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("^token-for-(.+)$");

    @Override
    public String issue(String userId) {
        return "token-for-" + userId;
    }

    @Override
    public Optional<String> verify(String token) {
        Matcher matcher = TOKEN_PATTERN.matcher(token);
        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
    }
}

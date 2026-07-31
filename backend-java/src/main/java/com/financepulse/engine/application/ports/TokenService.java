package com.financepulse.engine.application.ports;

import java.util.Optional;

public interface TokenService {

    String issue(String userId);

    Optional<String> verify(String token);
}

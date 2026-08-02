package com.financepulse.engine.application.ports;

import com.financepulse.engine.domain.user.Email;
import com.financepulse.engine.domain.user.User;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findByEmail(Email email);

    Optional<User> findById(String id);

    void save(User user);

    void update(User user);
}

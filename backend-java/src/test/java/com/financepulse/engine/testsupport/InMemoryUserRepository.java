package com.financepulse.engine.testsupport;

import com.financepulse.engine.application.ports.UserRepository;
import com.financepulse.engine.domain.user.Email;
import com.financepulse.engine.domain.user.User;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryUserRepository implements UserRepository {

    private final Map<String, User> usersById = new LinkedHashMap<>();

    @Override
    public Optional<User> findByEmail(Email email) {
        return usersById.values().stream().filter(user -> user.getEmail().equals(email)).findFirst();
    }

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(usersById.get(id));
    }

    @Override
    public void save(User user) {
        usersById.put(user.getId(), user);
    }

    @Override
    public void update(User user) {
        usersById.put(user.getId(), user);
    }
}

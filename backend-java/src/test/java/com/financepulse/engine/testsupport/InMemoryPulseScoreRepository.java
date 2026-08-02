package com.financepulse.engine.testsupport;

import com.financepulse.engine.application.ports.PulseScoreRepository;
import com.financepulse.engine.domain.pulsescore.PulseScoreSnapshot;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InMemoryPulseScoreRepository implements PulseScoreRepository {

    private final Map<String, PulseScoreSnapshot> snapshotsById = new LinkedHashMap<>();

    @Override
    public List<PulseScoreSnapshot> findAllByUserId(String userId) {
        return snapshotsById.values().stream().filter(snapshot -> snapshot.getUserId().equals(userId)).toList();
    }

    @Override
    public void saveOrUpdate(PulseScoreSnapshot snapshot) {
        snapshotsById.values().stream()
                .filter(existing -> existing.getUserId().equals(snapshot.getUserId()) && existing.getScoreDate().equals(snapshot.getScoreDate()))
                .map(PulseScoreSnapshot::getId)
                .findFirst()
                .ifPresent(snapshotsById::remove);

        snapshotsById.put(snapshot.getId(), snapshot);
    }
}

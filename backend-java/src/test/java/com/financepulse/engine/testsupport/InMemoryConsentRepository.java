package com.financepulse.engine.testsupport;

import com.financepulse.engine.application.ports.ConsentRepository;
import com.financepulse.engine.domain.user.ConsentRecord;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InMemoryConsentRepository implements ConsentRepository {

    private final Map<String, ConsentRecord> recordsById = new LinkedHashMap<>();

    @Override
    public List<ConsentRecord> findAllByUserId(String userId) {
        return recordsById.values().stream().filter(record -> record.getUserId().equals(userId)).toList();
    }

    @Override
    public void save(ConsentRecord consentRecord) {
        recordsById.put(consentRecord.getId(), consentRecord);
    }
}

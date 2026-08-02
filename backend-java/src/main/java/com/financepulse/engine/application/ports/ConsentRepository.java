package com.financepulse.engine.application.ports;

import com.financepulse.engine.domain.user.ConsentRecord;
import java.util.List;

/** Toda leitura/escrita é escopada por userId na própria assinatura (RF-047, rules.md § 4). Append-only — sem update/delete. */
public interface ConsentRepository {

    List<ConsentRecord> findAllByUserId(String userId);

    void save(ConsentRecord consentRecord);
}

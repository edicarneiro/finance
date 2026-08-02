package com.financepulse.engine.application.ports;

import com.financepulse.engine.domain.pulsescore.PulseScoreSnapshot;
import java.util.List;

/** Toda leitura/escrita é escopada por userId na própria assinatura (RF-047, rules.md § 4). */
public interface PulseScoreRepository {

    List<PulseScoreSnapshot> findAllByUserId(String userId);

    /** Upsert por (userId, scoreDate) — no máximo um snapshot por usuário por dia civil (ver ADR-0020). */
    void saveOrUpdate(PulseScoreSnapshot snapshot);
}

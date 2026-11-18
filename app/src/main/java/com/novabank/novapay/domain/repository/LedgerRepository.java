package com.novabank.novapay.domain.repository;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.transaction.LedgerDirection;
import com.novabank.novapay.domain.model.transaction.LedgerEntry;

import java.time.Instant;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;

/**
 * Hợp đồng đọc sổ cái biến động số dư tại accounts/{accountId}/ledger (Pure Java 17).
 */
public interface LedgerRepository {

    Observable<AppResult<List<LedgerEntry>>> observeLedger(
            String accountId,
            LedgerDirection directionOrNull,
            Instant fromInclusive,
            Instant toExclusive
    );
}

package com.novabank.novapay.data.mapper;

import com.google.firebase.firestore.DocumentSnapshot;
import com.novabank.novapay.domain.model.transaction.LedgerDirection;
import com.novabank.novapay.domain.model.transaction.LedgerEntry;

import java.util.Map;

public final class LedgerMapper {

    private LedgerMapper() {
    }

    public static LedgerEntry fromSnapshot(DocumentSnapshot snapshot, String accountId) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }
        return fromMap(snapshot.getId(), accountId, snapshot.getData());
    }

    public static LedgerEntry fromMap(String entryId, String accountId, Map<String, Object> data) {
        if (entryId == null || accountId == null || data == null) {
            return null;
        }
        return new LedgerEntry(
                entryId,
                accountId,
                FirestoreValueMapper.toStringOrEmpty(data.get("txId")),
                LedgerDirection.fromString(FirestoreValueMapper.toStringOrEmpty(data.get("direction"))),
                FirestoreValueMapper.toLong(data.get("amount"), 0L),
                FirestoreValueMapper.toLong(data.get("balanceAfter"), 0L),
                FirestoreValueMapper.toStringOrEmpty(data.get("description")),
                FirestoreValueMapper.toInstant(data.get("createdAt"))
        );
    }
}

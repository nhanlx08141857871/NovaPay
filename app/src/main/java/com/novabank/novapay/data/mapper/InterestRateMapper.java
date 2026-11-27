package com.novabank.novapay.data.mapper;

import com.google.firebase.firestore.DocumentSnapshot;
import com.novabank.novapay.domain.model.rate.InterestRate;

import java.util.Map;

public final class InterestRateMapper {

    private InterestRateMapper() {
    }

    public static InterestRate fromSnapshot(DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }
        return fromMap(snapshot.getId(), snapshot.getData());
    }

    public static InterestRate fromMap(String rateId, Map<String, Object> data) {
        if (rateId == null || data == null) {
            return null;
        }
        return new InterestRate(
                rateId,
                FirestoreValueMapper.toInt(data.get("termMonths"), 0),
                FirestoreValueMapper.toDouble(data.get("annualRate"), 0.0),
                FirestoreValueMapper.toInstant(data.get("effectiveFrom")),
                FirestoreValueMapper.toStringOrEmpty(data.get("updatedBy")),
                FirestoreValueMapper.toInstant(data.get("updatedAt"))
        );
    }
}

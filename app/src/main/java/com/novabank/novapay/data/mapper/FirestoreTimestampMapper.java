package com.novabank.novapay.data.mapper;

import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;

import java.time.Instant;

public final class FirestoreTimestampMapper {

    private FirestoreTimestampMapper() {
    }

    @Nullable
    public static Instant toInstant(@Nullable Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanoseconds());
    }

    @Nullable
    public static Timestamp toTimestamp(@Nullable Instant instant) {
        if (instant == null) {
            return null;
        }
        return new Timestamp(instant.getEpochSecond(), instant.getNano());
    }
}

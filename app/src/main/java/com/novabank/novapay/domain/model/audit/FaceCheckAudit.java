package com.novabank.novapay.domain.model.audit;

import java.time.Instant;
import java.util.Objects;

public final class FaceCheckAudit {

    private final String draftId;
    private final String uid;
    private final String txId;
    private final String imageBase64;
    private final boolean manualAuditRequired;
    private final Instant createdAt;

    public FaceCheckAudit(
            String draftId,
            String uid,
            String txId,
            String imageBase64,
            boolean manualAuditRequired,
            Instant createdAt
    ) {
        this.draftId = Objects.requireNonNull(draftId, "draftId must not be null");
        this.uid = uid != null ? uid : "";
        this.txId = txId;
        this.imageBase64 = imageBase64;
        this.manualAuditRequired = manualAuditRequired;
        this.createdAt = createdAt;
    }

    public String getDraftId() {
        return draftId;
    }

    public String getUid() {
        return uid;
    }

    public String getTxId() {
        return txId;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public boolean isManualAuditRequired() {
        return manualAuditRequired;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FaceCheckAudit that)) return false;
        return manualAuditRequired == that.manualAuditRequired
                && Objects.equals(draftId, that.draftId)
                && Objects.equals(uid, that.uid)
                && Objects.equals(txId, that.txId)
                && Objects.equals(imageBase64, that.imageBase64)
                && Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(draftId, uid, txId, imageBase64, manualAuditRequired, createdAt);
    }
}

package com.novabank.novapay.domain.model.user;

import java.time.Instant;
import java.util.Objects;

public final class EkycInfo {

    private final EkycStatus status;
    private final String verifiedBy;
    private final Instant verifiedAt;

    public EkycInfo(EkycStatus status, String verifiedBy, Instant verifiedAt) {
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.verifiedBy = verifiedBy != null ? verifiedBy : "";
        this.verifiedAt = verifiedAt;
    }

    public EkycStatus getStatus() {
        return status;
    }

    public String getVerifiedBy() {
        return verifiedBy;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public boolean isVerified() {
        return status == EkycStatus.VERIFIED;
    }
}

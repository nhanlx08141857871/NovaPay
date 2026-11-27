package com.novabank.novapay.domain.model.transaction;

import java.time.Instant;
import java.util.Objects;

public final class TransactionDraft {

    private final String draftId;
    private final boolean requiresBiometric;
    private final boolean requiresFace;
    private final Instant otpExpiresAt;

    public TransactionDraft(
            String draftId,
            boolean requiresBiometric,
            boolean requiresFace,
            Instant otpExpiresAt
    ) {
        this.draftId = Objects.requireNonNull(draftId, "draftId must not be null");
        this.requiresBiometric = requiresBiometric;
        this.requiresFace = requiresFace;
        this.otpExpiresAt = otpExpiresAt;
    }

    public String getDraftId() {
        return draftId;
    }

    public boolean isRequiresBiometric() {
        return requiresBiometric;
    }

    public boolean isRequiresFace() {
        return requiresFace;
    }

    public Instant getOtpExpiresAt() {
        return otpExpiresAt;
    }
}

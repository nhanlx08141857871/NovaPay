package com.novabank.novapay.domain.model.audit;

import java.time.Instant;
import java.util.Objects;

public final class AuditLog {

    private final String logId;
    private final String actorUid;
    private final String action;
    private final String targetId;
    private final String details;
    private final Instant createdAt;

    public AuditLog(
            String logId,
            String actorUid,
            String action,
            String targetId,
            String details,
            Instant createdAt
    ) {
        this.logId = Objects.requireNonNull(logId, "logId must not be null");
        this.actorUid = actorUid != null ? actorUid : "";
        this.action = action != null ? action : "";
        this.targetId = targetId != null ? targetId : "";
        this.details = details != null ? details : "";
        this.createdAt = createdAt;
    }

    public String getLogId() {
        return logId;
    }

    public String getActorUid() {
        return actorUid;
    }

    public String getAction() {
        return action;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getDetails() {
        return details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuditLog that)) return false;
        return Objects.equals(logId, that.logId)
                && Objects.equals(actorUid, that.actorUid)
                && Objects.equals(action, that.action)
                && Objects.equals(targetId, that.targetId)
                && Objects.equals(details, that.details)
                && Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(logId, actorUid, action, targetId, details, createdAt);
    }
}

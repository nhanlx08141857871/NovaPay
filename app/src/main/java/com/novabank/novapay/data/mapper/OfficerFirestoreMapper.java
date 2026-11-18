package com.novabank.novapay.data.mapper;

import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.GeoPoint;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.domain.model.audit.AuditLog;
import com.novabank.novapay.domain.model.audit.FaceCheckAudit;
import com.novabank.novapay.domain.model.branch.Branch;
import com.novabank.novapay.domain.model.branch.BranchType;
import com.novabank.novapay.domain.model.branch.GeoLocation;
import com.novabank.novapay.domain.model.rate.InterestRate;
import com.novabank.novapay.domain.model.user.CustomerProfile;

import java.time.Instant;
import java.util.Map;

public final class OfficerFirestoreMapper {

    private OfficerFirestoreMapper() {
    }

    @Nullable
    public static CustomerProfile toCustomerProfile(@Nullable DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            return null;
        }
        String ekycStatus = "NONE";
        String ekycVerifiedBy = null;
        Instant ekycVerifiedAt = null;

        Object rawEkyc = doc.get("ekyc");
        if (rawEkyc instanceof Map<?, ?> ekycMap) {
            Object rawStatus = ekycMap.get("status");
            if (rawStatus instanceof String s && !s.trim().isEmpty()) {
                ekycStatus = s;
            }
            Object rawVerifiedBy = ekycMap.get("verifiedBy");
            if (rawVerifiedBy instanceof String s) {
                ekycVerifiedBy = s;
            }
            Object rawVerifiedAt = ekycMap.get("verifiedAt");
            if (rawVerifiedAt instanceof Timestamp ts) {
                ekycVerifiedAt = FirestoreTimestampMapper.toInstant(ts);
            }
        }

        return new CustomerProfile(
                doc.getId(),
                safeString(doc.getString("role"), Constants.ROLE_CUSTOMER),
                safeString(doc.getString("fullName"), ""),
                safeString(doc.getString("email"), ""),
                safeString(doc.getString("phone"), ""),
                safeString(doc.getString("nationalId"), ""),
                safeString(doc.getString("dob"), ""),
                safeString(doc.getString("address"), ""),
                safeString(doc.getString("avatarUrl"), ""),
                safeString(doc.getString("status"), Constants.USER_STATUS_ACTIVE),
                ekycStatus,
                ekycVerifiedBy,
                ekycVerifiedAt,
                safeString(doc.getString("createdBy"), ""),
                FirestoreTimestampMapper.toInstant(doc.getTimestamp("createdAt")),
                FirestoreTimestampMapper.toInstant(doc.getTimestamp("updatedAt"))
        );
    }

    @Nullable
    public static AuditLog toAuditLog(@Nullable DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            return null;
        }
        String detailsText = "";
        Object rawDetails = doc.get("details");
        if (rawDetails instanceof String s) {
            detailsText = s;
        } else if (rawDetails instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (sb.length() > 0) {
                    sb.append(" • ");
                }
                sb.append(entry.getKey()).append(": ").append(entry.getValue());
            }
            detailsText = sb.toString();
        } else if (rawDetails != null) {
            detailsText = String.valueOf(rawDetails);
        }

        return new AuditLog(
                doc.getId(),
                safeString(doc.getString("actorUid"), ""),
                safeString(doc.getString("action"), ""),
                safeString(doc.getString("targetId"), ""),
                detailsText,
                FirestoreTimestampMapper.toInstant(doc.getTimestamp("createdAt"))
        );
    }

    @Nullable
    public static FaceCheckAudit toFaceCheckAudit(@Nullable DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            return null;
        }
        Boolean manual = doc.getBoolean("manualAuditRequired");
        return new FaceCheckAudit(
                doc.getId(),
                safeString(doc.getString("uid"), ""),
                doc.getString("txId"),
                doc.getString("imageBase64"),
                Boolean.TRUE.equals(manual),
                FirestoreTimestampMapper.toInstant(doc.getTimestamp("createdAt"))
        );
    }

    @Nullable
    public static InterestRate toInterestRate(@Nullable DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            return null;
        }
        Long rawTerm = doc.getLong("termMonths");
        Double rawRate = doc.getDouble("annualRate");
        if (rawTerm == null || rawRate == null) {
            return null;
        }
        return new InterestRate(
                doc.getId(),
                rawTerm.intValue(),
                rawRate,
                FirestoreTimestampMapper.toInstant(doc.getTimestamp("effectiveFrom")),
                safeString(doc.getString("updatedBy"), ""),
                FirestoreTimestampMapper.toInstant(doc.getTimestamp("updatedAt"))
        );
    }

    @Nullable
    public static Branch toBranch(@Nullable DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            return null;
        }
        GeoPoint geoPoint = doc.getGeoPoint("location");
        GeoLocation location = GeoPointMapper.toDomain(geoPoint);
        if (location == null) {
            return null;
        }
        String rawType = safeString(doc.getString("type"), "BRANCH");
        BranchType type = "ATM".equalsIgnoreCase(rawType) ? BranchType.ATM : BranchType.BRANCH;
        return new Branch(
                doc.getId(),
                safeString(doc.getString("name"), ""),
                safeString(doc.getString("address"), ""),
                location,
                safeString(doc.getString("phone"), ""),
                safeString(doc.getString("openHours"), ""),
                type
        );
    }

    private static String safeString(@Nullable String value, String fallback) {
        return value != null ? value : fallback;
    }
}

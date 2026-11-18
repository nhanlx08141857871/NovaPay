package com.novabank.novapay.domain.model.user;

import com.novabank.novapay.core.common.Constants;

import java.time.Instant;
import java.util.Objects;

public final class CustomerProfile {

    private final String uid;
    private final String role;
    private final String fullName;
    private final String email;
    private final String phone;
    private final String nationalId;
    private final String dob;
    private final String address;
    private final String avatarUrl;
    private final String status;
    private final String ekycStatus;
    private final String ekycVerifiedBy;
    private final Instant ekycVerifiedAt;
    private final String createdBy;
    private final Instant createdAt;
    private final Instant updatedAt;

    public CustomerProfile(
            String uid,
            String role,
            String fullName,
            String email,
            String phone,
            String nationalId,
            String dob,
            String address,
            String avatarUrl,
            String status,
            String ekycStatus,
            String ekycVerifiedBy,
            Instant ekycVerifiedAt,
            String createdBy,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.uid = Objects.requireNonNull(uid, "uid must not be null");
        this.role = role != null ? role : Constants.ROLE_CUSTOMER;
        this.fullName = fullName != null ? fullName : "";
        this.email = email != null ? email : "";
        this.phone = phone != null ? phone : "";
        this.nationalId = nationalId != null ? nationalId : "";
        this.dob = dob != null ? dob : "";
        this.address = address != null ? address : "";
        this.avatarUrl = avatarUrl != null ? avatarUrl : "";
        this.status = status != null ? status : Constants.USER_STATUS_ACTIVE;
        this.ekycStatus = ekycStatus != null ? ekycStatus : "NONE";
        this.ekycVerifiedBy = ekycVerifiedBy;
        this.ekycVerifiedAt = ekycVerifiedAt;
        this.createdBy = createdBy != null ? createdBy : "";
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getUid() {
        return uid;
    }

    public String getRole() {
        return role;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getNationalId() {
        return nationalId;
    }

    public String getDob() {
        return dob;
    }

    public String getAddress() {
        return address;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getStatus() {
        return status;
    }

    public String getEkycStatus() {
        return ekycStatus;
    }

    public String getEkycVerifiedBy() {
        return ekycVerifiedBy;
    }

    public Instant getEkycVerifiedAt() {
        return ekycVerifiedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isActive() {
        return Constants.USER_STATUS_ACTIVE.equalsIgnoreCase(status);
    }

    public boolean isLocked() {
        return Constants.USER_STATUS_LOCKED.equalsIgnoreCase(status);
    }

    public boolean isEkycVerified() {
        return Constants.EKYC_STATUS_VERIFIED.equalsIgnoreCase(ekycStatus);
    }

    public boolean isEkycPending() {
        return Constants.EKYC_STATUS_PENDING.equalsIgnoreCase(ekycStatus);
    }

    public boolean isEkycRejected() {
        return Constants.EKYC_STATUS_REJECTED.equalsIgnoreCase(ekycStatus);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomerProfile that)) return false;
        return Objects.equals(uid, that.uid)
                && Objects.equals(role, that.role)
                && Objects.equals(fullName, that.fullName)
                && Objects.equals(email, that.email)
                && Objects.equals(phone, that.phone)
                && Objects.equals(nationalId, that.nationalId)
                && Objects.equals(dob, that.dob)
                && Objects.equals(address, that.address)
                && Objects.equals(status, that.status)
                && Objects.equals(ekycStatus, that.ekycStatus)
                && Objects.equals(updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid, role, fullName, email, phone, nationalId, dob, address, status, ekycStatus, updatedAt);
    }
}

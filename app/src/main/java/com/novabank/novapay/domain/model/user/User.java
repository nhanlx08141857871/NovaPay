package com.novabank.novapay.domain.model.user;

import java.time.Instant;
import java.util.Objects;

/**
 * Thực thể Người dùng chuẩn Pure Java 17 (tầng domain).
 */
public final class User {

    private final String uid;
    private final UserRole role;
    private final String fullName;
    private final String email;
    private final String phone;
    private final String nationalId;
    private final String dob;
    private final String address;
    private final String avatarUrl;
    private final UserStatus status;
    private final EkycInfo ekyc;
    private final String createdBy;
    private final Instant createdAt;
    private final Instant updatedAt;

    public User(
            String uid,
            UserRole role,
            String fullName,
            String email,
            String phone,
            String nationalId,
            String dob,
            String address,
            String avatarUrl,
            UserStatus status,
            EkycInfo ekyc,
            String createdBy,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.uid = Objects.requireNonNull(uid, "uid must not be null");
        this.role = role != null ? role : UserRole.CUSTOMER;
        this.fullName = fullName != null ? fullName : "";
        this.email = email != null ? email : "";
        this.phone = phone != null ? phone : "";
        this.nationalId = nationalId != null ? nationalId : "";
        this.dob = dob != null ? dob : "";
        this.address = address != null ? address : "";
        this.avatarUrl = avatarUrl != null ? avatarUrl : "";
        this.status = status != null ? status : UserStatus.ACTIVE;
        this.ekyc = ekyc != null ? ekyc : new EkycInfo(EkycStatus.UNVERIFIED, "", null);
        this.createdBy = createdBy != null ? createdBy : "";
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getUid() {
        return uid;
    }

    public UserRole getRole() {
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

    public UserStatus getStatus() {
        return status;
    }

    public EkycInfo getEkyc() {
        return ekyc;
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
        return status == UserStatus.ACTIVE;
    }

    public boolean isEkycVerified() {
        return ekyc.isVerified();
    }
}

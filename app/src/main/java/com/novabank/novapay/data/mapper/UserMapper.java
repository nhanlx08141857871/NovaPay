package com.novabank.novapay.data.mapper;

import com.google.firebase.firestore.DocumentSnapshot;
import com.novabank.novapay.domain.model.user.EkycInfo;
import com.novabank.novapay.domain.model.user.EkycStatus;
import com.novabank.novapay.domain.model.user.User;
import com.novabank.novapay.domain.model.user.UserRole;
import com.novabank.novapay.domain.model.user.UserSecurityInfo;
import com.novabank.novapay.domain.model.user.UserStatus;

import java.util.Map;

/**
 * Chuyển đổi Firestore Document users/{uid} sang domain model User.
 */
public final class UserMapper {

    private UserMapper() {
    }

    public static User fromSnapshot(DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }
        return fromMap(snapshot.getId(), snapshot.getData());
    }

    public static User fromMap(String uid, Map<String, Object> data) {
        if (uid == null || data == null) {
            return null;
        }
        Map<String, Object> ekycMap = FirestoreValueMapper.toMap(data.get("ekyc"));
        EkycInfo ekycInfo;
        if (ekycMap != null) {
            ekycInfo = new EkycInfo(
                    EkycStatus.fromString(FirestoreValueMapper.toStringOrEmpty(ekycMap.get("status"))),
                    FirestoreValueMapper.toStringOrEmpty(ekycMap.get("verifiedBy")),
                    FirestoreValueMapper.toInstant(ekycMap.get("verifiedAt"))
            );
        } else {
            ekycInfo = new EkycInfo(EkycStatus.UNVERIFIED, "", null);
        }

        Map<String, Object> securityMap = FirestoreValueMapper.toMap(data.get("security"));
        UserSecurityInfo securityInfo;
        if (securityMap != null) {
            boolean hasPin = Boolean.TRUE.equals(securityMap.get("hasPin"));
            securityInfo = new UserSecurityInfo(
                    hasPin,
                    FirestoreValueMapper.toInstant(securityMap.get("pinUpdatedAt"))
            );
        } else {
            securityInfo = new UserSecurityInfo(false, null);
        }

        return new User(
                uid,
                UserRole.fromString(FirestoreValueMapper.toStringOrEmpty(data.get("role"))),
                FirestoreValueMapper.toStringOrEmpty(data.get("fullName")),
                FirestoreValueMapper.toStringOrEmpty(data.get("email")),
                FirestoreValueMapper.toStringOrEmpty(data.get("phone")),
                FirestoreValueMapper.toStringOrEmpty(data.get("nationalId")),
                FirestoreValueMapper.toStringOrEmpty(data.get("dob")),
                FirestoreValueMapper.toStringOrEmpty(data.get("address")),
                FirestoreValueMapper.toStringOrEmpty(data.get("avatarUrl")),
                UserStatus.fromString(FirestoreValueMapper.toStringOrEmpty(data.get("status"))),
                ekycInfo,
                securityInfo,
                FirestoreValueMapper.toStringOrEmpty(data.get("createdBy")),
                FirestoreValueMapper.toInstant(data.get("createdAt")),
                FirestoreValueMapper.toInstant(data.get("updatedAt"))
        );
    }
}

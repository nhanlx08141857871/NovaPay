package com.novabank.novapay.domain.repository;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.user.UserRole;

import io.reactivex.rxjava3.core.Single;

/**
 * Hợp đồng xác thực phiên đăng nhập và phân quyền Custom Claim (Pure Java 17).
 */
public interface AuthRepository {

    Single<AppResult<UserRole>> signIn(String email, String password);

    Single<AppResult<UserRole>> refreshRole();

    void signOut();

    boolean isSignedIn();

    String getCurrentUid();

    Single<AppResult<Boolean>> changePassword(String currentPassword, String newPassword);

    Single<AppResult<Boolean>> sendPasswordResetEmail(String email);
}

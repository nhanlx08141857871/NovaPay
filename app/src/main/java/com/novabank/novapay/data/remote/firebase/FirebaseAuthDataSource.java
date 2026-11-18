package com.novabank.novapay.data.remote.firebase;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.core.security.SessionManager;
import com.novabank.novapay.domain.model.user.UserRole;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Single;

/**
 * Nguồn dữ liệu xác thực Firebase Auth và đọc Custom Claim role.
 */
@Singleton
public class FirebaseAuthDataSource {

    private final FirebaseAuth firebaseAuth;
    private final SessionManager sessionManager;

    @Inject
    public FirebaseAuthDataSource(FirebaseAuth firebaseAuth, SessionManager sessionManager) {
        this.firebaseAuth = firebaseAuth;
        this.sessionManager = sessionManager;
    }

    public Single<AppResult<UserRole>> signIn(String email, String password) {
        return Single.create(emitter ->
                firebaseAuth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener(authResult -> {
                            FirebaseUser user = authResult.getUser();
                            if (user == null) {
                                if (!emitter.isDisposed()) {
                                    emitter.onSuccess(AppResult.error(
                                            "AUTH_FAILED",
                                            "Không thể lấy thông tin phiên đăng nhập."
                                    ));
                                }
                                return;
                            }
                            fetchRoleFromToken(user, emitter);
                        })
                        .addOnFailureListener(ex -> {
                            if (!emitter.isDisposed()) {
                                String code = (ex instanceof FirebaseAuthInvalidUserException
                                        || ex instanceof FirebaseAuthInvalidCredentialsException)
                                        ? "INVALID_CREDENTIALS"
                                        : "AUTH_ERROR";
                                String message = "INVALID_CREDENTIALS".equals(code)
                                        ? "Email hoặc mật khẩu không chính xác."
                                        : (ex.getMessage() != null ? ex.getMessage() : "Đăng nhập thất bại.");
                                emitter.onSuccess(AppResult.error(code, message));
                            }
                        })
        );
    }

    public Single<AppResult<UserRole>> refreshRole() {
        return Single.create(emitter -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user == null) {
                if (!emitter.isDisposed()) {
                    emitter.onSuccess(AppResult.error("UNAUTHENTICATED", "Chưa đăng nhập."));
                }
                return;
            }
            fetchRoleFromToken(user, emitter);
        });
    }

    public void signOut() {
        sessionManager.signOut();
    }

    public boolean isSignedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    @Nullable
    public String getCurrentUid() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    private void fetchRoleFromToken(
            FirebaseUser user,
            io.reactivex.rxjava3.core.SingleEmitter<AppResult<UserRole>> emitter
    ) {
        user.getIdToken(true)
                .addOnSuccessListener(tokenResult -> {
                    Map<String, Object> claims = tokenResult.getClaims();
                    Object rawRole = claims != null ? claims.get("role") : null;
                    UserRole role = UserRole.fromString(rawRole != null ? String.valueOf(rawRole) : Constants.ROLE_CUSTOMER);
                    sessionManager.updateCachedRole(role.asClaimValue());
                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(AppResult.success(role));
                    }
                })
                .addOnFailureListener(ex -> {
                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(AppResult.error(
                                "TOKEN_ERROR",
                                ex.getMessage() != null ? ex.getMessage() : "Không thể xác thực quyền tài khoản."
                        ));
                    }
                });
    }
}

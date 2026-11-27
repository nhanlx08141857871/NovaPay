package com.novabank.novapay.core.security;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.core.common.SingleLiveEvent;
import com.novabank.novapay.core.notification.BalanceAlertSubscriber;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Quản lý phiên đăng nhập toàn cục của NovaPay:
 * - Lắng nghe realtime users/{uid}: nếu status == "LOCKED", cưỡng bức signOut() ngay lập tức.
 * - Nạp Custom Claim role ("CUSTOMER" / "OFFICER") từ Firebase ID Token.
 * - Lưu pendingDeepLink và kiểm tra phân quyền DeepLink theo vai trò.
 */
@Singleton
public class SessionManager {

    public static final String DEEPLINK_OFFICER = "novapay://officer";
    public static final String DEEPLINK_CHATBOT = "novapay://chatbot";
    public static final String DEEPLINK_QR = "novapay://qr";
    public static final String DEEPLINK_DEPOSIT = "novapay://deposit";
    public static final String DEEPLINK_FACE_CHECK_PREFIX = "novapay://face-check?draftId=";

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;
    private final BalanceAlertSubscriber balanceAlertSubscriber;
    private final SingleLiveEvent<Boolean> accountLockedEvent = new SingleLiveEvent<>();
    private final MutableLiveData<String> roleClaimLiveData = new MutableLiveData<>();

    private ListenerRegistration userDocListener;
    private String observedUid;
    private volatile String cachedRole = Constants.ROLE_CUSTOMER;
    private volatile boolean roleClaimLoaded = false;
    private volatile Uri pendingDeepLink;

    private final FirebaseAuth.AuthStateListener authStateListener = auth -> {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            stopUserListener();
            cachedRole = Constants.ROLE_CUSTOMER;
            roleClaimLoaded = false;
        } else {
            attachUserListener(user.getUid());
            refreshRoleClaimAsync(user);
        }
    };

    @Inject
    public SessionManager(
            FirebaseAuth firebaseAuth,
            FirebaseFirestore firestore,
            BalanceAlertSubscriber balanceAlertSubscriber
    ) {
        this.firebaseAuth = firebaseAuth;
        this.firestore = firestore;
        this.balanceAlertSubscriber = balanceAlertSubscriber;
        this.firebaseAuth.addAuthStateListener(authStateListener);
        FirebaseUser current = this.firebaseAuth.getCurrentUser();
        if (current != null) {
            attachUserListener(current.getUid());
            refreshRoleClaimAsync(current);
        }
    }

    public SingleLiveEvent<Boolean> getAccountLockedEvent() {
        return accountLockedEvent;
    }

    public LiveData<String> getRoleClaimLiveData() {
        return roleClaimLiveData;
    }

    public boolean isSignedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    @Nullable
    public String getCurrentUid() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    @NonNull
    public String getCachedRole() {
        return cachedRole;
    }

    public boolean isRoleClaimLoaded() {
        return roleClaimLoaded;
    }

    public void updateCachedRole(@Nullable String role) {
        if (Constants.ROLE_OFFICER.equalsIgnoreCase(role)) {
            this.cachedRole = Constants.ROLE_OFFICER;
        } else {
            this.cachedRole = Constants.ROLE_CUSTOMER;
        }
        this.roleClaimLoaded = true;
        this.roleClaimLiveData.postValue(this.cachedRole);
    }

    public void setPendingDeepLink(@Nullable Uri uri) {
        this.pendingDeepLink = uri;
    }

    @Nullable
    public Uri peekPendingDeepLink() {
        return pendingDeepLink;
    }

    @Nullable
    public Uri consumePendingDeepLinkIfAllowed(@NonNull String role, @Nullable String currentUid) {
        Uri uri = this.pendingDeepLink;
        this.pendingDeepLink = null;
        if (uri == null) {
            return null;
        }
        return isDeepLinkAllowedForRole(uri.toString(), role, currentUid) ? uri : null;
    }

    public void startUserSession() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            attachUserListener(user.getUid());
            refreshRoleClaimAsync(user);
        }
    }

    public void signOut() {
        pendingDeepLink = null;
        stopUserListener();
        cachedRole = Constants.ROLE_CUSTOMER;
        roleClaimLoaded = false;
        firebaseAuth.signOut();
    }

    private void refreshRoleClaimAsync(@NonNull FirebaseUser user) {
        user.getIdToken(false)
                .addOnSuccessListener(result -> {
                    Map<String, Object> claims = result.getClaims();
                    Object rawRole = claims != null ? claims.get("role") : null;
                    updateCachedRole(rawRole != null ? String.valueOf(rawRole) : Constants.ROLE_CUSTOMER);
                })
                .addOnFailureListener(ex -> {
                    if (!roleClaimLoaded && firebaseAuth.getCurrentUser() != null) {
                        updateCachedRole(cachedRole);
                    }
                });
    }

    private synchronized void attachUserListener(@NonNull String uid) {
        if (uid.equals(observedUid) && userDocListener != null) {
            return;
        }
        stopUserListener();
        observedUid = uid;
        userDocListener = firestore.collection("users")
                .document(uid)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || !snapshot.exists()) {
                        return;
                    }
                    String status = snapshot.getString("status");
                    if (Constants.USER_STATUS_LOCKED.equalsIgnoreCase(status)) {
                        forceLogoutDueToLock();
                    }
                });
        balanceAlertSubscriber.start(uid);
    }

    private synchronized void stopUserListener() {
        if (userDocListener != null) {
            userDocListener.remove();
            userDocListener = null;
        }
        observedUid = null;
        balanceAlertSubscriber.stop();
    }

    private void forceLogoutDueToLock() {
        signOut();
        accountLockedEvent.postValue(Boolean.TRUE);
    }

    public static boolean isDeepLinkAllowedForRole(@Nullable String uriString, @Nullable String role) {
        return isDeepLinkAllowedForRole(uriString, role, null);
    }

    public static boolean isDeepLinkAllowedForRole(
            @Nullable String uriString,
            @Nullable String role,
            @Nullable String currentUid
    ) {
        if (uriString == null || uriString.trim().isEmpty() || role == null || role.trim().isEmpty()) {
            return false;
        }
        URI parsed;
        try {
            parsed = URI.create(uriString.trim());
        } catch (IllegalArgumentException ex) {
            return false;
        }
        if (parsed.getScheme() == null || !Constants.DEEPLINK_SCHEME.equalsIgnoreCase(parsed.getScheme())) {
            return false;
        }
        String host = parsed.getHost() != null ? parsed.getHost().toLowerCase(Locale.ROOT) : "";
        boolean isOfficer = Constants.ROLE_OFFICER.equalsIgnoreCase(role.trim());

        switch (host) {
            case "officer":
                return isOfficer;
            case "home":
            case "accounts":
            case "transactions":
            case "transfer":
            case "qr":
            case "deposit":
            case "bills":
            case "topup":
            case "services":
            case "chatbot":
            case "face-check":
            case "notifications":
            case "savings":
            case "security":
            case "receipt":
                return !isOfficer;
            case "branches":
            case "profile":
                return true;
            case "ekyc":
                if (isOfficer) {
                    return true;
                }
                String targetUid = extractQueryParam(parsed.getRawQuery(), "targetUid");
                if (targetUid == null || targetUid.isEmpty()) {
                    return true;
                }
                return currentUid != null && targetUid.equals(currentUid);
            default:
                return false;
        }
    }

    @Nullable
    private static String extractQueryParam(@Nullable String rawQuery, @NonNull String key) {
        if (rawQuery == null || rawQuery.isEmpty()) {
            return null;
        }
        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String k = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
            if (key.equals(k)) {
                return URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8).trim();
            }
        }
        return null;
    }
}

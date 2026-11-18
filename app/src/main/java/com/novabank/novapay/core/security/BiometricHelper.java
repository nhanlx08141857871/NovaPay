package com.novabank.novapay.core.security;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.concurrent.Executor;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Hỗ trợ xác thực sinh trắc học chuẩn SPEC_CHOT.md Q1:
 * Sử dụng BIOMETRIC_STRONG | DEVICE_CREDENTIAL để tự động fallback về PIN/Pattern/Mật khẩu
 * trên máy ảo hoặc thiết bị không có cảm biến vân tay (TUYỆT ĐỐI không gọi setNegativeButtonText).
 */
@Singleton
public class BiometricHelper {

    public static final int ALLOWED_AUTHENTICATORS =
            BiometricManager.Authenticators.BIOMETRIC_STRONG
                    | BiometricManager.Authenticators.DEVICE_CREDENTIAL;

    public interface AuthCallback {
        void onAuthSucceeded();

        void onAuthError(int errorCode, @NonNull String message);
    }

    @Inject
    public BiometricHelper() {
    }

    public boolean canAuthenticate(@NonNull Context context) {
        BiometricManager biometricManager = BiometricManager.from(context);
        int status = biometricManager.canAuthenticate(ALLOWED_AUTHENTICATORS);
        return status == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public void authenticate(
            @NonNull Fragment fragment,
            @NonNull String title,
            @NonNull String subtitle,
            @NonNull AuthCallback callback
    ) {
        Executor executor = ContextCompat.getMainExecutor(fragment.requireContext());
        BiometricPrompt prompt = new BiometricPrompt(
                fragment,
                executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        callback.onAuthSucceeded();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        callback.onAuthError(errorCode, errString.toString());
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                    }
                }
        );

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
                .build();

        prompt.authenticate(promptInfo);
    }
}

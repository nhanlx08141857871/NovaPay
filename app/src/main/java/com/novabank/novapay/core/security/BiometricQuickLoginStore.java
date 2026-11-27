package com.novabank.novapay.core.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Kho lưu trữ an toàn thông tin đăng nhập nhanh sinh trắc học được mã hóa AES/GCM/NoPadding
 * bằng khóa bí mật lưu trong Android Keystore.
 */
@Singleton
public class BiometricQuickLoginStore {

    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "novapay_biometric_quick_login_key_v1";
    private static final String PREFS_NAME = "novapay_biometric_quick_login_prefs";
    private static final String KEY_ENABLED = "quick_login_enabled";
    private static final String KEY_EMAIL_HINT = "quick_login_email_hint";
    private static final String KEY_CIPHERTEXT = "quick_login_ciphertext";
    private static final String KEY_IV = "quick_login_iv";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;

    public static final class Credentials {
        public final String email;
        public final String password;

        public Credentials(@NonNull String email, @NonNull String password) {
            this.email = email;
            this.password = password;
        }
    }

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    @Inject
    public BiometricQuickLoginStore(@ApplicationContext Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isEnabled() {
        return prefs.getBoolean(KEY_ENABLED, false)
                && prefs.contains(KEY_CIPHERTEXT)
                && prefs.contains(KEY_IV);
    }

    public boolean isEnabledForEmail(@Nullable String email) {
        if (!isEnabled()) {
            return false;
        }
        return matchesEnrolledEmail(getEnrolledEmail(), email);
    }

    public static boolean matchesEnrolledEmail(@Nullable String enrolledEmail, @Nullable String candidateEmail) {
        if (enrolledEmail == null || candidateEmail == null) {
            return false;
        }
        String normalizedEnrolled = enrolledEmail.trim();
        String normalizedCandidate = candidateEmail.trim();
        return !normalizedEnrolled.isEmpty()
                && !normalizedCandidate.isEmpty()
                && normalizedEnrolled.equalsIgnoreCase(normalizedCandidate);
    }

    @Nullable
    public String getEnrolledEmail() {
        return prefs.getString(KEY_EMAIL_HINT, null);
    }

    public boolean saveCredentials(@NonNull String email, @NonNull String password) {
        String normalizedEmail = email.trim();
        if (normalizedEmail.isEmpty() || password.isEmpty()) {
            return false;
        }
        try {
            SecretKey key = getOrCreateSecretKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] iv = cipher.getIV();
            String json = gson.toJson(new Credentials(normalizedEmail, password));
            byte[] encrypted = cipher.doFinal(json.getBytes(StandardCharsets.UTF_8));

            prefs.edit()
                    .putBoolean(KEY_ENABLED, true)
                    .putString(KEY_EMAIL_HINT, normalizedEmail)
                    .putString(KEY_CIPHERTEXT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                    .putString(KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                    .apply();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Nullable
    public Credentials readCredentials() {
        if (!isEnabled()) {
            return null;
        }
        String cipherTextB64 = prefs.getString(KEY_CIPHERTEXT, null);
        String ivB64 = prefs.getString(KEY_IV, null);
        if (cipherTextB64 == null || ivB64 == null) {
            return null;
        }
        try {
            SecretKey key = getOrCreateSecretKey();
            byte[] encrypted = Base64.decode(cipherTextB64, Base64.NO_WRAP);
            byte[] iv = Base64.decode(ivB64, Base64.NO_WRAP);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plainBytes = cipher.doFinal(encrypted);
            String json = new String(plainBytes, StandardCharsets.UTF_8);
            Credentials creds = gson.fromJson(json, Credentials.class);
            if (creds == null || creds.email == null || creds.password == null) {
                return null;
            }
            return creds;
        } catch (GeneralSecurityException | IllegalArgumentException | JsonSyntaxException ex) {
            clear();
            return null;
        }
    }

    public void clear() {
        prefs.edit()
                .remove(KEY_ENABLED)
                .remove(KEY_EMAIL_HINT)
                .remove(KEY_CIPHERTEXT)
                .remove(KEY_IV)
                .apply();
    }

    @NonNull
    private SecretKey getOrCreateSecretKey() throws GeneralSecurityException {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            if (keyStore.containsAlias(KEY_ALIAS)) {
                KeyStore.Entry entry = keyStore.getEntry(KEY_ALIAS, null);
                if (entry instanceof KeyStore.SecretKeyEntry secretKeyEntry) {
                    return secretKeyEntry.getSecretKey();
                }
            }
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
            );
            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
            )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build();
            keyGenerator.init(spec);
            return keyGenerator.generateKey();
        } catch (GeneralSecurityException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new GeneralSecurityException("Failed to access AndroidKeyStore", ex);
        }
    }
}

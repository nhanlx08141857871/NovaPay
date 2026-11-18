package com.novabank.novapay.data.remote.server;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * OkHttp Interceptor tự động đính kèm header Authorization: Bearer <Firebase_ID_Token>
 * cho mọi request từ @BackendRetrofit.
 */
@Singleton
public class AuthInterceptor implements Interceptor {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final long TOKEN_FETCH_TIMEOUT_SECONDS = 10L;

    @Inject
    public AuthInterceptor() {
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();

        // Không ghi đè nếu request đã tự gắn sẵn Authorization hoặc gọi /health
        if (originalRequest.header(HEADER_AUTHORIZATION) != null
                || originalRequest.url().encodedPath().endsWith("/health")) {
            return chain.proceed(originalRequest);
        }

        String idToken = fetchCurrentIdTokenSafely();
        if (idToken == null || idToken.isEmpty()) {
            return chain.proceed(originalRequest);
        }

        Request authenticatedRequest = originalRequest.newBuilder()
                .header(HEADER_AUTHORIZATION, BEARER_PREFIX + idToken)
                .build();
        return chain.proceed(authenticatedRequest);
    }

    private String fetchCurrentIdTokenSafely() {
        try {
            if (FirebaseApp.getApps(FirebaseApp.getInstance().getApplicationContext()).isEmpty()) {
                return null;
            }
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                return null;
            }
            GetTokenResult tokenResult = Tasks.await(
                    currentUser.getIdToken(false),
                    TOKEN_FETCH_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            );
            return tokenResult != null ? tokenResult.getToken() : null;
        } catch (Exception ignored) {
            // Cho phép request tiếp tục khi chưa có google-services.json hoặc token lỗi mạng tạm thời
            return null;
        }
    }
}

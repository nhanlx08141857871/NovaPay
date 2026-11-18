package com.novabank.novapay.presentation.customer.payment;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

/**
 * Trợ lý tích hợp Stripe Android SDK PaymentSheet (Test Mode) cho luồng GATEWAY:
 * initiate -> confirm (nhận clientSecret) -> PaymentSheet -> /payments/finalize.
 * Bắt buộc khởi tạo trong Fragment.onCreate() trước khi Fragment chuyển sang trạng thái STARTED.
 */
public final class StripeGatewayHelper {

    public interface Callback {
        void onPaymentCompleted(@NonNull String draftId);

        void onPaymentCanceled();

        void onPaymentFailed(@NonNull String errorMessage);
    }

    private final PaymentSheet paymentSheet;
    private final Callback callback;
    private String activeDraftId;

    public StripeGatewayHelper(@NonNull Fragment fragment, @NonNull Callback callback) {
        this.callback = callback;
        this.paymentSheet = new PaymentSheet(fragment, this::handleResult);
    }

    public boolean presentPaymentSheet(
            @NonNull Context context,
            @NonNull String publishableKey,
            @NonNull String merchantName,
            @NonNull String draftId,
            @Nullable String clientSecret
    ) {
        String trimmedKey = publishableKey.trim();
        if (!trimmedKey.startsWith("pk_test_") && !trimmedKey.startsWith("pk_live_")) {
            return false;
        }
        if (clientSecret == null || clientSecret.trim().isEmpty()) {
            return false;
        }
        this.activeDraftId = draftId;
        PaymentConfiguration.init(context.getApplicationContext(), trimmedKey);
        PaymentSheet.Configuration configuration = new PaymentSheet.Configuration.Builder(merchantName)
                .allowsDelayedPaymentMethods(false)
                .build();
        paymentSheet.presentWithPaymentIntent(clientSecret.trim(), configuration);
        return true;
    }

    private void handleResult(@NonNull PaymentSheetResult result) {
        if (result instanceof PaymentSheetResult.Completed) {
            String completedDraft = activeDraftId != null ? activeDraftId : "";
            activeDraftId = null;
            callback.onPaymentCompleted(completedDraft);
        } else if (result instanceof PaymentSheetResult.Canceled) {
            activeDraftId = null;
            callback.onPaymentCanceled();
        } else if (result instanceof PaymentSheetResult.Failed failed) {
            activeDraftId = null;
            Throwable error = failed.getError();
            String msg = (error != null && error.getLocalizedMessage() != null)
                    ? error.getLocalizedMessage()
                    : "Stripe PaymentSheet error";
            callback.onPaymentFailed(msg);
        }
    }
}

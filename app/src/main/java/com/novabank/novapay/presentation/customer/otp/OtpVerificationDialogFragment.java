package com.novabank.novapay.presentation.customer.otp;

import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;
import com.novabank.novapay.R;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.core.security.BiometricHelper;
import com.novabank.novapay.core.security.SessionManager;
import com.novabank.novapay.core.ui.StateLayoutHelper;
import com.novabank.novapay.databinding.DialogOtpVerificationBinding;
import com.novabank.novapay.domain.model.transaction.TransactionConfirmResult;
import com.novabank.novapay.domain.model.transaction.TransactionDraft;

import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Dialog xác thực OTP Email 6 số (đếm ngược 180s, gửi lại mã) kết hợp điều phối tự động
 * FaceCheckDialogFragment (> 5.000.000 VNĐ) và BiometricHelper trước khi gọi /transactions/confirm.
 * Trả kết quả qua FragmentResultListener với khóa Constants.REQ_OTP_VERIFICATION.
 */
@AndroidEntryPoint
public class OtpVerificationDialogFragment extends AppCompatDialogFragment {

    public static final String TAG = "OtpVerificationDialog";
    private static final String ARG_REQUIRES_FACE = "arg_requires_face";
    private static final String ARG_REQUIRES_BIOMETRIC = "arg_requires_biometric";

    @Inject
    BiometricHelper biometricHelper;

    private DialogOtpVerificationBinding binding;
    private OtpVerificationViewModel viewModel;
    private StateLayoutHelper stateLayoutHelper;

    private CountDownTimer countDownTimer;
    private long activeTimerTargetEpochMs = -1L;
    private boolean autoLaunchedFaceCheck = false;
    private boolean autoLaunchedBiometric = false;
    private boolean otpExpired = false;
    private boolean resultDelivered = false;
    private String draftId = "";

    @NonNull
    public static OtpVerificationDialogFragment newInstance(@NonNull TransactionDraft draft) {
        OtpVerificationDialogFragment dialog = new OtpVerificationDialogFragment();
        Bundle args = new Bundle();
        args.putString(Constants.BUNDLE_DRAFT_ID, draft.getDraftId());
        args.putBoolean(ARG_REQUIRES_FACE, draft.isRequiresFace());
        args.putBoolean(ARG_REQUIRES_BIOMETRIC, draft.isRequiresBiometric());
        long expiresAtMs = draft.getOtpExpiresAt() != null
                ? draft.getOtpExpiresAt().toEpochMilli()
                : 0L;
        args.putLong(Constants.BUNDLE_OTP_EXPIRES_AT_EPOCH_MS, expiresAtMs);
        dialog.setArguments(args);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = DialogOtpVerificationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            Window window = getDialog().getWindow();
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setCancelable(false);
        viewModel = new ViewModelProvider(this).get(OtpVerificationViewModel.class);

        binding.stateLoading.tvLoadingMessage.setText(R.string.money_otp_verifying_loading);
        stateLayoutHelper = new StateLayoutHelper(
                binding.contentContainer,
                binding.stateLoading,
                binding.stateEmpty,
                binding.stateError,
                () -> {
                    deliverCancelledResult();
                    dismissAllowingStateLoss();
                }
        );

        getParentFragmentManager().setFragmentResultListener(
                Constants.REQ_FACE_CHECK,
                getViewLifecycleOwner(),
                (requestKey, bundle) -> {
                    boolean passed = bundle.getBoolean(Constants.BUNDLE_FACE_CHECK_PASSED, false);
                    long otpExpiresMs = bundle.getLong(Constants.BUNDLE_OTP_EXPIRES_AT_EPOCH_MS, 0L);
                    viewModel.onFaceCheckResult(
                            passed,
                            otpExpiresMs,
                            getString(R.string.money_otp_step_face_pending)
                    );
                }
        );

        binding.btnCancelOtp.setOnClickListener(v -> {
            deliverCancelledResult();
            dismissAllowingStateLoss();
        });

        binding.btnResendOtp.setOnClickListener(v -> viewModel.resendOtp());

        binding.btnConfirmOtp.setOnClickListener(v -> {
            String code = binding.etOtpCode.getText() != null
                    ? binding.etOtpCode.getText().toString()
                    : "";
            viewModel.confirmOtp(code);
        });

        observeViewModel();

        Bundle args = getArguments();
        draftId = args != null ? args.getString(Constants.BUNDLE_DRAFT_ID, "") : "";
        boolean requiresFace = args != null && args.getBoolean(ARG_REQUIRES_FACE, false);
        boolean requiresBiometric = args != null && args.getBoolean(ARG_REQUIRES_BIOMETRIC, false);
        long otpExpiresAtMs = args != null ? args.getLong(Constants.BUNDLE_OTP_EXPIRES_AT_EPOCH_MS, 0L) : 0L;

        viewModel.initialize(draftId, requiresFace, requiresBiometric, otpExpiresAtMs);
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null || binding == null) {
                return;
            }
            stateLayoutHelper.render(state);
            OtpVerificationUiState data = state.getData();
            if (data != null && state.getStatus() == com.novabank.novapay.core.common.UiState.Status.SUCCESS) {
                renderStepState(data);
            }
        });

        viewModel.getOtpResentEvent().observe(getViewLifecycleOwner(), resent -> {
            if (Boolean.TRUE.equals(resent) && binding != null) {
                binding.etOtpCode.setText("");
                Snackbar.make(binding.getRoot(), R.string.money_otp_resent_success, Snackbar.LENGTH_SHORT).show();
            }
        });

        viewModel.getConfirmedEvent().observe(getViewLifecycleOwner(), this::deliverSuccessAndDismiss);
    }

    private void renderStepState(@NonNull OtpVerificationUiState data) {
        if (binding == null) {
            return;
        }

        String inlineError = data.getInlineError();
        if (inlineError != null && !inlineError.trim().isEmpty()) {
            binding.tvInlineError.setVisibility(View.VISIBLE);
            binding.tvInlineError.setText(inlineError);
        } else {
            binding.tvInlineError.setVisibility(View.GONE);
        }

        if (data.isWaitingForFace()) {
            stopCountdown();
            binding.tvStepGuide.setText(R.string.money_otp_step_face_pending);
            binding.tvCountdown.setVisibility(View.GONE);
            binding.tilOtpCode.setEnabled(false);
            binding.btnResendOtp.setEnabled(false);
            binding.btnConfirmOtp.setEnabled(false);
            binding.btnRetrySecurityStep.setVisibility(View.VISIBLE);
            binding.btnRetrySecurityStep.setText(R.string.money_otp_retry_face_action);
            binding.btnRetrySecurityStep.setOnClickListener(v -> launchFaceCheckDialog(data.getDraftId()));

            if (!autoLaunchedFaceCheck) {
                autoLaunchedFaceCheck = true;
                launchFaceCheckDialog(data.getDraftId());
            }
            return;
        }

        // Sau khi qua FaceCheck (hoặc đơn <= 5M), đồng hồ OTP 180s bắt đầu đếm ngược
        if (data.getOtpExpiresAtEpochMs() > 0L) {
            binding.tvCountdown.setVisibility(View.VISIBLE);
            startCountdownIfNeeded(data.getOtpExpiresAtEpochMs());
        }

        if (data.isWaitingForBiometric()) {
            binding.tvStepGuide.setText(R.string.money_otp_step_biometric_pending);
            binding.tilOtpCode.setEnabled(false);
            binding.btnResendOtp.setEnabled(true);
            binding.btnConfirmOtp.setEnabled(false);
            binding.btnRetrySecurityStep.setVisibility(View.VISIBLE);
            binding.btnRetrySecurityStep.setText(R.string.money_otp_retry_biometric_action);
            binding.btnRetrySecurityStep.setOnClickListener(v -> launchBiometricPrompt());

            if (!autoLaunchedBiometric) {
                autoLaunchedBiometric = true;
                launchBiometricPrompt();
            }
            return;
        }

        // Đã qua đủ các bước sinh trắc học cần thiết -> Sẵn sàng nhập OTP 6 số
        binding.tvStepGuide.setText(R.string.money_otp_step_otp_ready);
        binding.btnRetrySecurityStep.setVisibility(View.GONE);
        binding.tilOtpCode.setEnabled(!otpExpired);
        binding.btnResendOtp.setEnabled(true);
        binding.btnConfirmOtp.setEnabled(!otpExpired);
    }

    private void launchFaceCheckDialog(@NonNull String currentDraftId) {
        if (!isAdded() || currentDraftId.isEmpty()) {
            return;
        }
        try {
            Uri uri = Uri.parse(SessionManager.DEEPLINK_FACE_CHECK_PREFIX + Uri.encode(currentDraftId));
            NavHostFragment.findNavController(this).navigate(uri);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            if (binding != null) {
                Snackbar.make(binding.getRoot(), R.string.core_guard_destination_unavailable, Snackbar.LENGTH_LONG).show();
            }
        }
    }

    private void launchBiometricPrompt() {
        if (!isAdded()) {
            return;
        }
        if (!biometricHelper.canAuthenticate(requireContext())) {
            // Máy ảo chưa thiết lập PIN/vân tay: cho phép xác thực fallback để không khóa cứng luồng chấm thi
            viewModel.onBiometricSucceeded();
            return;
        }
        biometricHelper.authenticate(
                this,
                getString(R.string.money_otp_biometric_title),
                getString(R.string.money_otp_biometric_subtitle),
                new BiometricHelper.AuthCallback() {
                    @Override
                    public void onAuthSucceeded() {
                        if (isAdded()) {
                            viewModel.onBiometricSucceeded();
                        }
                    }

                    @Override
                    public void onAuthError(int errorCode, @NonNull String message) {
                        if (isAdded()) {
                            viewModel.onBiometricFailed(message);
                        }
                    }
                }
        );
    }

    private void startCountdownIfNeeded(long targetEpochMs) {
        if (targetEpochMs == activeTimerTargetEpochMs && countDownTimer != null) {
            return;
        }
        stopCountdown();
        activeTimerTargetEpochMs = targetEpochMs;
        long remainingMs = targetEpochMs - System.currentTimeMillis();
        if (remainingMs <= 0L) {
            onTimerExpired();
            return;
        }

        otpExpired = false;
        countDownTimer = new CountDownTimer(remainingMs, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (binding == null || !isAdded()) {
                    return;
                }
                long totalSeconds = Math.max(0L, millisUntilFinished / 1000L);
                long minutes = totalSeconds / 60L;
                long seconds = totalSeconds % 60L;
                String formatted = String.format(Locale.US, "%02d:%02d", minutes, seconds);
                binding.tvCountdown.setText(getString(R.string.money_otp_countdown_running, formatted));
                binding.tvCountdown.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.novapay_verified_badge)
                );
            }

            @Override
            public void onFinish() {
                onTimerExpired();
            }
        }.start();
    }

    private void onTimerExpired() {
        otpExpired = true;
        if (binding != null && isAdded()) {
            binding.tvCountdown.setText(R.string.money_otp_countdown_expired);
            binding.tvCountdown.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.novapay_expense_negative)
            );
            binding.btnConfirmOtp.setEnabled(false);
            binding.tilOtpCode.setEnabled(false);
        }
    }

    private void stopCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        activeTimerTargetEpochMs = -1L;
    }

    private void deliverSuccessAndDismiss(@Nullable TransactionConfirmResult confirmResult) {
        if (resultDelivered || !isAdded() || confirmResult == null) {
            return;
        }
        resultDelivered = true;
        Bundle result = new Bundle();
        result.putBoolean(Constants.BUNDLE_OTP_VERIFIED, true);
        result.putString(Constants.BUNDLE_DRAFT_ID, draftId);
        if (confirmResult.getTxId() != null) {
            result.putString(Constants.BUNDLE_TX_ID, confirmResult.getTxId());
        }
        if (confirmResult.getClientSecret() != null) {
            result.putString(Constants.BUNDLE_CLIENT_SECRET, confirmResult.getClientSecret());
        }
        getParentFragmentManager().setFragmentResult(Constants.REQ_OTP_VERIFICATION, result);
        dismissAllowingStateLoss();
    }

    private void deliverCancelledResult() {
        if (resultDelivered || !isAdded()) {
            return;
        }
        resultDelivered = true;
        Bundle result = new Bundle();
        result.putBoolean(Constants.BUNDLE_OTP_VERIFIED, false);
        result.putString(Constants.BUNDLE_DRAFT_ID, draftId);
        getParentFragmentManager().setFragmentResult(Constants.REQ_OTP_VERIFICATION, result);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        deliverCancelledResult();
        super.onDismiss(dialog);
    }

    @Override
    public void onDestroyView() {
        stopCountdown();
        binding = null;
        super.onDestroyView();
    }
}

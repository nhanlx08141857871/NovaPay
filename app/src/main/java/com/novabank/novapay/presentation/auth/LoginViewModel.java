package com.novabank.novapay.presentation.auth;

import android.net.Uri;
import android.util.Patterns;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novabank.novapay.R;
import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.core.common.SingleLiveEvent;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.core.security.BiometricQuickLoginStore;
import com.novabank.novapay.core.security.SessionManager;
import com.novabank.novapay.domain.model.user.User;
import com.novabank.novapay.domain.model.user.UserRole;
import com.novabank.novapay.domain.repository.AuthRepository;
import com.novabank.novapay.domain.repository.UserRepository;
import com.novabank.novapay.domain.usecase.auth.SignInUseCase;
import com.novabank.novapay.domain.usecase.security.SendPasswordResetEmailUseCase;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * ViewModel Đăng nhập Firebase Auth, kiểm tra Custom Claim role và trạng thái LOCKED.
 */
@HiltViewModel
public class LoginViewModel extends ViewModel {

    private final SignInUseCase signInUseCase;
    private final SendPasswordResetEmailUseCase sendPasswordResetEmailUseCase;
    private final AuthRepository authRepository;
    private final UserRepository userRepository;
    private final SessionManager sessionManager;
    private final BiometricQuickLoginStore biometricQuickLoginStore;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<UiState<LoginUiState>> uiState =
            new MutableLiveData<>(UiState.success(new LoginUiState(true, null)));
    private final SingleLiveEvent<String> navigateDeepLinkEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Integer> messageResEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> messageTextEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> resetEmailSentEvent = new SingleLiveEvent<>();

    @Inject
    public LoginViewModel(
            SignInUseCase signInUseCase,
            SendPasswordResetEmailUseCase sendPasswordResetEmailUseCase,
            AuthRepository authRepository,
            UserRepository userRepository,
            SessionManager sessionManager,
            BiometricQuickLoginStore biometricQuickLoginStore
    ) {
        this.signInUseCase = signInUseCase;
        this.sendPasswordResetEmailUseCase = sendPasswordResetEmailUseCase;
        this.authRepository = authRepository;
        this.userRepository = userRepository;
        this.sessionManager = sessionManager;
        this.biometricQuickLoginStore = biometricQuickLoginStore;
    }

    public LiveData<UiState<LoginUiState>> getUiState() {
        return uiState;
    }

    public SingleLiveEvent<String> getNavigateDeepLinkEvent() {
        return navigateDeepLinkEvent;
    }

    public SingleLiveEvent<Integer> getMessageResEvent() {
        return messageResEvent;
    }

    public SingleLiveEvent<String> getMessageTextEvent() {
        return messageTextEvent;
    }

    public SingleLiveEvent<String> getResetEmailSentEvent() {
        return resetEmailSentEvent;
    }

    public void checkExistingSession() {
        if (!authRepository.isSignedIn()) {
            uiState.setValue(UiState.success(new LoginUiState(true, null)));
            return;
        }
        uiState.setValue(UiState.loading());
        disposables.add(
                authRepository.refreshRole()
                        .flatMap(this::verifyActiveProfile)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                this::handleAuthOutcome,
                                throwable -> resetToReadyForm()
                        )
        );
    }

    public void onLoginClicked(@NonNull String rawEmail, @NonNull String password) {
        String email = rawEmail.trim();
        if (email.isEmpty() || password.isEmpty()) {
            messageResEvent.setValue(R.string.core_login_empty_credentials);
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            messageResEvent.setValue(R.string.core_login_invalid_email);
            return;
        }

        uiState.setValue(UiState.loading());
        disposables.add(
                signInUseCase.execute(email, password)
                        .flatMap(this::verifyActiveProfile)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                result -> {
                                    if (result.isSuccess() && biometricQuickLoginStore.isEnabledForEmail(email)) {
                                        biometricQuickLoginStore.saveCredentials(email, password);
                                    }
                                    handleAuthOutcome(result);
                                },
                                throwable -> {
                                    resetToReadyForm();
                                    messageResEvent.setValue(R.string.core_error_network_generic);
                                }
                        )
        );
    }

    public void sendPasswordResetEmail(@NonNull String rawEmail) {
        String email = rawEmail.trim();
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            messageResEvent.setValue(R.string.core_login_invalid_email);
            return;
        }
        disposables.add(
                sendPasswordResetEmailUseCase.execute(email)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> {
                            if (result.isSuccess()) {
                                resetEmailSentEvent.setValue(email);
                            } else {
                                String msg = result.getMessage();
                                if (msg != null && !msg.trim().isEmpty()) {
                                    messageTextEvent.setValue(msg);
                                } else {
                                    messageResEvent.setValue(R.string.core_error_network_generic);
                                }
                            }
                        }, throwable -> messageResEvent.setValue(R.string.core_error_network_generic))
        );
    }

    public void resetToReadyForm() {
        uiState.setValue(UiState.success(new LoginUiState(true, null)));
    }

    private Single<AppResult<UserRole>> verifyActiveProfile(AppResult<UserRole> roleResult) {
        if (roleResult.isError() || roleResult.getData() == null) {
            return Single.just(roleResult);
        }
        UserRole role = roleResult.getData();
        return userRepository.getMyProfile().map(profileResult -> {
            if (profileResult.isSuccess() && profileResult.getData() != null) {
                User user = profileResult.getData();
                if (!user.isActive()) {
                    sessionManager.signOut();
                    return AppResult.error("USER_LOCKED", null);
                }
            }
            return AppResult.success(role);
        });
    }

    private void handleAuthOutcome(AppResult<UserRole> result) {
        if (result.isError() || result.getData() == null) {
            if ("USER_LOCKED".equals(result.getErrorCode())) {
                resetToReadyForm();
                messageResEvent.setValue(R.string.core_session_locked_message);
                return;
            }
            resetToReadyForm();
            String msg = result.getMessage();
            if (msg != null && !msg.trim().isEmpty()) {
                messageTextEvent.setValue(msg);
            } else {
                messageResEvent.setValue(R.string.core_error_network_generic);
            }
            return;
        }

        UserRole role = result.getData();
        sessionManager.updateCachedRole(role.asClaimValue());
        sessionManager.startUserSession();
        uiState.setValue(UiState.success(new LoginUiState(true, role.asClaimValue())));

        boolean hadPending = sessionManager.peekPendingDeepLink() != null;
        Uri pending = sessionManager.consumePendingDeepLinkIfAllowed(
                role.asClaimValue(),
                authRepository.getCurrentUid()
        );
        if (hadPending && pending == null) {
            messageResEvent.setValue(R.string.core_guard_role_denied);
        }
        if (pending != null) {
            navigateDeepLinkEvent.setValue(pending.toString());
        } else if (role == UserRole.OFFICER) {
            navigateDeepLinkEvent.setValue(SessionManager.DEEPLINK_OFFICER);
        } else {
            navigateDeepLinkEvent.setValue(Constants.DEEPLINK_HOME);
        }
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}

package com.novabank.novapay.presentation.customer.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novabank.novapay.R;
import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.core.common.SingleLiveEvent;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.domain.model.user.User;
import com.novabank.novapay.domain.usecase.auth.SignOutUseCase;
import com.novabank.novapay.domain.usecase.user.ObserveMyProfileUseCase;
import com.novabank.novapay.domain.usecase.user.UpdateMyProfileUseCase;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

@HiltViewModel
public class ProfileViewModel extends ViewModel {

    private final ObserveMyProfileUseCase observeMyProfileUseCase;
    private final UpdateMyProfileUseCase updateMyProfileUseCase;
    private final SignOutUseCase signOutUseCase;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<UiState<ProfileUiState>> uiState =
            new MutableLiveData<>(UiState.loading());
    private final SingleLiveEvent<Integer> messageResEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> messageTextEvent = new SingleLiveEvent<>();

    private User currentUser;

    @Inject
    public ProfileViewModel(
            ObserveMyProfileUseCase observeMyProfileUseCase,
            UpdateMyProfileUseCase updateMyProfileUseCase,
            SignOutUseCase signOutUseCase
    ) {
        this.observeMyProfileUseCase = observeMyProfileUseCase;
        this.updateMyProfileUseCase = updateMyProfileUseCase;
        this.signOutUseCase = signOutUseCase;
    }

    public void loadProfile() {
        disposables.clear();
        uiState.setValue(UiState.loading());
        disposables.add(
                observeMyProfileUseCase.execute()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::handleProfileResult, throwable ->
                                uiState.setValue(UiState.error(throwable.getMessage())))
        );
    }

    public void saveProfile(String phone, String address, String avatarUrl) {
        if (currentUser == null || !currentUser.isActive()) {
            messageResEvent.setValue(R.string.core_profile_locked_readonly);
            return;
        }
        uiState.setValue(UiState.success(new ProfileUiState(currentUser, true)));
        disposables.add(
                updateMyProfileUseCase.execute(phone, address, avatarUrl)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> {
                            uiState.setValue(UiState.success(new ProfileUiState(currentUser, false)));
                            if (result.isSuccess()) {
                                messageResEvent.setValue(R.string.core_profile_save_success);
                            } else {
                                messageTextEvent.setValue(result.getMessage());
                            }
                        }, throwable -> {
                            uiState.setValue(UiState.success(new ProfileUiState(currentUser, false)));
                            messageResEvent.setValue(R.string.core_error_network_generic);
                        })
        );
    }

    public void signOut() {
        signOutUseCase.execute();
    }

    private void handleProfileResult(AppResult<User> result) {
        if (result.isError()) {
            if ("NOT_FOUND".equals(result.getErrorCode())) {
                uiState.setValue(UiState.empty());
            } else {
                uiState.setValue(UiState.error(result.getMessage()));
            }
            return;
        }
        User user = result.getData();
        if (user == null) {
            uiState.setValue(UiState.empty());
            return;
        }
        currentUser = user;
        uiState.setValue(UiState.success(new ProfileUiState(user, false)));
    }

    public LiveData<UiState<ProfileUiState>> getUiState() {
        return uiState;
    }

    public SingleLiveEvent<Integer> getMessageResEvent() {
        return messageResEvent;
    }

    public SingleLiveEvent<String> getMessageTextEvent() {
        return messageTextEvent;
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}

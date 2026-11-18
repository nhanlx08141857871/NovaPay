package com.novabank.novapay.presentation.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novabank.novapay.core.common.SingleLiveEvent;
import com.novabank.novapay.core.common.UiState;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel khung cho màn hình Đăng nhập (Day-0 Skeleton — Thành viên A tích hợp Firebase Auth ở 3B).
 */
@HiltViewModel
public class LoginViewModel extends ViewModel {

    private final MutableLiveData<UiState<LoginUiState>> uiState =
            new MutableLiveData<>(UiState.success(new LoginUiState(true)));
    private final SingleLiveEvent<Integer> messageResEvent = new SingleLiveEvent<>();

    @Inject
    public LoginViewModel() {
    }

    public LiveData<UiState<LoginUiState>> getUiState() {
        return uiState;
    }

    public SingleLiveEvent<Integer> getMessageResEvent() {
        return messageResEvent;
    }

    public void onLoginClicked(String email, String password, int emptyWarningRes, int skeletonNoticeRes) {
        if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
            messageResEvent.setValue(emptyWarningRes);
            return;
        }
        messageResEvent.setValue(skeletonNoticeRes);
    }
}

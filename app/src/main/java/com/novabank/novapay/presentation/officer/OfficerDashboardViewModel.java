package com.novabank.novapay.presentation.officer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.core.common.SingleLiveEvent;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.domain.model.officer.OfficerDashboardSummary;
import com.novabank.novapay.domain.usecase.officer.GetOfficerDashboardUseCase;
import com.novabank.novapay.domain.usecase.officer.OfficerSignOutUseCase;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

@HiltViewModel
public class OfficerDashboardViewModel extends ViewModel {

    private final GetOfficerDashboardUseCase getOfficerDashboardUseCase;
    private final OfficerSignOutUseCase officerSignOutUseCase;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<UiState<OfficerDashboardUiState>> uiState =
            new MutableLiveData<>(UiState.loading());
    private final SingleLiveEvent<String> messageEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Boolean> loggedOutEvent = new SingleLiveEvent<>();

    @Inject
    public OfficerDashboardViewModel(
            GetOfficerDashboardUseCase getOfficerDashboardUseCase,
            OfficerSignOutUseCase officerSignOutUseCase
    ) {
        this.getOfficerDashboardUseCase = getOfficerDashboardUseCase;
        this.officerSignOutUseCase = officerSignOutUseCase;
        loadDashboard();
    }

    public LiveData<UiState<OfficerDashboardUiState>> getUiState() {
        return uiState;
    }

    public LiveData<String> getMessageEvent() {
        return messageEvent;
    }

    public LiveData<Boolean> getLoggedOutEvent() {
        return loggedOutEvent;
    }

    public void loadDashboard() {
        uiState.setValue(UiState.loading());
        disposables.add(
                getOfficerDashboardUseCase.execute()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                this::handleDashboardResult,
                                error -> uiState.setValue(UiState.error(error.getMessage()))
                        )
        );
    }

    public void onLogoutConfirmed() {
        disposables.add(
                officerSignOutUseCase.execute()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> loggedOutEvent.setValue(true),
                                error -> messageEvent.setValue(error.getMessage())
                        )
        );
    }

    private void handleDashboardResult(AppResult<OfficerDashboardSummary> result) {
        if (result == null) {
            uiState.setValue(UiState.empty());
            return;
        }
        if (result.isError()) {
            uiState.setValue(UiState.error(result.getMessage()));
            return;
        }
        OfficerDashboardSummary summary = result.getData();
        if (summary == null) {
            uiState.setValue(UiState.empty());
            return;
        }
        uiState.setValue(UiState.success(new OfficerDashboardUiState(summary)));
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}

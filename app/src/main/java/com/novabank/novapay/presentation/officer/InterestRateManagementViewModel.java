package com.novabank.novapay.presentation.officer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novabank.novapay.R;
import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.core.common.SingleLiveEvent;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.domain.model.rate.InterestRate;
import com.novabank.novapay.domain.usecase.rate.ObserveInterestRatesUseCase;
import com.novabank.novapay.domain.usecase.rate.SaveInterestRateUseCase;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

@HiltViewModel
public class InterestRateManagementViewModel extends ViewModel {

    private final ObserveInterestRatesUseCase observeInterestRatesUseCase;
    private final SaveInterestRateUseCase saveInterestRateUseCase;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private Disposable observeDisposable;

    private final MutableLiveData<UiState<InterestRateManagementUiState>> uiState =
            new MutableLiveData<>(UiState.loading());
    private final MutableLiveData<Boolean> submitting = new MutableLiveData<>(false);
    private final SingleLiveEvent<Integer> termErrorResEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Integer> annualRateErrorResEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> feedbackMessageEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Integer> feedbackResEvent = new SingleLiveEvent<>();

    @Inject
    public InterestRateManagementViewModel(
            ObserveInterestRatesUseCase observeInterestRatesUseCase,
            SaveInterestRateUseCase saveInterestRateUseCase
    ) {
        this.observeInterestRatesUseCase = observeInterestRatesUseCase;
        this.saveInterestRateUseCase = saveInterestRateUseCase;
        loadInterestRates();
    }

    public LiveData<UiState<InterestRateManagementUiState>> getUiState() {
        return uiState;
    }

    public LiveData<Boolean> getSubmitting() {
        return submitting;
    }

    public LiveData<Integer> getTermErrorResEvent() {
        return termErrorResEvent;
    }

    public LiveData<Integer> getAnnualRateErrorResEvent() {
        return annualRateErrorResEvent;
    }

    public LiveData<String> getFeedbackMessageEvent() {
        return feedbackMessageEvent;
    }

    public LiveData<Integer> getFeedbackResEvent() {
        return feedbackResEvent;
    }

    public void loadInterestRates() {
        if (observeDisposable != null && !observeDisposable.isDisposed()) {
            observeDisposable.dispose();
        }
        uiState.setValue(UiState.loading());
        observeDisposable = observeInterestRatesUseCase.execute()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::handleRatesResult,
                        error -> uiState.setValue(UiState.error(error.getMessage()))
                );
        disposables.add(observeDisposable);
    }

    public void saveInterestRate(String termMonthsRaw, String annualRateRaw, int successResId) {
        if (Boolean.TRUE.equals(submitting.getValue())) {
            return;
        }
        int termMonths;
        try {
            termMonths = Integer.parseInt(termMonthsRaw != null ? termMonthsRaw.trim() : "");
        } catch (NumberFormatException ex) {
            termErrorResEvent.setValue(R.string.officer_rate_validation_term);
            return;
        }
        if (termMonths < 0 || termMonths > InterestRate.MAX_TERM_MONTHS) {
            termErrorResEvent.setValue(R.string.officer_rate_validation_term);
            return;
        }

        double annualRate;
        try {
            String normalizedRate = annualRateRaw != null
                    ? annualRateRaw.trim().replace(',', '.')
                    : "";
            annualRate = Double.parseDouble(normalizedRate);
        } catch (NumberFormatException ex) {
            annualRateErrorResEvent.setValue(R.string.officer_rate_validation_annual);
            return;
        }
        if (!InterestRate.isValidTermAndRate(termMonths, annualRate)) {
            annualRateErrorResEvent.setValue(R.string.officer_rate_validation_annual);
            return;
        }

        submitting.setValue(true);
        disposables.add(
                saveInterestRateUseCase.execute(termMonths, annualRate)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                result -> {
                                    submitting.setValue(false);
                                    if (result.isSuccess()) {
                                        feedbackResEvent.setValue(successResId);
                                    } else {
                                        feedbackMessageEvent.setValue(result.getMessage());
                                    }
                                },
                                error -> {
                                    submitting.setValue(false);
                                    feedbackMessageEvent.setValue(error.getMessage());
                                }
                        )
        );
    }

    private void handleRatesResult(AppResult<List<InterestRate>> result) {
        if (result == null) {
            uiState.setValue(UiState.empty());
            return;
        }
        if (result.isError()) {
            uiState.setValue(UiState.error(result.getMessage()));
            return;
        }
        List<InterestRate> rates = result.getData();
        if (rates == null || rates.isEmpty()) {
            uiState.setValue(UiState.empty());
            return;
        }
        uiState.setValue(UiState.success(new InterestRateManagementUiState(rates)));
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}

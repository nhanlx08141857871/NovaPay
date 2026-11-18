package com.novabank.novapay.presentation.customer.history;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.domain.model.transaction.LedgerDirection;
import com.novabank.novapay.domain.model.transaction.LedgerEntry;
import com.novabank.novapay.domain.usecase.transaction.ObserveAccountLedgerUseCase;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

@HiltViewModel
public class TransactionHistoryViewModel extends ViewModel {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of(Constants.ZONE_HO_CHI_MINH);

    private final ObserveAccountLedgerUseCase observeAccountLedgerUseCase;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<UiState<TransactionHistoryUiState>> uiState =
            new MutableLiveData<>(UiState.loading());

    private LedgerDirection currentDirection = null;
    private boolean monthOnlyRange = true;

    @Inject
    public TransactionHistoryViewModel(ObserveAccountLedgerUseCase observeAccountLedgerUseCase) {
        this.observeAccountLedgerUseCase = observeAccountLedgerUseCase;
    }

    public void initialize(boolean monthOnly) {
        this.monthOnlyRange = monthOnly;
        reload();
    }

    public void setDirectionFilter(LedgerDirection directionOrNull) {
        this.currentDirection = directionOrNull;
        reload();
    }

    public void setMonthOnlyRange(boolean monthOnly) {
        this.monthOnlyRange = monthOnly;
        reload();
    }

    public void reload() {
        disposables.clear();
        uiState.setValue(UiState.loading());

        Instant fromInclusive;
        Instant toExclusive;
        if (monthOnlyRange) {
            YearMonth ym = YearMonth.now(VIETNAM_ZONE);
            fromInclusive = ym.atDay(1).atStartOfDay(VIETNAM_ZONE).toInstant();
            toExclusive = ym.plusMonths(1).atDay(1).atStartOfDay(VIETNAM_ZONE).toInstant();
        } else {
            LocalDate today = LocalDate.now(VIETNAM_ZONE);
            fromInclusive = today.minusDays(90).atStartOfDay(VIETNAM_ZONE).toInstant();
            toExclusive = today.plusDays(1).atStartOfDay(VIETNAM_ZONE).toInstant();
        }

        disposables.add(
                observeAccountLedgerUseCase.execute(null, currentDirection, fromInclusive, toExclusive)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::handleResult, throwable ->
                                uiState.setValue(UiState.error(throwable.getMessage())))
        );
    }

    private void handleResult(AppResult<List<LedgerEntry>> result) {
        if (result.isError()) {
            uiState.setValue(UiState.error(result.getMessage()));
            return;
        }
        List<LedgerEntry> entries = result.getData();
        if (entries == null || entries.isEmpty()) {
            uiState.setValue(UiState.empty());
            return;
        }
        uiState.setValue(UiState.success(new TransactionHistoryUiState(
                entries,
                currentDirection,
                monthOnlyRange
        )));
    }

    public LiveData<UiState<TransactionHistoryUiState>> getUiState() {
        return uiState;
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}

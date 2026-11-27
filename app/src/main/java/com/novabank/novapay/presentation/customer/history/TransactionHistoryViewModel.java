package com.novabank.novapay.presentation.customer.history;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.transaction.LedgerDirection;
import com.novabank.novapay.domain.model.transaction.LedgerEntry;
import com.novabank.novapay.domain.model.user.User;
import com.novabank.novapay.domain.usecase.account.ObserveMyAccountsUseCase;
import com.novabank.novapay.domain.usecase.transaction.LedgerFilterHelper;
import com.novabank.novapay.domain.usecase.transaction.ObserveAccountLedgerUseCase;
import com.novabank.novapay.domain.usecase.user.ObserveMyProfileUseCase;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

@HiltViewModel
public class TransactionHistoryViewModel extends ViewModel {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of(Constants.ZONE_HO_CHI_MINH);

    private final ObserveAccountLedgerUseCase observeAccountLedgerUseCase;
    private final ObserveMyAccountsUseCase observeMyAccountsUseCase;
    private final ObserveMyProfileUseCase observeMyProfileUseCase;
    private final CompositeDisposable metaDisposables = new CompositeDisposable();
    private final CompositeDisposable ledgerDisposables = new CompositeDisposable();

    private final MutableLiveData<UiState<TransactionHistoryUiState>> uiState =
            new MutableLiveData<>(UiState.loading());
    private final MutableLiveData<List<Account>> accountsLiveData =
            new MutableLiveData<>(Collections.emptyList());

    private List<Account> accounts = Collections.emptyList();
    private String customerName = "";
    private List<LedgerEntry> rawEntries = Collections.emptyList();
    private String selectedAccountId = null;
    private LedgerDirection currentDirection = null;
    private TransactionHistoryUiState.RangeMode rangeMode = TransactionHistoryUiState.RangeMode.THIS_MONTH;
    private Instant customStart = null;
    private Instant customEnd = null;
    private String searchKeyword = "";

    @Inject
    public TransactionHistoryViewModel(
            ObserveAccountLedgerUseCase observeAccountLedgerUseCase,
            ObserveMyAccountsUseCase observeMyAccountsUseCase,
            ObserveMyProfileUseCase observeMyProfileUseCase
    ) {
        this.observeAccountLedgerUseCase = observeAccountLedgerUseCase;
        this.observeMyAccountsUseCase = observeMyAccountsUseCase;
        this.observeMyProfileUseCase = observeMyProfileUseCase;
    }

    public void initialize(boolean monthOnly) {
        this.rangeMode = monthOnly
                ? TransactionHistoryUiState.RangeMode.THIS_MONTH
                : TransactionHistoryUiState.RangeMode.RECENT_90_DAYS;
        loadMetadata();
        reload();
    }

    private void loadMetadata() {
        metaDisposables.clear();
        metaDisposables.add(
                observeMyAccountsUseCase.execute()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> {
                            if (result.isSuccess() && result.getData() != null) {
                                accounts = result.getData();
                                accountsLiveData.setValue(accounts);
                                applyFilterAndEmitIfLoaded();
                            }
                        }, ignored -> {
                        })
        );
        metaDisposables.add(
                observeMyProfileUseCase.execute()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> {
                            if (result.isSuccess() && result.getData() != null) {
                                User user = result.getData();
                                customerName = user.getFullName();
                            }
                        }, ignored -> {
                        })
        );
    }

    public void setAccountFilter(@Nullable String accountIdOrNull) {
        this.selectedAccountId = accountIdOrNull;
        reload();
    }

    public void setDirectionFilter(@Nullable LedgerDirection directionOrNull) {
        this.currentDirection = directionOrNull;
        reload();
    }

    public void setRangeMode(@NonNull TransactionHistoryUiState.RangeMode mode) {
        this.rangeMode = mode;
        reload();
    }

    public void setCustomDateRange(@NonNull Instant startUtcMillis, @NonNull Instant endUtcMillis) {
        LocalDate startDate = startUtcMillis.atZone(ZoneId.of("UTC")).toLocalDate();
        LocalDate endDate = endUtcMillis.atZone(ZoneId.of("UTC")).toLocalDate();
        if (endDate.isBefore(startDate)) {
            LocalDate tmp = startDate;
            startDate = endDate;
            endDate = tmp;
        }
        this.customStart = startDate.atStartOfDay(VIETNAM_ZONE).toInstant();
        this.customEnd = endDate.plusDays(1).atStartOfDay(VIETNAM_ZONE).toInstant();
        this.rangeMode = TransactionHistoryUiState.RangeMode.CUSTOM;
        reload();
    }

    public void setSearchKeyword(@Nullable String keyword) {
        this.searchKeyword = keyword != null ? keyword : "";
        applyFilterAndEmitIfLoaded();
    }

    public void reload() {
        ledgerDisposables.clear();
        uiState.setValue(UiState.loading());

        Instant fromInclusive;
        Instant toExclusive;
        if (rangeMode == TransactionHistoryUiState.RangeMode.CUSTOM
                && customStart != null
                && customEnd != null) {
            fromInclusive = customStart;
            toExclusive = customEnd;
        } else if (rangeMode == TransactionHistoryUiState.RangeMode.THIS_MONTH) {
            YearMonth ym = YearMonth.now(VIETNAM_ZONE);
            fromInclusive = ym.atDay(1).atStartOfDay(VIETNAM_ZONE).toInstant();
            toExclusive = ym.plusMonths(1).atDay(1).atStartOfDay(VIETNAM_ZONE).toInstant();
        } else {
            LocalDate today = LocalDate.now(VIETNAM_ZONE);
            fromInclusive = today.minusDays(90).atStartOfDay(VIETNAM_ZONE).toInstant();
            toExclusive = today.plusDays(1).atStartOfDay(VIETNAM_ZONE).toInstant();
        }

        ledgerDisposables.add(
                observeAccountLedgerUseCase.execute(selectedAccountId, currentDirection, fromInclusive, toExclusive)
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
        rawEntries = result.getData() != null ? result.getData() : Collections.emptyList();
        applyFilterAndEmit();
    }

    private void applyFilterAndEmitIfLoaded() {
        UiState<TransactionHistoryUiState> current = uiState.getValue();
        if (current != null && (current.getStatus() == UiState.Status.SUCCESS || current.getStatus() == UiState.Status.EMPTY)) {
            applyFilterAndEmit();
        }
    }

    private void applyFilterAndEmit() {
        List<LedgerEntry> filtered = LedgerFilterHelper.filterByKeyword(rawEntries, searchKeyword);
        if (filtered.isEmpty()) {
            uiState.setValue(UiState.empty());
            return;
        }
        LedgerFilterHelper.Summary summary = LedgerFilterHelper.computeSummary(filtered);
        uiState.setValue(UiState.success(new TransactionHistoryUiState(
                filtered,
                accounts,
                selectedAccountId,
                currentDirection,
                rangeMode,
                customStart,
                customEnd,
                summary.getTotalIn(),
                summary.getTotalOut(),
                customerName
        )));
    }

    public LiveData<UiState<TransactionHistoryUiState>> getUiState() {
        return uiState;
    }

    public LiveData<List<Account>> getAccountsLiveData() {
        return accountsLiveData;
    }

    @Override
    protected void onCleared() {
        metaDisposables.clear();
        ledgerDisposables.clear();
        super.onCleared();
    }
}

package com.novabank.novapay.domain.usecase.transaction;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.account.AccountType;
import com.novabank.novapay.domain.model.transaction.LedgerDirection;
import com.novabank.novapay.domain.model.transaction.LedgerEntry;
import com.novabank.novapay.domain.model.transaction.MonthlyCashflowSummary;
import com.novabank.novapay.domain.repository.AccountRepository;
import com.novabank.novapay.domain.repository.LedgerRepository;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Observable;

public final class ObserveMonthlyCashflowUseCase {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of(Constants.ZONE_HO_CHI_MINH);
    private static final DateTimeFormatter MONTH_LABEL_FORMATTER =
            DateTimeFormatter.ofPattern("MM/yyyy", Locale.forLanguageTag("vi-VN"));

    private final AccountRepository accountRepository;
    private final LedgerRepository ledgerRepository;

    @Inject
    public ObserveMonthlyCashflowUseCase(
            AccountRepository accountRepository,
            LedgerRepository ledgerRepository
    ) {
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;
    }

    public Observable<AppResult<MonthlyCashflowSummary>> execute() {
        YearMonth currentMonth = YearMonth.now(VIETNAM_ZONE);
        Instant startOfMonth = currentMonth.atDay(1).atStartOfDay(VIETNAM_ZONE).toInstant();
        Instant startOfNextMonth = currentMonth.plusMonths(1).atDay(1).atStartOfDay(VIETNAM_ZONE).toInstant();
        String monthLabel = currentMonth.format(MONTH_LABEL_FORMATTER);

        return accountRepository.observeMyAccounts().switchMap(accountsResult -> {
            if (accountsResult.isError()) {
                return Observable.just(AppResult.error(
                        accountsResult.getErrorCode(),
                        accountsResult.getMessage()
                ));
            }
            List<Account> accounts = accountsResult.getData();
            if (accounts == null || accounts.isEmpty()) {
                return Observable.just(AppResult.success(
                        new MonthlyCashflowSummary(monthLabel, 0L, 0L, 0)
                ));
            }

            List<Observable<AppResult<List<LedgerEntry>>>> streams = new ArrayList<>();
            for (Account account : accounts) {
                if (account.getType() == AccountType.MORTGAGE) {
                    continue;
                }
                streams.add(ledgerRepository.observeLedger(
                        account.getAccountId(),
                        null,
                        startOfMonth,
                        startOfNextMonth
                ));
            }

            if (streams.isEmpty()) {
                return Observable.just(AppResult.success(
                        new MonthlyCashflowSummary(monthLabel, 0L, 0L, 0)
                ));
            }

            return Observable.combineLatest(streams, results -> {
                List<LedgerEntry> allEntries = new ArrayList<>();
                AppResult<List<LedgerEntry>> firstError = null;
                boolean anySuccess = false;

                for (Object raw : results) {
                    @SuppressWarnings("unchecked")
                    AppResult<List<LedgerEntry>> item = (AppResult<List<LedgerEntry>>) raw;
                    if (item.isSuccess()) {
                        anySuccess = true;
                        if (item.getData() != null) {
                            allEntries.addAll(item.getData());
                        }
                    } else if (firstError == null) {
                        firstError = item;
                    }
                }

                if (!anySuccess && firstError != null) {
                    return AppResult.error(firstError.getErrorCode(), firstError.getMessage());
                }
                return AppResult.success(aggregate(monthLabel, allEntries));
            });
        });
    }

    public static MonthlyCashflowSummary aggregate(String monthLabel, List<LedgerEntry> entries) {
        long totalIn = 0L;
        long totalOut = 0L;
        int count = 0;
        if (entries != null) {
            for (LedgerEntry entry : entries) {
                if (entry == null) {
                    continue;
                }
                long amount = Math.max(0L, entry.getAmount());
                if (entry.getDirection() == LedgerDirection.IN) {
                    totalIn += amount;
                } else {
                    totalOut += amount;
                }
                count++;
            }
        }
        return new MonthlyCashflowSummary(monthLabel, totalIn, totalOut, count);
    }
}

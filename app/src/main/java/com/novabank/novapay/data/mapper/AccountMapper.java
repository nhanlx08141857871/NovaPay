package com.novabank.novapay.data.mapper;

import com.google.firebase.firestore.DocumentSnapshot;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.account.AccountStatus;
import com.novabank.novapay.domain.model.account.AccountType;
import com.novabank.novapay.domain.model.account.MortgageInfo;
import com.novabank.novapay.domain.model.account.SavingInfo;

import java.util.Map;

/**
 * Chuyển đổi Firestore Document accounts/{accountId} sang domain model Account.
 */
public final class AccountMapper {

    private AccountMapper() {
    }

    public static Account fromSnapshot(DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }
        return fromMap(snapshot.getId(), snapshot.getData());
    }

    public static Account fromMap(String accountId, Map<String, Object> data) {
        if (accountId == null || data == null) {
            return null;
        }
        String ownerUid = FirestoreValueMapper.toStringOrEmpty(data.get("ownerUid"));
        AccountType type = parseAccountType(FirestoreValueMapper.toStringOrEmpty(data.get("type")));
        String accountNumber = FirestoreValueMapper.toStringOrEmpty(data.get("accountNumber"));
        AccountStatus status = parseAccountStatus(FirestoreValueMapper.toStringOrEmpty(data.get("status")));
        long balance = FirestoreValueMapper.toLong(data.get("balance"), 0L);
        String currency = FirestoreValueMapper.toStringOrEmpty(data.get("currency"));
        if (currency.isEmpty()) {
            currency = Constants.CURRENCY_VND;
        }
        long perTxnLimit = FirestoreValueMapper.toLong(data.get("perTxnLimit"), 0L);
        long dailyLimit = FirestoreValueMapper.toLong(data.get("dailyLimit"), 0L);

        SavingInfo saving = parseSavingInfo(FirestoreValueMapper.toMap(data.get("saving")));
        MortgageInfo mortgage = parseMortgageInfo(FirestoreValueMapper.toMap(data.get("mortgage")));

        return new Account(
                accountId,
                ownerUid,
                type,
                accountNumber,
                status,
                balance,
                currency,
                perTxnLimit,
                dailyLimit,
                saving,
                mortgage,
                FirestoreValueMapper.toInstant(data.get("createdAt"))
        );
    }

    private static AccountType parseAccountType(String raw) {
        if ("SAVING".equalsIgnoreCase(raw)) {
            return AccountType.SAVING;
        }
        if ("MORTGAGE".equalsIgnoreCase(raw)) {
            return AccountType.MORTGAGE;
        }
        return AccountType.CHECKING;
    }

    private static AccountStatus parseAccountStatus(String raw) {
        if ("FROZEN".equalsIgnoreCase(raw)) {
            return AccountStatus.FROZEN;
        }
        if ("CLOSED".equalsIgnoreCase(raw)) {
            return AccountStatus.CLOSED;
        }
        return AccountStatus.ACTIVE;
    }

    private static SavingInfo parseSavingInfo(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        return new SavingInfo(
                FirestoreValueMapper.toInt(map.get("termMonths"), 0),
                FirestoreValueMapper.toLong(map.get("principal"), 0L),
                FirestoreValueMapper.toStringOrEmpty(map.get("interestRateId")),
                FirestoreValueMapper.toInstant(map.get("openedAt")),
                FirestoreValueMapper.toInstant(map.get("maturityDate"))
        );
    }

    private static MortgageInfo parseMortgageInfo(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        String freq = FirestoreValueMapper.toStringOrEmpty(map.get("paymentFrequency"));
        if (freq.isEmpty()) {
            freq = MortgageInfo.FREQUENCY_MONTHLY;
        }
        return new MortgageInfo(
                FirestoreValueMapper.toLong(map.get("loanAmount"), 0L),
                FirestoreValueMapper.toLong(map.get("outstandingBalance"), 0L),
                FirestoreValueMapper.toInt(map.get("termMonths"), 0),
                FirestoreValueMapper.toDouble(map.get("annualRate"), 0.0),
                freq,
                FirestoreValueMapper.toLong(map.get("installmentAmount"), 0L),
                FirestoreValueMapper.toInstant(map.get("nextDueDate"))
        );
    }
}

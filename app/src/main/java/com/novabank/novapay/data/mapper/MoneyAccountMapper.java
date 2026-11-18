package com.novabank.novapay.data.mapper;

import com.google.firebase.firestore.DocumentSnapshot;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.account.AccountStatus;
import com.novabank.novapay.domain.model.account.AccountType;
import com.novabank.novapay.domain.model.account.MortgageInfo;
import com.novabank.novapay.domain.model.account.SavingInfo;

import java.time.Instant;
import java.util.Map;

/**
 * Mapper cho collection accounts/{accountId} phục vụ cơ chế fallback khi Module A chưa merge.
 */
public final class MoneyAccountMapper {

    private MoneyAccountMapper() {
    }

    public static Account fromDocument(DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }
        Map<String, Object> data = snapshot.getData();
        return fromMap(snapshot.getId(), data);
    }

    public static Account fromMap(String accountId, Map<String, Object> data) {
        if (accountId == null || data == null) {
            return null;
        }
        String ownerUid = FirestoreMapUtils.toStringOrEmpty(data.get("ownerUid"));
        String accountNumber = FirestoreMapUtils.toStringOrEmpty(data.get("accountNumber"));
        if (ownerUid.isEmpty() || accountNumber.isEmpty()) {
            return null;
        }

        AccountType type = parseAccountType(FirestoreMapUtils.toStringOrNull(data.get("type")));
        AccountStatus status = parseAccountStatus(FirestoreMapUtils.toStringOrNull(data.get("status")));
        if (type == null || status == null) {
            return null;
        }

        long balance = FirestoreMapUtils.toLong(data.get("balance"), 0L);
        String currency = FirestoreMapUtils.toStringOrNull(data.get("currency"));
        long perTxnLimit = FirestoreMapUtils.toLong(data.get("perTxnLimit"), 0L);
        long dailyLimit = FirestoreMapUtils.toLong(data.get("dailyLimit"), 0L);
        Instant createdAt = FirestoreMapUtils.toInstant(data.get("createdAt"));

        SavingInfo saving = parseSavingInfo(FirestoreMapUtils.toStringMap(data.get("saving")));
        MortgageInfo mortgage = parseMortgageInfo(FirestoreMapUtils.toStringMap(data.get("mortgage")));

        return new Account(
                accountId,
                ownerUid,
                type,
                accountNumber,
                status,
                balance,
                currency != null ? currency : "VND",
                perTxnLimit,
                dailyLimit,
                saving,
                mortgage,
                createdAt
        );
    }

    private static SavingInfo parseSavingInfo(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        return new SavingInfo(
                FirestoreMapUtils.toInt(map.get("termMonths"), 0),
                FirestoreMapUtils.toLong(map.get("principal"), 0L),
                FirestoreMapUtils.toStringOrNull(map.get("interestRateId")),
                FirestoreMapUtils.toInstant(map.get("openedAt")),
                FirestoreMapUtils.toInstant(map.get("maturityDate"))
        );
    }

    private static MortgageInfo parseMortgageInfo(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        return new MortgageInfo(
                FirestoreMapUtils.toLong(map.get("loanAmount"), 0L),
                FirestoreMapUtils.toLong(map.get("outstandingBalance"), 0L),
                FirestoreMapUtils.toInt(map.get("termMonths"), 0),
                FirestoreMapUtils.toDouble(map.get("annualRate"), 0.0),
                FirestoreMapUtils.toStringOrNull(map.get("paymentFrequency")),
                FirestoreMapUtils.toLong(map.get("installmentAmount"), 0L),
                FirestoreMapUtils.toInstant(map.get("nextDueDate"))
        );
    }

    private static AccountType parseAccountType(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return AccountType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static AccountStatus parseAccountStatus(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return AccountStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

package com.novabank.novapay.data.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.account.AccountStatus;
import com.novabank.novapay.domain.model.account.AccountType;
import com.novabank.novapay.domain.model.rate.InterestRate;
import com.novabank.novapay.domain.model.transaction.LedgerDirection;
import com.novabank.novapay.domain.model.transaction.LedgerEntry;
import com.novabank.novapay.domain.model.user.EkycStatus;
import com.novabank.novapay.domain.model.user.User;
import com.novabank.novapay.domain.model.user.UserRole;
import com.novabank.novapay.domain.model.user.UserStatus;

import org.junit.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class ModuleAMapperTest {

    @Test
    public void accountMapper_mapsSavingAndMortgageFieldsSafely() {
        Map<String, Object> savingMap = new HashMap<>();
        savingMap.put("termMonths", 12);
        savingMap.put("principal", 100_000_000L);
        savingMap.put("interestRateId", "rate_12m");
        savingMap.put("openedAt", "2026-01-01T00:00:00Z");
        savingMap.put("maturityDate", "2027-01-01T00:00:00Z");

        Map<String, Object> doc = new HashMap<>();
        doc.put("ownerUid", "u1");
        doc.put("type", "SAVING");
        doc.put("accountNumber", "2001000001");
        doc.put("status", "ACTIVE");
        doc.put("balance", 100_000_000L);
        doc.put("currency", "VND");
        doc.put("saving", savingMap);

        Account mapped = AccountMapper.fromMap("acc_sav_1", doc);
        assertNotNull(mapped);
        assertEquals(AccountType.SAVING, mapped.getType());
        assertEquals(AccountStatus.ACTIVE, mapped.getStatus());
        assertEquals(100_000_000L, mapped.getBalance());
        assertFalse(mapped.canBePaymentSource());
        assertNotNull(mapped.getSaving());
        assertEquals(12, mapped.getSaving().getTermMonths());
        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), mapped.getSaving().getOpenedAt());
    }

    @Test
    public void userMapper_mapsEkycAndLockedStatus() {
        Map<String, Object> ekyc = new HashMap<>();
        ekyc.put("status", "VERIFIED");
        ekyc.put("verifiedBy", "off_1");

        Map<String, Object> security = new HashMap<>();
        security.put("hasPin", Boolean.TRUE);
        security.put("pinUpdatedAt", "2026-02-01T10:00:00Z");

        Map<String, Object> doc = new HashMap<>();
        doc.put("role", "CUSTOMER");
        doc.put("fullName", "Nguyen Hoang Anh");
        doc.put("status", "LOCKED");
        doc.put("ekyc", ekyc);
        doc.put("security", security);

        User user = UserMapper.fromMap("u1", doc);
        assertNotNull(user);
        assertEquals(UserRole.CUSTOMER, user.getRole());
        assertEquals(UserStatus.LOCKED, user.getStatus());
        assertFalse(user.isActive());
        assertTrue(user.isEkycVerified());
        assertEquals(EkycStatus.VERIFIED, user.getEkyc().getStatus());
        assertTrue(user.hasTransactionPin());
        assertTrue(user.getSecurity().hasPin());
        assertEquals(Instant.parse("2026-02-01T10:00:00Z"), user.getSecurity().getPinUpdatedAt());

        doc.remove("security");
        User userWithoutSecurity = UserMapper.fromMap("u2", doc);
        assertNotNull(userWithoutSecurity);
        assertFalse(userWithoutSecurity.hasTransactionPin());
        assertFalse(userWithoutSecurity.getSecurity().hasPin());
    }

    @Test
    public void ledgerAndRateMappers_mapLongAndDoubleCorrectly() {
        Map<String, Object> ledgerDoc = new HashMap<>();
        ledgerDoc.put("txId", "tx_1");
        ledgerDoc.put("direction", "IN");
        ledgerDoc.put("amount", 5_000_000L);
        ledgerDoc.put("balanceAfter", 25_000_000L);
        ledgerDoc.put("description", "Nap tien");
        ledgerDoc.put("createdAt", 1_700_000_000_000L);

        LedgerEntry entry = LedgerMapper.fromMap("led_1", "acc_1", ledgerDoc);
        assertNotNull(entry);
        assertEquals(LedgerDirection.IN, entry.getDirection());
        assertEquals(5_000_000L, entry.getAmount());
        assertEquals(25_000_000L, entry.getBalanceAfter());
        assertTrue(entry.isInflow());

        Map<String, Object> rateDoc = new HashMap<>();
        rateDoc.put("termMonths", 12);
        rateDoc.put("annualRate", 6.25);
        InterestRate rate = InterestRateMapper.fromMap("rate_12m", rateDoc);
        assertNotNull(rate);
        assertEquals(12, rate.getTermMonths());
        assertEquals(6.25, rate.getAnnualRate(), 0.0001);
    }
}

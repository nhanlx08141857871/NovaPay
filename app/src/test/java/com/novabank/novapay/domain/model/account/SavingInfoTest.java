package com.novabank.novapay.domain.model.account;

import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SavingInfoTest {

    @Test
    public void calculateMonthlyProfit_floorsResultAsLongWithoutFloatingPointDrift() {
        // 100.000.000 VNĐ * 6.5%/năm / 12 tháng = 541.666,66... -> floor = 541.666 VNĐ
        long profit = SavingInfo.calculateMonthlyProfit(100_000_000L, 6.5);
        assertEquals(541_666L, profit);
        assertEquals(0L, SavingInfo.calculateMonthlyProfit(0L, 6.5));
    }

    @Test
    public void account_canBePaymentSource_onlyAllowsActiveChecking() {
        Account activeChecking = new Account(
                "acc1", "u1", AccountType.CHECKING, "100001",
                AccountStatus.ACTIVE, 10_000_000L, "VND", 50_000_000L, 100_000_000L,
                null, null, Instant.now()
        );
        Account activeSaving = new Account(
                "acc2", "u1", AccountType.SAVING, "200001",
                AccountStatus.ACTIVE, 50_000_000L, "VND", 0L, 0L,
                null, null, Instant.now()
        );
        Account frozenChecking = new Account(
                "acc3", "u1", AccountType.CHECKING, "100002",
                AccountStatus.FROZEN, 10_000_000L, "VND", 50_000_000L, 100_000_000L,
                null, null, Instant.now()
        );

        assertTrue(activeChecking.canBePaymentSource());
        assertFalse(activeSaving.canBePaymentSource());
        assertFalse(frozenChecking.canBePaymentSource());
    }
}

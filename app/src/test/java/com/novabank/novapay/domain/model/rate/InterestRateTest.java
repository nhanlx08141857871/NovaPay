package com.novabank.novapay.domain.model.rate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.novabank.novapay.domain.model.account.SavingInfo;

import org.junit.Test;

public class InterestRateTest {

    @Test
    public void isValidTermAndRate_enforcesBoundaries() {
        assertTrue(InterestRate.isValidTermAndRate(0, 0.5));
        assertTrue(InterestRate.isValidTermAndRate(6, 5.8));
        assertTrue(InterestRate.isValidTermAndRate(120, 50.0));

        assertFalse(InterestRate.isValidTermAndRate(-1, 5.0));
        assertFalse(InterestRate.isValidTermAndRate(121, 5.0));
        assertFalse(InterestRate.isValidTermAndRate(6, 0.0));
        assertFalse(InterestRate.isValidTermAndRate(6, -2.0));
        assertFalse(InterestRate.isValidTermAndRate(6, 50.1));
        assertFalse(InterestRate.isValidTermAndRate(6, Double.NaN));
        assertFalse(InterestRate.isValidTermAndRate(6, Double.POSITIVE_INFINITY));
    }

    @Test
    public void savingMonthlyProfit_matchesLongFloorFormula() {
        long profit = SavingInfo.calculateMonthlyProfit(12_000_000L, 6.0);
        assertEquals(60_000L, profit);
    }
}

package com.novabank.novapay.domain.model.account;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class SavingCalculatorTest {

    @Test
    public void calculateMaturityAndMonthlyInterest_usesFloorLongMath() {
        // 10_000_000 * 6.0% / 12 = 50_000 / month
        assertEquals(50_000L, SavingCalculator.calculateMonthlyProfit(10_000_000L, 6.0));
        // 10_000_000 * 6.0% * 6 / 12 = 300_000
        assertEquals(300_000L, SavingCalculator.calculateMaturityInterest(10_000_000L, 6.0, 6));
        // Early: 10_000_000 * 0.5% * 73 / 365 = 10_000
        assertEquals(10_000L, SavingCalculator.calculateEarlyInterest(10_000_000L, 0.5, 73L));
    }

    @Test
    public void previewSettlement_earlyVsMatured_returnsExactExpectedBreakdown() {
        Instant openedAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant maturity = openedAt.plus(180, ChronoUnit.DAYS);
        Account saving = new Account(
                "sav_1",
                "u1",
                AccountType.SAVING,
                "2001000099",
                AccountStatus.ACTIVE,
                10_000_000L,
                "VND",
                0L,
                0L,
                new SavingInfo(6, 10_000_000L, "rate_6m", openedAt, maturity),
                null,
                openedAt
        );
        SavingProjection projection = new SavingProjection(saving, null, 6.0, 50_000L);

        // Before maturity (73 days held -> demand rate 0.5% -> 10_000)
        SavingSettlementPreview early = SavingCalculator.previewSettlement(
                projection,
                0.5,
                openedAt.plus(73, ChronoUnit.DAYS)
        );
        assertTrue(early.isEarlySettlement());
        assertEquals(0.5, early.getAppliedAnnualRate(), 0.0001);
        assertEquals(10_000L, early.getInterestAmount());
        assertEquals(10_010_000L, early.getTotalPayoutAmount());

        // On maturity (full 6 months at 6.0% -> 300_000)
        SavingSettlementPreview matured = SavingCalculator.previewSettlement(
                projection,
                0.5,
                maturity
        );
        assertFalse(matured.isEarlySettlement());
        assertEquals(6.0, matured.getAppliedAnnualRate(), 0.0001);
        assertEquals(300_000L, matured.getInterestAmount());
        assertEquals(10_300_000L, matured.getTotalPayoutAmount());
    }

    @Test
    public void estimateMaturityDate_usesHoChiMinhCalendarMonthsWithMonthEndClamping() {
        assertEquals(
                Instant.parse("2026-07-15T03:00:00Z"),
                SavingCalculator.estimateMaturityDate(Instant.parse("2026-01-15T03:00:00Z"), 6)
        );
        // 2026-01-31 12:00+07:00 + 1 month -> 2026-02-28 12:00+07:00
        assertEquals(
                Instant.parse("2026-02-28T05:00:00Z"),
                SavingCalculator.estimateMaturityDate(Instant.parse("2026-01-31T05:00:00Z"), 1)
        );
        // Leap year: 2024-01-31 12:00+07:00 + 1 month -> 2024-02-29 12:00+07:00
        assertEquals(
                Instant.parse("2024-02-29T05:00:00Z"),
                SavingCalculator.estimateMaturityDate(Instant.parse("2024-01-31T05:00:00Z"), 1)
        );
        // 2026-01-31T18:00:00Z is 2026-02-01 01:00+07:00 -> +1 month = 2026-03-01 01:00+07:00
        assertEquals(
                Instant.parse("2026-02-28T18:00:00Z"),
                SavingCalculator.estimateMaturityDate(Instant.parse("2026-01-31T18:00:00Z"), 1)
        );
    }
}

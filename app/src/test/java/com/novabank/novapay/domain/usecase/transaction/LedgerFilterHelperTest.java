package com.novabank.novapay.domain.usecase.transaction;

import static org.junit.Assert.assertEquals;

import com.novabank.novapay.domain.model.transaction.LedgerDirection;
import com.novabank.novapay.domain.model.transaction.LedgerEntry;

import org.junit.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public class LedgerFilterHelperTest {

    @Test
    public void filterByKeyword_matchesDescriptionTxIdAndAmount() {
        Instant now = Instant.parse("2026-11-20T10:00:00Z");
        List<LedgerEntry> entries = Arrays.asList(
                new LedgerEntry("e1", "acc1", "tx_elec_101", LedgerDirection.OUT, 350_000L, 4_650_000L, "Thanh toán tiền điện EVN", now),
                new LedgerEntry("e2", "acc1", "tx_recv_202", LedgerDirection.IN, 2_000_000L, 6_650_000L, "Nhận chuyển khoản nội bộ", now)
        );

        assertEquals(1, LedgerFilterHelper.filterByKeyword(entries, "EVN").size());
        assertEquals(1, LedgerFilterHelper.filterByKeyword(entries, "tx_recv_202").size());
        assertEquals(1, LedgerFilterHelper.filterByKeyword(entries, "2000000").size());
        assertEquals(2, LedgerFilterHelper.filterByKeyword(entries, "   ").size());
    }

    @Test
    public void computeSummary_sumsInAndOutAsLong() {
        Instant now = Instant.now();
        List<LedgerEntry> entries = Arrays.asList(
                new LedgerEntry("e1", "acc1", "tx1", LedgerDirection.IN, 1_500_000L, 1_500_000L, "In 1", now),
                new LedgerEntry("e2", "acc1", "tx2", LedgerDirection.OUT, 450_000L, 1_050_000L, "Out 1", now),
                new LedgerEntry("e3", "acc1", "tx3", LedgerDirection.IN, 250_000L, 1_300_000L, "In 2", now)
        );

        LedgerFilterHelper.Summary summary = LedgerFilterHelper.computeSummary(entries);
        assertEquals(1_750_000L, summary.getTotalIn());
        assertEquals(450_000L, summary.getTotalOut());
        assertEquals(3, summary.getCount());
    }
}

package com.novabank.novapay.domain.usecase.transaction;

import com.novabank.novapay.domain.model.transaction.LedgerEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Bộ lọc từ khóa và tính tổng Thu/Chi kiểu long cho danh sách sổ cái (Pure Java 17).
 */
public final class LedgerFilterHelper {

    public static final class Summary {
        private final long totalIn;
        private final long totalOut;
        private final int count;

        public Summary(long totalIn, long totalOut, int count) {
            this.totalIn = totalIn;
            this.totalOut = totalOut;
            this.count = count;
        }

        public long getTotalIn() {
            return totalIn;
        }

        public long getTotalOut() {
            return totalOut;
        }

        public int getCount() {
            return count;
        }
    }

    private LedgerFilterHelper() {
    }

    public static List<LedgerEntry> filterByKeyword(List<LedgerEntry> source, String rawKeyword) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        String keyword = rawKeyword != null ? rawKeyword.trim().toLowerCase(Locale.ROOT) : "";
        if (keyword.isEmpty()) {
            return new ArrayList<>(source);
        }
        String digitsKeyword = keyword.replaceAll("[^0-9]", "");
        List<LedgerEntry> filtered = new ArrayList<>();
        for (LedgerEntry entry : source) {
            if (entry == null) {
                continue;
            }
            String desc = entry.getDescription() != null ? entry.getDescription().toLowerCase(Locale.ROOT) : "";
            String txId = entry.getTxId() != null ? entry.getTxId().toLowerCase(Locale.ROOT) : "";
            String amountStr = String.valueOf(entry.getAmount());
            boolean matchesText = desc.contains(keyword) || txId.contains(keyword);
            boolean matchesAmount = !digitsKeyword.isEmpty() && amountStr.contains(digitsKeyword);
            if (matchesText || matchesAmount) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    public static Summary computeSummary(List<LedgerEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return new Summary(0L, 0L, 0);
        }
        long totalIn = 0L;
        long totalOut = 0L;
        int count = 0;
        for (LedgerEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            count++;
            if (entry.isInflow()) {
                totalIn += Math.max(0L, entry.getAmount());
            } else {
                totalOut += Math.max(0L, entry.getAmount());
            }
        }
        return new Summary(totalIn, totalOut, count);
    }
}

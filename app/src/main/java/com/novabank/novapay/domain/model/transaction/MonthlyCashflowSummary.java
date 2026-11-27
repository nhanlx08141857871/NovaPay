package com.novabank.novapay.domain.model.transaction;

public final class MonthlyCashflowSummary {

    private final String monthLabel;
    private final long totalIn;
    private final long totalOut;
    private final int entryCount;

    public MonthlyCashflowSummary(String monthLabel, long totalIn, long totalOut, int entryCount) {
        this.monthLabel = monthLabel != null ? monthLabel : "";
        this.totalIn = Math.max(0L, totalIn);
        this.totalOut = Math.max(0L, totalOut);
        this.entryCount = Math.max(0, entryCount);
    }

    public String getMonthLabel() {
        return monthLabel;
    }

    public long getTotalIn() {
        return totalIn;
    }

    public long getTotalOut() {
        return totalOut;
    }

    public int getEntryCount() {
        return entryCount;
    }
}

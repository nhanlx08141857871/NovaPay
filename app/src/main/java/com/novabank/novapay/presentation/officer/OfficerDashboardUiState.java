package com.novabank.novapay.presentation.officer;

import com.novabank.novapay.domain.model.audit.AuditLog;
import com.novabank.novapay.domain.model.audit.FaceCheckAudit;
import com.novabank.novapay.domain.model.officer.OfficerDashboardSummary;

import java.util.List;

public final class OfficerDashboardUiState {

    private final String officerEmail;
    private final int totalCustomers;
    private final int pendingEkycCount;
    private final int dailyTransactionsCount;
    private final int manualFaceAuditCount;
    private final List<FaceCheckAudit> pendingFaceAudits;
    private final List<AuditLog> recentAuditLogs;

    public OfficerDashboardUiState(OfficerDashboardSummary summary) {
        this.officerEmail = summary.getOfficerEmail();
        this.totalCustomers = summary.getTotalCustomers();
        this.pendingEkycCount = summary.getPendingEkycCount();
        this.dailyTransactionsCount = summary.getDailyTransactionsCount();
        this.manualFaceAuditCount = summary.getManualFaceAuditCount();
        this.pendingFaceAudits = summary.getPendingFaceAudits();
        this.recentAuditLogs = summary.getRecentAuditLogs();
    }

    public String getOfficerEmail() {
        return officerEmail;
    }

    public int getTotalCustomers() {
        return totalCustomers;
    }

    public int getPendingEkycCount() {
        return pendingEkycCount;
    }

    public int getDailyTransactionsCount() {
        return dailyTransactionsCount;
    }

    public int getManualFaceAuditCount() {
        return manualFaceAuditCount;
    }

    public List<FaceCheckAudit> getPendingFaceAudits() {
        return pendingFaceAudits;
    }

    public List<AuditLog> getRecentAuditLogs() {
        return recentAuditLogs;
    }
}

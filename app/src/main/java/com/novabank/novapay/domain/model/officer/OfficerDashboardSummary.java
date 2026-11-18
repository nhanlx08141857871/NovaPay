package com.novabank.novapay.domain.model.officer;

import com.novabank.novapay.domain.model.audit.AuditLog;
import com.novabank.novapay.domain.model.audit.FaceCheckAudit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class OfficerDashboardSummary {

    private final String officerEmail;
    private final int totalCustomers;
    private final int pendingEkycCount;
    private final int dailyTransactionsCount;
    private final int manualFaceAuditCount;
    private final List<FaceCheckAudit> pendingFaceAudits;
    private final List<AuditLog> recentAuditLogs;

    public OfficerDashboardSummary(
            String officerEmail,
            int totalCustomers,
            int pendingEkycCount,
            int dailyTransactionsCount,
            int manualFaceAuditCount,
            List<FaceCheckAudit> pendingFaceAudits,
            List<AuditLog> recentAuditLogs
    ) {
        this.officerEmail = officerEmail != null ? officerEmail : "";
        this.totalCustomers = totalCustomers;
        this.pendingEkycCount = pendingEkycCount;
        this.dailyTransactionsCount = dailyTransactionsCount;
        this.manualFaceAuditCount = manualFaceAuditCount;
        this.pendingFaceAudits = pendingFaceAudits == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(pendingFaceAudits));
        this.recentAuditLogs = recentAuditLogs == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(recentAuditLogs));
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

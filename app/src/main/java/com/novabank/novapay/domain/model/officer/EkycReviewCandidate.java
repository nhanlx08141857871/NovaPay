package com.novabank.novapay.domain.model.officer;

import com.novabank.novapay.domain.model.audit.FaceCheckAudit;
import com.novabank.novapay.domain.model.user.CustomerProfile;

import java.util.Objects;

public final class EkycReviewCandidate {

    private final CustomerProfile customer;
    private final String ekycImageBase64;
    private final FaceCheckAudit manualAuditItem;

    public EkycReviewCandidate(
            CustomerProfile customer,
            String ekycImageBase64,
            FaceCheckAudit manualAuditItem
    ) {
        this.customer = Objects.requireNonNull(customer, "customer must not be null");
        this.ekycImageBase64 = ekycImageBase64;
        this.manualAuditItem = manualAuditItem;
    }

    public CustomerProfile getCustomer() {
        return customer;
    }

    public String getEkycImageBase64() {
        return ekycImageBase64;
    }

    public FaceCheckAudit getManualAuditItem() {
        return manualAuditItem;
    }

    public String getUid() {
        return customer.getUid();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EkycReviewCandidate that)) return false;
        return Objects.equals(customer, that.customer)
                && Objects.equals(ekycImageBase64, that.ekycImageBase64)
                && Objects.equals(manualAuditItem, that.manualAuditItem);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customer, ekycImageBase64, manualAuditItem);
    }
}

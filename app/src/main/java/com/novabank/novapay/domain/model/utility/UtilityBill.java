package com.novabank.novapay.domain.model.utility;

import java.time.Instant;
import java.util.Objects;

public final class UtilityBill {

    private final String billId;
    private final UtilityBillType type;
    private final String providerName;
    private final String customerCode;
    private final String customerName;
    private final String period;
    private final long amount;
    private final Instant dueDate;
    private final UtilityBillStatus status;

    public UtilityBill(
            String billId,
            UtilityBillType type,
            String providerName,
            String customerCode,
            String customerName,
            String period,
            long amount,
            Instant dueDate,
            UtilityBillStatus status
    ) {
        this.billId = Objects.requireNonNull(billId, "billId must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.providerName = providerName != null ? providerName : "";
        this.customerCode = customerCode != null ? customerCode : "";
        this.customerName = customerName != null ? customerName : "";
        this.period = period != null ? period : "";
        this.amount = amount;
        this.dueDate = dueDate;
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public String getBillId() {
        return billId;
    }

    public UtilityBillType getType() {
        return type;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getCustomerCode() {
        return customerCode;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getPeriod() {
        return period;
    }

    public long getAmount() {
        return amount;
    }

    public Instant getDueDate() {
        return dueDate;
    }

    public UtilityBillStatus getStatus() {
        return status;
    }
}

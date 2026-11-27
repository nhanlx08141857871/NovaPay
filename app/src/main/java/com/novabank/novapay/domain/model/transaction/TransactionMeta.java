package com.novabank.novapay.domain.model.transaction;

import java.util.Objects;

public final class TransactionMeta {

    private final String billId;
    private final String billCode;
    private final String phone;
    private final String itemId;
    private final String orderId;
    private final Integer quantity;
    private final String mortgageAccountId;
    private final String gatewayRef;

    public TransactionMeta(
            String billId,
            String billCode,
            String phone,
            String itemId,
            String orderId,
            String mortgageAccountId,
            String gatewayRef
    ) {
        this(billId, billCode, phone, itemId, orderId, null, mortgageAccountId, gatewayRef);
    }

    public TransactionMeta(
            String billId,
            String billCode,
            String phone,
            String itemId,
            String orderId,
            Integer quantity,
            String mortgageAccountId,
            String gatewayRef
    ) {
        this.billId = trimToNull(billId);
        this.billCode = trimToNull(billCode);
        this.phone = trimToNull(phone);
        this.itemId = trimToNull(itemId);
        this.orderId = trimToNull(orderId);
        this.quantity = (quantity != null && quantity > 0) ? quantity : null;
        this.mortgageAccountId = trimToNull(mortgageAccountId);
        this.gatewayRef = trimToNull(gatewayRef);
    }

    public static TransactionMeta empty() {
        return new TransactionMeta(null, null, null, null, null, null, null, null);
    }

    public static TransactionMeta forBill(String billId, String billCode) {
        return new TransactionMeta(billId, billCode, null, null, null, null, null, null);
    }

    public static TransactionMeta forTopup(String phone) {
        return new TransactionMeta(null, null, phone, null, null, null, null, null);
    }

    public static TransactionMeta forService(String itemId, String orderId) {
        return new TransactionMeta(null, null, null, itemId, orderId, null, null, null);
    }

    public static TransactionMeta forServiceQuantity(String itemId, int quantity) {
        return new TransactionMeta(null, null, null, itemId, null, quantity, null, null);
    }

    public static TransactionMeta forMortgage(String mortgageAccountId) {
        return new TransactionMeta(null, null, null, null, null, null, mortgageAccountId, null);
    }

    public static TransactionMeta forGateway(String gatewayRef) {
        return new TransactionMeta(null, null, null, null, null, null, null, gatewayRef);
    }

    public String getBillId() {
        return billId;
    }

    public String getBillCode() {
        return billCode;
    }

    public String getPhone() {
        return phone;
    }

    public String getItemId() {
        return itemId;
    }

    public String getOrderId() {
        return orderId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public String getMortgageAccountId() {
        return mortgageAccountId;
    }

    public String getGatewayRef() {
        return gatewayRef;
    }

    public boolean isEmpty() {
        return billId == null
                && billCode == null
                && phone == null
                && itemId == null
                && orderId == null
                && quantity == null
                && mortgageAccountId == null
                && gatewayRef == null;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransactionMeta that)) return false;
        return Objects.equals(billId, that.billId)
                && Objects.equals(billCode, that.billCode)
                && Objects.equals(phone, that.phone)
                && Objects.equals(itemId, that.itemId)
                && Objects.equals(orderId, that.orderId)
                && Objects.equals(quantity, that.quantity)
                && Objects.equals(mortgageAccountId, that.mortgageAccountId)
                && Objects.equals(gatewayRef, that.gatewayRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(billId, billCode, phone, itemId, orderId, quantity, mortgageAccountId, gatewayRef);
    }
}

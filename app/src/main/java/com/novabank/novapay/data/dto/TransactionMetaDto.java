package com.novabank.novapay.data.dto;

public final class TransactionMetaDto {

    private String billId;
    private String billCode;
    private String phone;
    private String itemId;
    private String orderId;
    private Integer quantity;
    private String mortgageAccountId;
    private String gatewayRef;

    public TransactionMetaDto() {
    }

    public TransactionMetaDto(
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

    public TransactionMetaDto(
            String billId,
            String billCode,
            String phone,
            String itemId,
            String orderId,
            Integer quantity,
            String mortgageAccountId,
            String gatewayRef
    ) {
        this.billId = billId;
        this.billCode = billCode;
        this.phone = phone;
        this.itemId = itemId;
        this.orderId = orderId;
        this.quantity = quantity;
        this.mortgageAccountId = mortgageAccountId;
        this.gatewayRef = gatewayRef;
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
}

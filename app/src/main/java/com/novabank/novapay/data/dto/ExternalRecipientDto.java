package com.novabank.novapay.data.dto;

/**
 * DTO thông tin người nhận liên ngân hàng gửi/nhận với Server.
 */
public final class ExternalRecipientDto {

    private String bankCode;
    private String accountNumber;
    private String name;

    public ExternalRecipientDto() {
    }

    public ExternalRecipientDto(String bankCode, String accountNumber, String name) {
        this.bankCode = bankCode;
        this.accountNumber = accountNumber;
        this.name = name;
    }

    public String getBankCode() {
        return bankCode;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getName() {
        return name;
    }
}

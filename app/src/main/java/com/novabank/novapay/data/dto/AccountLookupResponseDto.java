package com.novabank.novapay.data.dto;

public final class AccountLookupResponseDto {

    private String accountId;
    private String accountNumber;
    private String ownerName;

    public AccountLookupResponseDto() {
    }

    public AccountLookupResponseDto(String accountId, String accountNumber, String ownerName) {
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }
}

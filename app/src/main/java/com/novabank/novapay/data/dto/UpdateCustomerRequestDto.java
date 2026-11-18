package com.novabank.novapay.data.dto;

public final class UpdateCustomerRequestDto {
    private final String fullName;
    private final String phone;
    private final String address;
    private final String status;

    public UpdateCustomerRequestDto(String fullName, String phone, String address, String status) {
        this.fullName = fullName;
        this.phone = phone;
        this.address = address;
        this.status = status;
    }
}

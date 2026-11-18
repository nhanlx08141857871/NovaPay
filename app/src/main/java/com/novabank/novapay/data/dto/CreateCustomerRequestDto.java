package com.novabank.novapay.data.dto;

public final class CreateCustomerRequestDto {
    private final String fullName;
    private final String email;
    private final String phone;
    private final String nationalId;
    private final String dob;
    private final String address;
    private final String initialPassword;

    public CreateCustomerRequestDto(
            String fullName,
            String email,
            String phone,
            String nationalId,
            String dob,
            String address,
            String initialPassword
    ) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.nationalId = nationalId;
        this.dob = dob;
        this.address = address;
        this.initialPassword = initialPassword;
    }
}

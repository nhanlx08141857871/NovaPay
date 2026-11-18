package com.novabank.novapay.domain.model.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CustomerValidationTest {

    @Test
    public void isValidEmail_checksFormat() {
        assertTrue(CustomerValidation.isValidEmail("customer@novapay.vn"));
        assertTrue(CustomerValidation.isValidEmail("  user.name+1@gmail.com  "));
        assertFalse(CustomerValidation.isValidEmail("invalid-email"));
        assertFalse(CustomerValidation.isValidEmail("user@domain"));
        assertFalse(CustomerValidation.isValidEmail(null));
    }

    @Test
    public void isValidPhone_acceptsVietnameseMobileFormats() {
        assertTrue(CustomerValidation.isValidPhone("0901234567"));
        assertTrue(CustomerValidation.isValidPhone("0901 234 567"));
        assertTrue(CustomerValidation.isValidPhone("+84901234567"));
        assertFalse(CustomerValidation.isValidPhone("12345"));
        assertFalse(CustomerValidation.isValidPhone("09012345"));
        assertFalse(CustomerValidation.isValidPhone(null));
    }

    @Test
    public void isValidNationalId_accepts9Or12Digits() {
        assertTrue(CustomerValidation.isValidNationalId("123456789"));
        assertTrue(CustomerValidation.isValidNationalId("079099001234"));
        assertFalse(CustomerValidation.isValidNationalId("1234567890"));
        assertFalse(CustomerValidation.isValidNationalId("07909900123A"));
        assertFalse(CustomerValidation.isValidNationalId(""));
    }

    @Test
    public void normalizeDobToIso_acceptsValidPastDatesAndRejectsInvalidOrFuture() {
        assertEquals("1999-05-20", CustomerValidation.normalizeDobToIso("1999-05-20"));
        assertEquals("1999-05-20", CustomerValidation.normalizeDobToIso("20/05/1999"));
        assertNull(CustomerValidation.normalizeDobToIso("2026-02-30"));
        assertNull(CustomerValidation.normalizeDobToIso("2999-01-01"));
        assertNull(CustomerValidation.normalizeDobToIso("not-a-date"));
    }

    @Test
    public void matchesQuery_filtersAcrossNameEmailPhoneAndNationalId() {
        CustomerProfile profile = new CustomerProfile(
                "uid_1",
                "CUSTOMER",
                "Nguyễn Văn An",
                "an.nguyen@novapay.vn",
                "0901234567",
                "079099001234",
                "1999-05-20",
                "Quận 7, TP.HCM",
                "",
                "ACTIVE",
                "VERIFIED",
                null,
                null,
                "officer_1",
                null,
                null
        );

        assertTrue(CustomerValidation.matchesQuery(profile, ""));
        assertTrue(CustomerValidation.matchesQuery(profile, "văn an"));
        assertTrue(CustomerValidation.matchesQuery(profile, "AN.NGUYEN"));
        assertTrue(CustomerValidation.matchesQuery(profile, "090123"));
        assertTrue(CustomerValidation.matchesQuery(profile, "079099"));
        assertFalse(CustomerValidation.matchesQuery(profile, "trần bình"));
    }
}

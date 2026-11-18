package com.novabank.novapay.core.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CurrencyFormatterTest {

    @Test
    public void formatVnd_formatsPositiveZeroAndNegativeAmountsWithDots() {
        assertEquals("0 ₫", CurrencyFormatter.formatVnd(0L));
        assertEquals("50.000 ₫", CurrencyFormatter.formatVnd(50_000L));
        assertEquals("1.250.000 ₫", CurrencyFormatter.formatVnd(1_250_000L));
        assertEquals("5.000.000.000 ₫", CurrencyFormatter.formatVnd(5_000_000_000L));
        assertEquals("-350.000 ₫", CurrencyFormatter.formatVnd(-350_000L));
    }

    @Test
    public void formatSignedVnd_addsExpectedPrefix() {
        assertEquals("+2.000.000 ₫", CurrencyFormatter.formatSignedVnd(2_000_000L, true));
        assertEquals("-150.000 ₫", CurrencyFormatter.formatSignedVnd(150_000L, false));
        assertEquals("0 ₫", CurrencyFormatter.formatSignedVnd(0L, true));
    }

    @Test
    public void parseVndDigits_extractsCleanLongValue() {
        assertEquals(1_250_000L, CurrencyFormatter.parseVndDigits("1.250.000 ₫"));
        assertEquals(0L, CurrencyFormatter.parseVndDigits(""));
        assertEquals(0L, CurrencyFormatter.parseVndDigits(null));
    }
}

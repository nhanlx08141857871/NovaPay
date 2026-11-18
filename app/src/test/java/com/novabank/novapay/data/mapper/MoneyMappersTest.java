package com.novabank.novapay.data.mapper;

import com.google.firebase.Timestamp;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.account.AccountStatus;
import com.novabank.novapay.domain.model.account.AccountType;
import com.novabank.novapay.domain.model.bank.Bank;
import com.novabank.novapay.domain.model.bank.Beneficiary;
import com.novabank.novapay.domain.model.transaction.Transaction;
import com.novabank.novapay.domain.model.transaction.TransactionStatus;
import com.novabank.novapay.domain.model.transaction.TransactionType;
import com.novabank.novapay.domain.model.utility.Order;
import com.novabank.novapay.domain.model.utility.ServiceCategory;
import com.novabank.novapay.domain.model.utility.ServiceItem;
import com.novabank.novapay.domain.model.utility.UtilityBill;
import com.novabank.novapay.domain.model.utility.UtilityBillStatus;
import com.novabank.novapay.domain.model.utility.UtilityBillType;

import org.junit.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MoneyMappersTest {

    @Test
    public void transactionMapper_parseOtpExpiresAt_supportsNumericIsoAndNull() {
        long epochMs = 1_763_440_000_123L;
        assertEquals(Instant.ofEpochMilli(epochMs),
                TransactionMapper.parseOtpExpiresAt(new JsonPrimitive(epochMs)));
        assertEquals(Instant.parse("2026-11-18T04:15:30Z"),
                TransactionMapper.parseOtpExpiresAt(new JsonPrimitive("2026-11-18T04:15:30Z")));
        assertEquals(Instant.ofEpochMilli(epochMs),
                TransactionMapper.parseOtpExpiresAt(new JsonPrimitive(String.valueOf(epochMs))));
        assertNull(TransactionMapper.parseOtpExpiresAt(JsonNull.INSTANCE));
        assertNull(TransactionMapper.parseOtpExpiresAt(null));
    }

    @Test
    public void transactionMapper_fromFirestoreMap_mapsTimestampAndLongMoney() {
        Map<String, Object> ext = new HashMap<>();
        ext.put("bankCode", "VCB");
        ext.put("accountNumber", "0071000123456");
        ext.put("name", "Phạm Minh Tuấn");

        Map<String, Object> meta = new HashMap<>();
        meta.put("billId", "bill-01");

        Map<String, Object> doc = new HashMap<>();
        doc.put("type", "TRANSFER_INTERBANK");
        doc.put("fromAccountId", "acc-chk-1");
        doc.put("toExternal", ext);
        doc.put("amount", 6_500_000L);
        doc.put("fee", 0L);
        doc.put("note", "Thanh toán hợp đồng");
        doc.put("status", "SUCCESS");
        doc.put("requiresBiometric", true);
        doc.put("biometricPassed", true);
        doc.put("otpVerified", true);
        doc.put("faceChecked", true);
        doc.put("meta", meta);
        doc.put("initiatedBy", "uid-1");
        doc.put("createdAt", new Timestamp(1_763_440_000L, 500_000_000));

        Transaction tx = TransactionMapper.fromFirestoreMap("tx-100", doc);
        assertNotNull(tx);
        assertEquals("tx-100", tx.getTxId());
        assertEquals(TransactionType.TRANSFER_INTERBANK, tx.getType());
        assertEquals(TransactionStatus.SUCCESS, tx.getStatus());
        assertEquals(6_500_000L, tx.getAmount());
        assertEquals("VCB", tx.getToExternal().getBankCode());
        assertEquals("bill-01", tx.getMeta().getBillId());
        assertTrue(tx.isFaceChecked());
        assertEquals(Instant.ofEpochSecond(1_763_440_000L, 500_000_000), tx.getCreatedAt());
    }

    @Test
    public void moneyAccountMapper_fromMap_mapsCheckingAndMortgageWithLongVnd() {
        Map<String, Object> mortgageMap = new HashMap<>();
        mortgageMap.put("loanAmount", 1_000_000_000L);
        mortgageMap.put("outstandingBalance", 850_000_000L);
        mortgageMap.put("termMonths", 120);
        mortgageMap.put("annualRate", 7.8);
        mortgageMap.put("paymentFrequency", "MONTHLY");
        mortgageMap.put("installmentAmount", 12_500_000L);
        mortgageMap.put("nextDueDate", new Timestamp(1_765_000_000L, 0));

        Map<String, Object> doc = new HashMap<>();
        doc.put("ownerUid", "uid-1");
        doc.put("type", "MORTGAGE");
        doc.put("accountNumber", "MOR9001");
        doc.put("status", "ACTIVE");
        doc.put("balance", 0L);
        doc.put("currency", "VND");
        doc.put("perTxnLimit", 50_000_000L);
        doc.put("dailyLimit", 100_000_000L);
        doc.put("mortgage", mortgageMap);
        doc.put("createdAt", new Timestamp(1_700_000_000L, 0));

        Account acc = MoneyAccountMapper.fromMap("acc-mor-1", doc);
        assertNotNull(acc);
        assertEquals(AccountType.MORTGAGE, acc.getType());
        assertEquals(AccountStatus.ACTIVE, acc.getStatus());
        assertNotNull(acc.getMortgage());
        assertEquals(850_000_000L, acc.getMortgage().getOutstandingBalance());
        assertEquals(12_500_000L, acc.getMortgage().getInstallmentAmount());
        assertEquals(Instant.ofEpochSecond(1_765_000_000L), acc.getMortgage().getNextDueDate());
    }

    @Test
    public void utilityMapper_handlesStringAndMapDetailsAndOrders() {
        Map<String, Object> detailsMap = new LinkedHashMap<>();
        detailsMap.put("Hãng bay", "Vietnam Airlines");
        detailsMap.put("Chặng", "SGN - HAN");

        Map<String, Object> itemDoc = new HashMap<>();
        itemDoc.put("category", "FLIGHT");
        itemDoc.put("title", "Vé máy bay VN210");
        itemDoc.put("price", 1_890_000L);
        itemDoc.put("details", detailsMap);
        itemDoc.put("imageUrl", "https://example.com/vn.png");

        ServiceItem item = UtilityMapper.serviceItemFromMap("item-1", itemDoc);
        assertNotNull(item);
        assertEquals(ServiceCategory.FLIGHT, item.getCategory());
        assertEquals(1_890_000L, item.getPrice());
        assertTrue(item.getDetails().contains("Vietnam Airlines"));
        assertTrue(item.getDetails().contains("SGN - HAN"));

        Map<String, Object> billDoc = new HashMap<>();
        billDoc.put("type", "ELECTRIC");
        billDoc.put("providerName", "EVN TP.HCM");
        billDoc.put("customerCode", "PE010001");
        billDoc.put("customerName", "Nguyễn Thị B");
        billDoc.put("period", "11/2026");
        billDoc.put("amount", 645_000L);
        billDoc.put("dueDate", new Timestamp(1_764_000_000L, 0));
        billDoc.put("status", "UNPAID");

        UtilityBill bill = UtilityMapper.billFromMap("bill-1", billDoc);
        assertNotNull(bill);
        assertEquals(UtilityBillType.ELECTRIC, bill.getType());
        assertEquals(UtilityBillStatus.UNPAID, bill.getStatus());
        assertEquals(645_000L, bill.getAmount());

        Map<String, Object> orderDoc = new HashMap<>();
        orderDoc.put("uid", "uid-1");
        orderDoc.put("itemId", "item-1");
        orderDoc.put("category", "FLIGHT");
        orderDoc.put("quantity", 2);
        orderDoc.put("totalAmount", 3_780_000L);
        orderDoc.put("status", "CONFIRMED");
        orderDoc.put("txId", "tx-200");
        orderDoc.put("createdAt", new Timestamp(1_763_440_100L, 0));

        Order order = UtilityMapper.orderFromMap("ord-1", orderDoc);
        assertNotNull(order);
        assertEquals(2, order.getQuantity());
        assertEquals(3_780_000L, order.getTotalAmount());
        assertEquals("tx-200", order.getTxId());

        Map<String, Object> bankDoc = new HashMap<>();
        bankDoc.put("name", "Vietcombank");
        bankDoc.put("logoUrl", "https://example.com/vcb.png");
        bankDoc.put("napasCode", "970436");
        Bank bank = BankBeneficiaryMapper.bankFromMap("VCB", bankDoc);
        assertNotNull(bank);
        assertEquals("970436", bank.getNapasCode());

        Beneficiary b = new Beneficiary("ben-1", "Trần C", "1000888", "VCB", "Cty");
        Map<String, Object> serialized = BankBeneficiaryMapper.beneficiaryToMap(b);
        Beneficiary roundTrip = BankBeneficiaryMapper.beneficiaryFromMap("ben-1", serialized);
        assertEquals(b, roundTrip);
    }
}

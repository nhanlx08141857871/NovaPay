package com.novabank.novapay.data.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.firebase.Timestamp;
import com.novabank.novapay.domain.model.notification.AppNotification;
import com.novabank.novapay.domain.model.notification.NotificationCategory;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class NotificationAndSavingsMapperTest {

    @Test
    public void notificationMapper_mapsFullPayloadAndNullableFieldsSafely() {
        Map<String, Object> data = new HashMap<>();
        data.put("category", "BALANCE");
        data.put("direction", "IN");
        data.put("title", "Biến động số dư +1.200.000 ₫");
        data.put("body", "Nhận tiền từ tài khoản 1001000002");
        data.put("amount", 1_200_000L);
        data.put("balanceAfter", 8_200_000L);
        data.put("accountNumber", "1001000001");
        data.put("txId", "tx_abc_1");
        data.put("deepLink", "novapay://receipt/tx_abc_1");
        data.put("read", false);
        data.put("createdAt", new Timestamp(1_795_000_000L, 0));

        AppNotification mapped = NotificationMapper.fromMap("notif_1", data);
        assertNotNull(mapped);
        assertEquals("notif_1", mapped.getNotifId());
        assertEquals(NotificationCategory.BALANCE, mapped.getCategory());
        assertTrue(mapped.isInflow());
        assertEquals(Long.valueOf(1_200_000L), mapped.getAmount());
        assertEquals(Long.valueOf(8_200_000L), mapped.getBalanceAfter());
        assertEquals("tx_abc_1", mapped.getTxId());
        assertFalse(mapped.isRead());
        assertNotNull(mapped.getCreatedAt());
    }

    @Test
    public void notificationMapper_handlesSecurityNotificationWithoutAmount() {
        Map<String, Object> data = new HashMap<>();
        data.put("category", "SECURITY");
        data.put("title", "Đã cập nhật Smart PIN");
        data.put("body", "Mã PIN giao dịch đã được thiết lập.");
        data.put("read", true);

        AppNotification mapped = NotificationMapper.fromMap("notif_sec", data);
        assertNotNull(mapped);
        assertEquals(NotificationCategory.SECURITY, mapped.getCategory());
        assertNull(mapped.getAmount());
        assertNull(mapped.getBalanceAfter());
        assertTrue(mapped.isRead());
    }
}

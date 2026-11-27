package com.novabank.novapay.data.mapper;

import com.google.firebase.firestore.DocumentSnapshot;
import com.novabank.novapay.domain.model.utility.Order;
import com.novabank.novapay.domain.model.utility.ServiceCategory;
import com.novabank.novapay.domain.model.utility.ServiceItem;
import com.novabank.novapay.domain.model.utility.UtilityBill;
import com.novabank.novapay.domain.model.utility.UtilityBillStatus;
import com.novabank.novapay.domain.model.utility.UtilityBillType;

import java.util.Map;
import java.util.StringJoiner;

public final class UtilityMapper {

    private UtilityMapper() {
    }

    public static UtilityBill billFromDocument(DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }
        return billFromMap(snapshot.getId(), snapshot.getData());
    }

    public static UtilityBill billFromMap(String billId, Map<String, Object> data) {
        if (billId == null || data == null) {
            return null;
        }
        UtilityBillType type = parseBillType(FirestoreMapUtils.toStringOrNull(data.get("type")));
        UtilityBillStatus status = parseBillStatus(FirestoreMapUtils.toStringOrNull(data.get("status")));
        if (type == null || status == null) {
            return null;
        }
        return new UtilityBill(
                billId,
                type,
                FirestoreMapUtils.toStringOrEmpty(data.get("providerName")),
                FirestoreMapUtils.toStringOrEmpty(data.get("customerCode")),
                FirestoreMapUtils.toStringOrEmpty(data.get("customerName")),
                FirestoreMapUtils.toStringOrEmpty(data.get("period")),
                FirestoreMapUtils.toLong(data.get("amount"), 0L),
                FirestoreMapUtils.toInstant(data.get("dueDate")),
                status
        );
    }

    public static ServiceItem serviceItemFromDocument(DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }
        return serviceItemFromMap(snapshot.getId(), snapshot.getData());
    }

    public static ServiceItem serviceItemFromMap(String itemId, Map<String, Object> data) {
        if (itemId == null || data == null) {
            return null;
        }
        ServiceCategory category = parseServiceCategory(FirestoreMapUtils.toStringOrNull(data.get("category")));
        if (category == null) {
            return null;
        }
        return new ServiceItem(
                itemId,
                category,
                FirestoreMapUtils.toStringOrEmpty(data.get("title")),
                FirestoreMapUtils.toLong(data.get("price"), 0L),
                parseDetailsSafely(data.get("details")),
                FirestoreMapUtils.toStringOrEmpty(data.get("imageUrl"))
        );
    }

    public static Order orderFromDocument(DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }
        return orderFromMap(snapshot.getId(), snapshot.getData());
    }

    public static Order orderFromMap(String orderId, Map<String, Object> data) {
        if (orderId == null || data == null) {
            return null;
        }
        ServiceCategory category = parseServiceCategory(FirestoreMapUtils.toStringOrNull(data.get("category")));
        if (category == null) {
            return null;
        }
        return new Order(
                orderId,
                FirestoreMapUtils.toStringOrEmpty(data.get("uid")),
                FirestoreMapUtils.toStringOrEmpty(data.get("itemId")),
                category,
                FirestoreMapUtils.toInt(data.get("quantity"), 1),
                FirestoreMapUtils.toLong(data.get("totalAmount"), 0L),
                FirestoreMapUtils.toStringOrEmpty(data.get("status")),
                FirestoreMapUtils.toStringOrNull(data.get("txId")),
                FirestoreMapUtils.toInstant(data.get("createdAt"))
        );
    }

    public static String parseDetailsSafely(Object rawDetails) {
        if (rawDetails == null) {
            return "";
        }
        if (rawDetails instanceof String text) {
            return text.trim();
        }
        if (rawDetails instanceof Map<?, ?> map) {
            StringJoiner joiner = new StringJoiner(" • ");
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    String val = String.valueOf(entry.getValue()).trim();
                    if (!val.isEmpty()) {
                        joiner.add(String.valueOf(entry.getKey()).trim() + ": " + val);
                    }
                }
            }
            return joiner.toString();
        }
        return String.valueOf(rawDetails).trim();
    }

    private static UtilityBillType parseBillType(String raw) {
        if (raw == null) return null;
        try {
            return UtilityBillType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static UtilityBillStatus parseBillStatus(String raw) {
        if (raw == null) return null;
        try {
            return UtilityBillStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static ServiceCategory parseServiceCategory(String raw) {
        if (raw == null) return null;
        try {
            return ServiceCategory.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

package com.novabank.novapay.data.mapper;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.novabank.novapay.data.dto.AccountLookupResponseDto;
import com.novabank.novapay.data.dto.ConfirmTransactionResponseDto;
import com.novabank.novapay.data.dto.ExternalRecipientDto;
import com.novabank.novapay.data.dto.FinalizePaymentResponseDto;
import com.novabank.novapay.data.dto.InitiateTransactionRequestDto;
import com.novabank.novapay.data.dto.InitiateTransactionResponseDto;
import com.novabank.novapay.data.dto.TransactionMetaDto;
import com.novabank.novapay.domain.model.transaction.AccountLookupResult;
import com.novabank.novapay.domain.model.transaction.ExternalRecipient;
import com.novabank.novapay.domain.model.transaction.PaymentFinalizeResult;
import com.novabank.novapay.domain.model.transaction.Transaction;
import com.novabank.novapay.domain.model.transaction.TransactionConfirmResult;
import com.novabank.novapay.domain.model.transaction.TransactionDraft;
import com.novabank.novapay.domain.model.transaction.TransactionInitiateRequest;
import com.novabank.novapay.domain.model.transaction.TransactionMeta;
import com.novabank.novapay.domain.model.transaction.TransactionStatus;
import com.novabank.novapay.domain.model.transaction.TransactionType;

import java.time.Instant;
import java.util.Map;

/**
 * Mapper hai chiều giữa DTO/Firestore và các thực thể Giao dịch thuần Java ở tầng domain.
 */
public final class TransactionMapper {

    private TransactionMapper() {
    }

    public static InitiateTransactionRequestDto toRequestDto(TransactionInitiateRequest req) {
        if (req == null) {
            return null;
        }
        ExternalRecipientDto extDto = null;
        if (req.getToExternal() != null) {
            extDto = new ExternalRecipientDto(
                    req.getToExternal().getBankCode(),
                    req.getToExternal().getAccountNumber(),
                    req.getToExternal().getName()
            );
        }
        TransactionMetaDto metaDto = null;
        if (req.getMeta() != null && !req.getMeta().isEmpty()) {
            TransactionMeta m = req.getMeta();
            metaDto = new TransactionMetaDto(
                    m.getBillId(),
                    m.getBillCode(),
                    m.getPhone(),
                    m.getItemId(),
                    m.getOrderId(),
                    m.getQuantity(),
                    m.getMortgageAccountId(),
                    m.getGatewayRef()
            );
        }
        return new InitiateTransactionRequestDto(
                req.getType().name(),
                req.getFromAccountId(),
                req.getToAccountId(),
                extDto,
                req.getAmount(),
                req.getNote(),
                metaDto
        );
    }

    public static AccountLookupResult toLookupResult(AccountLookupResponseDto dto) {
        if (dto == null || dto.getAccountId() == null || dto.getAccountNumber() == null) {
            return null;
        }
        return new AccountLookupResult(dto.getAccountId(), dto.getAccountNumber(), dto.getOwnerName());
    }

    public static TransactionDraft toDraft(InitiateTransactionResponseDto dto) {
        if (dto == null || dto.getDraftId() == null) {
            return null;
        }
        return new TransactionDraft(
                dto.getDraftId(),
                dto.isRequiresBiometric(),
                dto.isRequiresFace(),
                parseOtpExpiresAt(dto.getOtpExpiresAt())
        );
    }

    public static TransactionConfirmResult toConfirmResult(ConfirmTransactionResponseDto dto) {
        if (dto == null) {
            return null;
        }
        return new TransactionConfirmResult(dto.getTxId(), dto.getStatus(), dto.getClientSecret());
    }

    public static PaymentFinalizeResult toFinalizeResult(FinalizePaymentResponseDto dto) {
        if (dto == null) {
            return null;
        }
        return new PaymentFinalizeResult(dto.getTxId(), dto.getStatus());
    }

    /**
     * Giải mã otpExpiresAt linh hoạt: hỗ trợ cả JSON number (epoch millis) lẫn ISO-8601 string hoặc null.
     */
    public static Instant parseOtpExpiresAt(JsonElement element) {
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return null;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isNumber()) {
            try {
                return Instant.ofEpochMilli(primitive.getAsLong());
            } catch (RuntimeException e) {
                return null;
            }
        }
        if (primitive.isString()) {
            String raw = primitive.getAsString();
            if (raw == null || raw.trim().isEmpty()) {
                return null;
            }
            String trimmed = raw.trim();
            try {
                return Instant.parse(trimmed);
            } catch (RuntimeException ignored) {
                try {
                    return Instant.ofEpochMilli(Long.parseLong(trimmed));
                } catch (NumberFormatException ignored2) {
                    return null;
                }
            }
        }
        return null;
    }

    public static Transaction fromDocument(DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }
        return fromFirestoreMap(snapshot.getId(), snapshot.getData());
    }

    public static Transaction fromFirestoreMap(String txId, Map<String, Object> data) {
        if (txId == null || data == null) {
            return null;
        }
        TransactionType type = parseTransactionType(FirestoreMapUtils.toStringOrNull(data.get("type")));
        TransactionStatus status = parseTransactionStatus(FirestoreMapUtils.toStringOrNull(data.get("status")));
        if (type == null || status == null) {
            return null;
        }

        ExternalRecipient external = parseExternalRecipient(FirestoreMapUtils.toStringMap(data.get("toExternal")));
        TransactionMeta meta = parseTransactionMeta(FirestoreMapUtils.toStringMap(data.get("meta")));

        return new Transaction(
                txId,
                type,
                FirestoreMapUtils.toStringOrNull(data.get("fromAccountId")),
                FirestoreMapUtils.toStringOrNull(data.get("fromAccountNumber")),
                FirestoreMapUtils.toStringOrNull(data.get("toAccountId")),
                FirestoreMapUtils.toStringOrNull(data.get("toAccountNumber")),
                FirestoreMapUtils.toStringOrNull(data.get("toOwnerName")),
                external,
                FirestoreMapUtils.toLong(data.get("amount"), 0L),
                FirestoreMapUtils.toLong(data.get("fee"), 0L),
                FirestoreMapUtils.toStringOrEmpty(data.get("note")),
                status,
                FirestoreMapUtils.toBoolean(data.get("requiresBiometric"), false),
                FirestoreMapUtils.toBoolean(data.get("biometricPassed"), false),
                FirestoreMapUtils.toBoolean(data.get("otpVerified"), false),
                FirestoreMapUtils.toBoolean(data.get("faceChecked"), false),
                meta,
                FirestoreMapUtils.toStringOrNull(data.get("initiatedBy")),
                FirestoreMapUtils.toInstant(data.get("createdAt"))
        );
    }

    private static ExternalRecipient parseExternalRecipient(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        return new ExternalRecipient(
                FirestoreMapUtils.toStringOrEmpty(map.get("bankCode")),
                FirestoreMapUtils.toStringOrEmpty(map.get("accountNumber")),
                FirestoreMapUtils.toStringOrEmpty(map.get("name"))
        );
    }

    private static TransactionMeta parseTransactionMeta(Map<String, Object> map) {
        if (map == null) {
            return TransactionMeta.empty();
        }
        return new TransactionMeta(
                FirestoreMapUtils.toStringOrNull(map.get("billId")),
                FirestoreMapUtils.toStringOrNull(map.get("billCode")),
                FirestoreMapUtils.toStringOrNull(map.get("phone")),
                FirestoreMapUtils.toStringOrNull(map.get("itemId")),
                FirestoreMapUtils.toStringOrNull(map.get("orderId")),
                FirestoreMapUtils.toStringOrNull(map.get("mortgageAccountId")),
                FirestoreMapUtils.toStringOrNull(map.get("gatewayRef"))
        );
    }

    private static TransactionType parseTransactionType(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return TransactionType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static TransactionStatus parseTransactionStatus(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return TransactionStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

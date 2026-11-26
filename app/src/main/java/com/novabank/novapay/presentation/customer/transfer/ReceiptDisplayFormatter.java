package com.novabank.novapay.presentation.customer.transfer;

import com.novabank.novapay.domain.model.transaction.ExternalRecipient;
import com.novabank.novapay.domain.model.transaction.Transaction;
import com.novabank.novapay.domain.model.transaction.TransactionMeta;
import com.novabank.novapay.domain.model.transaction.TransactionType;

/**
 * Pure Java formatter cho màn hình Biên lai giao dịch điện tử (ReceiptFragment):
 * Ưu tiên hiển thị số tài khoản và tên người nhận (fromAccountNumber, toAccountNumber, toOwnerName),
 * tự động fallback về documentId đối với các giao dịch cũ chưa lưu snapshot.
 */
public final class ReceiptDisplayFormatter {

    private ReceiptDisplayFormatter() {
    }

    public static String formatFromAccount(Transaction tx) {
        if (tx == null) {
            return "-";
        }
        String accountNumber = trimToNull(tx.getFromAccountNumber());
        if (accountNumber != null) {
            return accountNumber;
        }
        String accountId = trimToNull(tx.getFromAccountId());
        return accountId != null ? accountId : "-";
    }

    public static String formatTargetSummary(Transaction tx) {
        if (tx == null) {
            return "-";
        }

        ExternalRecipient ext = tx.getToExternal();
        if (ext != null && !ext.getAccountNumber().isEmpty()) {
            return ext.getName() + " • " + ext.getBankCode() + " (" + ext.getAccountNumber() + ")";
        }

        String toAccountNumber = trimToNull(tx.getToAccountNumber());
        String toOwnerName = trimToNull(tx.getToOwnerName());
        TransactionMeta meta = tx.getMeta();
        TransactionType type = tx.getType();

        if (type == TransactionType.TRANSFER_INTERNAL) {
            if (toOwnerName != null && toAccountNumber != null) {
                return toOwnerName + " (" + toAccountNumber + ")";
            }
            if (toAccountNumber != null) {
                return toAccountNumber;
            }
            if (toOwnerName != null) {
                return toOwnerName;
            }
            String toAccountId = trimToNull(tx.getToAccountId());
            if (toAccountId != null) {
                return toAccountId;
            }
        } else if (type == TransactionType.GATEWAY) {
            if (toOwnerName != null && toAccountNumber != null) {
                return "Stripe • " + toOwnerName + " (" + toAccountNumber + ")";
            }
            if (toAccountNumber != null) {
                return "Stripe • " + toAccountNumber;
            }
            if (meta != null && meta.getGatewayRef() != null) {
                return "Stripe • " + meta.getGatewayRef();
            }
        } else if (type == TransactionType.BILL) {
            String code = toAccountNumber != null
                    ? toAccountNumber
                    : (meta != null && meta.getBillCode() != null
                            ? meta.getBillCode()
                            : (meta != null ? meta.getBillId() : null));
            if (toOwnerName != null && code != null) {
                return toOwnerName + " • " + code;
            }
            if (code != null) {
                return code;
            }
            if (toOwnerName != null) {
                return toOwnerName;
            }
        } else if (type == TransactionType.TOPUP) {
            String phone = toAccountNumber != null
                    ? toAccountNumber
                    : (meta != null ? meta.getPhone() : null);
            if (phone != null) {
                return phone;
            }
        } else if (type == TransactionType.SERVICE_PAYMENT) {
            String orderId = toAccountNumber != null
                    ? toAccountNumber
                    : (meta != null ? meta.getOrderId() : null);
            if (toOwnerName != null) {
                return orderId != null ? toOwnerName + " (" + orderId + ")" : toOwnerName;
            }
            if (meta != null && meta.getItemId() != null) {
                return orderId != null ? meta.getItemId() + " (" + orderId + ")" : meta.getItemId();
            }
        } else if (type == TransactionType.MORTGAGE_PAYMENT) {
            String mortNumber = toAccountNumber != null
                    ? toAccountNumber
                    : (meta != null ? meta.getMortgageAccountId() : null);
            if (toOwnerName != null && mortNumber != null) {
                return toOwnerName + " (" + mortNumber + ")";
            }
            if (mortNumber != null) {
                return mortNumber;
            }
        }

        // Fallback tổng quát (WITHDRAW, DEPOSIT hoặc giao dịch cũ thiếu trường mới)
        if (toOwnerName != null && toAccountNumber != null) {
            return toOwnerName + " (" + toAccountNumber + ")";
        }
        if (toAccountNumber != null) {
            return toAccountNumber;
        }
        String toAccountId = trimToNull(tx.getToAccountId());
        if (toAccountId != null) {
            return toAccountId;
        }
        if (meta != null) {
            if (meta.getBillCode() != null) {
                return meta.getBillCode();
            }
            if (meta.getBillId() != null) {
                return meta.getBillId();
            }
            if (meta.getPhone() != null) {
                return meta.getPhone();
            }
            if (meta.getMortgageAccountId() != null) {
                return meta.getMortgageAccountId();
            }
            if (meta.getItemId() != null) {
                return meta.getOrderId() != null
                        ? meta.getItemId() + " (" + meta.getOrderId() + ")"
                        : meta.getItemId();
            }
            if (meta.getGatewayRef() != null) {
                return "Stripe • " + meta.getGatewayRef();
            }
        }
        return formatFromAccount(tx);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

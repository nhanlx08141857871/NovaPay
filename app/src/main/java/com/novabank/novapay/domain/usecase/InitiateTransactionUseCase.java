package com.novabank.novapay.domain.usecase;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.transaction.ExternalRecipient;
import com.novabank.novapay.domain.model.transaction.TransactionDraft;
import com.novabank.novapay.domain.model.transaction.TransactionInitiateRequest;
import com.novabank.novapay.domain.model.transaction.TransactionMeta;
import com.novabank.novapay.domain.model.transaction.TransactionType;
import com.novabank.novapay.domain.repository.TransactionRepository;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public class InitiateTransactionUseCase {

    private final TransactionRepository transactionRepository;

    @Inject
    public InitiateTransactionUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Single<AppResult<TransactionDraft>> execute(TransactionInitiateRequest request, Account fromAccount) {
        if (request == null || request.getType() == null) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Thông tin giao dịch không hợp lệ."));
        }
        if (fromAccount == null) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Vui lòng chọn tài khoản thanh toán nguồn."));
        }
        if (!fromAccount.canBePaymentSource()) {
            return Single.just(AppResult.error(
                    "ACCOUNT_NOT_ACTIVE",
                    "Chỉ tài khoản thanh toán CHECKING đang hoạt động mới được thực hiện giao dịch."
            ));
        }
        if (!fromAccount.getAccountId().equals(request.getFromAccountId())) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Tài khoản nguồn không khớp với yêu cầu giao dịch."));
        }
        if (request.getAmount() <= 0L) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Số tiền giao dịch phải lớn hơn 0 đ."));
        }

        AppResult<TransactionDraft> typeValidation = validateByType(request, fromAccount);
        if (typeValidation != null) {
            return Single.just(typeValidation);
        }

        TransactionType type = request.getType();
        if (type != TransactionType.GATEWAY && type != TransactionType.DEPOSIT) {
            if (fromAccount.getPerTxnLimit() > 0L && request.getAmount() > fromAccount.getPerTxnLimit()) {
                return Single.just(AppResult.error("LIMIT_EXCEEDED", "Số tiền vượt quá hạn mức mỗi giao dịch."));
            }
            if (request.getAmount() > fromAccount.getBalance()) {
                return Single.just(AppResult.error("INSUFFICIENT_FUNDS", "Số dư tài khoản thanh toán không đủ."));
            }
        }

        return transactionRepository.initiateTransaction(request);
    }

    private static AppResult<TransactionDraft> validateByType(TransactionInitiateRequest request, Account fromAccount) {
        TransactionType type = request.getType();
        TransactionMeta meta = request.getMeta();

        switch (type) {
            case TRANSFER_INTERNAL:
                if (isBlank(request.getToAccountId())) {
                    return AppResult.error("INVALID_ARGUMENT", "Thiếu thông tin tài khoản nhận nội bộ.");
                }
                if (fromAccount.getAccountId().equals(request.getToAccountId().trim())) {
                    return AppResult.error("INVALID_ARGUMENT", "Không thể tự chuyển tiền cho chính tài khoản nguồn.");
                }
                return null;
            case TRANSFER_INTERBANK:
                ExternalRecipient ext = request.getToExternal();
                if (ext == null || isBlank(ext.getBankCode()) || isBlank(ext.getAccountNumber()) || isBlank(ext.getName())) {
                    return AppResult.error("INVALID_ARGUMENT", "Thiếu thông tin người nhận liên ngân hàng.");
                }
                return null;
            case MORTGAGE_PAYMENT:
                if (meta == null || isBlank(meta.getMortgageAccountId())) {
                    return AppResult.error("INVALID_ARGUMENT", "Thiếu mã tài khoản vay thế chấp cần thanh toán.");
                }
                return null;
            case BILL:
                if (meta == null || isBlank(meta.getBillId())) {
                    return AppResult.error("INVALID_ARGUMENT", "Thiếu mã hóa đơn cần thanh toán.");
                }
                return null;
            case TOPUP:
                if (meta == null || isBlank(meta.getPhone())) {
                    return AppResult.error("INVALID_ARGUMENT", "Vui lòng nhập số điện thoại cần nạp tiền.");
                }
                return null;
            case SERVICE_PAYMENT:
                if (meta == null || isBlank(meta.getItemId())) {
                    return AppResult.error("INVALID_ARGUMENT", "Thiếu mã dịch vụ hoặc sản phẩm thanh toán.");
                }
                return null;
            case DEPOSIT:
            case WITHDRAW:
            case GATEWAY:
            default:
                return null;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

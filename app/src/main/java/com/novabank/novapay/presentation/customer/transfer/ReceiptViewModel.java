package com.novabank.novapay.presentation.customer.transfer;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.domain.model.transaction.Transaction;
import com.novabank.novapay.domain.usecase.GetTransactionReceiptUseCase;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * ViewModel tải chi tiết giao dịch từ Firestore transactions/{txId} phục vụ hiển thị & xuất ảnh PNG.
 */
@HiltViewModel
public class ReceiptViewModel extends ViewModel {

    private final GetTransactionReceiptUseCase getTransactionReceiptUseCase;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final MutableLiveData<UiState<ReceiptUiState>> uiState = new MutableLiveData<>();

    @Inject
    public ReceiptViewModel(GetTransactionReceiptUseCase getTransactionReceiptUseCase) {
        this.getTransactionReceiptUseCase = getTransactionReceiptUseCase;
    }

    public LiveData<UiState<ReceiptUiState>> getUiState() {
        return uiState;
    }

    public void loadReceipt(@Nullable String txId) {
        if (txId == null || txId.trim().isEmpty()) {
            uiState.setValue(UiState.empty());
            return;
        }
        uiState.setValue(UiState.loading());
        disposables.add(
                getTransactionReceiptUseCase.execute(txId.trim())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> {
                            if (result.isSuccess()) {
                                Transaction tx = result.getData();
                                if (tx == null) {
                                    uiState.setValue(UiState.empty());
                                } else {
                                    uiState.setValue(UiState.success(new ReceiptUiState(tx)));
                                }
                            } else if ("NOT_FOUND".equals(result.getErrorCode())
                                    || "TRANSACTION_NOT_FOUND".equals(result.getErrorCode())) {
                                uiState.setValue(UiState.empty());
                            } else {
                                uiState.setValue(UiState.error(result.getMessage()));
                            }
                        }, throwable -> uiState.setValue(UiState.error(throwable.getMessage())))
        );
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}

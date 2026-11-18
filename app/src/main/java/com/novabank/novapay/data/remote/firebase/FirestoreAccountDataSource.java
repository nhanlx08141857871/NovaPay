package com.novabank.novapay.data.remote.firebase;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.data.mapper.AccountMapper;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.account.AccountType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

/**
 * Nguồn dữ liệu Firestore cho collection accounts/{accountId}.
 * Tuân thủ firestore.rules & firestore.indexes.json: chỉ lọc whereEqualTo("ownerUid", uid)
 * và sắp xếp trên bộ nhớ Java (không dùng orderBy chưa tạo index).
 */
@Singleton
public class FirestoreAccountDataSource {

    private final FirebaseFirestore firestore;

    @Inject
    public FirestoreAccountDataSource(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public Observable<AppResult<List<Account>>> observeAccountsForUser(String uid) {
        return Observable.create(emitter -> {
            ListenerRegistration registration = firestore.collection("accounts")
                    .whereEqualTo("ownerUid", uid)
                    .addSnapshotListener((snapshots, error) -> {
                        if (emitter.isDisposed()) {
                            return;
                        }
                        if (error != null) {
                            emitter.onNext(AppResult.error(
                                    "FIRESTORE_ERROR",
                                    error.getMessage() != null ? error.getMessage() : "Không thể tải danh sách tài khoản."
                            ));
                            return;
                        }
                        List<Account> accounts = new ArrayList<>();
                        if (snapshots != null) {
                            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                Account mapped = AccountMapper.fromSnapshot(doc);
                                if (mapped != null) {
                                    accounts.add(mapped);
                                }
                            }
                        }
                        sortAccounts(accounts);
                        emitter.onNext(AppResult.success(accounts));
                    });
            emitter.setCancellable(registration::remove);
        });
    }

    public Observable<AppResult<Account>> observeAccountById(String accountId) {
        return Observable.create(emitter -> {
            ListenerRegistration registration = firestore.collection("accounts")
                    .document(accountId)
                    .addSnapshotListener((snapshot, error) -> {
                        if (emitter.isDisposed()) {
                            return;
                        }
                        if (error != null) {
                            emitter.onNext(AppResult.error(
                                    "FIRESTORE_ERROR",
                                    error.getMessage() != null ? error.getMessage() : "Không thể tải thông tin tài khoản."
                            ));
                            return;
                        }
                        if (snapshot == null || !snapshot.exists()) {
                            emitter.onNext(AppResult.error("NOT_FOUND", "Không tìm thấy tài khoản."));
                            return;
                        }
                        Account mapped = AccountMapper.fromSnapshot(snapshot);
                        if (mapped == null) {
                            emitter.onNext(AppResult.error("MAPPING_ERROR", "Dữ liệu tài khoản không hợp lệ."));
                        } else {
                            emitter.onNext(AppResult.success(mapped));
                        }
                    });
            emitter.setCancellable(registration::remove);
        });
    }

    public Single<AppResult<Account>> getAccountById(String accountId) {
        return Single.create(emitter ->
                firestore.collection("accounts")
                        .document(accountId)
                        .get()
                        .addOnSuccessListener(snapshot -> {
                            if (emitter.isDisposed()) {
                                return;
                            }
                            if (snapshot == null || !snapshot.exists()) {
                                emitter.onSuccess(AppResult.error("NOT_FOUND", "Không tìm thấy tài khoản."));
                                return;
                            }
                            Account mapped = AccountMapper.fromSnapshot(snapshot);
                            if (mapped == null) {
                                emitter.onSuccess(AppResult.error("MAPPING_ERROR", "Dữ liệu tài khoản không hợp lệ."));
                            } else {
                                emitter.onSuccess(AppResult.success(mapped));
                            }
                        })
                        .addOnFailureListener(ex -> {
                            if (!emitter.isDisposed()) {
                                emitter.onSuccess(AppResult.error(
                                        "FIRESTORE_ERROR",
                                        ex.getMessage() != null ? ex.getMessage() : "Không thể tải tài khoản."
                                ));
                            }
                        })
        );
    }

    private static void sortAccounts(List<Account> accounts) {
        accounts.sort(Comparator
                .comparingInt((Account a) -> typeOrder(a.getType()))
                .thenComparing(Account::getAccountNumber));
    }

    private static int typeOrder(AccountType type) {
        if (type == AccountType.CHECKING) {
            return 0;
        }
        if (type == AccountType.SAVING) {
            return 1;
        }
        return 2;
    }
}

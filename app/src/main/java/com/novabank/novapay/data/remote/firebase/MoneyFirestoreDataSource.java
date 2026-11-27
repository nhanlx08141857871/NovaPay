package com.novabank.novapay.data.remote.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.data.mapper.BankBeneficiaryMapper;
import com.novabank.novapay.data.mapper.MoneyAccountMapper;
import com.novabank.novapay.data.mapper.TransactionMapper;
import com.novabank.novapay.data.mapper.UtilityMapper;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.bank.Bank;
import com.novabank.novapay.domain.model.bank.Beneficiary;
import com.novabank.novapay.domain.model.transaction.Transaction;
import com.novabank.novapay.domain.model.utility.Order;
import com.novabank.novapay.domain.model.utility.ServiceCategory;
import com.novabank.novapay.domain.model.utility.ServiceItem;
import com.novabank.novapay.domain.model.utility.UtilityBill;
import com.novabank.novapay.domain.model.utility.UtilityBillType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

@Singleton
public class MoneyFirestoreDataSource {

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;

    @Inject
    public MoneyFirestoreDataSource(FirebaseAuth firebaseAuth, FirebaseFirestore firestore) {
        this.firebaseAuth = firebaseAuth;
        this.firestore = firestore;
    }

    public Observable<AppResult<List<Account>>> observeMyAccounts() {
        String uid = currentUidOrNull();
        if (uid == null) {
            return Observable.just(AppResult.error("UNAUTHENTICATED", "Vui lòng đăng nhập để tiếp tục."));
        }
        return Observable.create(emitter -> {
            ListenerRegistration registration = firestore.collection("accounts")
                    .whereEqualTo("ownerUid", uid)
                    .addSnapshotListener((snapshots, error) -> {
                        if (emitter.isDisposed()) {
                            return;
                        }
                        if (error != null) {
                            emitter.onNext(AppResult.error("FIRESTORE_ERROR", error.getMessage()));
                            return;
                        }
                        List<Account> accounts = new ArrayList<>();
                        if (snapshots != null) {
                            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                Account mapped = MoneyAccountMapper.fromDocument(doc);
                                if (mapped != null) {
                                    accounts.add(mapped);
                                }
                            }
                        }
                        emitter.onNext(AppResult.success(accounts));
                    });
            emitter.setCancellable(registration::remove);
        });
    }

    public Single<AppResult<List<Account>>> getMyAccountsOnce() {
        String uid = currentUidOrNull();
        if (uid == null) {
            return Single.just(AppResult.error("UNAUTHENTICATED", "Vui lòng đăng nhập để tiếp tục."));
        }
        return Single.create(emitter -> firestore.collection("accounts")
                .whereEqualTo("ownerUid", uid)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (emitter.isDisposed()) return;
                    List<Account> accounts = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Account mapped = MoneyAccountMapper.fromDocument(doc);
                        if (mapped != null) {
                            accounts.add(mapped);
                        }
                    }
                    emitter.onSuccess(AppResult.success(accounts));
                })
                .addOnFailureListener(e -> {
                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(AppResult.error("FIRESTORE_ERROR", e.getMessage()));
                    }
                }));
    }

    public Single<AppResult<Account>> getAccountById(String accountId) {
        if (currentUidOrNull() == null) {
            return Single.just(AppResult.error("UNAUTHENTICATED", "Vui lòng đăng nhập để tiếp tục."));
        }
        return Single.create(emitter -> firestore.collection("accounts")
                .document(accountId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (emitter.isDisposed()) return;
                    Account account = MoneyAccountMapper.fromDocument(snapshot);
                    if (account == null) {
                        emitter.onSuccess(AppResult.error("ACCOUNT_NOT_FOUND", "Không tìm thấy tài khoản."));
                    } else {
                        emitter.onSuccess(AppResult.success(account));
                    }
                })
                .addOnFailureListener(e -> {
                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(AppResult.error("FIRESTORE_ERROR", e.getMessage()));
                    }
                }));
    }

    public Single<AppResult<Transaction>> getTransactionById(String txId) {
        if (currentUidOrNull() == null) {
            return Single.just(AppResult.error("UNAUTHENTICATED", "Vui lòng đăng nhập để tiếp tục."));
        }
        return Single.create(emitter -> firestore.collection("transactions")
                .document(txId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (emitter.isDisposed()) return;
                    Transaction tx = TransactionMapper.fromDocument(snapshot);
                    if (tx == null) {
                        emitter.onSuccess(AppResult.error("TRANSACTION_NOT_FOUND", "Không tìm thấy giao dịch."));
                    } else {
                        emitter.onSuccess(AppResult.success(tx));
                    }
                })
                .addOnFailureListener(e -> {
                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(AppResult.error("FIRESTORE_ERROR", e.getMessage()));
                    }
                }));
    }

    public Single<AppResult<List<Bank>>> getSupportedBanks() {
        if (currentUidOrNull() == null) {
            return Single.just(AppResult.error("UNAUTHENTICATED", "Vui lòng đăng nhập để tiếp tục."));
        }
        return Single.create(emitter -> firestore.collection("banks")
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (emitter.isDisposed()) return;
                    List<Bank> banks = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Bank bank = BankBeneficiaryMapper.bankFromDocument(doc);
                        if (bank != null) {
                            banks.add(bank);
                        }
                    }
                    banks.sort(Comparator.comparing(Bank::getName, String.CASE_INSENSITIVE_ORDER));
                    emitter.onSuccess(AppResult.success(banks));
                })
                .addOnFailureListener(e -> {
                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(AppResult.error("FIRESTORE_ERROR", e.getMessage()));
                    }
                }));
    }

    public Observable<AppResult<List<Beneficiary>>> observeBeneficiaries() {
        String uid = currentUidOrNull();
        if (uid == null) {
            return Observable.just(AppResult.error("UNAUTHENTICATED", "Vui lòng đăng nhập để tiếp tục."));
        }
        return Observable.create(emitter -> {
            ListenerRegistration registration = firestore.collection("users")
                    .document(uid)
                    .collection("beneficiaries")
                    .addSnapshotListener((snapshots, error) -> {
                        if (emitter.isDisposed()) return;
                        if (error != null) {
                            emitter.onNext(AppResult.error("FIRESTORE_ERROR", error.getMessage()));
                            return;
                        }
                        List<Beneficiary> list = new ArrayList<>();
                        if (snapshots != null) {
                            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                Beneficiary b = BankBeneficiaryMapper.beneficiaryFromDocument(doc);
                                if (b != null) {
                                    list.add(b);
                                }
                            }
                        }
                        list.sort(Comparator.comparing(Beneficiary::getName, String.CASE_INSENSITIVE_ORDER));
                        emitter.onNext(AppResult.success(list));
                    });
            emitter.setCancellable(registration::remove);
        });
    }

    public Single<AppResult<Beneficiary>> saveBeneficiary(Beneficiary beneficiary) {
        String uid = currentUidOrNull();
        if (uid == null) {
            return Single.just(AppResult.error("UNAUTHENTICATED", "Vui lòng đăng nhập để tiếp tục."));
        }
        String docId = !beneficiary.getId().isEmpty()
                ? beneficiary.getId()
                : buildDeterministicBeneficiaryId(beneficiary.getBankCode(), beneficiary.getAccountNumber());

        DocumentReference docRef = firestore.collection("users")
                .document(uid)
                .collection("beneficiaries")
                .document(docId);

        Map<String, Object> payload = BankBeneficiaryMapper.beneficiaryToMap(beneficiary);
        return Single.create(emitter -> docRef.set(payload)
                .addOnSuccessListener(unused -> {
                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(AppResult.success(new Beneficiary(
                                docId,
                                beneficiary.getName(),
                                beneficiary.getAccountNumber(),
                                beneficiary.getBankCode(),
                                beneficiary.getAlias()
                        )));
                    }
                })
                .addOnFailureListener(e -> {
                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(AppResult.error("FIRESTORE_ERROR", e.getMessage()));
                    }
                }));
    }

    public Single<AppResult<Boolean>> deleteBeneficiary(String beneficiaryId) {
        String uid = currentUidOrNull();
        if (uid == null) {
            return Single.just(AppResult.error("UNAUTHENTICATED", "Vui lòng đăng nhập để tiếp tục."));
        }
        return Single.create(emitter -> firestore.collection("users")
                .document(uid)
                .collection("beneficiaries")
                .document(beneficiaryId)
                .delete()
                .addOnSuccessListener(unused -> {
                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(AppResult.success(Boolean.TRUE));
                    }
                })
                .addOnFailureListener(e -> {
                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(AppResult.error("FIRESTORE_ERROR", e.getMessage()));
                    }
                }));
    }

    public Single<AppResult<List<UtilityBill>>> lookupUnpaidBills(String customerCode, UtilityBillType type) {
        if (currentUidOrNull() == null) {
            return Single.just(AppResult.error("UNAUTHENTICATED", "Vui lòng đăng nhập để tiếp tục."));
        }
        // Khớp chính xác composite index (customerCode ASC, status ASC) trong firestore.indexes.json
        return Single.create(emitter -> firestore.collection("utility_bills")
                .whereEqualTo("customerCode", customerCode)
                .whereEqualTo("status", "UNPAID")
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (emitter.isDisposed()) return;
                    List<UtilityBill> bills = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        UtilityBill bill = UtilityMapper.billFromDocument(doc);
                        if (bill != null && (type == null || bill.getType() == type)) {
                            bills.add(bill);
                        }
                    }
                    bills.sort(Comparator.comparing(
                            UtilityBill::getDueDate,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ));
                    emitter.onSuccess(AppResult.success(bills));
                })
                .addOnFailureListener(e -> {
                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(AppResult.error("FIRESTORE_ERROR", e.getMessage()));
                    }
                }));
    }

    public Single<AppResult<List<ServiceItem>>> getServiceCatalog(ServiceCategory category) {
        if (currentUidOrNull() == null) {
            return Single.just(AppResult.error("UNAUTHENTICATED", "Vui lòng đăng nhập để tiếp tục."));
        }
        Query query = category != null
                ? firestore.collection("service_catalog").whereEqualTo("category", category.name())
                : firestore.collection("service_catalog");

        return Single.create(emitter -> query.get()
                .addOnSuccessListener(snapshots -> {
                    if (emitter.isDisposed()) return;
                    List<ServiceItem> items = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        ServiceItem item = UtilityMapper.serviceItemFromDocument(doc);
                        if (item != null) {
                            items.add(item);
                        }
                    }
                    items.sort(Comparator.comparing(ServiceItem::getTitle, String.CASE_INSENSITIVE_ORDER));
                    emitter.onSuccess(AppResult.success(items));
                })
                .addOnFailureListener(e -> {
                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(AppResult.error("FIRESTORE_ERROR", e.getMessage()));
                    }
                }));
    }

    public Observable<AppResult<List<Order>>> observeMyOrders() {
        String uid = currentUidOrNull();
        if (uid == null) {
            return Observable.just(AppResult.error("UNAUTHENTICATED", "Vui lòng đăng nhập để tiếp tục."));
        }
        // Khớp composite index (uid ASC, createdAt DESC) trong firestore.indexes.json
        return Observable.create(emitter -> {
            ListenerRegistration registration = firestore.collection("orders")
                    .whereEqualTo("uid", uid)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .addSnapshotListener((snapshots, error) -> {
                        if (emitter.isDisposed()) return;
                        if (error != null) {
                            emitter.onNext(AppResult.error("FIRESTORE_ERROR", error.getMessage()));
                            return;
                        }
                        List<Order> orders = new ArrayList<>();
                        if (snapshots != null) {
                            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                Order order = UtilityMapper.orderFromDocument(doc);
                                if (order != null) {
                                    orders.add(order);
                                }
                            }
                        }
                        emitter.onNext(AppResult.success(orders));
                    });
            emitter.setCancellable(registration::remove);
        });
    }

    private String currentUidOrNull() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    private static String buildDeterministicBeneficiaryId(String bankCode, String accountNumber) {
        String prefix = (bankCode == null || bankCode.trim().isEmpty()) ? "NOVAPAY" : bankCode.trim();
        String raw = prefix + "_" + accountNumber.trim();
        return raw.replaceAll("[^A-Za-z0-9_-]", "_");
    }
}

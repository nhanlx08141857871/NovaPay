package com.novabank.novapay.data.remote.firebase;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.data.mapper.LedgerMapper;
import com.novabank.novapay.domain.model.transaction.LedgerDirection;
import com.novabank.novapay.domain.model.transaction.LedgerEntry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Observable;

/**
 * Nguồn dữ liệu Firestore cho sổ cái accounts/{accountId}/ledger (khớp index direction ASC, createdAt DESC).
 */
@Singleton
public class FirestoreLedgerDataSource {

    private final FirebaseFirestore firestore;

    @Inject
    public FirestoreLedgerDataSource(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public Observable<AppResult<List<LedgerEntry>>> observeLedger(
            String accountId,
            LedgerDirection directionOrNull,
            Instant fromInclusive,
            Instant toExclusive
    ) {
        return Observable.create(emitter -> {
            Query query = firestore.collection("accounts")
                    .document(accountId)
                    .collection("ledger");

            if (directionOrNull != null) {
                query = query.whereEqualTo("direction", directionOrNull.name());
            }
            if (fromInclusive != null) {
                query = query.whereGreaterThanOrEqualTo(
                        "createdAt",
                        new Timestamp(fromInclusive.getEpochSecond(), fromInclusive.getNano())
                );
            }
            if (toExclusive != null) {
                query = query.whereLessThan(
                        "createdAt",
                        new Timestamp(toExclusive.getEpochSecond(), toExclusive.getNano())
                );
            }
            query = query.orderBy("createdAt", Query.Direction.DESCENDING);

            ListenerRegistration registration = query.addSnapshotListener((snapshots, error) -> {
                if (emitter.isDisposed()) {
                    return;
                }
                if (error != null) {
                    emitter.onNext(AppResult.error(
                            "FIRESTORE_ERROR",
                            error.getMessage() != null ? error.getMessage() : "Không thể tải lịch sử biến động số dư."
                    ));
                    return;
                }
                List<LedgerEntry> entries = new ArrayList<>();
                if (snapshots != null) {
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        LedgerEntry mapped = LedgerMapper.fromSnapshot(doc, accountId);
                        if (mapped != null) {
                            entries.add(mapped);
                        }
                    }
                }
                emitter.onNext(AppResult.success(entries));
            });
            emitter.setCancellable(registration::remove);
        });
    }
}

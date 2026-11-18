package com.novabank.novapay.data.remote.firebase;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.data.mapper.InterestRateMapper;
import com.novabank.novapay.domain.model.rate.InterestRate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Observable;

/**
 * Nguồn dữ liệu Firestore cho bảng interest_rates/{rateId}.
 * Không dùng orderBy kết hợp whereEqualTo do không có composite index trên interest_rates.
 */
@Singleton
public class FirestoreInterestRateDataSource {

    private final FirebaseFirestore firestore;

    @Inject
    public FirestoreInterestRateDataSource(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public Observable<AppResult<List<InterestRate>>> observeAllRates() {
        return Observable.create(emitter -> {
            ListenerRegistration registration = firestore.collection("interest_rates")
                    .addSnapshotListener((snapshots, error) -> {
                        if (emitter.isDisposed()) {
                            return;
                        }
                        if (error != null) {
                            emitter.onNext(AppResult.error(
                                    "FIRESTORE_ERROR",
                                    error.getMessage() != null ? error.getMessage() : "Không thể tải biểu lãi suất."
                            ));
                            return;
                        }
                        List<InterestRate> rates = new ArrayList<>();
                        if (snapshots != null) {
                            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                InterestRate mapped = InterestRateMapper.fromSnapshot(doc);
                                if (mapped != null) {
                                    rates.add(mapped);
                                }
                            }
                        }
                        rates.sort(Comparator.comparingInt(InterestRate::getTermMonths));
                        emitter.onNext(AppResult.success(rates));
                    });
            emitter.setCancellable(registration::remove);
        });
    }

    public Observable<AppResult<InterestRate>> observeRateForTerm(int termMonths) {
        return Observable.create(emitter -> {
            ListenerRegistration registration = firestore.collection("interest_rates")
                    .whereEqualTo("termMonths", termMonths)
                    .addSnapshotListener((snapshots, error) -> {
                        if (emitter.isDisposed()) {
                            return;
                        }
                        if (error != null) {
                            emitter.onNext(AppResult.error(
                                    "FIRESTORE_ERROR",
                                    error.getMessage() != null ? error.getMessage() : "Không thể tải lãi suất kỳ hạn."
                            ));
                            return;
                        }
                        InterestRate latest = null;
                        if (snapshots != null) {
                            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                InterestRate candidate = InterestRateMapper.fromSnapshot(doc);
                                if (candidate != null && (latest == null || isNewer(candidate, latest))) {
                                    latest = candidate;
                                }
                            }
                        }
                        if (latest == null) {
                            emitter.onNext(AppResult.error("NOT_FOUND", "Chưa có biểu lãi suất cho kỳ hạn này."));
                        } else {
                            emitter.onNext(AppResult.success(latest));
                        }
                    });
            emitter.setCancellable(registration::remove);
        });
    }

    private static boolean isNewer(InterestRate candidate, InterestRate current) {
        Instant cTime = candidate.getUpdatedAt() != null ? candidate.getUpdatedAt() : candidate.getEffectiveFrom();
        Instant curTime = current.getUpdatedAt() != null ? current.getUpdatedAt() : current.getEffectiveFrom();
        if (cTime == null) {
            return false;
        }
        if (curTime == null) {
            return true;
        }
        return cTime.isAfter(curTime);
    }
}

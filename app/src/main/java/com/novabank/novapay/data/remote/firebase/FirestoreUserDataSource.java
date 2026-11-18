package com.novabank.novapay.data.remote.firebase;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.data.mapper.UserMapper;
import com.novabank.novapay.domain.model.user.User;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

/**
 * Nguồn dữ liệu Firestore cho users/{uid}.
 * Chỉ cập nhật đúng 4 trường được firestore.rules cho phép: phone, address, avatarUrl, updatedAt.
 */
@Singleton
public class FirestoreUserDataSource {

    private final FirebaseFirestore firestore;

    @Inject
    public FirestoreUserDataSource(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public Observable<AppResult<User>> observeUser(String uid) {
        return Observable.create(emitter -> {
            ListenerRegistration registration = firestore.collection("users")
                    .document(uid)
                    .addSnapshotListener((snapshot, error) -> {
                        if (emitter.isDisposed()) {
                            return;
                        }
                        if (error != null) {
                            emitter.onNext(AppResult.error(
                                    "FIRESTORE_ERROR",
                                    error.getMessage() != null ? error.getMessage() : "Không thể tải hồ sơ người dùng."
                            ));
                            return;
                        }
                        if (snapshot == null || !snapshot.exists()) {
                            emitter.onNext(AppResult.error("NOT_FOUND", "Không tìm thấy hồ sơ người dùng."));
                            return;
                        }
                        User user = UserMapper.fromSnapshot(snapshot);
                        if (user == null) {
                            emitter.onNext(AppResult.error("MAPPING_ERROR", "Dữ liệu hồ sơ không hợp lệ."));
                        } else {
                            emitter.onNext(AppResult.success(user));
                        }
                    });
            emitter.setCancellable(registration::remove);
        });
    }

    public Single<AppResult<User>> getUser(String uid) {
        return Single.create(emitter ->
                firestore.collection("users")
                        .document(uid)
                        .get()
                        .addOnSuccessListener(snapshot -> {
                            if (emitter.isDisposed()) {
                                return;
                            }
                            if (snapshot == null || !snapshot.exists()) {
                                emitter.onSuccess(AppResult.error("NOT_FOUND", "Không tìm thấy hồ sơ người dùng."));
                                return;
                            }
                            User user = UserMapper.fromSnapshot(snapshot);
                            if (user == null) {
                                emitter.onSuccess(AppResult.error("MAPPING_ERROR", "Dữ liệu hồ sơ không hợp lệ."));
                            } else {
                                emitter.onSuccess(AppResult.success(user));
                            }
                        })
                        .addOnFailureListener(ex -> {
                            if (!emitter.isDisposed()) {
                                emitter.onSuccess(AppResult.error(
                                        "FIRESTORE_ERROR",
                                        ex.getMessage() != null ? ex.getMessage() : "Không thể tải hồ sơ."
                                ));
                            }
                        })
        );
    }

    public Single<AppResult<Void>> updateAllowedProfileFields(
            String uid,
            String phone,
            String address,
            String avatarUrl
    ) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("phone", phone);
        updates.put("address", address);
        updates.put("avatarUrl", avatarUrl);
        updates.put("updatedAt", FieldValue.serverTimestamp());

        return Single.create(emitter ->
                firestore.collection("users")
                        .document(uid)
                        .update(updates)
                        .addOnSuccessListener(unused -> {
                            if (!emitter.isDisposed()) {
                                emitter.onSuccess(AppResult.success(null));
                            }
                        })
                        .addOnFailureListener(ex -> {
                            if (!emitter.isDisposed()) {
                                emitter.onSuccess(AppResult.error(
                                        "UPDATE_FAILED",
                                        ex.getMessage() != null ? ex.getMessage() : "Không thể cập nhật hồ sơ."
                                ));
                            }
                        })
        );
    }
}

package com.novabank.novapay.data.repository;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.core.security.SessionManager;
import com.novabank.novapay.core.util.DateUtils;
import com.novabank.novapay.data.dto.CreateCustomerRequestDto;
import com.novabank.novapay.data.dto.EkycReviewRequestDto;
import com.novabank.novapay.data.dto.UpdateCustomerRequestDto;
import com.novabank.novapay.data.mapper.ApiErrorParser;
import com.novabank.novapay.data.mapper.FirestoreTimestampMapper;
import com.novabank.novapay.data.mapper.OfficerFirestoreMapper;
import com.novabank.novapay.data.remote.server.OfficerApi;
import com.novabank.novapay.domain.model.audit.AuditLog;
import com.novabank.novapay.domain.model.audit.FaceCheckAudit;
import com.novabank.novapay.domain.model.officer.EkycReviewCandidate;
import com.novabank.novapay.domain.model.officer.OfficerDashboardSummary;
import com.novabank.novapay.domain.model.user.CustomerProfile;
import com.novabank.novapay.domain.repository.OfficerRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

@Singleton
public final class OfficerRepositoryImpl implements OfficerRepository {

    private final FirebaseFirestore firestore;
    private final FirebaseAuth firebaseAuth;
    private final SessionManager sessionManager;
    private final OfficerApi officerApi;
    private final ApiErrorParser apiErrorParser;

    @Inject
    public OfficerRepositoryImpl(
            FirebaseFirestore firestore,
            FirebaseAuth firebaseAuth,
            SessionManager sessionManager,
            OfficerApi officerApi,
            ApiErrorParser apiErrorParser
    ) {
        this.firestore = firestore;
        this.firebaseAuth = firebaseAuth;
        this.sessionManager = sessionManager;
        this.officerApi = officerApi;
        this.apiErrorParser = apiErrorParser;
    }

    @Override
    public Single<AppResult<OfficerDashboardSummary>> getDashboardSummary() {
        return Single.<AppResult<OfficerDashboardSummary>>create(emitter -> {
            Task<QuerySnapshot> customersTask = firestore.collection("users")
                    .whereEqualTo("role", Constants.ROLE_CUSTOMER)
                    .get();

            Task<QuerySnapshot> todayTxTask = firestore.collection("transactions")
                    .whereGreaterThanOrEqualTo(
                            "createdAt",
                            FirestoreTimestampMapper.toTimestamp(DateUtils.startOfTodayVietnam())
                    )
                    .get();

            Task<QuerySnapshot> faceChecksTask = firestore.collection("face_checks")
                    .whereEqualTo("manualAuditRequired", true)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(20)
                    .get();

            Task<QuerySnapshot> auditLogsTask = firestore.collection("audit_logs")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(20)
                    .get();

            Tasks.whenAllComplete(customersTask, todayTxTask, faceChecksTask, auditLogsTask)
                    .addOnCompleteListener(allTask -> {
                        if (emitter.isDisposed()) {
                            return;
                        }
                        if (!customersTask.isSuccessful() && !auditLogsTask.isSuccessful()) {
                            Exception ex = customersTask.getException();
                            String msg = ex != null && ex.getMessage() != null
                                    ? ex.getMessage()
                                    : "Không thể tải bảng điều khiển nhân viên.";
                            emitter.onSuccess(AppResult.error("FIRESTORE_ERROR", msg));
                            return;
                        }

                        int totalCustomers = 0;
                        int pendingEkycCount = 0;
                        if (customersTask.isSuccessful() && customersTask.getResult() != null) {
                            for (DocumentSnapshot doc : customersTask.getResult().getDocuments()) {
                                totalCustomers++;
                                CustomerProfile profile = OfficerFirestoreMapper.toCustomerProfile(doc);
                                if (profile != null && profile.isEkycPending()) {
                                    pendingEkycCount++;
                                }
                            }
                        }

                        int dailySuccessTxCount = 0;
                        if (todayTxTask.isSuccessful() && todayTxTask.getResult() != null) {
                            for (DocumentSnapshot doc : todayTxTask.getResult().getDocuments()) {
                                String status = doc.getString("status");
                                if ("SUCCESS".equalsIgnoreCase(status)) {
                                    dailySuccessTxCount++;
                                }
                            }
                        }

                        List<FaceCheckAudit> pendingFaceAudits = new ArrayList<>();
                        if (faceChecksTask.isSuccessful() && faceChecksTask.getResult() != null) {
                            for (DocumentSnapshot doc : faceChecksTask.getResult().getDocuments()) {
                                FaceCheckAudit item = OfficerFirestoreMapper.toFaceCheckAudit(doc);
                                if (item != null) {
                                    pendingFaceAudits.add(item);
                                }
                            }
                        }

                        List<AuditLog> recentAuditLogs = new ArrayList<>();
                        if (auditLogsTask.isSuccessful() && auditLogsTask.getResult() != null) {
                            for (DocumentSnapshot doc : auditLogsTask.getResult().getDocuments()) {
                                AuditLog log = OfficerFirestoreMapper.toAuditLog(doc);
                                if (log != null) {
                                    recentAuditLogs.add(log);
                                }
                            }
                        }

                        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                        String officerEmail = currentUser != null && currentUser.getEmail() != null
                                ? currentUser.getEmail()
                                : "";

                        emitter.onSuccess(AppResult.success(new OfficerDashboardSummary(
                                officerEmail,
                                totalCustomers,
                                pendingEkycCount,
                                dailySuccessTxCount,
                                pendingFaceAudits.size(),
                                pendingFaceAudits,
                                recentAuditLogs
                        )));
                    });
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Observable<AppResult<List<CustomerProfile>>> observeCustomers() {
        return Observable.<AppResult<List<CustomerProfile>>>create(emitter -> {
            ListenerRegistration registration = firestore.collection("users")
                    .whereEqualTo("role", Constants.ROLE_CUSTOMER)
                    .addSnapshotListener((snapshot, error) -> {
                        if (emitter.isDisposed()) {
                            return;
                        }
                        if (error != null) {
                            emitter.onNext(AppResult.error(
                                    "FIRESTORE_ERROR",
                                    error.getMessage() != null
                                            ? error.getMessage()
                                            : "Không thể tải danh sách khách hàng."
                            ));
                            return;
                        }
                        List<CustomerProfile> customers = new ArrayList<>();
                        if (snapshot != null) {
                            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                CustomerProfile profile = OfficerFirestoreMapper.toCustomerProfile(doc);
                                if (profile != null) {
                                    customers.add(profile);
                                }
                            }
                        }
                        customers.sort(customerComparator());
                        emitter.onNext(AppResult.success(customers));
                    });
            emitter.setCancellable(registration::remove);
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Single<AppResult<String>> createCustomer(
            String fullName,
            String email,
            String phone,
            String nationalId,
            String dob,
            String address,
            String initialPassword
    ) {
        CreateCustomerRequestDto body = new CreateCustomerRequestDto(
                fullName, email, phone, nationalId, dob, address, initialPassword
        );
        return officerApi.createCustomer(body)
                .map(dto -> AppResult.success(dto.getUid()))
                .onErrorReturn(apiErrorParser::toErrorResult)
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<AppResult<Boolean>> updateCustomerProfile(
            String uid,
            String fullName,
            String phone,
            String address
    ) {
        UpdateCustomerRequestDto body = new UpdateCustomerRequestDto(fullName, phone, address, null);
        return officerApi.updateCustomer(uid, body)
                .map(dto -> AppResult.success(dto.isOk()))
                .onErrorReturn(apiErrorParser::toErrorResult)
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<AppResult<Boolean>> updateCustomerStatus(String uid, String nextStatus) {
        UpdateCustomerRequestDto body = new UpdateCustomerRequestDto(null, null, null, nextStatus);
        return officerApi.updateCustomer(uid, body)
                .map(dto -> AppResult.success(dto.isOk()))
                .onErrorReturn(apiErrorParser::toErrorResult)
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<AppResult<List<EkycReviewCandidate>>> getEkycReviewCandidates() {
        return Single.<AppResult<List<EkycReviewCandidate>>>create(emitter -> {
            Task<QuerySnapshot> customersTask = firestore.collection("users")
                    .whereEqualTo("role", Constants.ROLE_CUSTOMER)
                    .get();

            Task<QuerySnapshot> faceChecksTask = firestore.collection("face_checks")
                    .whereEqualTo("manualAuditRequired", true)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(50)
                    .get();

            Tasks.whenAllComplete(customersTask, faceChecksTask)
                    .addOnCompleteListener(initialTask -> {
                        if (emitter.isDisposed()) {
                            return;
                        }
                        if (!customersTask.isSuccessful()) {
                            Exception ex = customersTask.getException();
                            emitter.onSuccess(AppResult.error(
                                    "FIRESTORE_ERROR",
                                    ex != null && ex.getMessage() != null
                                            ? ex.getMessage()
                                            : "Không thể tải danh sách hồ sơ eKYC."
                            ));
                            return;
                        }

                        Map<String, FaceCheckAudit> manualAuditByUid = new HashMap<>();
                        if (faceChecksTask.isSuccessful() && faceChecksTask.getResult() != null) {
                            for (DocumentSnapshot doc : faceChecksTask.getResult().getDocuments()) {
                                FaceCheckAudit audit = OfficerFirestoreMapper.toFaceCheckAudit(doc);
                                if (audit != null && !audit.getUid().isEmpty()
                                        && !manualAuditByUid.containsKey(audit.getUid())) {
                                    manualAuditByUid.put(audit.getUid(), audit);
                                }
                            }
                        }

                        List<CustomerProfile> relevantCustomers = new ArrayList<>();
                        if (customersTask.getResult() != null) {
                            for (DocumentSnapshot doc : customersTask.getResult().getDocuments()) {
                                CustomerProfile profile = OfficerFirestoreMapper.toCustomerProfile(doc);
                                if (profile != null && (!profile.isEkycVerified()
                                        || manualAuditByUid.containsKey(profile.getUid()))) {
                                    relevantCustomers.add(profile);
                                }
                            }
                        }

                        if (relevantCustomers.isEmpty()) {
                            emitter.onSuccess(AppResult.success(Collections.emptyList()));
                            return;
                        }

                        List<Task<DocumentSnapshot>> imageTasks = new ArrayList<>(relevantCustomers.size());
                        for (CustomerProfile profile : relevantCustomers) {
                            imageTasks.add(
                                    firestore.collection("ekyc_images")
                                            .document(profile.getUid())
                                            .get()
                            );
                        }

                        Tasks.whenAllComplete(imageTasks).addOnCompleteListener(imgDoneTask -> {
                            if (emitter.isDisposed()) {
                                return;
                            }
                            List<EkycReviewCandidate> candidates = new ArrayList<>(relevantCustomers.size());
                            for (int i = 0; i < relevantCustomers.size(); i++) {
                                CustomerProfile profile = relevantCustomers.get(i);
                                Task<DocumentSnapshot> imgTask = imageTasks.get(i);
                                String imageBase64 = null;
                                if (imgTask.isSuccessful() && imgTask.getResult() != null
                                        && imgTask.getResult().exists()) {
                                    imageBase64 = imgTask.getResult().getString("imageBase64");
                                }
                                FaceCheckAudit audit = manualAuditByUid.get(profile.getUid());
                                candidates.add(new EkycReviewCandidate(profile, imageBase64, audit));
                            }
                            candidates.sort((a, b) -> Integer.compare(priorityOf(a), priorityOf(b)));
                            emitter.onSuccess(AppResult.success(candidates));
                        });
                    });
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Single<AppResult<String>> getEkycImageBase64(String uid) {
        return Single.<AppResult<String>>create(emitter -> firestore.collection("ekyc_images")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (emitter.isDisposed()) {
                        return;
                    }
                    String base64 = doc.exists() ? doc.getString("imageBase64") : null;
                    emitter.onSuccess(AppResult.success(base64));
                })
                .addOnFailureListener(error -> {
                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(AppResult.error(
                                "FIRESTORE_ERROR",
                                error.getMessage() != null
                                        ? error.getMessage()
                                        : "Không thể tải ảnh eKYC."
                        ));
                    }
                })).subscribeOn(Schedulers.io());
    }

    @Override
    public Single<AppResult<String>> reviewEkyc(String uid, String decision) {
        return officerApi.reviewEkyc(new EkycReviewRequestDto(uid, decision))
                .map(dto -> AppResult.success(dto.getStatus()))
                .onErrorReturn(apiErrorParser::toErrorResult)
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Completable signOut() {
        return Completable.fromAction(sessionManager::signOut);
    }

    private static int priorityOf(EkycReviewCandidate c) {
        if (c.getCustomer().isEkycPending()) {
            return 0;
        }
        if (c.getManualAuditItem() != null) {
            return 1;
        }
        return 2;
    }

    private static Comparator<CustomerProfile> customerComparator() {
        return (a, b) -> {
            Instant ca = a.getCreatedAt();
            Instant cb = b.getCreatedAt();
            if (ca != null && cb != null) {
                int cmp = cb.compareTo(ca);
                if (cmp != 0) {
                    return cmp;
                }
            } else if (ca != null) {
                return -1;
            } else if (cb != null) {
                return 1;
            }
            return a.getFullName().compareToIgnoreCase(b.getFullName());
        };
    }
}

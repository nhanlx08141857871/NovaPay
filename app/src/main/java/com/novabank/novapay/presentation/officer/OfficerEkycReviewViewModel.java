package com.novabank.novapay.presentation.officer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.core.common.SingleLiveEvent;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.domain.model.officer.EkycReviewCandidate;
import com.novabank.novapay.domain.usecase.officer.GetEkycReviewCandidatesUseCase;
import com.novabank.novapay.domain.usecase.officer.ReviewEkycUseCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

@HiltViewModel
public class OfficerEkycReviewViewModel extends ViewModel {

    private final GetEkycReviewCandidatesUseCase getEkycReviewCandidatesUseCase;
    private final ReviewEkycUseCase reviewEkycUseCase;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<UiState<OfficerEkycReviewUiState>> uiState =
            new MutableLiveData<>(UiState.loading());
    private final MutableLiveData<Boolean> reviewInProgress = new MutableLiveData<>(false);
    private final SingleLiveEvent<String> feedbackMessageEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Integer> feedbackResEvent = new SingleLiveEvent<>();

    private List<EkycReviewCandidate> allCandidates = Collections.emptyList();
    private OfficerEkycReviewUiState.FilterMode currentFilter =
            OfficerEkycReviewUiState.FilterMode.PENDING_ONLY;

    @Inject
    public OfficerEkycReviewViewModel(
            GetEkycReviewCandidatesUseCase getEkycReviewCandidatesUseCase,
            ReviewEkycUseCase reviewEkycUseCase
    ) {
        this.getEkycReviewCandidatesUseCase = getEkycReviewCandidatesUseCase;
        this.reviewEkycUseCase = reviewEkycUseCase;
        loadPendingEkyc();
    }

    public LiveData<UiState<OfficerEkycReviewUiState>> getUiState() {
        return uiState;
    }

    public LiveData<Boolean> getReviewInProgress() {
        return reviewInProgress;
    }

    public LiveData<String> getFeedbackMessageEvent() {
        return feedbackMessageEvent;
    }

    public LiveData<Integer> getFeedbackResEvent() {
        return feedbackResEvent;
    }

    public void setFilterMode(OfficerEkycReviewUiState.FilterMode filterMode) {
        if (filterMode != null) {
            this.currentFilter = filterMode;
            applyFilterAndEmit();
        }
    }

    public void loadPendingEkyc() {
        uiState.setValue(UiState.loading());
        disposables.add(
                getEkycReviewCandidatesUseCase.execute()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                this::handleCandidatesResult,
                                error -> uiState.setValue(UiState.error(error.getMessage()))
                        )
        );
    }

    public void reviewEkyc(String uid, String decision, int approvedResId, int rejectedResId) {
        if (Boolean.TRUE.equals(reviewInProgress.getValue())) {
            return;
        }
        reviewInProgress.setValue(true);
        disposables.add(
                reviewEkycUseCase.execute(uid, decision)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                result -> {
                                    reviewInProgress.setValue(false);
                                    if (result.isSuccess()) {
                                        boolean approved = Constants.EKYC_STATUS_VERIFIED.equals(decision);
                                        feedbackResEvent.setValue(approved ? approvedResId : rejectedResId);
                                        loadPendingEkyc();
                                    } else {
                                        feedbackMessageEvent.setValue(result.getMessage());
                                    }
                                },
                                error -> {
                                    reviewInProgress.setValue(false);
                                    feedbackMessageEvent.setValue(error.getMessage());
                                }
                        )
        );
    }

    private void handleCandidatesResult(AppResult<List<EkycReviewCandidate>> result) {
        if (result == null) {
            uiState.setValue(UiState.empty());
            return;
        }
        if (result.isError()) {
            uiState.setValue(UiState.error(result.getMessage()));
            return;
        }
        allCandidates = result.getData() != null ? result.getData() : Collections.emptyList();
        applyFilterAndEmit();
    }

    private void applyFilterAndEmit() {
        List<EkycReviewCandidate> filtered = new ArrayList<>();
        for (EkycReviewCandidate item : allCandidates) {
            switch (currentFilter) {
                case PENDING_ONLY:
                    if (item.getCustomer().isEkycPending()) {
                        filtered.add(item);
                    }
                    break;
                case ALL_UNVERIFIED:
                    if (!item.getCustomer().isEkycVerified()) {
                        filtered.add(item);
                    }
                    break;
                case MANUAL_FACE_AUDIT:
                    if (item.getManualAuditItem() != null) {
                        filtered.add(item);
                    }
                    break;
            }
        }
        if (filtered.isEmpty()) {
            uiState.setValue(UiState.empty());
        } else {
            uiState.setValue(UiState.success(new OfficerEkycReviewUiState(filtered, currentFilter)));
        }
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}

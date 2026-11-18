package com.novabank.novapay.presentation.officer;

import com.novabank.novapay.domain.model.officer.EkycReviewCandidate;

import java.util.List;

public final class OfficerEkycReviewUiState {

    public enum FilterMode {
        PENDING_ONLY,
        ALL_UNVERIFIED,
        MANUAL_FACE_AUDIT
    }

    private final List<EkycReviewCandidate> candidates;
    private final FilterMode filterMode;

    public OfficerEkycReviewUiState(List<EkycReviewCandidate> candidates, FilterMode filterMode) {
        this.candidates = candidates;
        this.filterMode = filterMode;
    }

    public List<EkycReviewCandidate> getCandidates() {
        return candidates;
    }

    public FilterMode getFilterMode() {
        return filterMode;
    }
}

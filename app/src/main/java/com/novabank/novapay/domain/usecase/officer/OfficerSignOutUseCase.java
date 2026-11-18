package com.novabank.novapay.domain.usecase.officer;

import com.novabank.novapay.domain.repository.OfficerRepository;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Completable;

public final class OfficerSignOutUseCase {

    private final OfficerRepository officerRepository;

    @Inject
    public OfficerSignOutUseCase(OfficerRepository officerRepository) {
        this.officerRepository = officerRepository;
    }

    public Completable execute() {
        return officerRepository.signOut();
    }
}

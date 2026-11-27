package com.novabank.novapay.domain.usecase;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.utility.ServiceCategory;
import com.novabank.novapay.domain.model.utility.ServiceItem;
import com.novabank.novapay.domain.repository.UtilityRepository;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public class GetServiceCatalogUseCase {

    private final UtilityRepository utilityRepository;

    @Inject
    public GetServiceCatalogUseCase(UtilityRepository utilityRepository) {
        this.utilityRepository = utilityRepository;
    }

    public Single<AppResult<List<ServiceItem>>> execute(ServiceCategory category) {
        return utilityRepository.getServiceCatalog(category);
    }
}

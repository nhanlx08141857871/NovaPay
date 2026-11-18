package com.novabank.novapay.domain.repository;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.rate.InterestRate;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;

/**
 * Hợp đồng quan sát bảng lãi suất tiết kiệm theo kỳ hạn (Pure Java 17).
 */
public interface InterestRateRepository {

    Observable<AppResult<List<InterestRate>>> observeAllRates();

    Observable<AppResult<InterestRate>> observeRateForTerm(int termMonths);
}

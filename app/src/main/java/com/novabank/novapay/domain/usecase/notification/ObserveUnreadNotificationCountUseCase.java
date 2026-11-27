package com.novabank.novapay.domain.usecase.notification;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.repository.NotificationRepository;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Observable;

public final class ObserveUnreadNotificationCountUseCase {

    private final NotificationRepository notificationRepository;

    @Inject
    public ObserveUnreadNotificationCountUseCase(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Observable<AppResult<Integer>> execute() {
        return notificationRepository.observeUnreadCount();
    }
}

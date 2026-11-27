package com.novabank.novapay.domain.usecase.notification;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.repository.NotificationRepository;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public final class MarkAllNotificationsReadUseCase {

    private final NotificationRepository notificationRepository;

    @Inject
    public MarkAllNotificationsReadUseCase(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Single<AppResult<Boolean>> execute() {
        return notificationRepository.markAllAsRead();
    }
}

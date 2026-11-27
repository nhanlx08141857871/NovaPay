package com.novabank.novapay.domain.usecase.notification;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.repository.NotificationRepository;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public final class MarkNotificationReadUseCase {

    private final NotificationRepository notificationRepository;

    @Inject
    public MarkNotificationReadUseCase(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Single<AppResult<Boolean>> execute(String notifId) {
        if (notifId == null || notifId.trim().isEmpty()) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Mã thông báo không hợp lệ."));
        }
        return notificationRepository.markAsRead(notifId.trim());
    }
}

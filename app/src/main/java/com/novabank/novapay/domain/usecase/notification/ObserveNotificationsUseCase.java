package com.novabank.novapay.domain.usecase.notification;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.notification.AppNotification;
import com.novabank.novapay.domain.repository.NotificationRepository;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Observable;

public final class ObserveNotificationsUseCase {

    private final NotificationRepository notificationRepository;

    @Inject
    public ObserveNotificationsUseCase(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Observable<AppResult<List<AppNotification>>> execute() {
        return notificationRepository.observeNotifications();
    }
}

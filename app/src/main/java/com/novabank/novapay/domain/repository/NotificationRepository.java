package com.novabank.novapay.domain.repository;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.notification.AppNotification;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

/**
 * Hợp đồng quan sát hộp thư thông báo users/{uid}/notifications và đánh dấu đã đọc (Pure Java 17).
 */
public interface NotificationRepository {

    Observable<AppResult<List<AppNotification>>> observeNotifications();

    Observable<AppResult<Integer>> observeUnreadCount();

    Single<AppResult<Boolean>> markAsRead(String notifId);

    Single<AppResult<Boolean>> markAllAsRead();
}

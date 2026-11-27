package com.novabank.novapay.domain.usecase.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.notification.AppNotification;
import com.novabank.novapay.domain.model.notification.NotificationCategory;
import com.novabank.novapay.domain.repository.NotificationRepository;

import org.junit.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public class NotificationUseCasesTest {

    private final NotificationRepository fakeRepo = new NotificationRepository() {
        @Override
        public Observable<AppResult<List<AppNotification>>> observeNotifications() {
            return Observable.just(AppResult.success(Collections.singletonList(
                    new AppNotification(
                            "n1",
                            NotificationCategory.BALANCE,
                            "IN",
                            "Nhận tiền",
                            "Cộng 500.000 ₫",
                            500_000L,
                            5_500_000L,
                            "1001000001",
                            "tx_1",
                            "novapay://receipt/tx_1",
                            false,
                            Instant.now()
                    )
            )));
        }

        @Override
        public Observable<AppResult<Integer>> observeUnreadCount() {
            return Observable.just(AppResult.success(1));
        }

        @Override
        public Single<AppResult<Boolean>> markAsRead(String notifId) {
            return Single.just(AppResult.success(Boolean.TRUE));
        }

        @Override
        public Single<AppResult<Boolean>> markAllAsRead() {
            return Single.just(AppResult.success(Boolean.TRUE));
        }
    };

    @Test
    public void notificationUseCases_delegateAndValidateIds() {
        ObserveNotificationsUseCase observeUseCase = new ObserveNotificationsUseCase(fakeRepo);
        assertEquals(1, observeUseCase.execute().blockingFirst().getData().size());

        ObserveUnreadNotificationCountUseCase unreadUseCase = new ObserveUnreadNotificationCountUseCase(fakeRepo);
        assertEquals(Integer.valueOf(1), unreadUseCase.execute().blockingFirst().getData());

        MarkNotificationReadUseCase markReadUseCase = new MarkNotificationReadUseCase(fakeRepo);
        assertTrue(markReadUseCase.execute("  ").blockingGet().isError());
        assertTrue(markReadUseCase.execute("n1").blockingGet().isSuccess());

        MarkAllNotificationsReadUseCase markAllUseCase = new MarkAllNotificationsReadUseCase(fakeRepo);
        assertTrue(markAllUseCase.execute().blockingGet().isSuccess());
    }
}

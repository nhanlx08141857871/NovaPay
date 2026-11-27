package com.novabank.novapay.core.notification;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.novabank.novapay.MainActivity;
import com.novabank.novapay.R;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.domain.model.notification.AppNotification;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Tạo kênh thông báo IMPORTANCE_HIGH và hiển thị Android System Notification
 * khi có biến động số dư / tiết kiệm / bảo mật / eKYC.
 */
@Singleton
public class SystemNotificationHelper {

    private final Context context;

    @Inject
    public SystemNotificationHelper(@ApplicationContext Context context) {
        this.context = context;
        ensureChannel();
    }

    public void ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) {
                return;
            }
            NotificationChannel existing = manager.getNotificationChannel(Constants.NOTIFICATION_CHANNEL_BALANCE_ALERTS);
            if (existing == null) {
                NotificationChannel channel = new NotificationChannel(
                        Constants.NOTIFICATION_CHANNEL_BALANCE_ALERTS,
                        context.getString(R.string.core_notif_channel_name),
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription(context.getString(R.string.core_notif_channel_desc));
                channel.enableVibration(true);
                manager.createNotificationChannel(channel);
            }
        }
    }

    public void showNotification(@NonNull AppNotification notification) {
        if (!canPostNotifications()) {
            return;
        }
        ensureChannel();

        String targetUri = resolveTargetDeepLink(notification);
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(targetUri));
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int requestCode = notification.getNotifId().hashCode();
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = !notification.getTitle().isEmpty()
                ? notification.getTitle()
                : context.getString(R.string.app_name);
        String body = notification.getBody();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context,
                Constants.NOTIFICATION_CHANNEL_BALANCE_ALERTS
        )
                .setSmallIcon(R.drawable.ic_np_bell)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(context).notify(requestCode, builder.build());
    }

    private boolean canPostNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    @NonNull
    public static String resolveTargetDeepLink(@NonNull AppNotification notification) {
        String deepLink = notification.getDeepLink();
        if (deepLink != null && deepLink.startsWith(Constants.DEEPLINK_SCHEME + "://")) {
            return deepLink;
        }
        if (notification.getTxId() != null && !notification.getTxId().isEmpty()) {
            return Constants.DEEPLINK_RECEIPT_PREFIX + Uri.encode(notification.getTxId());
        }
        return Constants.DEEPLINK_NOTIFICATIONS;
    }
}

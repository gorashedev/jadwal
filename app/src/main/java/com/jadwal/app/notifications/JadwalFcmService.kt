package com.jadwal.notifications

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.jadwal.MainActivity
import com.jadwal.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * JadwalFcmService — يستقبل إشعارات Push من Firebase Cloud Messaging.
 *
 * يدعم نوعين من الإشعارات:
 * 1. "daily_reminder" — تذكير يومي مبرمَج من الـ Server
 * 2. "exam_alert" — تنبيه امتحان من الـ Server
 *
 * مثال على Payload المُرسَل من Server:
 * {
 *   "to": "<fcm_token>",
 *   "data": {
 *     "type": "daily_reminder",
 *     "subject": "رياضيات",
 *     "tasks_count": "3"
 *   }
 * }
 *
 * يجب إضافة هذا في AndroidManifest.xml:
 * <service
 *     android:name=".notifications.JadwalFcmService"
 *     android:exported="false">
 *     <intent-filter>
 *         <action android:name="com.google.firebase.MESSAGING_EVENT"/>
 *     </intent-filter>
 * </service>
 */
@AndroidEntryPoint
class JadwalFcmService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationManager: JadwalNotificationManager

    @Inject
    lateinit var tokenRepository: FcmTokenRepository

    // ===== استقبال رسالة جديدة من FCM =====
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        val notification = remoteMessage.notification

        when (data["type"]) {
            "daily_reminder" -> {
                notificationManager.showDailyReminder(
                    subjectName = data["subject"] ?: "",
                    tasksCount = data["tasks_count"]?.toIntOrNull() ?: 0,
                )
            }
            "exam_alert" -> {
                val subject = data["subject"] ?: return
                val days = data["days_until"]?.toIntOrNull() ?: return
                notificationManager.showExamAlert(subject, days)
            }
            else -> {
                // إشعار عام — يُعرض كإشعار نصي
                if (notification != null) {
                    showGenericNotification(
                        title = notification.title ?: "جدول",
                        body = notification.body ?: "",
                    )
                }
            }
        }
    }

    // ===== تجديد رمز FCM =====
    // يُستدعى عند تجديد الـ Token من Firebase تلقائياً
    override fun onNewToken(token: String) {
        tokenRepository.saveToken(token)
    }

    // ===== عرض إشعار عام =====
    private fun showGenericNotification(title: String, body: String) {
        val tapIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, JadwalNotificationManager.CHANNEL_DAILY_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(
            System.currentTimeMillis().toInt(),
            notification,
        )
    }
}

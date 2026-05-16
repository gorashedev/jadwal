package com.jadwal.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.jadwal.MainActivity
import com.jadwal.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JadwalNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_DAILY_REMINDER = "jadwal_daily_reminder"
        const val CHANNEL_EXAM_ALERT = "jadwal_exam_alert"
        const val CHANNEL_SESSION_COMPLETE = "jadwal_session_complete"

        const val NOTIF_ID_DAILY_REMINDER = 1001
        const val NOTIF_ID_EXAM_ALERT = 1002
        const val NOTIF_ID_SESSION_COMPLETE = 1003
    }

    // ===== إنشاء قنوات الإشعارات — يجب استدعاؤها مرة واحدة عند بدء التطبيق =====
    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // قناة التذكير اليومي
            NotificationChannel(
                CHANNEL_DAILY_REMINDER,
                "التذكير اليومي",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تذكير يومي بجدول المذاكرة"
                enableVibration(true)
                notificationManager.createNotificationChannel(this)
            }

            // قناة تنبيه الامتحانات
            NotificationChannel(
                CHANNEL_EXAM_ALERT,
                "تنبيهات الامتحانات",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات عند اقتراب موعد الامتحان"
                enableVibration(true)
                notificationManager.createNotificationChannel(this)
            }

            // قناة إتمام الجلسة
            NotificationChannel(
                CHANNEL_SESSION_COMPLETE,
                "إتمام الجلسة",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعار عند الانتهاء من جلسة مذاكرة"
                notificationManager.createNotificationChannel(this)
            }
        }
    }

    // ===== إشعار التذكير اليومي =====
    fun showDailyReminder(subjectName: String = "", tasksCount: Int = 0) {
        val tapIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "وقت المذاكرة! 📚"
        val body = when {
            tasksCount > 0 && subjectName.isNotBlank() ->
                "لديك $tasksCount مهام اليوم، ابدأ بـ $subjectName"
            tasksCount > 0 ->
                "لديك $tasksCount مهام مذاكرة اليوم — ابدأ الآن!"
            else ->
                "لا تنسَ المذاكرة اليوم، كل يوم يُقربك من هدفك"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID_DAILY_REMINDER, notification)
    }

    // ===== إشعار تنبيه الامتحان =====
    fun showExamAlert(subjectName: String, daysUntil: Int) {
        val tapIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val body = when (daysUntil) {
            0 -> "امتحان $subjectName اليوم! حظاً موفقاً 🍀"
            1 -> "امتحان $subjectName غداً — راجع ملاحظاتك الآن"
            else -> "امتحان $subjectName بعد $daysUntil أيام — تأكد من مراجعة جميع الفصول"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_EXAM_ALERT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⚠️ امتحان قريب!")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID_EXAM_ALERT, notification)
    }

    // ===== إشعار إتمام الجلسة =====
    fun showSessionComplete(subjectName: String, minutesStudied: Int) {
        val body = "أتممت $minutesStudied دقيقة في $subjectName — أحسنت! 🎉"

        val notification = NotificationCompat.Builder(context, CHANNEL_SESSION_COMPLETE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("جلسة مكتملة ✅")
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID_SESSION_COMPLETE, notification)
    }
}

package com.jadwal

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class JadwalApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // قناة التذكير اليومي
            NotificationChannel(
                CHANNEL_DAILY,
                "تذكير يومي",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "تذكير بجدول المذاكرة اليومي"
                manager.createNotificationChannel(this)
            }

            // قناة تذكير الامتحان
            NotificationChannel(
                CHANNEL_EXAM,
                "تذكير امتحان",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "تذكير بمواعيد الامتحانات القادمة"
                manager.createNotificationChannel(this)
            }
        }
    }

    companion object {
        const val CHANNEL_DAILY = "jadwal_daily"
        const val CHANNEL_EXAM = "jadwal_exam"
    }
}

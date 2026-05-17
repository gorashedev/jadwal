package com.jadwal.app.di

import android.content.Context
import androidx.work.WorkManager
import com.jadwal.app.notifications.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * NotificationModule — يُسجّل جميع كلاسات الإشعارات في Hilt.
 *
 * ملاحظة: DailyReminderWorker و ExamAlertWorker يُحقَنان عبر
 * @HiltWorker + @AssistedInject — لا تحتاج تسجيلهما هنا.
 * الـ WorkManager نفسه يتولى إنشاءهما عبر HiltWorkerFactory.
 */
@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {

    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager = WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun provideJadwalNotificationManager(
        @ApplicationContext context: Context
    ): JadwalNotificationManager = JadwalNotificationManager(context)

    @Provides
    @Singleton
    fun provideNotificationScheduler(
        @ApplicationContext context: Context
    ): NotificationScheduler = NotificationScheduler(context)
}

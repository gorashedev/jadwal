package com.jadwal.app.di

import android.content.Context
import androidx.room.Room
import com.jadwal.data.local.JadwalDatabase
import com.jadwal.data.local.dao.ExamDao
import com.jadwal.data.local.dao.ScheduleDao
import com.jadwal.data.local.dao.SessionDao
import com.jadwal.data.local.dao.SubjectDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): JadwalDatabase =
        Room.databaseBuilder(
            context,
            JadwalDatabase::class.java,
            JadwalDatabase.DATABASE_NAME,
        )
            .fallbackToDestructiveMigration() // للـ dev فقط — في الـ production استخدم Migrations
            .build()

    @Provides
    fun provideSubjectDao(db: JadwalDatabase): SubjectDao = db.subjectDao()

    @Provides
    fun provideExamDao(db: JadwalDatabase): ExamDao = db.examDao()

    @Provides
    fun provideScheduleDao(db: JadwalDatabase): ScheduleDao = db.scheduleDao()

    @Provides
    fun provideSessionDao(db: JadwalDatabase): SessionDao = db.sessionDao()
}

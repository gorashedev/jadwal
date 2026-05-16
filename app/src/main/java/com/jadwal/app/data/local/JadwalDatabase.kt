package com.jadwal.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jadwal.data.local.dao.ExamDao
import com.jadwal.data.local.dao.ScheduleDao
import com.jadwal.data.local.dao.SessionDao
import com.jadwal.data.local.dao.SubjectDao
import com.jadwal.data.local.entity.ExamEntity
import com.jadwal.data.local.entity.ScheduleItemEntity
import com.jadwal.data.local.entity.SessionEntity
import com.jadwal.data.local.entity.SubjectEntity

@Database(
    entities = [
        SubjectEntity::class,
        ExamEntity::class,
        ScheduleItemEntity::class,
        SessionEntity::class,
    ],
    version = 1,
    exportSchema = true,    // يولّد JSON schema في app/schemas/ للـ Migration
)
@TypeConverters(Converters::class)
abstract class JadwalDatabase : RoomDatabase() {

    abstract fun subjectDao(): SubjectDao
    abstract fun examDao(): ExamDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun sessionDao(): SessionDao

    companion object {
        const val DATABASE_NAME = "jadwal_db"
    }
}

package com.jadwal.app.di

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import com.jadwal.BuildConfig
import com.jadwal.data.preferences.UserPreferencesDataStore
import com.jadwal.data.repository.AIRepository
import com.jadwal.data.repository.AIRepositoryImpl
import com.jadwal.domain.algorithm.DefaultScheduleAlgorithm
import com.jadwal.domain.algorithm.ScheduleAlgorithm
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideUserPreferences(
        @ApplicationContext context: Context
    ): UserPreferencesDataStore = UserPreferencesDataStore(context)

    @Provides
    @Singleton
    fun provideGenerativeModel(): GenerativeModel =
        GenerativeModel(
            // gemini-1.5-flash — اسم النموذج الصحيح لـ SDK 0.9.0 (بدون -latest)
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
        )

    @Provides
    @Singleton
    fun provideAIRepository(generativeModel: GenerativeModel): AIRepository =
        AIRepositoryImpl(generativeModel)

    @Provides
    @Singleton
    fun provideScheduleAlgorithm(impl: DefaultScheduleAlgorithm): ScheduleAlgorithm = impl

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            install(Postgrest)
            install(Auth)
        }
    }
}

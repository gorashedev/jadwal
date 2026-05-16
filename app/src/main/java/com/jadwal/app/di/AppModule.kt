package com.jadwal.di

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import com.jadwal.BuildConfig
import com.jadwal.data.preferences.UserPreferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
            modelName = "gemini-1.5-flash",  // مجاني وسريع
            apiKey = BuildConfig.GEMINI_API_KEY,
        )
}

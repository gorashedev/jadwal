package com.jadwal.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jadwal.data.preferences.UserPreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: UserPreferencesDataStore,
) : ViewModel() {

    fun completeOnboarding() {
        viewModelScope.launch {
            prefs.setOnboardingDone(true)
        }
    }
}

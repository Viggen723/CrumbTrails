package featuresAPI.settings.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import featuresAPI.settings.data.UserProfile
import featuresAPI.settings.data.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SettingsStatus {
    data object Idle : SettingsStatus
    data object Loading : SettingsStatus
    data object Saved : SettingsStatus
    data class Error(val message: String) : SettingsStatus
}

class SettingsViewModel(
    private val repository: UserProfileRepository = UserProfileRepository()
) : ViewModel() {

    private val _profile = MutableStateFlow(UserProfile())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private val _status = MutableStateFlow<SettingsStatus>(SettingsStatus.Idle)
    val status: StateFlow<SettingsStatus> = _status.asStateFlow()

    val userEmail: String
        get() = repository.currentUserEmail

    val firebaseDisplayName: String
        get() = repository.currentFirebaseDisplayName

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _status.value = SettingsStatus.Loading
            val result = repository.getCurrentProfile()
            result.onSuccess { profile ->
                _profile.value = profile
                _status.value = SettingsStatus.Idle
            }.onFailure { exception ->
                _status.value = SettingsStatus.Error(exception.toSafeMessage())
            }
        }
    }

    fun saveProfile(username: String, displayName: String) {
        viewModelScope.launch {
            _status.value = SettingsStatus.Loading
            val result = repository.saveCurrentProfile(username, displayName)
            result.onSuccess {
                _profile.value = _profile.value.copy(
                    username = username.trim(),
                    displayName = displayName.trim(),
                    updatedAt = System.currentTimeMillis()
                )
                _status.value = SettingsStatus.Saved
            }.onFailure { exception ->
                _status.value = SettingsStatus.Error(exception.toSafeMessage())
            }
        }
    }

    private fun Throwable.toSafeMessage(): String {
        return "Profile update failed (${javaClass.simpleName})."
    }
}

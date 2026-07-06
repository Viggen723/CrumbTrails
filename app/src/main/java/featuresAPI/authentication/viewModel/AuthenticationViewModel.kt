package featuresAPI.authentication.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import featuresAPI.authentication.data.AuthenticationRepository
import featuresAPI.authentication.data.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthenticationViewModel(private val repository: AuthenticationRepository) : ViewModel() {

    private val _loginState = MutableStateFlow<Resource<String>>(Resource.Idle)
    val loginState: StateFlow<Resource<String>> = _loginState.asStateFlow()

    fun performLogin(emailAddress: String, securityText: String) {
        if (emailAddress.isBlank() || securityText.isBlank()) {
            _loginState.value = Resource.Error("Fields cannot be empty")
            return
        }

        viewModelScope.launch {
            repository.loginWithEmail(emailAddress, securityText).collect { result ->
                _loginState.value = result
            }
        }
    }

    fun performSignUp(emailAddress: String, securityText: String) {
        if (emailAddress.isBlank() || securityText.isBlank()) {
            _loginState.value = Resource.Error("Fields cannot be empty!")
            return
        }

        viewModelScope.launch {
            repository.signUpWithEmail(emailAddress, securityText).collect { result ->
                _loginState.value = result
            }
        }
    }

    fun logOut()
    {
        repository.logUserOut()
    }

    fun clearErrorState() {
        _loginState.value = Resource.Idle
    }
}
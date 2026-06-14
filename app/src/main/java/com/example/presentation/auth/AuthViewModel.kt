package com.example.presentation.auth

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.config.SessionManager
import com.example.data.models.User
import com.example.data.repository.TarikiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "AuthViewModel"

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Checks if the user exists with the entered phone number.
     * Direct login - NO OTP/SMS.
     */
    fun loginOrRegister(
        phone: String,
        userType: String,
        onExistingUser: (User) -> Unit,
        onNewUser: (User) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            TarikiRepository.loginOrRegister(phone, userType)
                .onSuccess { user ->
                    _isLoading.value = false
                    // Save session details globally in DataStore Preferences
                    SessionManager.saveUser(user)
                    
                    if (user.fullName.isNullOrBlank()) {
                        // User has no name yet - brand new account setup
                        onNewUser(user)
                    } else {
                        // Fully setup existing user
                        onExistingUser(user)
                    }
                }
                .onFailure { exception ->
                    _isLoading.value = false
                    val errorMsg = "حدث خطأ بالاتصال بالخادم. يرجى المحاولة لاحقاً."
                    _error.value = errorMsg
                    onError(errorMsg)
                    Log.e(TAG, "loginOrRegister failed", exception)
                }
        }
    }
}

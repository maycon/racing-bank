package com.hacknroll.racing_bank.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hacknroll.racing_bank.data.models.LoginResponse
import com.hacknroll.racing_bank.data.models.OnboardingResponse
import com.hacknroll.racing_bank.data.models.TokenResponse
import com.hacknroll.racing_bank.data.repository.BankRepository
import com.hacknroll.racing_bank.utils.Resource
import com.hacknroll.racing_bank.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val sessionManager = SessionManager(application)
    private val repository = BankRepository(sessionManager)
    
    // Login State
    private val _loginState = MutableStateFlow<Resource<LoginResponse>>(Resource.Idle())
    val loginState: StateFlow<Resource<LoginResponse>> = _loginState
    
    // Register State
    private val _registerState = MutableStateFlow<Resource<OnboardingResponse>>(Resource.Idle())
    val registerState: StateFlow<Resource<OnboardingResponse>> = _registerState
    
    // Two Factor State
    private val _twoFactorState = MutableStateFlow<Resource<TokenResponse>>(Resource.Idle())
    val twoFactorState: StateFlow<Resource<TokenResponse>> = _twoFactorState
    
    // Current user info
    private val _currentUsername = MutableStateFlow("")
    val currentUsername: StateFlow<String> = _currentUsername
    
    private val _totpSecret = MutableStateFlow("")
    val totpSecret: StateFlow<String> = _totpSecret
    
    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = Resource.Loading()
            _currentUsername.value = username
            repository.login(username, password).collectLatest { resource ->
                _loginState.value = resource
            }
        }
    }
    
    fun register(username: String, password: String) {
        viewModelScope.launch {
            _currentUsername.value = username
            repository.register(username, password).collectLatest { resource ->
                _registerState.value = resource
                if (resource is Resource.Success) {
                    _totpSecret.value = resource.data.totpSecret
                }
            }
        }
    }
    
    fun verifyTwoFactor(token: String) {
        viewModelScope.launch {
            repository.verifyTwoFactor(token).collectLatest { resource ->
                _twoFactorState.value = resource
            }
        }
    }
    
    fun logout() {
        repository.logout()
    }
    
    fun completeLogout() {
        repository.completeLogout()
    }
    
    fun resetStates() {
        _loginState.value = Resource.Loading()
        _registerState.value = Resource.Loading()
        _twoFactorState.value = Resource.Loading()
    }
}

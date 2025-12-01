package com.hacknroll.racing_bank.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hacknroll.racing_bank.data.models.*
import com.hacknroll.racing_bank.data.repository.BankRepository
import com.hacknroll.racing_bank.utils.Resource
import com.hacknroll.racing_bank.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val sessionManager = SessionManager(application)
    private val repository = BankRepository(sessionManager)
    
    // Balance State
    private val _balanceState = MutableStateFlow<Resource<BalanceResponse>>(Resource.Idle())
    val balanceState: StateFlow<Resource<BalanceResponse>> = _balanceState
    
    // Deposit State
    private val _depositState = MutableStateFlow<Resource<GenericResponse>>(Resource.Idle())
    val depositState: StateFlow<Resource<GenericResponse>> = _depositState
    
    // Withdrawal State
    private val _withdrawalState = MutableStateFlow<Resource<GenericResponse>>(Resource.Idle())
    val withdrawalState: StateFlow<Resource<GenericResponse>> = _withdrawalState
    
    // Transfer State
    private val _transferState = MutableStateFlow<Resource<TransferResponse>>(Resource.Idle())
    val transferState: StateFlow<Resource<TransferResponse>> = _transferState
    
    // Statement State
    private val _statementState = MutableStateFlow<Resource<StatementResponse>>(Resource.Idle())
    val statementState: StateFlow<Resource<StatementResponse>> = _statementState
    
    // Fund Subscription State
    private val _subscriptionState = MutableStateFlow<Resource<FundOperationResponse>>(Resource.Idle())
    val subscriptionState: StateFlow<Resource<FundOperationResponse>> = _subscriptionState
    
    // Fund Redemption State
    private val _redemptionState = MutableStateFlow<Resource<FundOperationResponse>>(Resource.Idle())
    val redemptionState: StateFlow<Resource<FundOperationResponse>> = _redemptionState
    
    // Fund Info State
    private val _fundInfoState = MutableStateFlow<Resource<FundInfoResponse>>(Resource.Idle())
    val fundInfoState: StateFlow<Resource<FundInfoResponse>> = _fundInfoState
    
    init {
        refreshBalance()
    }
    
    fun refreshBalance() {
        viewModelScope.launch {
            repository.getBalance().collectLatest { resource ->
                _balanceState.value = resource
            }
        }
    }
    
    fun deposit(amount: Double) {
        viewModelScope.launch {
            repository.deposit(amount).collectLatest { resource ->
                _depositState.value = resource
                if (resource is Resource.Success) {
                    refreshBalance()
                }
            }
        }
    }
    
    fun withdraw(amount: Double) {
        viewModelScope.launch {
            repository.withdraw(amount).collectLatest { resource ->
                _withdrawalState.value = resource
                if (resource is Resource.Success) {
                    refreshBalance()
                }
            }
        }
    }
    
    fun transfer(toUsername: String, amount: Double) {
        viewModelScope.launch {
            repository.transfer(toUsername, amount).collectLatest { resource ->
                _transferState.value = resource
                if (resource is Resource.Success) {
                    refreshBalance()
                }
            }
        }
    }
    
    fun loadStatement() {
        viewModelScope.launch {
            repository.getStatement().collectLatest { resource ->
                _statementState.value = resource
            }
        }
    }
    
    fun subscribeToFund(amount: Double) {
        viewModelScope.launch {
            repository.subscribeToFund(amount).collectLatest { resource ->
                _subscriptionState.value = resource
                if (resource is Resource.Success) {
                    refreshBalance()
                    loadFundInfo()
                }
            }
        }
    }
    
    fun redeemFromFund(amount: Double) {
        viewModelScope.launch {
            repository.redeemFromFund(amount).collectLatest { resource ->
                _redemptionState.value = resource
                if (resource is Resource.Success) {
                    refreshBalance()
                    loadFundInfo()
                }
            }
        }
    }
    
    fun loadFundInfo() {
        viewModelScope.launch {
            repository.getFundInfo().collectLatest { resource ->
                _fundInfoState.value = resource
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
        _depositState.value = Resource.Loading()
        _withdrawalState.value = Resource.Loading()
        _transferState.value = Resource.Loading()
        _subscriptionState.value = Resource.Loading()
        _redemptionState.value = Resource.Loading()
    }
}

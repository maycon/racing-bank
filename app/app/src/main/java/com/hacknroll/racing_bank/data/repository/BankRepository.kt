package com.hacknroll.racing_bank.data.repository

import android.util.Log
import com.hacknroll.racing_bank.data.api.BankApiService
import com.hacknroll.racing_bank.data.api.RetrofitClient
import com.hacknroll.racing_bank.data.models.*
import com.hacknroll.racing_bank.utils.Resource
import com.hacknroll.racing_bank.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.Response

class BankRepository(
    private val sessionManager: SessionManager
) {
    private val apiService: BankApiService = RetrofitClient.apiService

    private fun getAuthHeader(): String {
        return "Bearer ${sessionManager.getAuthToken() ?: ""}"
    }

    private fun getTempAuthHeader(): String {
        return "Bearer ${sessionManager.getTempToken() ?: ""}"
    }

    // Helper function to handle API responses
    private suspend fun <T> handleApiCall(
        apiCall: suspend () -> Response<T>
    ): Resource<T> {
        return try {
            val response = apiCall()
            if (response.isSuccessful) {
                response.body()?.let {
                    Resource.Success(it)
                } ?: Resource.Error("Empty response body")
            } else {
                if (response.code() == 401) {
                    sessionManager.clearSession()
                    SessionManager.triggerLogout()
                }
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                val errorMessage = parseErrorMessage(errorBody)
                Resource.Error(errorMessage, response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error occurred")
        }
    }

    private fun parseErrorMessage(errorBody: String): String {
        return try {
            val jsonObject = org.json.JSONObject(errorBody)
            jsonObject.optString("detail", errorBody)
        } catch (e: Exception) {
            errorBody
        }
    }

    // Authentication Functions
    fun register(username: String, password: String): Flow<Resource<OnboardingResponse>> = flow {
        emit(Resource.Loading())
        val request = OnboardingRequest(username, password)
        val result = handleApiCall { apiService.onboarding(request) }

        if (result is Resource.Success) {
            sessionManager.saveUserInfo(username, result.data.totpSecret)
        }
        emit(result)
        emit(Resource.Idle())
    }.flowOn(Dispatchers.IO)

    fun login(username: String, password: String): Flow<Resource<LoginResponse>> = flow {
        emit(Resource.Loading())
        val request = LoginRequest(username, password)
        val result = handleApiCall { apiService.login(request) }

        if (result is Resource.Success) {
            Log.d("DEBUG", "Created At: ${result.data.user.createdAt}")
            sessionManager.saveUserInfo(result.data.user.username, result.data.user.createdAt)
            sessionManager.saveTempToken(result.data.token)
        }
        emit(result)
        emit(Resource.Idle())
    }.flowOn(Dispatchers.IO)

    fun verifyTwoFactor(token: String): Flow<Resource<TokenResponse>> = flow {
        emit(Resource.Loading())
        val request = TwoFactorRequest(token)
        val tempToken = getTempAuthHeader()
        val result = handleApiCall { apiService.twoFactorAuth(request, tempToken) }

        if (result is Resource.Success) {
            sessionManager.saveAuthToken(result.data.accessToken)
            sessionManager.clearTempToken()
            sessionManager.setLoggedIn(true)
        }
        emit(result)
        emit(Resource.Idle())
    }.flowOn(Dispatchers.IO)

    // Account Management Functions
    fun getBalance(): Flow<Resource<BalanceResponse>> = flow {
        emit(Resource.Loading())
        val result = handleApiCall { apiService.getBalance(getAuthHeader()) }
        emit(result)
    }.flowOn(Dispatchers.IO)

    fun deposit(amount: Double): Flow<Resource<GenericResponse>> = flow {
        emit(Resource.Loading())
        val request = DepositRequest(amount)
        val result = handleApiCall { apiService.deposit(request, getAuthHeader()) }
        emit(result)
        emit(Resource.Idle())
    }.flowOn(Dispatchers.IO)

    fun withdraw(amount: Double): Flow<Resource<GenericResponse>> = flow {
        emit(Resource.Loading())
        val request = WithdrawalRequest(amount)
        val result = handleApiCall { apiService.withdrawal(request, getAuthHeader()) }
        emit(result)
        emit(Resource.Idle())
    }.flowOn(Dispatchers.IO)

    // Transfer Functions
    fun transfer(toUsername: String, amount: Double): Flow<Resource<TransferResponse>> = flow {
        emit(Resource.Loading())
        val request = TransferRequest(toUsername, amount)
        val result = handleApiCall { apiService.transfer(request, getAuthHeader()) }
        emit(result)
        emit(Resource.Idle())
    }.flowOn(Dispatchers.IO)

    fun getStatement(): Flow<Resource<StatementResponse>> = flow {
        emit(Resource.Loading())
        val result = handleApiCall { apiService.getStatement(getAuthHeader()) }
        emit(result)
        emit(Resource.Idle())
    }.flowOn(Dispatchers.IO)

    // Investment Fund Functions
    fun subscribeToFund(amount: Double): Flow<Resource<FundOperationResponse>> = flow {
        emit(Resource.Loading())
        val request = SubscriptionRequest(amount)
        val result = handleApiCall { apiService.subscribeToFund(request, getAuthHeader()) }
        emit(result)
        emit(Resource.Idle())
    }.flowOn(Dispatchers.IO)

    fun redeemFromFund(amount: Double): Flow<Resource<FundOperationResponse>> = flow {
        emit(Resource.Loading())
        val request = RedemptionRequest(amount)
        val result = handleApiCall { apiService.redeemFromFund(request, getAuthHeader()) }
        emit(result)
        emit(Resource.Idle())
    }.flowOn(Dispatchers.IO)

    fun getFundInfo(): Flow<Resource<FundInfoResponse>> = flow {
        emit(Resource.Loading())
        val result = handleApiCall { apiService.getFundInfo() }
        emit(result)
        emit(Resource.Idle())
    }.flowOn(Dispatchers.IO)

    // Utility Functions
    fun healthCheck(): Flow<Resource<Map<String, Any>>> = flow {
        emit(Resource.Loading())
        val result = handleApiCall { apiService.healthCheck() }
        emit(result)
        emit(Resource.Idle())
    }.flowOn(Dispatchers.IO)

    fun logout() {
        sessionManager.clearSession()
    }

    fun completeLogout() {
        sessionManager.clearAll()
    }
}
package com.hacknroll.racing_bank.data.api

import com.hacknroll.racing_bank.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface BankApiService {
    
    // Authentication Endpoints
    @POST("onboarding")
    suspend fun onboarding(
        @Body request: OnboardingRequest
    ): Response<OnboardingResponse>
    
    @POST("login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>
    
    @POST("2fa")
    suspend fun twoFactorAuth(
        @Body request: TwoFactorRequest,
        @Header("Authorization") token: String
    ): Response<TokenResponse>
    
    // Account Management Endpoints
    @POST("deposit")
    suspend fun deposit(
        @Body request: DepositRequest,
        @Header("Authorization") token: String
    ): Response<GenericResponse>
    
    @POST("withdrawal")
    suspend fun withdrawal(
        @Body request: WithdrawalRequest,
        @Header("Authorization") token: String
    ): Response<GenericResponse>
    
    @GET("balance")
    suspend fun getBalance(
        @Header("Authorization") token: String
    ): Response<BalanceResponse>
    
    // Transfer Endpoints
    @POST("transfer")
    suspend fun transfer(
        @Body request: TransferRequest,
        @Header("Authorization") token: String
    ): Response<TransferResponse>
    
    @GET("statement")
    suspend fun getStatement(
        @Header("Authorization") token: String
    ): Response<StatementResponse>
    
    // Investment Fund Endpoints
    @POST("subscribe")
    suspend fun subscribeToFund(
        @Body request: SubscriptionRequest,
        @Header("Authorization") token: String
    ): Response<FundOperationResponse>
    
    @POST("redemption")
    suspend fun redeemFromFund(
        @Body request: RedemptionRequest,
        @Header("Authorization") token: String
    ): Response<FundOperationResponse>
    
    @GET("fund/info")
    suspend fun getFundInfo(): Response<FundInfoResponse>
    
    // Utility Endpoints
    @GET("/")
    suspend fun getApiInfo(): Response<Map<String, Any>>
    
    @GET("health")
    suspend fun healthCheck(): Response<Map<String, Any>>
}

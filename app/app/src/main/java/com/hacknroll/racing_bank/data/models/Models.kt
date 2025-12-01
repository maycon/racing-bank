package com.hacknroll.racing_bank.data.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

// Authentication Models
@Parcelize
data class OnboardingRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
) : Parcelable

@Parcelize
data class OnboardingResponse(
    @SerializedName("username") val username: String,
    @SerializedName("totp_secret") val totpSecret: String,
    @SerializedName("totp_uri") val totpUri: String,
    @SerializedName("message") val message: String
) : Parcelable

@Parcelize
data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
) : Parcelable

@Parcelize
data class LoginUserResponse(
    @SerializedName("username") val username: String,
    @SerializedName("created_at") var createdAt: String
) : Parcelable

@Parcelize
data class LoginResponse(
    @SerializedName("message") val message: String,
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: LoginUserResponse
) : Parcelable

@Parcelize
data class TwoFactorRequest(
    @SerializedName("token") val token: String
) : Parcelable

@Parcelize
data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String = "bearer"
) : Parcelable

// Account Management Models
@Parcelize
data class DepositRequest(
    @SerializedName("amount") val amount: Double
) : Parcelable

@Parcelize
data class WithdrawalRequest(
    @SerializedName("amount") val amount: Double
) : Parcelable

@Parcelize
data class BalanceResponse(
    @SerializedName("username") val username: String,
    @SerializedName("cash_balance") val cashBalance: Double,
    @SerializedName("fund_shares") val fundShares: Double,
    @SerializedName("fund_value") val fundValue: Double,
    @SerializedName("total_portfolio") val totalPortfolio: Double
) : Parcelable

// Transfer Models
@Parcelize
data class TransferRequest(
    @SerializedName("to_username") val toUsername: String,
    @SerializedName("amount") val amount: Double
) : Parcelable

@Parcelize
data class TransferResponse(
    @SerializedName("status") val status: String,
    @SerializedName("transfer_id") val transferId: Int,
    @SerializedName("from_username") val fromUsername: String,
    @SerializedName("to_username") val toUsername: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("new_balance") val newBalance: Double
) : Parcelable

// Statement Models
@Parcelize
data class StatementResponse(
    @SerializedName("username") val username: String,
    @SerializedName("cash_balance") val cashBalance: Double,
    @SerializedName("fund_shares") val fundShares: Double,
    @SerializedName("fund_value") val fundValue: Double,
    @SerializedName("transactions") val transactions: List<TransactionItem>,
    @SerializedName("transfers") val transfers: List<TransferItem>
) : Parcelable

@Parcelize
data class TransactionItem(
    @SerializedName("id") val id: Int,
    @SerializedName("type") val type: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("description") val description: String?,
    @SerializedName("created_at") val createdAt: String
) : Parcelable

@Parcelize
data class TransferItem(
    @SerializedName("id") val id: Int,
    @SerializedName("from_username") val fromUsername: String,
    @SerializedName("to_username") val toUsername: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("created_at") val createdAt: String
) : Parcelable

// Investment Fund Models
@Parcelize
data class SubscriptionRequest(
    @SerializedName("amount") val amount: Double
) : Parcelable

@Parcelize
data class RedemptionRequest(
    @SerializedName("amount") val amount: Double
) : Parcelable

@Parcelize
data class FundOperationResponse(
    @SerializedName("status") val status: String,
    @SerializedName("operation") val operation: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("shares") val shares: Double,
    @SerializedName("share_price") val sharePrice: Double,
    @SerializedName("new_cash_balance") val newCashBalance: Double,
    @SerializedName("new_fund_shares") val newFundShares: Double
) : Parcelable

@Parcelize
data class FundInfoResponse(
    @SerializedName("total_value") val totalValue: Double,
    @SerializedName("total_shares") val totalShares: Double,
    @SerializedName("share_price") val sharePrice: Double,
    @SerializedName("updated_at") val updatedAt: String
) : Parcelable

// Generic Response
@Parcelize
data class GenericResponse(
    @SerializedName("message") val message: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("new_balance") val newBalance: Double? = null
) : Parcelable

// Error Response
@Parcelize
data class ErrorResponse(
    @SerializedName("detail") val detail: String
) : Parcelable

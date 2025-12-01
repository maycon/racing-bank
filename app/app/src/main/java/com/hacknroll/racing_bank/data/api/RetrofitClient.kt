package com.hacknroll.racing_bank.data.api

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.hacknroll.racing_bank.utils.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "https://bankao-api.hacknroll.academy/"

    private var sessionManager: SessionManager? = null

    fun initialize(context: Context) {
        sessionManager = SessionManager(context)
    }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()

        // Add auth token if available and not already present
        if (!original.header("Authorization").isNullOrEmpty()) {
            // Token already added manually, skip
        } else {
            sessionManager?.getAuthToken()?.let { token ->
                requestBuilder.header("Authorization", "Bearer $token")
            }
        }

        requestBuilder.header("Content-Type", "application/json")
        requestBuilder.header("Accept", "application/json")

        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val gson: Gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        .create()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val apiService: BankApiService = retrofit.create(BankApiService::class.java)

    // Dynamic base URL update
    fun updateBaseUrl(newUrl: String): BankApiService {
        val newRetrofit = Retrofit.Builder()
            .baseUrl(newUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        return newRetrofit.create(BankApiService::class.java)
    }
}

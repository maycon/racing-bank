package com.hacknroll.racing_bank.utils

sealed class Resource<T> {
    class Idle<T> : Resource<T>()
    class Loading<T> : Resource<T>()
    data class Success<T>(val data: T) : Resource<T>()
    data class Error<T>(val message: String, val code: Int? = null) : Resource<T>()
    
    fun isIdle(): Boolean = this is Idle
    fun isLoading(): Boolean = this is Loading
    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error
    
    fun getDataOrNull(): T? {
        return if (this is Success) data else null
    }
    
    fun getErrorOrNull(): String? {
        return if (this is Error) message else null
    }
}

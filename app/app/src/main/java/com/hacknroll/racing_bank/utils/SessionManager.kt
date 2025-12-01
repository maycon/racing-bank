package com.hacknroll.racing_bank.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey


class SessionManager(context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()


    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "com.hacknroll.racing_bank-secure",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    companion object {
        const val KEY_AUTH_TOKEN = "auth_token"
        const val KEY_TEMP_TOKEN = "temp_token"
        const val KEY_USERNAME = "username"
        const val KEY_CREATED_AT = "created_at"
        const val KEY_TOTP_SECRET = "totp_secret"
        const val KEY_IS_LOGGED_IN = "is_logged_in"
        const val KEY_LAST_LOGIN = "last_login"
        const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_SOUND_ENABLED = "sound_enabled"

        val logoutEvent = MutableLiveData<Boolean>()

        fun triggerLogout() {
            logoutEvent.postValue(true)
        }
    }
    
    // Authentication Token Management
    fun saveAuthToken(token: String) {
        sharedPreferences.edit()
            .putString(KEY_AUTH_TOKEN, token)
            .putLong(KEY_LAST_LOGIN, System.currentTimeMillis())
            .apply()
    }
    
    fun getAuthToken(): String? {
        return sharedPreferences.getString(KEY_AUTH_TOKEN, null)
    }
    
    fun saveTempToken(token: String) {
        sharedPreferences.edit()
            .putString(KEY_TEMP_TOKEN, token)
            .apply()
    }
    
    fun getTempToken(): String? {
        return sharedPreferences.getString(KEY_TEMP_TOKEN, null)
    }
    
    fun clearTempToken() {
        sharedPreferences.edit()
            .remove(KEY_TEMP_TOKEN)
            .apply()
    }
    
    // User Information Management
    fun saveUserInfo(username: String, createdAt: String, totpSecret: String? = null) {
        val editor = sharedPreferences.edit()
        editor.putString(KEY_USERNAME, username)
        editor.putString(KEY_CREATED_AT, createdAt)
        totpSecret?.let {
            editor.putString(KEY_TOTP_SECRET, it)
        }
        editor.apply()
    }

    fun getUsername(): String? {
        return sharedPreferences.getString(KEY_USERNAME, null)
    }

    fun getCreatedAt(): String? {
        return sharedPreferences.getString(KEY_CREATED_AT, null)
    }
    
    fun getTotpSecret(): String? {
        return sharedPreferences.getString(KEY_TOTP_SECRET, null)
    }
    
    // Session Management
    fun setLoggedIn(isLoggedIn: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
            .apply()
    }
    
    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false) && 
               getAuthToken() != null
    }
    
    fun getLastLogin(): Long {
        return sharedPreferences.getLong(KEY_LAST_LOGIN, 0)
    }
    
    fun isSessionExpired(): Boolean {
        val lastLogin = getLastLogin()
        val currentTime = System.currentTimeMillis()
        val sessionTimeout = 24 * 60 * 60 * 1000L // 24 hours
        return (currentTime - lastLogin) > sessionTimeout
    }
    
    // Settings Management
    fun setBiometricEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
            .apply()
    }
    
    fun isBiometricEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }
    
    fun setThemeMode(mode: String) {
        sharedPreferences.edit()
            .putString(KEY_THEME_MODE, mode)
            .apply()
    }
    
    fun getThemeMode(): String {
        return sharedPreferences.getString(KEY_THEME_MODE, "retro") ?: "retro"
    }
    
    fun setSoundEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_SOUND_ENABLED, enabled)
            .apply()
    }
    
    fun isSoundEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_SOUND_ENABLED, true)
    }
    
    // Clear Session
    fun clearSession() {
        sharedPreferences.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_TEMP_TOKEN)
            .remove(KEY_USERNAME)
            .remove(KEY_IS_LOGGED_IN)
            .remove(KEY_LAST_LOGIN)
            // Keep TOTP secret for re-login
            // Keep settings
            .apply()
    }
    
    // Complete Clear (Logout completely)
    fun clearAll() {
        val soundEnabled = isSoundEnabled()
        val themeMode = getThemeMode()
        
        sharedPreferences.edit()
            .clear()
            .putBoolean(KEY_SOUND_ENABLED, soundEnabled)
            .putString(KEY_THEME_MODE, themeMode)
            .apply()
    }
}

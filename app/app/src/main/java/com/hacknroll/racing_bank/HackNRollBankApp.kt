package com.hacknroll.racing_bank

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.hacknroll.racing_bank.data.api.RetrofitClient
import com.hacknroll.racing_bank.utils.SessionManager
import com.hacknroll.racing_bank.utils.SoundManager

class HackNRollBankApp : Application() {

    companion object {
        lateinit var instance: HackNRollBankApp
            private set

        fun getContext(): Context = instance.applicationContext
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Retrofit
        RetrofitClient.initialize(this)

        // Initialize Sound Manager
        SoundManager.initialize(this)

        // Set theme based on saved preference
        val sessionManager = SessionManager(this)
        when (sessionManager.getThemeMode()) {
            "retro" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        setupGlobalSettings()
    }


    private fun setupGlobalSettings() {
        // Configurar modo noturno padrão
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }
}

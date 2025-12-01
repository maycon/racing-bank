package com.hacknroll.racing_bank.ui.auth

interface AuthNavigator {
    fun navigateToLogin()
    fun navigateToRegister()
    fun navigateToTwoFactor(username: String)
    fun navigateToMain()
    fun showQRCode(totpUri: String)
}
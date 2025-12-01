package com.hacknroll.racing_bank.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hacknroll.racing_bank.R
import com.hacknroll.racing_bank.ui.auth.AuthActivity
import com.hacknroll.racing_bank.ui.main.fragments.*
import com.hacknroll.racing_bank.utils.SessionManager
import com.hacknroll.racing_bank.utils.SoundManager

class MainActivity : AppCompatActivity() {
    
    private lateinit var viewModel: MainViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var bottomNav: BottomNavigationView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        sessionManager = SessionManager(this)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        SessionManager.logoutEvent.observe(this) { shouldLogout ->
            if (shouldLogout) {
                SessionManager.logoutEvent.value = false // Reset
                performLogout()
            }
        }
        
        setupToolbar()
        setupBottomNavigation()
        
        // Load initial fragment
        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
        }
        
        // Load initial data
        viewModel.refreshBalance()
        viewModel.loadFundInfo()
        
        // Apply retro styling
        window.statusBarColor = getColor(R.color.retro_dark_purple)
    }
    
    private fun setupToolbar() {
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            title = "HACK N ROLL BANK"
            setDisplayShowHomeEnabled(false)
        }
    }
    
    private fun setupBottomNavigation() {
        bottomNav = findViewById(R.id.bottomNavigation)
        
        bottomNav.setOnItemSelectedListener { item ->
            SoundManager.playSound(SoundManager.SoundType.CLICK)
            
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    loadFragment(DashboardFragment())
                    true
                }
                R.id.nav_transfer -> {
                    loadFragment(TransferFragment())
                    true
                }
                R.id.nav_invest -> {
                    loadFragment(InvestmentFragment())
                    true
                }
                R.id.nav_statement -> {
                    loadFragment(StatementFragment())
                    true
                }
                R.id.nav_settings -> {
                    loadFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }
        
        // Apply retro colors
        bottomNav.setBackgroundColor(getColor(R.color.retro_dark))
        bottomNav.itemIconTintList = getColorStateList(R.color.bottom_nav_color)
        bottomNav.itemTextColor = getColorStateList(R.color.bottom_nav_color)
    }
    
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.fade_out
            )
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                SoundManager.playSound(SoundManager.SoundType.CLICK)
                viewModel.refreshBalance()
                true
            }
            R.id.action_logout -> {
                showLogoutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(this, R.style.RetroAlertDialog)
            .setTitle("LOGOUT")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("YES") { _, _ ->
                performLogout()
            }
            .setNegativeButton("NO", null)
            .show()
    }
    
    private fun performLogout() {
        SoundManager.playSound(SoundManager.SoundType.LOGOUT)
        viewModel.logout()
        
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        finish()
    }
    
    override fun onResume() {
        super.onResume()
        // Check session expiration
        if (!sessionManager.isLoggedIn() || sessionManager.isSessionExpired()) {
            performLogout()
        }
    }
    
    private fun showSessionExpiredDialog() {
        MaterialAlertDialogBuilder(this, R.style.RetroAlertDialog)
            .setTitle("SESSION EXPIRED")
            .setMessage("Your session has expired. Please login again.")
            .setPositiveButton("OK") { _, _ ->
                performLogout()
            }
            .setCancelable(false)
            .show()
    }
}

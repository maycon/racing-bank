package com.hacknroll.racing_bank.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.hacknroll.racing_bank.R
import com.hacknroll.racing_bank.ui.main.MainActivity
import com.hacknroll.racing_bank.utils.SessionManager
import com.hacknroll.racing_bank.utils.SoundManager

class AuthActivity : AppCompatActivity(), AuthNavigator {

    private lateinit var viewPager: ViewPager
    private lateinit var tabLayout: TabLayout
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        sessionManager = SessionManager(this)

        setupViewPager()
        setupTabLayout()

        // Apply retro styling
        window.statusBarColor = getColor(R.color.retro_dark_purple)
    }

    private fun setupViewPager() {
        viewPager = findViewById(R.id.viewPager)
        val adapter = AuthPagerAdapter(supportFragmentManager)
        viewPager.adapter = adapter

        // Disable swipe
        viewPager.setOnTouchListener { _, _ -> true }
    }

    private fun setupTabLayout() {
        tabLayout = findViewById(R.id.tabLayout)
        tabLayout.setupWithViewPager(viewPager)

        // Style tabs
        tabLayout.setSelectedTabIndicatorColor(getColor(R.color.retro_cyan))
        tabLayout.setTabTextColors(
            getColor(R.color.retro_pink),
            getColor(R.color.retro_cyan)
        )

        // Add click sound
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                SoundManager.playSound(SoundManager.SoundType.CLICK)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    override fun navigateToLogin() {
        // Restore tabs visibility
        tabLayout.visibility = android.view.View.VISIBLE
        viewPager.visibility = android.view.View.VISIBLE
        findViewById<android.view.View>(R.id.authContainer).visibility = android.view.View.GONE

        viewPager.currentItem = 0
    }

    override fun navigateToRegister() {
        viewPager.currentItem = 1
    }

    override fun navigateToTwoFactor(username: String) {
        val fragment = TwoFactorFragment.newInstance(username)
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(R.id.authContainer, fragment)
            .addToBackStack("twoFactor")
            .commit()

        // Hide tabs during 2FA
        tabLayout.visibility = android.view.View.GONE
        viewPager.visibility = android.view.View.GONE
        findViewById<android.view.View>(R.id.authContainer).visibility = android.view.View.VISIBLE
    }

    override fun navigateToMain() {
        SoundManager.playSound(SoundManager.SoundType.SUCCESS)
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        finish()
    }

    override fun showQRCode(totpUri: String) {
        val intent = Intent(this, QRScannerActivity::class.java)
        intent.putExtra("totp_uri", totpUri)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            // Show tabs again if returning from 2FA
            tabLayout.visibility = android.view.View.VISIBLE
            viewPager.visibility = android.view.View.VISIBLE
            findViewById<android.view.View>(R.id.authContainer).visibility = android.view.View.GONE
        } else {
            super.onBackPressed()
        }
    }

    inner class AuthPagerAdapter(fm: FragmentManager) :
        FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getCount(): Int = 2

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> LoginFragment()
                1 -> RegisterFragment()
                else -> LoginFragment()
            }
        }

        override fun getPageTitle(position: Int): CharSequence {
            return when (position) {
                0 -> "LOGIN"
                1 -> "REGISTER"
                else -> ""
            }
        }
    }
}
package com.hacknroll.racing_bank.ui.main.fragments

//import android.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.hacknroll.racing_bank.R
import com.hacknroll.racing_bank.ui.auth.AuthActivity
import com.hacknroll.racing_bank.ui.main.MainViewModel
import com.hacknroll.racing_bank.utils.SessionManager
import com.hacknroll.racing_bank.utils.SoundManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class SettingsFragment : Fragment() {

    private lateinit var viewModel: MainViewModel
    private lateinit var sessionManager: SessionManager

    // Profile views
    private lateinit var usernameText: TextView
    private lateinit var accountIdText: TextView
    private lateinit var memberSinceText: TextView

    // Security views
    private lateinit var changePasswordLayout: LinearLayout
    private lateinit var reset2FALayout: LinearLayout
    private lateinit var biometricLayout: LinearLayout
    private lateinit var biometricSwitch: SwitchCompat

    // Preferences views
    private lateinit var soundSwitch: SwitchCompat
    private lateinit var vibrationSwitch: SwitchCompat
    private lateinit var notificationsSwitch: SwitchCompat

    // Logout button
    private lateinit var logoutButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        sessionManager = SessionManager(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        loadUserInfo()
        setupListeners()
        loadPreferences()
    }

    private fun initViews(view: View) {
        // Profile
        usernameText = view.findViewById(R.id.usernameText)
        accountIdText = view.findViewById(R.id.accountIdText)
        memberSinceText = view.findViewById(R.id.memberSinceText)

        // Security
        changePasswordLayout = view.findViewById(R.id.changePasswordLayout)
        reset2FALayout = view.findViewById(R.id.reset2FALayout)
        biometricLayout = view.findViewById(R.id.biometricLayout)
        biometricSwitch = view.findViewById(R.id.biometricSwitch)

        // Preferences
        soundSwitch = view.findViewById(R.id.soundSwitch)
        vibrationSwitch = view.findViewById(R.id.vibrationSwitch)
        notificationsSwitch = view.findViewById(R.id.notificationsSwitch)

        // Logout
        logoutButton = view.findViewById(R.id.logoutButton)
    }

    fun getNaturalRelativeTime(createdAt: String): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            val stored = LocalDateTime.parse(createdAt, formatter)

            // interpret stored as UTC
            val instant = stored.atOffset(ZoneOffset.UTC).toInstant()

            val now = Instant.now()
            val seconds = Duration.between(instant, now).seconds

            when {
                seconds < 10 ->
                    "just now"

                seconds < 60 ->
                    "less than a minute ago"

                seconds < 120 ->
                    "a minute ago"

                seconds < 3600 -> {
                    val m = seconds / 60
                    "$m minutes ago"
                }

                seconds < 5400 ->
                    "about an hour ago"

                seconds < 86400 -> {
                    val h = seconds / 3600
                    "about $h hour${if (h == 1L) "" else "s"} ago"
                }

                seconds < 172800 ->
                    "about a day ago"

                seconds < 604800 -> {
                    val d = seconds / 86400
                    "about $d day${if (d == 1L) "" else "s"} ago"
                }

                seconds < 1209600 ->
                    "about a week ago"

                seconds < 2592000 -> {
                    val w = seconds / 604800
                    "about $w week${if (w == 1L) "" else "s"} ago"
                }

                seconds < 5184000 ->
                    "about a month ago"

                seconds < 31536000 -> {
                    val m = seconds / 2592000
                    "about $m month${if (m == 1L) "" else "s"} ago"
                }

                seconds < 63072000 ->
                    "about a year ago"

                else -> {
                    val y = seconds / 31536000
                    "about $y year${if (y == 1L) "" else "s"} ago"
                }
            }
        } catch (e: Exception) {
            "some time ago"
        }
    }



    private fun loadUserInfo() {
        val username = sessionManager.getUsername() ?: "User"
        val createdAt = getNaturalRelativeTime(sessionManager.getCreatedAt()!!)

        usernameText.text = username
        accountIdText.text = "Account ID: ${username.hashCode() and 0x7ffffff}"
        memberSinceText.text = "Member since: ${createdAt}"
    }

    private fun loadPreferences() {
        val prefs = requireContext().getSharedPreferences("app_prefs", 0)
        soundSwitch.isChecked = prefs.getBoolean("sound_enabled", true)
        vibrationSwitch.isChecked = prefs.getBoolean("vibration_enabled", true)
        notificationsSwitch.isChecked = prefs.getBoolean("notifications_enabled", true)
        biometricSwitch.isChecked = prefs.getBoolean("biometric_enabled", false)
    }

    private fun setupListeners() {
        // Security actions
        changePasswordLayout.setOnClickListener {
            SoundManager.playSound(SoundManager.SoundType.CLICK)
            showChangePasswordDialog()
        }

        reset2FALayout.setOnClickListener {
            SoundManager.playSound(SoundManager.SoundType.CLICK)
            showReset2FADialog()
        }

        biometricSwitch.setOnCheckedChangeListener { _, isChecked ->
            SoundManager.playSound(SoundManager.SoundType.CLICK)
            saveBiometricPreference(isChecked)
        }

        // Preference switches
        soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) SoundManager.playSound(SoundManager.SoundType.CLICK)
            saveSoundPreference(isChecked)
        }

        vibrationSwitch.setOnCheckedChangeListener { _, isChecked ->
            SoundManager.playSound(SoundManager.SoundType.CLICK)
            SoundManager.vibrate()
            saveVibrationPreference(isChecked)
        }

        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            SoundManager.playSound(SoundManager.SoundType.CLICK)
            saveNotificationPreference(isChecked)
        }

        // Logout
        logoutButton.setOnClickListener {
            SoundManager.playSound(SoundManager.SoundType.CLICK)
            showLogoutConfirmation()
        }
    }

    private fun showChangePasswordDialog() {
        MaterialAlertDialogBuilder(requireContext(), R.style.RetroAlertDialog)
            .setTitle("CHANGE PASSWORD")
            .setMessage("This feature will be available soon!")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showReset2FADialog() {
        MaterialAlertDialogBuilder(requireContext(), R.style.RetroAlertDialog)
            .setTitle("RESET 2FA")
            .setMessage("Are you sure you want to reset your 2FA?\nYou'll need to set it up again.")
            .setPositiveButton("RESET") { _, _ ->
                showMessage("2FA reset successful")
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(requireContext(), R.style.RetroAlertDialog)
            .setTitle("LOGOUT")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("LOGOUT") { _, _ ->
                performLogout()
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun performLogout() {
        SoundManager.playSound(SoundManager.SoundType.SUCCESS)
        sessionManager.clearSession()

        val intent = Intent(requireContext(), AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun saveBiometricPreference(enabled: Boolean) {
        val prefs = requireContext().getSharedPreferences("app_prefs", 0)
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
        showMessage(if (enabled) "Biometric authentication enabled" else "Biometric authentication disabled")
    }

    private fun saveSoundPreference(enabled: Boolean) {
        val prefs = requireContext().getSharedPreferences("app_prefs", 0)
        prefs.edit().putBoolean("sound_enabled", enabled).apply()
//        SoundManager.setSoundEnabled(enabled)
    }

    private fun saveVibrationPreference(enabled: Boolean) {
        val prefs = requireContext().getSharedPreferences("app_prefs", 0)
        prefs.edit().putBoolean("vibration_enabled", enabled).apply()
//        SoundManager.setVibrationEnabled(enabled)
    }

    private fun saveNotificationPreference(enabled: Boolean) {
        val prefs = requireContext().getSharedPreferences("app_prefs", 0)
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
        showMessage(if (enabled) "Notifications enabled" else "Notifications disabled")
    }

    private fun showMessage(message: String) {
        view?.let {
            val snackbar = Snackbar.make(it, message, Snackbar.LENGTH_SHORT)
            snackbar.setBackgroundTint(resources.getColor(R.color.retro_cyan, null))
            snackbar.setTextColor(resources.getColor(R.color.retro_white, null))
            snackbar.show()
        }
    }
}
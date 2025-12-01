package com.hacknroll.racing_bank.ui.auth

//import android.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.hacknroll.racing_bank.R
import com.hacknroll.racing_bank.utils.Resource
import com.hacknroll.racing_bank.utils.SoundManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private lateinit var viewModel: AuthViewModel
    private lateinit var navigator: AuthNavigator

    private lateinit var usernameInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var confirmPasswordInput: TextInputEditText
    private lateinit var usernameLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var confirmPasswordLayout: TextInputLayout
    private lateinit var registerButton: Button
    private lateinit var loginLink: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[AuthViewModel::class.java]
        navigator = requireActivity() as AuthNavigator
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupListeners()
        observeViewModel()
    }

    private fun initViews(view: View) {
        usernameInput = view.findViewById(R.id.usernameInput)
        passwordInput = view.findViewById(R.id.passwordInput)
        confirmPasswordInput = view.findViewById(R.id.confirmPasswordInput)
        usernameLayout = view.findViewById(R.id.usernameLayout)
        passwordLayout = view.findViewById(R.id.passwordLayout)
        confirmPasswordLayout = view.findViewById(R.id.confirmPasswordLayout)
        registerButton = view.findViewById(R.id.registerButton)
        loginLink = view.findViewById(R.id.loginLink)
        progressBar = view.findViewById(R.id.progressBar)

        // Apply retro styling
        applyRetroStyling()
    }

    private fun applyRetroStyling() {
        // Set retro colors
        registerButton.setBackgroundColor(resources.getColor(R.color.retro_pink, null))
        loginLink.setTextColor(resources.getColor(R.color.retro_cyan, null))

        // Add typing sound effect for inputs
        listOf(usernameInput, passwordInput, confirmPasswordInput).forEach { input ->
            input.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) SoundManager.playSound(SoundManager.SoundType.CLICK)
            }
        }
    }

    private fun setupListeners() {
        registerButton.setOnClickListener {
            SoundManager.playSound(SoundManager.SoundType.CLICK)
            SoundManager.vibrate()
            performRegistration()
        }

        loginLink.setOnClickListener {
            SoundManager.playSound(SoundManager.SoundType.CLICK)
            navigator.navigateToLogin()
        }
    }

    private fun performRegistration() {
        val username = usernameInput.text.toString().trim()
        val password = passwordInput.text.toString()
        val confirmPassword = confirmPasswordInput.text.toString()

        // Validate inputs
        if (!validateInputs(username, password, confirmPassword)) {
            return
        }

        // Clear previous errors
        usernameLayout.error = null
        passwordLayout.error = null
        confirmPasswordLayout.error = null

        // Perform registration
        viewModel.register(username, password)
    }

    private fun validateInputs(username: String, password: String, confirmPassword: String): Boolean {
        var isValid = true

        // Username validation
        if (username.isEmpty()) {
            usernameLayout.error = "Username required"
            SoundManager.playSound(SoundManager.SoundType.ERROR)
            isValid = false
        } else if (username.length < 3) {
            usernameLayout.error = "Username must be at least 3 characters"
            SoundManager.playSound(SoundManager.SoundType.ERROR)
            isValid = false
        } else if (!username.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            usernameLayout.error = "Username can only contain letters, numbers, and underscores"
            SoundManager.playSound(SoundManager.SoundType.ERROR)
            isValid = false
        }

        // Password validation
        if (password.isEmpty()) {
            passwordLayout.error = "Password required"
            SoundManager.playSound(SoundManager.SoundType.ERROR)
            isValid = false
        } else if (password.length < 6) {
            passwordLayout.error = "Password must be at least 6 characters"
            SoundManager.playSound(SoundManager.SoundType.ERROR)
            isValid = false
        }

        // Confirm password validation
        if (confirmPassword.isEmpty()) {
            confirmPasswordLayout.error = "Please confirm your password"
            SoundManager.playSound(SoundManager.SoundType.ERROR)
            isValid = false
        } else if (password != confirmPassword) {
            confirmPasswordLayout.error = "Passwords do not match"
            SoundManager.playSound(SoundManager.SoundType.ERROR)
            isValid = false
        }

        return isValid
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.registerState.collectLatest { resource ->
                when (resource) {
                    is Resource.Idle -> {
                        showLoading(false)
                    }
                    is Resource.Loading -> {
                        showLoading(true)
                    }

                    is Resource.Success -> {
                        showLoading(false)
                        SoundManager.playSound(SoundManager.SoundType.SUCCESS)
                        showSuccessDialog(resource.data.totpUri, resource.data.totpSecret)
                    }

                    is Resource.Error -> {
                        showLoading(false)
                        SoundManager.playSound(SoundManager.SoundType.ERROR)
                        SoundManager.vibrateError()
                        showError(resource.message)
                    }
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        registerButton.isEnabled = !show
        usernameInput.isEnabled = !show
        passwordInput.isEnabled = !show
        confirmPasswordInput.isEnabled = !show

        if (show) {
            registerButton.text = "CREATING ACCOUNT..."
        } else {
            registerButton.text = "REGISTER"
        }
    }

    private fun showError(message: String) {
        if (!isAdded) return
        view?.let {
            val snackbar = Snackbar.make(it, message, Snackbar.LENGTH_LONG)
            snackbar.setBackgroundTint(resources.getColor(R.color.retro_red, null))
            snackbar.setTextColor(resources.getColor(R.color.retro_white, null))
            snackbar.show()
        }
    }

    private fun showSuccessDialog(totpUri: String, totpSecret: String) {
        MaterialAlertDialogBuilder(requireContext(), R.style.RetroAlertDialog)
            .setTitle("✅ REGISTRATION SUCCESSFUL!")
            .setMessage(
                """
                Account created successfully!
                
                IMPORTANT: Set up 2FA to secure your account.
                
                1. Install Google Authenticator
                2. Scan the QR code or enter the secret key
                3. Use the 6-digit code to login
                
                Secret Key: $totpSecret
                """.trimIndent()
            )
            .setPositiveButton("SHOW QR CODE") { _, _ ->
                navigator.showQRCode(totpUri)
            }
            .setNegativeButton("CONTINUE TO LOGIN") { _, _ ->
                navigator.navigateToLogin()
            }
            .setCancelable(false)
            .show()
    }
}
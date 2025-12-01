package com.hacknroll.racing_bank.ui.auth

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

class LoginFragment : Fragment() {
    
    private lateinit var viewModel: AuthViewModel
    private lateinit var navigator: AuthNavigator
    
    private lateinit var usernameInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var usernameLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var loginButton: Button
    private lateinit var registerLink: TextView
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
        return inflater.inflate(R.layout.fragment_login, container, false)
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
        usernameLayout = view.findViewById(R.id.usernameLayout)
        passwordLayout = view.findViewById(R.id.passwordLayout)
        loginButton = view.findViewById(R.id.loginButton)
        registerLink = view.findViewById(R.id.registerLink)
        progressBar = view.findViewById(R.id.progressBar)
        
        // Apply retro styling
        applyRetroStyling()
    }
    
    private fun applyRetroStyling() {
        // Set retro colors
        loginButton.setBackgroundColor(resources.getColor(R.color.retro_cyan, null))
        registerLink.setTextColor(resources.getColor(R.color.retro_pink, null))
        
        // Add typing sound effect for inputs
        usernameInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) SoundManager.playSound(SoundManager.SoundType.CLICK)
        }
        
        passwordInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) SoundManager.playSound(SoundManager.SoundType.CLICK)
        }
    }
    
    private fun setupListeners() {
        loginButton.setOnClickListener {
            SoundManager.playSound(SoundManager.SoundType.CLICK)
            SoundManager.vibrate()
            performLogin()
        }
        
        registerLink.setOnClickListener {
            SoundManager.playSound(SoundManager.SoundType.CLICK)
            navigator.navigateToRegister()
        }
    }
    
    private fun performLogin() {
        val username = usernameInput.text.toString().trim()
        val password = passwordInput.text.toString()
        
        // Validate inputs
        if (!validateInputs(username, password)) {
            return
        }
        
        // Clear previous errors
        usernameLayout.error = null
        passwordLayout.error = null
        
        // Perform login
        viewModel.login(username, password)
    }
    
    private fun validateInputs(username: String, password: String): Boolean {
        var isValid = true
        
        if (username.isEmpty()) {
            usernameLayout.error = "Username required"
            SoundManager.playSound(SoundManager.SoundType.ERROR)
            isValid = false
        } else if (username.length < 3) {
            usernameLayout.error = "Username too short"
            SoundManager.playSound(SoundManager.SoundType.ERROR)
            isValid = false
        }
        
        if (password.isEmpty()) {
            passwordLayout.error = "Password required"
            SoundManager.playSound(SoundManager.SoundType.ERROR)
            isValid = false
        } else if (password.length < 6) {
            passwordLayout.error = "Password too short"
            SoundManager.playSound(SoundManager.SoundType.ERROR)
            isValid = false
        }
        
        return isValid
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loginState.collectLatest { resource ->
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
                        // Navigate to 2FA
                        val username = usernameInput.text.toString().trim()
                        navigator.navigateToTwoFactor(username)
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
        loginButton.isEnabled = !show
        usernameInput.isEnabled = !show
        passwordInput.isEnabled = !show
        
        if (show) {
            loginButton.text = "AUTHENTICATING..."
        } else {
            loginButton.text = "LOGIN"
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
}

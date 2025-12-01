package com.hacknroll.racing_bank.ui.auth

import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.hacknroll.racing_bank.R
import com.hacknroll.racing_bank.utils.Resource
import com.hacknroll.racing_bank.utils.SessionManager
import com.hacknroll.racing_bank.utils.SoundManager
import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordConfig
import dev.turingcomplete.kotlinonetimepassword.TimeBasedOneTimePasswordGenerator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import org.apache.commons.codec.binary.Base32

class TwoFactorFragment : Fragment() {
    
    companion object {
        private const val ARG_USERNAME = "username"
        
        fun newInstance(username: String): TwoFactorFragment {
            return TwoFactorFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USERNAME, username)
                }
            }
        }
    }
    
    private lateinit var viewModel: AuthViewModel
    private lateinit var navigator: AuthNavigator
    private lateinit var sessionManager: SessionManager
    
    private lateinit var titleText: TextView
    private lateinit var instructionText: TextView
    private lateinit var codeInputs: List<EditText>
    private lateinit var verifyButton: Button
    private lateinit var resendButton: Button
    private lateinit var timerText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var backToLoginLink: TextView

    private var countDownTimer: CountDownTimer? = null
    private var username: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[AuthViewModel::class.java]
        navigator = requireActivity() as AuthNavigator
        sessionManager = SessionManager(requireContext())
        username = arguments?.getString(ARG_USERNAME) ?: ""
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_two_factor, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupCodeInputs()
        setupListeners()
        observeViewModel()
        startTimer()
        
        // Show test code if in debug mode
        showTestCode()
    }
    
    private fun initViews(view: View) {
        titleText = view.findViewById(R.id.titleText)
        instructionText = view.findViewById(R.id.instructionText)
        
        codeInputs = listOf(
            view.findViewById(R.id.code1),
            view.findViewById(R.id.code2),
            view.findViewById(R.id.code3),
            view.findViewById(R.id.code4),
            view.findViewById(R.id.code5),
            view.findViewById(R.id.code6)
        )
        
        verifyButton = view.findViewById(R.id.verifyButton)
        resendButton = view.findViewById(R.id.resendButton)
        timerText = view.findViewById(R.id.timerText)
        progressBar = view.findViewById(R.id.progressBar)
        backToLoginLink = view.findViewById(R.id.backToLoginLink)
        
        // Set username in title
        titleText.text = "Welcome back, $username!"
        
        // Apply retro styling
        verifyButton.setBackgroundColor(resources.getColor(R.color.retro_green, null))
    }
    
    private fun setupCodeInputs() {
        codeInputs.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1) {
                        // Move to next input
                        if (index < codeInputs.size - 1) {
                            codeInputs[index + 1].requestFocus()
                        }
                        
                        // Check if all inputs are filled
                        if (codeInputs.all { it.text.isNotEmpty() }) {
                            verifyButton.isEnabled = true
                            // Auto-submit
                            performVerification()
                        }
                    } else if (s?.isEmpty() == true && index > 0) {
                        // Move to previous input on delete
                        codeInputs[index - 1].requestFocus()
                    }
                    
                    // Play typing sound
                    if (s?.isNotEmpty() == true) {
                        SoundManager.playSound(SoundManager.SoundType.CLICK)
                    }
                }
            })
        }
        
        // Focus first input
        codeInputs[0].requestFocus()
    }
    
    private fun setupListeners() {
        verifyButton.setOnClickListener {
            SoundManager.playSound(SoundManager.SoundType.CLICK)
            performVerification()
        }
        
        resendButton.setOnClickListener {
            SoundManager.playSound(SoundManager.SoundType.CLICK)
            // In a real app, this would resend the code
            showMessage("New code sent to your authenticator app")
            startTimer()
        }

        backToLoginLink.setOnClickListener {
            SoundManager.playSound(SoundManager.SoundType.CLICK)

            // Add listener to restore views after animation completes
            parentFragmentManager.addOnBackStackChangedListener(object : androidx.fragment.app.FragmentManager.OnBackStackChangedListener {
                override fun onBackStackChanged() {
                    if (parentFragmentManager.backStackEntryCount == 0) {
                        navigator.navigateToLogin()
                        parentFragmentManager.removeOnBackStackChangedListener(this)
                    }
                }
            })

            parentFragmentManager.popBackStack()
        }
    }
    
    private fun performVerification() {
        val code = codeInputs.joinToString("") { it.text.toString() }
        
        if (code.length != 6) {
            showError("Please enter all 6 digits")
            return
        }
        
        viewModel.verifyTwoFactor(code)
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.twoFactorState.collectLatest { resource ->
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
                        SoundManager.vibrateSuccess()
                        navigator.navigateToMain()
                    }
                    is Resource.Error -> {
                        showLoading(false)
                        SoundManager.playSound(SoundManager.SoundType.ERROR)
                        SoundManager.vibrateError()
                        showError(resource.message)
                        clearCodeInputs()
                    }
                }
            }
        }
    }
    
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        verifyButton.isEnabled = !show
        codeInputs.forEach { it.isEnabled = !show }
        
        if (show) {
            verifyButton.text = "VERIFYING..."
        } else {
            verifyButton.text = "VERIFY"
        }
    }
    
    private fun clearCodeInputs() {
        codeInputs.forEach { it.text.clear() }
        codeInputs[0].requestFocus()
    }
    
    private fun startTimer() {
        countDownTimer?.cancel()
        
        resendButton.isEnabled = false
        
        countDownTimer = object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                timerText.text = "Code expires in: ${seconds}s"
                timerText.setTextColor(
                    if (seconds <= 10) 
                        resources.getColor(R.color.retro_red, null)
                    else 
                        resources.getColor(R.color.retro_cyan, null)
                )
            }
            
            override fun onFinish() {
                timerText.text = "Code expired"
                timerText.setTextColor(resources.getColor(R.color.retro_red, null))
                resendButton.isEnabled = true
            }
        }.start()
    }
    
    private fun showTestCode() {
        // For testing: Generate and show TOTP code if we have the secret
        sessionManager.getTotpSecret()?.let { secret ->
            try {
                val config = TimeBasedOneTimePasswordConfig(
                    codeDigits = 6,
                    hmacAlgorithm = HmacAlgorithm.SHA1,
                    timeStep = 30,
                    timeStepUnit = TimeUnit.SECONDS
                )
                
                val base32 = Base32()
                val secretBytes = base32.decode(secret)
                val generator = TimeBasedOneTimePasswordGenerator(secretBytes, config)
                val code = generator.generate()
                
                instructionText.text = "Enter code from authenticator\n(Test code: $code)"
            } catch (e: Exception) {
                // Ignore if secret is invalid
            }
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
    
    private fun showMessage(message: String) {
        view?.let {
            val snackbar = Snackbar.make(it, message, Snackbar.LENGTH_SHORT)
            snackbar.setBackgroundTint(resources.getColor(R.color.retro_cyan, null))
            snackbar.setTextColor(resources.getColor(R.color.retro_dark, null))
            snackbar.show()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
    }
}

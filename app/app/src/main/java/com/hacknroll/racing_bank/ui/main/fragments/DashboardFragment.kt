package com.hacknroll.racing_bank.ui.main.fragments

//import android.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.hacknroll.racing_bank.R
import com.hacknroll.racing_bank.data.models.BalanceResponse
import com.hacknroll.racing_bank.ui.main.MainViewModel
import com.hacknroll.racing_bank.utils.Resource
import com.hacknroll.racing_bank.utils.SessionManager
import com.hacknroll.racing_bank.utils.SoundManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class DashboardFragment : Fragment() {
    
    private lateinit var viewModel: MainViewModel
    private lateinit var sessionManager: SessionManager
    
    // Views
    private lateinit var welcomeText: TextView
    private lateinit var cashBalanceText: TextView
    private lateinit var fundBalanceText: TextView
    private lateinit var totalBalanceText: TextView
    private lateinit var cashCard: CardView
    private lateinit var fundCard: CardView
    private lateinit var quickActionsCard: CardView
    private lateinit var depositButton: Button
    private lateinit var withdrawButton: Button
    private lateinit var transferButton: Button
    private lateinit var investButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var refreshFab: FloatingActionButton
    
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
    
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
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupListeners()
        observeViewModel()
    }
    
    private fun initViews(view: View) {
        welcomeText = view.findViewById(R.id.welcomeText)
        cashBalanceText = view.findViewById(R.id.cashBalanceText)
        fundBalanceText = view.findViewById(R.id.fundBalanceText)
        totalBalanceText = view.findViewById(R.id.totalBalanceText)
        cashCard = view.findViewById(R.id.cashCard)
        fundCard = view.findViewById(R.id.fundCard)
        quickActionsCard = view.findViewById(R.id.quickActionsCard)
        depositButton = view.findViewById(R.id.depositButton)
        withdrawButton = view.findViewById(R.id.withdrawButton)
        transferButton = view.findViewById(R.id.transferButton)
        investButton = view.findViewById(R.id.investButton)
        progressBar = view.findViewById(R.id.progressBar)
        refreshFab = view.findViewById(R.id.refreshFab)
        
        // Set welcome message
        val username = sessionManager.getUsername() ?: "User"
        welcomeText.text = "Welcome back, $username!"
        
        applyRetroStyling()
    }
    
    private fun applyRetroStyling() {
        // Card backgrounds
        cashCard.setCardBackgroundColor(resources.getColor(R.color.retro_dark_purple, null))
        fundCard.setCardBackgroundColor(resources.getColor(R.color.retro_dark_blue, null))
        quickActionsCard.setCardBackgroundColor(resources.getColor(R.color.retro_dark, null))
        
        // Button colors
        depositButton.setBackgroundColor(resources.getColor(R.color.retro_green, null))
        withdrawButton.setBackgroundColor(resources.getColor(R.color.retro_orange, null))
        transferButton.setBackgroundColor(resources.getColor(R.color.retro_cyan, null))
        investButton.setBackgroundColor(resources.getColor(R.color.retro_pink, null))
        
        // FAB color
        refreshFab.backgroundTintList = resources.getColorStateList(R.color.retro_yellow, null)
    }
    
    private fun setupListeners() {
        depositButton.setOnClickListener {
            SoundManager.playSound(SoundManager.SoundType.CLICK)
            showDepositDialog()
        }
        
        withdrawButton.setOnClickListener {
            SoundManager.playSound(SoundManager.SoundType.CLICK)
            showWithdrawDialog()
        }
        
        transferButton.setOnClickListener {
            SoundManager.playSound(SoundManager.SoundType.CLICK)
            // Navigate to transfer fragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, TransferFragment())
                .addToBackStack(null)
                .commit()
        }
        
        investButton.setOnClickListener {
            SoundManager.playSound(SoundManager.SoundType.CLICK)
            // Navigate to investment fragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, InvestmentFragment())
                .addToBackStack(null)
                .commit()
        }
        
        refreshFab.setOnClickListener {
            SoundManager.playSound(SoundManager.SoundType.CLICK)
            animateRefresh()
            viewModel.refreshBalance()
        }
    }
    
    private fun animateRefresh() {
        refreshFab.animate()
            .rotation(refreshFab.rotation + 360f)
            .setDuration(500)
            .start()
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.balanceState.collectLatest { resource ->
                when (resource) {
                    is Resource.Idle -> {
                        showLoading(false)
                    }
                    is Resource.Loading -> {
                        showLoading(true)
                    }
                    is Resource.Success -> {
                        showLoading(false)
                        updateBalanceDisplay(resource.data)
                    }
                    is Resource.Error -> {
                        showLoading(false)
                        showError(resource.message)
                    }
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.depositState.collectLatest { resource ->
                when (resource) {
                    is Resource.Success -> {
                        SoundManager.playSound(SoundManager.SoundType.COIN)
                        SoundManager.vibrateSuccess()
                        showSuccess("Deposit successful!")
                        viewModel.refreshBalance()
                    }
                    is Resource.Error -> {
                        SoundManager.playSound(SoundManager.SoundType.ERROR)
                        SoundManager.vibrateError()
                        showError(resource.message)
                    }
                    else -> {}
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.withdrawalState.collectLatest { resource ->
                when (resource) {
                    is Resource.Success -> {
                        SoundManager.playSound(SoundManager.SoundType.COIN)
                        SoundManager.vibrateSuccess()
                        showSuccess("Withdrawal successful!")
                        viewModel.refreshBalance()
                    }
                    is Resource.Error -> {
                        SoundManager.playSound(SoundManager.SoundType.ERROR)
                        SoundManager.vibrateError()
                        showError(resource.message)
                    }
                    else -> {}
                }
            }
        }
    }
    
    private fun updateBalanceDisplay(balance: BalanceResponse) {
        // Animate balance updates
        animateBalanceUpdate(cashBalanceText, balance.cashBalance)
        animateBalanceUpdate(fundBalanceText, balance.fundValue)
        animateBalanceUpdate(totalBalanceText, balance.totalPortfolio)
        
        // Update fund shares info
        view?.findViewById<TextView>(R.id.fundSharesText)?.text = 
            "Shares: ${String.format("%.4f", balance.fundShares)}"
    }
    
    private fun animateBalanceUpdate(textView: TextView, newValue: Double) {
        val formattedValue = currencyFormatter.format(newValue)
        
        textView.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                textView.text = formattedValue
                textView.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }
    
    private fun showDepositDialog() {
        val container = FrameLayout(requireContext()).apply {
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, 0)
        }

        val editText = EditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or 
                       android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "Enter amount"
        }

        container.addView(
            editText,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )

        MaterialAlertDialogBuilder(requireContext(), R.style.RetroAlertDialog)
            .setTitle("💰 DEPOSIT CASH")
            .setView(container)
            .setPositiveButton("DEPOSIT") { _, _ ->
                val amount = editText.text.toString().toDoubleOrNull()
                if (amount != null && amount > 0) {
                    viewModel.deposit(amount)
                } else {
                    showError("Invalid amount")
                }
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }
    
    private fun showWithdrawDialog() {
        val container = FrameLayout(requireContext()).apply {
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, 0)
        }

        val editText = EditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or 
                       android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "Enter amount"
        }

        container.addView(
            editText,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )

        MaterialAlertDialogBuilder(requireContext(), R.style.RetroAlertDialog)
            .setTitle("💸 WITHDRAW CASH")
            .setView(container)
            .setPositiveButton("WITHDRAW") { _, _ ->
                val amount = editText.text.toString().toDoubleOrNull()
                if (amount != null && amount > 0) {
                    viewModel.withdraw(amount)
                } else {
                    showError("Invalid amount")
                }
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }
    
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        refreshFab.isEnabled = !show
    }
    
    private fun showError(message: String) {
        if (!isAdded) return
        view?.let {
            val snackbar = Snackbar.make(it, message, Snackbar.LENGTH_LONG)
            snackbar.setBackgroundTint(resources.getColor(R.color.retro_dark_red, null))
            snackbar.setTextColor(resources.getColor(R.color.retro_white, null))
            snackbar.show()
        }
    }
    
    private fun showSuccess(message: String) {
        view?.let {
            val snackbar = Snackbar.make(it, message, Snackbar.LENGTH_SHORT)
            snackbar.setBackgroundTint(resources.getColor(R.color.retro_dark_green, null))
            snackbar.setTextColor(resources.getColor(R.color.retro_dark, null))
            snackbar.show()
        }
    }
}

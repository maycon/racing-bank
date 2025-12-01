package com.hacknroll.racing_bank.ui.main.fragments

//import android.app.AlertDialog
import android.annotation.SuppressLint
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.hacknroll.racing_bank.R
import com.hacknroll.racing_bank.ui.main.MainViewModel
import com.hacknroll.racing_bank.utils.Resource
import com.hacknroll.racing_bank.utils.SessionManager
import com.hacknroll.racing_bank.utils.SoundManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class InvestmentFragment : Fragment() {

    private lateinit var viewModel: MainViewModel
    private lateinit var sessionManager: SessionManager

    // Views
    private lateinit var sharePriceText: TextView
    private lateinit var totalFundValueText: TextView
    private lateinit var lastUpdatedText: TextView
    private lateinit var yourSharesText: TextView
    private lateinit var portfolioValueText: TextView
    private lateinit var cashBalanceText: TextView
    private lateinit var amountInput: TextInputEditText
    private lateinit var amountLayout: TextInputLayout
    private lateinit var subscribeButton: Button
    private lateinit var redeemButton: Button
    private lateinit var progressBar: ProgressBar

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)

    private var currentSharePrice = 0.0
    private var userShares = 0.0
    private var cashBalance = 0.0

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
        return inflater.inflate(R.layout.fragment_investment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupListeners()
        observeViewModel()

        // Load initial data
        viewModel.refreshBalance()
        viewModel.loadFundInfo()
    }

    private fun initViews(view: View) {
        sharePriceText = view.findViewById(R.id.sharePriceText)
        totalFundValueText = view.findViewById(R.id.totalFundValueText)
        lastUpdatedText = view.findViewById(R.id.lastUpdatedText)
        yourSharesText = view.findViewById(R.id.yourSharesText)
        portfolioValueText = view.findViewById(R.id.portfolioValueText)
        cashBalanceText = view.findViewById(R.id.cashBalanceText)
        amountInput = view.findViewById(R.id.amountInput)
        amountLayout = view.findViewById(R.id.amountLayout)
        subscribeButton = view.findViewById(R.id.subscribeButton)
        redeemButton = view.findViewById(R.id.redeemButton)
        progressBar = view.findViewById(R.id.progressBar)

        applyRetroStyling()
    }

    private fun applyRetroStyling() {
        // Add typing effect
        amountInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                SoundManager.playSound(SoundManager.SoundType.CLICK)
            }
        }
    }

    private fun setupListeners() {
        subscribeButton.setOnClickListener {
            SoundManager.playSound(SoundManager.SoundType.CLICK)
            showInvestDialog()
        }

        redeemButton.setOnClickListener {
            SoundManager.playSound(SoundManager.SoundType.CLICK)
            showRedeemDialog()
        }
    }

    private fun showInvestDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_invest, null)

        val amountInput = dialogView.findViewById<EditText>(R.id.amountInput)
        val availableText = dialogView.findViewById<TextView>(R.id.availableText)
        val quickAmountLayout = dialogView.findViewById<LinearLayout>(R.id.quickAmountLayout)

        availableText.text = "Available: ${currencyFormatter.format(cashBalance)}"

        // Add quick amount buttons
        val amounts = listOf(100.0, 500.0, 1000.0, 5000.0)
        amounts.forEach { amount ->
            if (amount <= cashBalance) {
                val button = Button(requireContext()).apply {
                    text = currencyFormatter.format(amount)
                    setOnClickListener {
                        amountInput.setText(amount.toString())
                        SoundManager.playSound(SoundManager.SoundType.CLICK)
                    }
                }
                quickAmountLayout.addView(button)
            }
        }

        MaterialAlertDialogBuilder(requireContext(), R.style.RetroAlertDialog)
            .setView(dialogView)
            .setPositiveButton("INVEST") { dialog, which ->
                val amount = amountInput.text.toString().toDoubleOrNull()
                if (amount != null && amount > 0 && amount <= cashBalance) {
                    viewModel.subscribeToFund(amount)
                } else {
                    showError("Invalid amount")
                }
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    @SuppressLint("SetTextI18n")
    private fun showRedeemDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_redeem, null)

        val amountInput = dialogView.findViewById<EditText>(R.id.amountInput)
        val availableText = dialogView.findViewById<TextView>(R.id.availableText)
        val shareValueText = dialogView.findViewById<TextView>(R.id.shareValueText)

        availableText.text = "Available shares: ${"%.2f".format(userShares)}"
        val sharePrice = currentSharePrice
        if (sharePrice > 0) {
            shareValueText.text = "Share value: ${currencyFormatter.format(sharePrice)}"
        }

        MaterialAlertDialogBuilder(requireContext(), R.style.RetroAlertDialog)
            .setView(dialogView)
            .setPositiveButton("REDEEM") { dialog, which ->
                val shares = amountInput.text.toString().toDoubleOrNull()
                if (shares != null && shares > 0 && shares <= userShares) {
                    if (currentSharePrice > 0) {
                        viewModel.redeemFromFund(shares * currentSharePrice)
                    } else {
                        showError("Share price not loaded. Please try again.")
                    }                } else {
                    showError("Invalid amount")
                }
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun observeViewModel() {
        // Observe balance
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.balanceState.collectLatest { resource ->
                when (resource) {
                    is Resource.Success -> {
                        cashBalance = resource.data.cashBalance
                        userShares = resource.data.fundShares
                        cashBalanceText.text = currencyFormatter.format(cashBalance)
                        yourSharesText.text = "%.2f".format(userShares)

                        val portfolioValue = userShares * currentSharePrice
                        portfolioValueText.text = currencyFormatter.format(portfolioValue)
                    }

                    else -> {}
                }
            }
        }

        // Observe fund info
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.fundInfoState.collectLatest { resource ->
                when (resource) {
                    is Resource.Idle -> {
                        // Initial state - do nothing
                    }

                    is Resource.Loading -> {
                        progressBar.visibility = View.VISIBLE
                    }

                    is Resource.Success -> {
                        progressBar.visibility = View.GONE
                        currentSharePrice = resource.data.sharePrice
                        sharePriceText.text = currencyFormatter.format(currentSharePrice)
                        totalFundValueText.text = currencyFormatter.format(resource.data.totalValue)

                        try {
                            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                                .parse(resource.data.updatedAt)
                            lastUpdatedText.text = "Last updated: ${dateFormatter.format(date)}"
                        } catch (e: Exception) {
                            lastUpdatedText.text = "Last updated: ${resource.data.updatedAt}"
                        }

                        // Update portfolio value
                        val portfolioValue = userShares * currentSharePrice
                        portfolioValueText.text = currencyFormatter.format(portfolioValue)
                    }

                    is Resource.Error -> {
                        progressBar.visibility = View.GONE
                        showError(resource.message)
                    }
                }
            }
        }

        // Observe subscription state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.subscriptionState.collectLatest { resource ->
                when (resource) {
                    is Resource.Idle -> {
                        // Initial state - do nothing
                    }

                    is Resource.Loading -> {
                        showLoading(true)
                    }

                    is Resource.Success -> {
                        showLoading(false)
                        SoundManager.playSound(SoundManager.SoundType.SUCCESS)
                        showSuccess("Investment successful!")
                        viewModel.refreshBalance()
//                        viewModel.getFundInfo()
                    }

                    is Resource.Error -> {
                        showLoading(false)
                        showError(resource.message)
                    }
                }
            }
        }

        // Observe redemption state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.redemptionState.collectLatest { resource ->
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
                        showSuccess("Redemption successful!")
                        viewModel.refreshBalance()
//                        viewModel.getFundInfo()
                    }

                    is Resource.Error -> {
                        showLoading(false)
                        showError(resource.message)
                    }
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        subscribeButton.isEnabled = !show
        redeemButton.isEnabled = !show
        amountInput.isEnabled = !show
    }

    private fun showError(message: String) {
        if (!isAdded) return
        view?.let {
            val snackbar = Snackbar.make(it, message, Snackbar.LENGTH_LONG)
            snackbar.setBackgroundTint(resources.getColor(R.color.retro_red, null))
            snackbar.setTextColor(resources.getColor(R.color.retro_white, null))
            snackbar.show()
        }
        SoundManager.playSound(SoundManager.SoundType.ERROR)
    }

    private fun showSuccess(message: String) {
        view?.let {
            val snackbar = Snackbar.make(it, message, Snackbar.LENGTH_SHORT)
            snackbar.setBackgroundTint(resources.getColor(R.color.retro_green, null))
            snackbar.setTextColor(resources.getColor(R.color.retro_dark, null))
            snackbar.show()
        }
    }
}
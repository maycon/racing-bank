package com.hacknroll.racing_bank.ui.main.fragments

//import android.app.AlertDialog
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
import java.util.Locale

class TransferFragment : Fragment() {

    private lateinit var viewModel: MainViewModel
    private lateinit var sessionManager: SessionManager

    // Views
    private lateinit var recipientInput: TextInputEditText
    private lateinit var recipientLayout: TextInputLayout
    private lateinit var amountInput: TextInputEditText
    private lateinit var amountLayout: TextInputLayout
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var descriptionLayout: TextInputLayout
    private lateinit var currentBalanceText: TextView
    private lateinit var transferButton: Button
    private lateinit var quickTransferLayout: LinearLayout
    private lateinit var progressBar: ProgressBar

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
    private var currentBalance = 0.0

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
        return inflater.inflate(R.layout.fragment_transfer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupListeners()
        observeViewModel()
        setupQuickTransferAmounts()

        // Load current balance
        viewModel.refreshBalance()
    }

    private fun initViews(view: View) {
        recipientInput = view.findViewById(R.id.recipientInput)
        recipientLayout = view.findViewById(R.id.recipientLayout)
        amountInput = view.findViewById(R.id.amountInput)
        amountLayout = view.findViewById(R.id.amountLayout)
        descriptionInput = view.findViewById(R.id.descriptionInput)
        descriptionLayout = view.findViewById(R.id.descriptionLayout)
        currentBalanceText = view.findViewById(R.id.currentBalanceText)
        transferButton = view.findViewById(R.id.transferButton)
        quickTransferLayout = view.findViewById(R.id.quickTransferLayout)
        progressBar = view.findViewById(R.id.progressBar)

        applyRetroStyling()
    }

    private fun applyRetroStyling() {
        transferButton.setBackgroundColor(resources.getColor(R.color.retro_cyan, null))

        // Add typing effects
        listOf(recipientInput, amountInput, descriptionInput).forEach { input ->
            input.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    SoundManager.playSound(SoundManager.SoundType.CLICK)
                }
            }
        }
    }

    private fun setupQuickTransferAmounts() {
        val amounts = listOf(10.0, 25.0, 50.0, 100.0, 500.0)

        amounts.forEach { amount ->
            val button = Button(requireContext()).apply {
                text = currencyFormatter.format(amount)
                setBackgroundColor(resources.getColor(R.color.retro_pink, null))
                setTextColor(resources.getColor(R.color.retro_white, null))
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    setMargins(4, 0, 4, 0)
                }

                setOnClickListener {
                    SoundManager.playSound(SoundManager.SoundType.CLICK)
                    amountInput.setText(amount.toString())
                }
            }
            quickTransferLayout.addView(button)
        }
    }

    private fun setupListeners() {
        transferButton.setOnClickListener {
            SoundManager.playSound(SoundManager.SoundType.CLICK)
            performTransfer()
        }

        // Add real-time validation
        amountInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateAmount()
            }
        }

        recipientInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateRecipient()
            }
        }
    }

    private fun validateRecipient(): Boolean {
        val recipient = recipientInput.text.toString().trim()

        return when {
            recipient.isEmpty() -> {
                recipientLayout.error = "Recipient username required"
                false
            }

            recipient.length < 3 -> {
                recipientLayout.error = "Username too short"
                false
            }

            recipient == sessionManager.getUsername() -> {
                recipientLayout.error = "Cannot transfer to yourself"
                false
            }

            else -> {
                recipientLayout.error = null
                true
            }
        }
    }

    private fun validateAmount(): Boolean {
        val amountText = amountInput.text.toString()
        val amount = amountText.toDoubleOrNull()

        return when {
            amountText.isEmpty() -> {
                amountLayout.error = "Amount required"
                false
            }

            amount == null || amount <= 0 -> {
                amountLayout.error = "Invalid amount"
                false
            }

            amount > currentBalance -> {
                amountLayout.error = "Insufficient funds"
                false
            }

            else -> {
                amountLayout.error = null
                true
            }
        }
    }

    private fun performTransfer() {
        val recipient = recipientInput.text.toString().trim()
        val amount = amountInput.text.toString().toDoubleOrNull()

        if (!validateRecipient() || !validateAmount() || amount == null) {
            SoundManager.playSound(SoundManager.SoundType.ERROR)
            return
        }

        // Show confirmation dialog
        showConfirmationDialog(recipient, amount)
    }

    private fun showConfirmationDialog(recipient: String, amount: Double) {
        val message = """
            Transfer Details:
            
            To: $recipient
            Amount: ${currencyFormatter.format(amount)}
            
            ⚠️ WARNING: This action cannot be undone!
            
            Confirm transfer?
        """.trimIndent()

        MaterialAlertDialogBuilder(requireContext(), R.style.RetroAlertDialog)
            .setTitle("💸 CONFIRM TRANSFER")
            .setMessage(message)
            .setPositiveButton("TRANSFER") { _, _ ->
                executeTransfer(recipient, amount)
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun executeTransfer(recipient: String, amount: Double) {
        viewModel.transfer(recipient, amount)
    }

    private fun observeViewModel() {
        // Observe balance
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.balanceState.collectLatest { resource ->
                when (resource) {
                    is Resource.Success -> {
                        currentBalance = resource.data.cashBalance
                        currentBalanceText.text = "Available: ${currencyFormatter.format(currentBalance)}"
                        currentBalanceText.setTextColor(
                            if (currentBalance > 0)
                                resources.getColor(R.color.retro_green, null)
                            else
                                resources.getColor(R.color.retro_red, null)
                        )
                    }

                    else -> {}
                }
            }
        }

        // Observe transfer state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.transferState.collectLatest { resource ->
                when (resource) {
                    is Resource.Idle -> {
                        showLoading(false)
                    }

                    is Resource.Loading -> {
                        showLoading(true)
                    }

                    is Resource.Success -> {
                        showLoading(false)
                        SoundManager.playSound(SoundManager.SoundType.TRANSFER)
                        SoundManager.vibrateSuccess()

                        showSuccessDialog(resource.data)
                        clearInputs()
                        viewModel.refreshBalance()
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

    private fun showSuccessDialog(transfer: com.hacknroll.racing_bank.data.models.TransferResponse) {
        val message = """
            ✅ Transfer Successful!
            
            Transfer ID: #${transfer.transferId}
            To: ${transfer.toUsername}
            Amount: ${currencyFormatter.format(transfer.amount)}
            New Balance: ${currencyFormatter.format(transfer.newBalance)}
            
            Status: ${transfer.status}
        """.trimIndent()

        MaterialAlertDialogBuilder(requireContext(), R.style.RetroAlertDialog)
            .setTitle("🎉 SUCCESS")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun clearInputs() {
        recipientInput.text?.clear()
        amountInput.text?.clear()
        descriptionInput.text?.clear()
        recipientLayout.error = null
        amountLayout.error = null
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        transferButton.isEnabled = !show
        recipientInput.isEnabled = !show
        amountInput.isEnabled = !show
        descriptionInput.isEnabled = !show

        if (show) {
            transferButton.text = "PROCESSING..."
        } else {
            transferButton.text = "TRANSFER"
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

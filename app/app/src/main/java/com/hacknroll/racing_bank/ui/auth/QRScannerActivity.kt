package com.hacknroll.racing_bank.ui.auth

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.hacknroll.racing_bank.R
import com.hacknroll.racing_bank.utils.SoundManager

class QRScannerActivity : AppCompatActivity() {
    
    private lateinit var qrCodeImageView: ImageView
    private lateinit var secretKeyText: TextView
    private lateinit var instructionText: TextView
    private lateinit var doneButton: Button
    private lateinit var copyButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)
        
        initViews()
        
        val totpUri = intent.getStringExtra("totp_uri")
        val totpSecret = extractSecretFromUri(totpUri)
        
        totpUri?.let { uri ->
            generateQRCode(uri)
        }
        
        totpSecret?.let { secret ->
            secretKeyText.text = formatSecret(secret)
        }
        
        setupListeners()
    }
    
    private fun initViews() {
        qrCodeImageView = findViewById(R.id.qrCodeImageView)
        secretKeyText = findViewById(R.id.secretKeyText)
        instructionText = findViewById(R.id.instructionText)
        doneButton = findViewById(R.id.doneButton)
        copyButton = findViewById(R.id.copyButton)
    }
    
    private fun setupListeners() {
        doneButton.setOnClickListener {
            SoundManager.playSound(SoundManager.SoundType.CLICK)
            finish()
            overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_up)
        }
        
        copyButton.setOnClickListener {
            SoundManager.playSound(SoundManager.SoundType.CLICK)
            copyToClipboard(secretKeyText.text.toString())
        }
    }
    
    private fun generateQRCode(text: String) {
        val writer = MultiFormatWriter()
        try {
            val bitMatrix: BitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            
            qrCodeImageView.setImageBitmap(bitmap)
        } catch (e: WriterException) {
            e.printStackTrace()
        }
    }
    
    private fun extractSecretFromUri(uri: String?): String? {
        if (uri == null) return null
        
        // Format: otpauth://totp/HackNRoll:username?secret=SECRET&issuer=HackNRoll
        val regex = "secret=([A-Z0-9]+)".toRegex()
        val matchResult = regex.find(uri)
        return matchResult?.groupValues?.get(1)
    }
    
    private fun formatSecret(secret: String): String {
        // Format secret in groups of 4 for easier reading
        return secret.chunked(4).joinToString(" ")
    }
    
    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("TOTP Secret", text)
        clipboard.setPrimaryClip(clip)
        
        // Show toast
        android.widget.Toast.makeText(this, "Secret key copied!", android.widget.Toast.LENGTH_SHORT).show()
        SoundManager.playSound(SoundManager.SoundType.SUCCESS)
    }
}
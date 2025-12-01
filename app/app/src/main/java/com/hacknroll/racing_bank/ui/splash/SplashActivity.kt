package com.hacknroll.racing_bank.ui.splash

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.hacknroll.racing_bank.R
import com.hacknroll.racing_bank.ui.auth.AuthActivity
import com.hacknroll.racing_bank.ui.main.MainActivity
import com.hacknroll.racing_bank.utils.SessionManager
import com.hacknroll.racing_bank.utils.SoundManager

class SplashActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    // private lateinit var repository: BankRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        sessionManager = SessionManager(this)
        // repository = BankRepository(sessionManager)

        // Animate logo and text
        animateSplash()

        // Check health and session after delay
        Handler(Looper.getMainLooper()).postDelayed({
            checkHealthAndSession()
        }, 2000)
    }

    private fun animateSplash() {
        val logo = findViewById<ImageView>(R.id.logo)
        val title = findViewById<TextView>(R.id.titleText)
        val subtitle = findViewById<TextView>(R.id.subtitleText)
        val loadingText = findViewById<TextView>(R.id.loadingText)

//        // Logo animation - car racing in from left
//        val logoTranslationX = ObjectAnimator.ofFloat(logo, View.TRANSLATION_X, -500f, 50f, 0f)
//        val logoScaleX = ObjectAnimator.ofFloat(logo, View.SCALE_X, 1f, 1.1f, 1f)
//        val logoScaleY = ObjectAnimator.ofFloat(logo, View.SCALE_Y, 1f, 1.1f, 1f)
//
//        val logoAnimSet = AnimatorSet()
//        logoAnimSet.playTogether(logoTranslationX, logoScaleX, logoScaleY)
//        logoAnimSet.duration = 1200
//        logoAnimSet.interpolator = AccelerateDecelerateInterpolator()

//        // Car races in and skids to a stop
//        val logoTranslationX = ObjectAnimator.ofFloat(logo, View.TRANSLATION_X, -800f, 20f, -10f, 0f)
//        logoTranslationX.duration = 800
//        logoTranslationX.interpolator = DecelerateInterpolator(2f)
//
//        // Slight tilt during skid
//        val logoRotation = ObjectAnimator.ofFloat(logo, View.ROTATION, -5f, 3f, 0f)
//        logoRotation.duration = 600
//        logoRotation.startDelay = 400
//
//        val logoAnimSet = AnimatorSet()
//        logoAnimSet.playTogether(logoTranslationX, logoRotation)


//        // Start small and far, zoom in fast
//        val logoTranslationX = ObjectAnimator.ofFloat(logo, View.TRANSLATION_X, -600f, 0f)
//        val logoScaleX = ObjectAnimator.ofFloat(logo, View.SCALE_X, 0.3f, 1.2f, 1f)
//        val logoScaleY = ObjectAnimator.ofFloat(logo, View.SCALE_Y, 0.3f, 1.2f, 1f)
//        val logoAlpha = ObjectAnimator.ofFloat(logo, View.ALPHA, 0f, 1f)
//
//        val logoAnimSet = AnimatorSet()
//        logoAnimSet.playTogether(logoTranslationX, logoScaleX, logoScaleY, logoAlpha)
//        logoAnimSet.duration = 600
//        logoAnimSet.interpolator = OvershootInterpolator(1.5f)

//        // Race across screen then return
//        val logoTranslationX = ObjectAnimator.ofFloat(logo, View.TRANSLATION_X, 800f, -100f, 0f)
//        logoTranslationX.duration = 1500
//
//        // Flip direction when returning
//        val logoScaleX = ObjectAnimator.ofFloat(logo, View.SCALE_X, -1f, -1f, 1f)
//        logoScaleX.duration = 1500
//
//        val logoAnimSet = AnimatorSet()
//        logoAnimSet.playTogether(logoTranslationX, logoScaleX)
//        logoAnimSet.interpolator = AccelerateDecelerateInterpolator()

//        // Jump arc animation
//        val logoTranslationX = ObjectAnimator.ofFloat(logo, View.TRANSLATION_X, -600f, 0f)
//        val logoTranslationY = ObjectAnimator.ofFloat(logo, View.TRANSLATION_Y, 200f, -50f, 0f)
//        val logoRotation = ObjectAnimator.ofFloat(logo, View.ROTATION, -15f, 5f, 0f)
//
//        val logoAnimSet = AnimatorSet()
//        logoAnimSet.playTogether(logoTranslationX, logoTranslationY, logoRotation)
//        logoAnimSet.duration = 1000
//        logoAnimSet.interpolator = DecelerateInterpolator()


//        // Pixelated entrance feel
//        val logoTranslationX = ObjectAnimator.ofFloat(logo, View.TRANSLATION_X, -400f, 0f)
//        val logoAlpha = ObjectAnimator.ofFloat(logo, View.ALPHA, 0f, 0.3f, 0.6f, 1f)
//
//        val logoAnimSet = AnimatorSet()
//        logoAnimSet.playTogether(logoTranslationX, logoAlpha)
//        logoAnimSet.duration = 800
//        logoAnimSet.interpolator = LinearInterpolator()

        // Random car animation
        val logoAnimSet = getRandomCarAnimation(logo)

        // Title animation
        val titleAlpha = ObjectAnimator.ofFloat(title, View.ALPHA, 0f, 1f)
        val titleTranslation = ObjectAnimator.ofFloat(title, View.TRANSLATION_Y, 50f, 0f)

        val titleAnimSet = AnimatorSet()
        titleAnimSet.playTogether(titleAlpha, titleTranslation)
        titleAnimSet.duration = 800
        titleAnimSet.startDelay = 500

        // Subtitle animation
        val subtitleAlpha = ObjectAnimator.ofFloat(subtitle, View.ALPHA, 0f, 1f)
        subtitleAlpha.duration = 600
        subtitleAlpha.startDelay = 1000

        // Loading text blinking
        val loadingBlink = ObjectAnimator.ofFloat(loadingText, View.ALPHA, 0f, 1f, 0f)
        loadingBlink.duration = 1000
        loadingBlink.repeatCount = ObjectAnimator.INFINITE
        loadingBlink.startDelay = 1200

        // Start animations
        logoAnimSet.start()
        titleAnimSet.start()
        subtitleAlpha.start()
        loadingBlink.start()

        // Play startup sound
        SoundManager.playSound(SoundManager.SoundType.LOGIN)
    }

    private fun getRandomCarAnimation(logo: ImageView): AnimatorSet {
        val animSet = AnimatorSet()

        when ((0..4).random()) {
            0 -> {
                // Derrapagem com parada brusca
                val translationX = ObjectAnimator.ofFloat(logo, View.TRANSLATION_X, -800f, 20f, -10f, 0f)
                translationX.duration = 800
                translationX.interpolator = DecelerateInterpolator(2f)

                val rotation = ObjectAnimator.ofFloat(logo, View.ROTATION, -5f, 3f, 0f)
                rotation.duration = 600
                rotation.startDelay = 400

                animSet.playTogether(translationX, rotation)
            }
            1 -> {
                // Turbo boost
                val translationX = ObjectAnimator.ofFloat(logo, View.TRANSLATION_X, -600f, 0f)
                val scaleX = ObjectAnimator.ofFloat(logo, View.SCALE_X, 0.3f, 1.2f, 1f)
                val scaleY = ObjectAnimator.ofFloat(logo, View.SCALE_Y, 0.3f, 1.2f, 1f)
                val alpha = ObjectAnimator.ofFloat(logo, View.ALPHA, 0f, 1f)

                animSet.playTogether(translationX, scaleX, scaleY, alpha)
                animSet.duration = 600
                animSet.interpolator = OvershootInterpolator(1.5f)
            }
            2 -> {
                // Salto estilo rampa
                val translationX = ObjectAnimator.ofFloat(logo, View.TRANSLATION_X, -600f, 0f)
                val translationY = ObjectAnimator.ofFloat(logo, View.TRANSLATION_Y, 200f, -50f, 0f)
                val rotation = ObjectAnimator.ofFloat(logo, View.ROTATION, -15f, 5f, 0f)

                animSet.playTogether(translationX, translationY, rotation)
                animSet.duration = 1000
                animSet.interpolator = DecelerateInterpolator()
            }
            3 -> {
                // Entrada pela direita com flip
                val translationX = ObjectAnimator.ofFloat(logo, View.TRANSLATION_X, 800f, -50f, 0f)
                val scaleX = ObjectAnimator.ofFloat(logo, View.SCALE_X, 1f, 1.1f, 1f)

                animSet.playTogether(translationX, scaleX)
                animSet.duration = 1000
                animSet.interpolator = DecelerateInterpolator(1.5f)
            }
            4 -> {
                // Queda do céu com bounce
                val translationY = ObjectAnimator.ofFloat(logo, View.TRANSLATION_Y, -800f, 30f, -15f, 0f)
                val rotation = ObjectAnimator.ofFloat(logo, View.ROTATION, 360f, 0f)

                animSet.playTogether(translationY, rotation)
                animSet.duration = 1200
                animSet.interpolator = DecelerateInterpolator(1.5f)
            }
        }

        return animSet
    }
    private fun checkHealthAndSession() {
        checkUserSession()
    }

    private fun checkUserSession() {
        if (sessionManager.isLoggedIn() && !sessionManager.isSessionExpired()) {
            navigateToMain()
        } else {
            sessionManager.clearSession()
            navigateToAuth()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }

    private fun navigateToAuth() {
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }
}

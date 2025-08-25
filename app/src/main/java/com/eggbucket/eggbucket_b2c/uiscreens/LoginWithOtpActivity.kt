package com.eggbucket.eggbucket_b2c.uiscreens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.animation.*
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.eggbucket.eggbucket_b2c.R
import com.eggbucket.eggbucket_b2c.databinding.ActivityLoginWithOtpBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class LoginWithOtpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityLoginWithOtpBinding
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginWithOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressBar = binding.progressBar

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginWithOtp)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        makeApiRequestWithRetries01()

        // Animate UI when activity starts
        animateEntry()

        // 🔼 When EditText focused → scroll up & move logo
        binding.etPhoneNumber.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.loginWithOtp.post {
                    binding.loginWithOtp.smoothScrollTo(0, binding.etPhoneNumber.top)
                    binding.appLogo.animate().translationY(-200f).setDuration(300).start()
                    binding.loginCard.animate().translationY(-200f).setDuration(300).start()
                }
            }
        }

        // 🔽 When 10 digits entered → hide keyboard, scroll down & reset logo
        binding.etPhoneNumber.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 10) {
                    // Hide keyboard
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(binding.etPhoneNumber.windowToken, 0)

                    // Reset UI back to normal
                    binding.loginWithOtp.post {
                        binding.loginWithOtp.smoothScrollTo(0, 0)
                        binding.appLogo.animate().translationY(0f).setDuration(300).start()
                        binding.loginCard.animate().translationY(0f).setDuration(300).start()
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Send OTP click with button animation
        binding.btnSendOtp.setOnClickListener {
            animateButton(binding.btnSendOtp)

            val phoneNumber = binding.etPhoneNumber.text.toString()
            if (phoneNumber.isNotEmpty()) {
                sendVerificationCode("+91${phoneNumber}")
            } else {
                Toast.makeText(this, "Please enter a phone number", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendVerificationCode(phoneNumber: String) {
        showProgress(true)
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    // PhoneAuth Callbacks
    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            showProgress(false)
            Toast.makeText(this@LoginWithOtpActivity, "Verification failed: ${e.message}", Toast.LENGTH_LONG).show()
        }

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            val phoneNumber = binding.etPhoneNumber.text.toString()
            showProgress(false)

            val intent = Intent(this@LoginWithOtpActivity, OtpVerificationActivity::class.java)
            intent.putExtra("verificationId", verificationId)
            intent.putExtra("phoneNumber", phoneNumber)
            startActivity(intent)

            // Smooth slide transition
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Authentication successful", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Fade animation for progress bar
    private fun showProgress(show: Boolean) {
        if (show) {
            progressBar.visibility = View.VISIBLE
            val fadeIn = AlphaAnimation(0f, 1f)
            fadeIn.duration = 300
            progressBar.startAnimation(fadeIn)
        } else {
            val fadeOut = AlphaAnimation(1f, 0f)
            fadeOut.duration = 300
            fadeOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationRepeat(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    progressBar.visibility = View.INVISIBLE
                }
            })
            progressBar.startAnimation(fadeOut)
        }
    }

    // Entry animation for logo and card
    private fun animateEntry() {
        val logo = binding.appLogo
        val card = binding.loginCard

        logo.alpha = 0f
        logo.scaleX = 0.7f
        logo.scaleY = 0.7f
        logo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(800)
            .setStartDelay(100)
            .start()

        card.translationY = 1000f
        card.alpha = 0f
        card.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(700)
            .setStartDelay(300)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    // Subtle scale animation on button click
    private fun animateButton(view: View) {
        val anim = ScaleAnimation(
            1f, 0.95f, 1f, 0.95f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        anim.duration = 100
        anim.repeatMode = Animation.REVERSE
        anim.repeatCount = 1
        view.startAnimation(anim)
    }

    // Dummy API warm-up call
    private fun makeApiRequestWithRetries01() {
        CoroutineScope(Dispatchers.IO).launch {
            val url = "https://b2c-backend-eik4.onrender.com/api/v1/customer/user/6363894956"
            var attempts = 0
            var success = false

            while (attempts < 3 && !success) {
                try {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    val responseCode = connection.responseCode

                    if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                        success = true
                        val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                            connection.inputStream.bufferedReader().use { it.readText() }
                        } else {
                            connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error details"
                        }
                        Log.d("API_RESPONSE", "Response Code: $responseCode, Response: $response")
                    } else {
                        Log.e("API_ERROR", "Unexpected Response code: $responseCode")
                    }
                } catch (e: Exception) {
                    Log.e("API_ERROR", "Exception: ${e.message}")
                } finally {
                    attempts++
                }
            }

            if (!success) {
                Log.e("API_ERROR", "API request failed after 3 attempts.")
            }
        }
    }
}

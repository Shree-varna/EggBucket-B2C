package com.eggbucket.eggbucket_b2c.uiscreens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.eggbucket.eggbucket_b2c.BottomNavigation.ui.BottomNavigationScreen
import com.eggbucket.eggbucket_b2c.R
import com.eggbucket.eggbucket_b2c.databinding.ActivityOtpVerificationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection


import java.net.URL


class OtpVerificationActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityOtpVerificationBinding
    private var verificationId: String? = null
    private var phoneNumber: String? = null
    private lateinit var timer: CountDownTimer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        startCountdownTimer()

        binding = ActivityOtpVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val slideInTop = AnimationUtils.loadAnimation(this, R.anim.slide_in_top)
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val zoomIn = AnimationUtils.loadAnimation(this, R.anim.zoom_in)
        val slideInBottom = AnimationUtils.loadAnimation(this, R.anim.slide_in_bottom)

        // 🔹 Start animations on views
        binding.tvOtpTitle.startAnimation(slideInTop)        // Title slides in
        binding.tvOtpSubtitle.startAnimation(fadeIn)         // Subtitle fades in
        binding.otpPinView.startAnimation(zoomIn)          // OTP boxes zoom in
        binding.btnVerifyOtp.startAnimation(slideInBottom)   // Verify button slides up
        binding.tvResendOtp.startAnimation(fadeIn)           // Resend fades in

        val backArrow = findViewById<ImageView>(R.id.imageView4)
        backArrow.setOnClickListener {
            val intent = Intent(this, LoginWithOtpActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish() // Optional: Closes the current activity so it's removed from the back stack
        }

        Log.d("pinview", "start pinview")

        // Show the keyboard programmatically
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
        Log.d("pinview2", "start pinview")

        // Set up text watcher for OTP input
        binding.otpPinView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // You can log the previous input or do any necessary checks here
                Log.d("pinview", "Before text changed: $s")
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                Log.d("pinview4", "On text changed: $s")
                binding.btnVerifyOtp.isEnabled = s?.length == 6
                if (s.toString().length == 6) {
                    Toast.makeText(this@OtpVerificationActivity, "Verifying...", Toast.LENGTH_SHORT).show()
                }
            }

            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 6) {
                    verifyCode(s.toString())
                    Log.d("loginwithotp", "start")
                }
            }
        })

        // Set insets for the view to support edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        verificationId = intent.getStringExtra("verificationId")
        phoneNumber = intent.getStringExtra("phoneNumber")
        binding.tvOtpSubtitle.text = "Enter the code from the SMS we sent to " + phoneNumber
    }

    private fun verifyCode(code: String) {
        verificationId?.let {
            val credential = PhoneAuthProvider.getCredential(it, code)
            Log.d("loginwithotp", "called signInWithPhoneAuthCredential")
            signInWithPhoneAuthCredential(credential)
        }
    }

    private fun startCountdownTimer() {
        timer = object : CountDownTimer(150000, 1000) { // 2 min 30 sec in milliseconds
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 60000
                val seconds = (millisUntilFinished % 60000) / 1000
                val time = String.format("%02d:%02d", minutes, seconds)
                binding.textView5.text = time
            }

            override fun onFinish() {
                binding.textView5.text = "00:00"
                // Handle any actions after the timer finishes here, such as disabling input or resending OTP
            }
        }
        timer.start()
    }


    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        Log.d("loginwithotp", "started signInWithPhoneAuthCredential")
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    Log.d("loginwithotp", "otp varification success ful")
                    // Save user details in SharedPreferences
                    val sharedPref = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
                    val editor = sharedPref.edit()
                    val sanitizedPhoneNumber = user?.phoneNumber?.replace("+91", "")
                    editor.putString("user_id", user?.uid)
                    editor.putString("user_phone", sanitizedPhoneNumber)
                    editor.commit()
                    Toast.makeText(this, "Authentication successful", Toast.LENGTH_SHORT).show()

                    // Check user details
                    val phoneNumber = sanitizedPhoneNumber ?: ""
                    Log.d("loginwithotp", "check curent details${phoneNumber}")
                    checkDetails(phoneNumber,this) { isSuccess ->
                        runOnUiThread {
                            if (isSuccess) {
                                Log.d("loginwithotp", "check curent details success}")
                               // Toast.makeText(this, "User details found!", Toast.LENGTH_SHORT).show()

                                // Navigate to BottomNavigationScreen
                                val intent = Intent(this, BottomNavigationScreen::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                startActivity(intent)
                            } else {
                                Log.d("loginwithotp", "check curent details failure}")
                                createAccount(phoneNumber)
                                // Navigate to BottomNavigationScreen
                                val intent = Intent(this, BottomNavigationScreen::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                startActivity(intent)
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    fun checkDetails(phoneNumber: String, context: Context, callback: (Boolean) -> Unit) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://b2c-backend-eik4.onrender.com/api/v1/customer/user/$phoneNumber")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback(false)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { // Ensure resources are properly closed
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        Log.d("loginwithotp", "Response Body:${responseBody}")
                        if (!responseBody.isNullOrEmpty()) {
                            try {
                                val jsonObject = JSONObject(responseBody)
                                val name = jsonObject.optString("name")
                                val phone = jsonObject.optString("phoneNumber")
                                val email = jsonObject.optString("email")


                                // Save to SharedPreferences
                                val sharedPreferences = context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
                                sharedPreferences.edit().apply {
                                    putString("name", name)
                                    putString("user_phone", phone)
                                    putString("email", email)
                                    apply()
                                }
                                callback(true)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                callback(false)
                            }
                        } else {
                            callback(false)
                        }
                    } else {
                        callback(false)
                    }
                }
            }
        })
    }
    private fun createAccount(phoneNumber: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val url = "https://b2c-backend-eik4.onrender.com/api/v1/customer/user"
            var attempts = 0
            var success = false

            // Define the default body as a JSON string
            val requestBody = JSONObject().apply {
                put("phone", phoneNumber)
                put("password", "1234567")
            }.toString()

            while (attempts < 2 && !success) {
                var connection: HttpURLConnection? = null
                try {
                    connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.doOutput = true

                    // Write the request body to the output stream
                    connection.outputStream.use { outputStream ->
                        outputStream.write(requestBody.toByteArray(Charsets.UTF_8))
                    }

                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                        // Handle success
                        success = true
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        Log.d("API_RESPONSE", response)
                    } else {
                        Log.e("API_ERROR", "Response code: $responseCode")
                        Log.e("API_ERROR", "Response message: ${connection.responseMessage}")
                    }
                } catch (e: Exception) {
                    Log.e("API_ERROR", "Exception: ${e.message}")
                } finally {
                    connection?.disconnect()
                    attempts++
                }
            }

            if (!success) {
                Log.e("API_ERROR", "API request failed after 2 attempts.")
            }
        }
    }


}





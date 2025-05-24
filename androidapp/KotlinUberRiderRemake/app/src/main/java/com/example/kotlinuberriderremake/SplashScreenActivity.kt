package com.example.kotlinuberriderremake

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.kotlinuberriderremake.Common.Common
import com.example.kotlinuberriderremake.Model.RiderModel
import com.example.kotlinuberriderremake.Utils.UserUtils
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import io.reactivex.Completable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.delay

class SplashScreenActivity : AppCompatActivity() {

    companion object {
        private const val LOGIN_REQUEST_CODE = 7171
    }

    lateinit var provider: List<AuthUI.IdpConfig>
    lateinit var firebaseAuth: FirebaseAuth
    lateinit var listener: FirebaseAuth.AuthStateListener

    private lateinit var database: FirebaseDatabase
    private lateinit var riderInfoRef: DatabaseReference

    private lateinit var progressBar: ProgressBar
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>

    override fun onStart() {
        super.onStart()
        delaySplashScreen()
    }

    @SuppressLint("CheckResult")
    private fun delaySplashScreen() {
        lifecycleScope.launch {
            delay(3000)
            firebaseAuth.addAuthStateListener(listener)
        }
    }

    override fun onStop() {
        if (firebaseAuth != null && listener != null) firebaseAuth.removeAuthStateListener(listener)
        super.onStop()
    }
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash_screen)
        setupSignInLauncher()
        init()

        }
    private fun init() {
        progressBar = findViewById(R.id.progress_bar)
        database = Firebase.database
        provider = listOf(
            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )
        riderInfoRef = database.getReference(Common.RIDER_INFO_REFERENCE);
        firebaseAuth = FirebaseAuth.getInstance()
        listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            lifecycleScope.launch {
                if (user != null) {
                    FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                        Log.d("myTOKEN", token)
                        UserUtils.updateToken(this@SplashScreenActivity, token)

                        checkUserFromFirebase()
                    }
                }



                else {

                        showLoginLayout()
                    }
                }
            }
    }


    private fun checkUserFromFirebase() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val userId = firebaseAuth.currentUser?.uid ?: run {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SplashScreenActivity, "User ID is null", Toast.LENGTH_SHORT).show()
                        progressBar.visibility = View.GONE
                    }
                    return@launch
                }

                val snapshot = withContext(Dispatchers.IO) {
                    riderInfoRef.child(userId).get().await()
                }

                withContext(Dispatchers.Main) {
                    if (snapshot.exists()) {
                        val model = snapshot.getValue(RiderModel::class.java)
                        goToHomeActivity(model)
                    } else {
                        showRegisterLayout()
                    }
                    progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SplashScreenActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                }
            }
        }
    }
    private fun showLoginLayout() {


        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(provider)
            .setTheme(R.style.Theme_KotlinUberRiderRemake) // Theme tùy chỉnh

            .build()

        signInLauncher.launch(signInIntent)
    }

    private fun setupSignInLauncher() {
        signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val response = IdpResponse.fromResultIntent(result.data)
            if (result.resultCode == Activity.RESULT_OK) {
                val user = firebaseAuth.currentUser
                showToast("Welcome: ${user?.uid}")
                lifecycleScope.launch {
                    checkUserFromFirebase()
                }
            } else {
                handleSignInError(response)
            }
        }
    }
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    private fun handleSignInError(response: IdpResponse?) {
        if (response == null) {
            showToast("Sign in canceled")
        } else {
            val errorMessage = response.error?.message ?: "Unknown error"
            showToast("Sign-in error: $errorMessage")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOGIN_REQUEST_CODE) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                val user = FirebaseAuth.getInstance().currentUser
                checkUserFromFirebase()
            } else {
                Toast.makeText(
                    this@SplashScreenActivity,
                    response!!.error!!.message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    @SuppressLint("MissingInflatedId")
    private fun showRegisterLayout() {
        val builder = AlertDialog.Builder(this, R.style.DialogTheme)
        val itemView = LayoutInflater.from(this).inflate(R.layout.layout_register, null)

        val edtFirstName = itemView.findViewById<TextInputEditText>(R.id.edt_first_name)
        val edtLastName = itemView.findViewById<TextInputEditText>(R.id.edt_last_name)
        val edtPhoneNumber = itemView.findViewById<TextInputEditText>(R.id.edt_phone_number)
        val btnContinue = itemView.findViewById<Button>(R.id.btn_register)

        val currentUser = firebaseAuth.currentUser
        if (currentUser?.phoneNumber != null && !TextUtils.isEmpty(currentUser.phoneNumber)) {
            edtPhoneNumber.setText(currentUser.phoneNumber)
        }

        builder.setView(itemView)
        val dialog = builder.create()
        dialog.show()

        btnContinue.setOnClickListener {
            val firstName = edtFirstName.text.toString().trim()
            val lastName = edtLastName.text.toString().trim()
            val phoneNumber = edtPhoneNumber.text.toString().trim()

            when {
                TextUtils.isEmpty(firstName) -> {
                    Toast.makeText(this, "Please enter First Name", Toast.LENGTH_SHORT).show()
                }
                TextUtils.isEmpty(lastName) -> {
                    Toast.makeText(this, "Please enter Last Name", Toast.LENGTH_SHORT).show()
                }
                TextUtils.isEmpty(phoneNumber) -> {
                    Toast.makeText(this, "Please enter Phone Number", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    progressBar.visibility = View.VISIBLE
                    val model = RiderModel().apply {
                        this.firstName = firstName
                        this.lastName = lastName
                        this.phoneNumber = phoneNumber

                    }

                    lifecycleScope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                val userId = firebaseAuth.currentUser?.uid ?: run {
                                    Toast.makeText(this@SplashScreenActivity, "User not logged in", Toast.LENGTH_SHORT).show()
                                    return@run
                                }
                                riderInfoRef.child(userId.toString()).setValue(model).await()
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@SplashScreenActivity, "Register Successfully!", Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                                goToHomeActivity(model)
                                progressBar.visibility = View.GONE
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@SplashScreenActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                                progressBar.visibility = View.GONE
                            }
                        }
                    }
                }
            }
        }
    }

    private fun goToHomeActivity(model: RiderModel?) {
        Common.currentRider = model // Don't forget it, please !!!
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }


}
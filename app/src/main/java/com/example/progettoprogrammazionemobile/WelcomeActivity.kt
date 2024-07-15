package com.example.progettoprogrammazionemobile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth

class WelcomeActivity : AppCompatActivity() {

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { res ->
        this.onSignInResult(res)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        // Check if the user is already signed in
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // User is signed in, navigate to MainActivity
            navigateToMainActivity(currentUser.displayName)
        } else {
            // User is not signed in, initiate sign-in flow
            startSignIn()
        }
    }

    private fun startSignIn() {
        // Choose authentication providers
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        // Create and launch sign-in intent
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .build()
        signInLauncher.launch(signInIntent)
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == RESULT_OK) {
            // Successfully signed in
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                navigateToMainActivity(user.displayName)
            }
        } else {
            // Sign in failed. If response is null, the user canceled the sign-in flow using the back button.
            Log.e("tag", "Sign-in failed")
        }
    }

    private fun navigateToMainActivity(displayName: String?) {
        val intent = Intent(this, MainActivity::class.java)
        displayName?.let {
            Log.e("tag", it)
            intent.putExtra("USER", it)
        }
        startActivity(intent)
        finish()
    }
}

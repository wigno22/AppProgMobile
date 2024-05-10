package com.example.progettoprogrammazionemobile

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.firebase.ui.auth.AuthUI

class MainActivity : AppCompatActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sign_out : Button = findViewById(R.id.sign_out)
        sign_out.setOnClickListener {
            AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener {
                    val intent = Intent(this, WelcomeActivity::class.java)
                    startActivity(intent)
                }
        }



        val TextName : TextView = findViewById(R.id.textView)
        TextName.setText("Benvenuto " + intent.extras?.getString("USER") )

    }


}
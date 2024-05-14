package com.example.progettoprogrammazionemobile

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.firebase.ui.auth.AuthUI
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore


data class Account (val nome: String, val valore: Double, val direction: Boolean )

class MainActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    @SuppressLint("SetTextI18n", "MissingInflatedId")
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



        val transaction: Button = findViewById(R.id.transaction_button)

        transaction.setOnClickListener {
            val currentUser = firebaseAuth.currentUser

            val CountValue = db.collection(currentUser.toString()).document("my_Account")

            CountValue.update("grade", FirebaseFirestore.FieldValue.increment(10))
        }



        db = Firebase.firestore
        firebaseAuth = FirebaseAuth.getInstance()

        val AccountList = mutableListOf<Account>()
        AccountList.add(Account("ProgMob", 10.5 , true))

        val grades = hashMapOf("Account" to AccountList )

        //test Content-owner only access
        db.collection(firebaseAuth.currentUser!!.uid).document("my_Account").set(grades)
    }




}


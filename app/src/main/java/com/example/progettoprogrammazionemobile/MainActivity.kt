package com.example.progettoprogrammazionemobile

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import com.firebase.ui.auth.AuthUI
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import java.lang.Math.random
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.*


data class AccountDetails (val iDNumber: String , val IBAN: String, val Balance: Number){}
data class Transaction (val iDNumber: String , val Valore: Number, val Type: Boolean, val Category: String, val Description: String, val Data: String ){}

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
        /*
                transaction.setOnClickListener {
                    val currentUser = firebaseAuth.currentUser

                    val CountValue = db.collection(currentUser.toString()).document("my_Account")

                    //CountValue.update("grade", FirebaseFirestore.FieldValue.increment(10))
                }
        */


        db = Firebase.firestore
        firebaseAuth = FirebaseAuth.getInstance()


        val codiceConto = SecureRandom().nextInt(90000000) + 10000000

        fun generateIban(random: SecureRandom): String {
            val countryCode = "IT"
            val bankCode = "12345"
            val accountNumber = codiceConto
            val controlDigits = random.nextInt(900) + 100
            return "$countryCode$controlDigits$bankCode$accountNumber"
        }

        val user = firebaseAuth.currentUser;
        val UID = user!!.uid
        val iban = generateIban(SecureRandom())
        var Date = LocalDateTime.now()

        val categorieValide = listOf("food", "transport", "shopping", "service", "entertainment", "salary", "household expenses", "subscription")
       // val AccountDetails = mutableListOf<AccountDetails>()
       // AccountDetails.add(AccountDetails(user, iban, 0))

        val Transactions = hashMapOf<String,Transaction>()

        val categoria = "food" // Questo dovrebbe essere il valore che stai cercando di impostare
        if (categoria in categorieValide) {

            Transactions.put(Date.toString() ,Transaction( UID ,3, false , categoria, " ", Date.toString() ))
            Date = Date.plusDays(2)
            Transactions.put(Date.toString() ,Transaction( UID ,4, false , categoria, " ", Date.toString() ))

        }
        else
        {
            Log.w(TAG, "Categoria non valida: $categoria")
        }


        Transactions.map {
            db.collection(firebaseAuth.currentUser!!.uid).document("Account").collection("Transaction").document(it.key).set(it.value)
        }

        db.collection(firebaseAuth.currentUser!!.uid).document("Account").set(AccountDetails(UID, iban, 0))
    }


}


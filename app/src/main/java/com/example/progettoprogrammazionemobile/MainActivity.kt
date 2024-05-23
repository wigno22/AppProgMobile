package com.example.progettoprogrammazionemobile

import android.annotation.SuppressLint
import android.content.ClipData.Item
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation.findNavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.security.SecureRandom
import java.time.LocalDateTime
import java.lang.Override as Override

data class AccountDetails(val iDNumber: String, val IBAN: String, val Balance: Number)
//data class Transaction(val iDNumber: String, val Valore: Number, val Type: Boolean, val Category: String, val Description: String, val Data: String)

class profilefrg: Fragment(R.layout.fragment_profile)
class accountfrg: Fragment(R.layout.fragment_account)
class operationfrg: Fragment(R.layout.fragment_operation)
class MainActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    @SuppressLint("SetTextI18n", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.nav_view)
        bottomNavigationView.setupWithNavController(navController)



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

        /*
        val user = firebaseAuth.currentUser
        val UID = user!!.uid
        val iban = generateIban(SecureRandom())
        var date = LocalDateTime.now()

        val categorieValide = listOf("food", "transport", "shopping", "service", "entertainment", "salary", "household expenses", "subscription")

        val transactions = hashMapOf<String, Transaction>()

        val categoria = "food"
        if (categoria in categorieValide) {
            transactions[date.toString()] = Transaction(UID, 3, false, categoria, " ", date.toString())
            date = date.plusDays(2)
            transactions[date.toString()] = Transaction(UID, 4, false, categoria, " ", date.toString())
        } else {
            Log.w("MainActivity", "Categoria non valida: $categoria")
        }

        transactions.map {
            db.collection(firebaseAuth.currentUser!!.uid).document("Account").collection("Transaction").document(it.key).set(it.value)
        }

        db.collection(firebaseAuth.currentUser!!.uid).document("Account").set(AccountDetails(UID, iban, 0))
        */

    }

}
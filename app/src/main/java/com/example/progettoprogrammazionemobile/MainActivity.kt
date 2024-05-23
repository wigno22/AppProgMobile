package com.example.progettoprogrammazionemobile

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView


data class AccountDetails(val iDNumber: String, val IBAN: String, val Balance: Number)
//data class Transaction(val iDNumber: String, val Valore: Number, val Type: Boolean, val Category: String, val Description: String, val Data: String)

class profilefrg: Fragment(R.layout.fragment_profile)
class accountfrg: Fragment(R.layout.fragment_account)
class operationfrg: Fragment(R.layout.fragment_operation)
class MainActivity : AppCompatActivity() {



    @SuppressLint("SetTextI18n", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.nav_view)
        bottomNavigationView.setupWithNavController(navController)





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
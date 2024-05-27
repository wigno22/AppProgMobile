package com.example.progettoprogrammazionemobile

import android.annotation.SuppressLint
import android.graphics.Color
import android.icu.text.NumberFormat
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*

data class AccountDetail(
    val uid: String = "",
    val iban: String = "",
    val balance: Double = 0.0
)

class AccountFragment : Fragment() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Firebase.firestore
        firebaseAuth = FirebaseAuth.getInstance()



        // Create account if it doesn't exist
        createAccountIfNotExists()
    }

    private fun createAccountIfNotExists() {
        val user = firebaseAuth.currentUser
        if (user != null) {
            val UID = user.uid
            val userDocRef = db.collection(UID).document("Account")

            userDocRef.get()
                .addOnSuccessListener { documentSnapshot ->
                    if (!documentSnapshot.exists()) {
                        val codiceConto = SecureRandom().nextInt(90000000) + 10000000

                        fun generateIban(random: SecureRandom): String {
                            val countryCode = "IT"
                            val bankCode = "12345"
                            val accountNumber = codiceConto
                            val controlDigits = random.nextInt(900) + 100
                            return "$countryCode$controlDigits$bankCode$accountNumber"
                        }

                        val iban = generateIban(SecureRandom())
                        val accountDetails = AccountDetail(UID, iban, 0.0)

                        userDocRef.set(accountDetails)
                            .addOnSuccessListener {
                                Log.d("AccountFragment", "Account successfully created!")
                            }
                            .addOnFailureListener { e ->
                                Log.w("AccountFragment", "Error creating account", e)
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("AccountFragment", "Error fetching account document", e)
                }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_account, container, false)

        val textName: TextView = rootView.findViewById(R.id.welcomeText)
        val textBalance: TextView = rootView.findViewById(R.id.textView)
        val gridTransactions: GridView = rootView.findViewById(R.id.idGVDati)

        val user = FirebaseAuth.getInstance().currentUser

        user?.let {
            val name = user.displayName
            textName.text = "Benvenuto $name"

        }

        val UID = user?.uid
        if (UID != null) {
            val userDocRef = db.collection(UID).document("Account")
            userDocRef.get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val balance = documentSnapshot.getDouble("balance")

                        val numberFormat = NumberFormat.getNumberInstance(Locale.ITALY)
                        val formattedAmount = numberFormat.format(balance)

                        // Set the text color based on the amount
                        if (balance!! <0.0) {
                            textBalance.setTextColor(Color.RED)
                        } else {
                            textBalance.setTextColor(Color.GREEN)
                        }



                        textBalance.text =  "$formattedAmount"

                        Log.e("AccountFragment", "Balance: $balance")
                    } else {
                        Log.e("AccountFragment", "Account document does not exist.")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("AccountFragment", "Failed to fetch account details: ${exception.message}")
                }

            // Fetch the last 5 transactions
            val transactionsRef = userDocRef.collection("Transaction")
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(150)

            transactionsRef.get()
                .addOnSuccessListener { querySnapshot ->
                    val transactions = mutableListOf<Transaction>()
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    for (document in querySnapshot.documents) {
                        var amount = document.getDouble("amount") ?: 0.0
                        val isOutgoing = document.getBoolean("outgoing")
                        val category = document.getString("category") ?: "Unknown"
                        val type = if (isOutgoing!!) "Uscita" else "Entrata"
                        if (isOutgoing!!) amount = amount*-1 else amount = amount*1
                        val date = document.getDate("date")?.let { dateFormat.format(it) } ?: "Unknown"



                        transactions.add(Transaction(date, type , amount , category))
                    }

                    val adapter = TransactionAdapter(requireContext(), transactions)
                    gridTransactions.adapter = adapter
                }
                .addOnFailureListener { exception ->
                    Log.e("AccountFragment", "Failed to fetch transactions: ${exception.message}")
                }
        } else {
            Log.e("AccountFragment", "User not authenticated")
        }

        return rootView
    }
}

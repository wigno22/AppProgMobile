package com.example.progettoprogrammazionemobile

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.security.SecureRandom

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
        val textTransactions: TextView = rootView.findViewById(R.id.textTransactions)

        val user = firebaseAuth.currentUser
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

                        textBalance.text = "Your Current Balance: $balance "

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

            var isOutgoing = " + "
            transactionsRef.get()
                .addOnSuccessListener { querySnapshot ->
                    val transactionsText = StringBuilder()
                    for (document in querySnapshot.documents) {
                        val amount = document.getDouble("amount")
                        isOutgoing = if (document.getBoolean("isOutgoing") == true) " - " else " + "

                        val category = document.getString("category")
                        transactionsText.append("Amount: $isOutgoing $amount, Category: $category \n")
                        Log.e("isOutgoing", "isOutgoing: $isOutgoing")
                    }

                    textTransactions.text = transactionsText.toString()

                    textTransactions.isVerticalScrollBarEnabled = true
                    textTransactions.isHorizontalScrollBarEnabled = true
                    textTransactions.movementMethod = android.text.method.ScrollingMovementMethod()
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

package com.example.progettoprogrammazionemobile

import android.annotation.SuppressLint
import android.graphics.Color
import android.icu.text.NumberFormat
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.GridView
import android.widget.Spinner
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*
import com.example.progettoprogrammazionemobile.databinding.FragmentAccountBinding

data class AccountDetail(
    val uid: String = "",
    val iban: String = "",
    val balance: Double = 0.0
)


class AccountFragment : Fragment() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var MonthSelection: Spinner
    private lateinit var YearSelection: Spinner
    private lateinit var gridTransactions: GridView

    private val viewModel: DataViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Firebase.firestore
        firebaseAuth = FirebaseAuth.getInstance()
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
        val binding: FragmentAccountBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_account, container, false
        )

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        val rootView = binding.root
        val textBalance: TextView = rootView.findViewById(R.id.textView)

        gridTransactions = rootView.findViewById(R.id.idGVDati)
        MonthSelection = rootView.findViewById(R.id.spinnerMonthAccount)
        YearSelection = rootView.findViewById(R.id.spinnerYearAccount)

        val user = FirebaseAuth.getInstance().currentUser
        val name = user?.displayName

        viewModel.setUsername("Welcome: $name")

        createAccountIfNotExists()

        val UID = user?.uid
        if (UID != null) {
            val userDocRef = db.collection(UID).document("Account")
            userDocRef.get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val balance = documentSnapshot.getDouble("balance") ?: 0.0
                        viewModel.setBalance(balance)

                        if (balance < 0.0) {
                            textBalance.setTextColor(Color.RED)
                        } else {
                            textBalance.setTextColor(Color.GREEN)
                        }


                        Log.e("AccountFragment", "Balance: $balance")
                    } else {
                        Log.e("AccountFragment", "Account document does not exist.")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("AccountFragment", "Failed to fetch account details: ${exception.message}")
                }

            val months = listOf("All", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
            val monthAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, months)
            monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            MonthSelection.adapter = monthAdapter

            MonthSelection.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    filterTransactions()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

            val years = getYearsList()
            val yearAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, years)
            yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            YearSelection.adapter = yearAdapter

            YearSelection.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    filterTransactions()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

            fetchTransactions()
        } else {
            Log.e("AccountFragment", "User not authenticated")
        }

        viewModel.balance.observe(viewLifecycleOwner, androidx.lifecycle.Observer { newBalance ->
            textBalance.text = NumberFormat.getCurrencyInstance().format(newBalance)

            if (newBalance < 0.0) {
                textBalance.setTextColor(Color.RED)
            } else {
                textBalance.setTextColor(Color.GREEN)
            }
        })


        viewModel.balance.observe(viewLifecycleOwner) { newBalance ->
            if (UID != null) {
                db.collection(UID).document("Account")
                    .update("balance", newBalance)
            }
        }

        return rootView
    }


    private fun fetchTransactions() {
        val user = FirebaseAuth.getInstance().currentUser
        val UID = user?.uid
        if (UID != null) {
            val userDocRef = db.collection(UID).document("Account")
            val transactionsRef = userDocRef.collection("Transaction")
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(150)

            transactionsRef.get()
                .addOnSuccessListener { querySnapshot ->
                    val transactions = mutableListOf<Transaction>()
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    var balance = 0.0
                    for (document in querySnapshot.documents) {
                        var amount = document.getDouble("amount") ?: 0.0
                        val isOutgoing = document.getBoolean("outgoing") ?: false
                        val category = document.getString("category") ?: "Unknown"
                        val type = if (isOutgoing) "Uscita" else "Entrata"
                        if (isOutgoing) amount *= -1 else amount *= 1
                        val date = document.getDate("date")?.let { dateFormat.format(it) } ?: "Unknown"

                        transactions.add(Transaction(date, type, amount, category))
                        balance += amount
                    }

                    viewModel.setBalance(balance)  // Aggiorna il saldo nel ViewModel

                    val adapter = TransactionAdapter(requireContext(), transactions)
                    gridTransactions.adapter = adapter
                }
                .addOnFailureListener { exception ->
                    Log.e("AccountFragment", "Failed to fetch transactions: ${exception.message}")
                }
        }
    }

    private fun filterTransactions() {
        val selectedMonth = MonthSelection.selectedItemPosition
        val selectedYear = YearSelection.selectedItem.toString().toIntOrNull()

        val user = FirebaseAuth.getInstance().currentUser
        val UID = user?.uid
        if (UID != null && selectedYear != null) {
            val userDocRef = db.collection(UID).document("Account")
            var transactionsRef = userDocRef.collection("Transaction")
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(150)

            if (selectedMonth > 0) {
                val calendar = Calendar.getInstance()
                calendar.set(selectedYear, selectedMonth - 1, 1, 0, 0, 0)
                val startDate = calendar.time
                calendar.set(selectedYear, selectedMonth, 1, 0, 0, 0)
                calendar.add(Calendar.DATE, -1)
                val endDate = calendar.time

                transactionsRef = transactionsRef
                    .whereGreaterThanOrEqualTo("date", startDate)
                    .whereLessThanOrEqualTo("date", endDate)
            } else {
                val startDate = GregorianCalendar(selectedYear, Calendar.JANUARY, 1).time
                val endDate = GregorianCalendar(selectedYear, Calendar.DECEMBER, 31).time

                transactionsRef = transactionsRef
                    .whereGreaterThanOrEqualTo("date", startDate)
                    .whereLessThanOrEqualTo("date", endDate)
            }

            transactionsRef.get()
                .addOnSuccessListener { querySnapshot ->
                    val transactions = mutableListOf<Transaction>()
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    for (document in querySnapshot.documents) {
                        var amount = document.getDouble("amount") ?: 0.0
                        val isOutgoing = document.getBoolean("outgoing")
                        val category = document.getString("category") ?: "Unknown"
                        val type = if (isOutgoing!!) "Uscita" else "Entrata"
                        if (isOutgoing) amount *= -1 else amount *= 1
                        val date = document.getDate("date")?.let { dateFormat.format(it) } ?: "Unknown"

                        transactions.add(Transaction(date, type, amount, category))
                    }

                    val adapter = TransactionAdapter(requireContext(), transactions)
                    gridTransactions.adapter = adapter
                }
                .addOnFailureListener { exception ->
                    Log.e("AccountFragment", "Failed to fetch filtered transactions: ${exception.message}")
                }
        }
    }
}

fun getYearsList(): List<String> {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    return (currentYear downTo (currentYear - 10)).map { it.toString() }
}

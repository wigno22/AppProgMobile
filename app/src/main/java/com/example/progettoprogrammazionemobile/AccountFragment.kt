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
import androidx.core.content.ContextCompat
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


//Classe generica per ogni utente
data class AccountDetail(
    val uid: String = "",
    val iban: String = "",
    val balance: Double = 0.0
)

class AccountFragment : Fragment() {
    //variabili che mi servono per accedere ad authentication e db
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    //variabili per utilizzare binding e viewmodel
    private val viewModel: DataViewModel by viewModels()
    private lateinit var binding: FragmentAccountBinding

    //alla creazione mi salvo db e auth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Firebase.firestore
        firebaseAuth = FirebaseAuth.getInstance()
    }

    //creo il conto dell'utente se è la prima volta che fa l'accesso
    private fun createAccountIfNotExists() {
        val user = firebaseAuth.currentUser
        if (user != null) {
            //accedo alla sua raccolta (univoca) nel db
            val UID = user.uid
            val userDocRef = db.collection(UID).document("Account")

            userDocRef.get()
                .addOnSuccessListener { documentSnapshot ->
                    if (!documentSnapshot.exists()) {

                        //simulo la creazione di un conto (codice, iban, saldo (a 0))
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
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_account, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        val rootView = binding.root
        val user = FirebaseAuth.getInstance().currentUser
        val name = user?.displayName

        //con il view model recupero nome dell' utente e lo stampo
        viewModel.setUsername("Welcome: $name")

        createAccountIfNotExists()

        val UID = user?.uid
        if (UID != null) {
            val userDocRef = db.collection(UID).document("Account")
            userDocRef.get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val balance = documentSnapshot.getDouble("balance") ?: 0.0
                        //con viewmodel stampo anche il saldo, recuperato da db
                        viewModel.setBalance(balance)

                        //cambio colore in base al valore del saldo
                        if (balance < 0.0) {
                            binding.textView.setTextColor(Color.RED)
                        } else {
                            binding.textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.verdescuro))
                        }

                        Log.e("AccountFragment", "Balance: $balance")
                    } else {
                        Log.e("AccountFragment", "Account document does not exist.")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("AccountFragment", "Failed to fetch account details: ${exception.message}")
                }

            //dichiaro la lista dei mesi e l'adapter per far scegliere all'utente i mesi dei quali vuole vedere le transazioni
            val months = listOf("All", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
            val monthAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, months)
            monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerMonthAccount.adapter = monthAdapter

            binding.spinnerMonthAccount.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    //quando seleziono un item nello spinner filtro le transazioni
                    filterTransactions()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

            //dichiaro la lista degli anni e l'adapter per far scegliere all'utente gli anni dei quali vuole vedere le transazioni
            //ho una funzione che mi prende fino a 10 anni primna
            val years = getYearsListWithAll()
            val yearAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, years)
            yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerYearAccount.adapter = yearAdapter

            binding.spinnerYearAccount.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    //filtro transazioni
                    filterTransactions()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

            //stampo transazioni
            fetchTransactions()
        } else {
            Log.e("AccountFragment", "User not authenticated")
        }

        //quando cambia saldo e lo voglio stampare cambio colore a seconda del valore
        viewModel.balance.observe(viewLifecycleOwner, androidx.lifecycle.Observer { newBalance ->
            binding.textView.text = NumberFormat.getCurrencyInstance().format(newBalance)

            if (newBalance < 0.0) {
                binding.textView.setTextColor(Color.RED)
            } else {
                binding.textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.verdescuro))
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

    //funzione per stampare le transazioni
    private fun fetchTransactions() {
        //le transazioni sono salvate in una raccolta all'interno dell'account di ogni utente
        val user = FirebaseAuth.getInstance().currentUser
        val UID = user?.uid
        if (UID != null) {
            val userDocRef = db.collection(UID).document("Account")
            val transactionsRef = userDocRef.collection("Transaction")
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(150)


            transactionsRef.get().addOnSuccessListener { querySnapshot ->
                    val transactions = mutableListOf<Transaction>()
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    var balance = 0.0
                    //scorro tutti i documenti per ottenere il saldo corretto poichè sarà dato dalla somma di tutte le transazioni
                    for (document in querySnapshot.documents) {
                        var amount = document.getDouble("amount") ?: 0.0
                        val isOutgoing = document.getBoolean("outgoing") ?: false
                        val category = document.getString("category") ?: "Unknown"
                        val type = if (isOutgoing) "Uscita" else "Entrata"
                        if (isOutgoing) amount *= -1 else amount *= 1
                        val date = document.getDate("date")?.let { dateFormat.format(it) } ?: "Unknown"

                        //aggiungo tutte le transazioni ad una lista
                        transactions.add(Transaction(date, type, amount, category))
                        balance += amount
                    }

                    viewModel.setBalance(balance)  // Aggiorna il saldo nel ViewModel

                    //per stampare correttamente tutte le transazioni passo la lista delle transazioni a questa funzione
                    val adapter = TransactionAdapter(requireContext(), transactions)
                    binding.idGVDati.adapter = adapter
                }
                .addOnFailureListener { exception ->
                    Log.e("AccountFragment", "Failed to fetch transactions: ${exception.message}")
                }
        }
    }

    //filtro le transazioni in base alla scelta effettuata negli spinner
    private fun filterTransactions() {
        val selectedMonth = binding.spinnerMonthAccount.selectedItemPosition
        val selectedYearString =  binding.spinnerYearAccount.selectedItem.toString()

        val user = FirebaseAuth.getInstance().currentUser
        val UID = user?.uid
        if (UID != null) {
            val userDocRef = db.collection(UID).document("Account")
            var transactionsRef = userDocRef.collection("Transaction")
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(150)

            if (selectedYearString == "All") {
                // Non applico filtri per l'anno
                //guardo se è selezionato un mese
                if (selectedMonth > 0) {
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.MONTH, selectedMonth - 1)
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    val startDate = calendar.time
                    calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                    val endDate = calendar.time
                    //filtro
                    transactionsRef = transactionsRef
                        .whereGreaterThanOrEqualTo("date", startDate)
                        .whereLessThanOrEqualTo("date", endDate)
                }
            } else {
                val selectedYear = selectedYearString.toInt()
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
            }

            //dopo aver filtrato per tutti i casi, salvo in transactioRef le transazioni che voglio visualizzare
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

                    //richiamo la funzione che stampa
                    val adapter = TransactionAdapter(requireContext(), transactions)
                    binding.idGVDati.adapter = adapter
                }
                .addOnFailureListener { exception ->
                    Log.e("AccountFragment", "Failed to fetch filtered transactions: ${exception.message}")
                }
        }
    }
}

fun getYearsListWithAll(): List<String> {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val years = (currentYear downTo (currentYear - 10)).map { it.toString() }.toMutableList()
    years.add(0, "All")
    return years
}

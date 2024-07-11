package com.example.progettoprogrammazionemobile

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.ContentValues.TAG
import android.graphics.Color
import android.icu.text.NumberFormat
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.ekn.gruzer.gaugelibrary.MultiGauge
import com.ekn.gruzer.gaugelibrary.Range
import com.example.progettoprogrammazionemobile.databinding.FragmentOperationBinding
import com.example.progettoprogrammazionemobile.databinding.FragmentProfileBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.type.DateTime
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class UserTransaction(
    val uid: String = "",
    val amount: Double = 0.0,
    val outgoing: Boolean = true,
    val category: String = "",
    val notes: String = "",
    val date: Date = Date()
)

class OperationFragment : Fragment() {


    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var binding: FragmentOperationBinding

    private val categoriesAll = listOf("food", "transport", "shopping", "service", "entertainment", "household expenses", "subscription")
    private val categoriesIncome = listOf("salary", "other")

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_operation, container, false)
        binding.lifecycleOwner = viewLifecycleOwner


        // Ottengo la data corrente
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val currentDate = dateFormat.format(calendar.time)

        //imposto data corrente come predefinita
        binding.dateEditText.setText(currentDate)


        //Imposto le categorie corrette sullo spinner a seconda del radio button selezionato
        updateCategorySpinner(categoriesIncome)

        binding.radiogroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.buttonplus -> updateCategorySpinner(categoriesIncome)
                R.id.buttonminus -> updateCategorySpinner(categoriesAll)
            }
        }


        //faccio selezionare all'utente i mesi di cui vuole vedere il saldo
        val months = listOf("All", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        val monthAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMonth.adapter = monthAdapter

        binding.spinnerMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                //aggiorno grafico
                updateGauges()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }

        //faccio selezionare all'utente gli anni di cui vuole vedere il saldo
        val years = getYearsList()
        val yearAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, years)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerYear.adapter =yearAdapter

        binding.spinnerYear.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                updateGauges()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }


        //gestisco cambio di data
        binding.dateEditText.setOnClickListener {
            showDatePickerDialog()
        }


        // Confermo transazione
        binding.buttonConfirm.setOnClickListener {
            addTransaction()
        }

        // aggiungo i 3 colori al gauge e range
        val positiveRange = Range().apply {
            color = Color.parseColor("#00b20b")
            from = 0.0
            to = 1000000.0
        }

        val negativeRange = Range().apply {
            color = Color.parseColor("#ce0000")
            from = 0.0
            to = 1000000.0
        }

        val deltaRange = Range().apply {
            color = Color.parseColor("#1976D2")
            from = 0.0
            to = 1000000.0
        }

        binding.multiGauge.addRange(positiveRange) // Green for positive transactions
        binding.multiGauge.addSecondRange(negativeRange) // Red for negative transactions
        binding.multiGauge.addThirdRange(deltaRange)

        //imposto valori predefiniti dei 3 cerchi
        binding.multiGauge.minValue = 0.0
        binding.multiGauge.maxValue = 100000.0
        binding.multiGauge.value = 0.0
        binding.multiGauge.secondMinValue = 0.0
        binding.multiGauge.secondMaxValue = 100000.0
        binding.multiGauge.secondValue = 0.0
        binding.multiGauge.thirdMinValue = 0.0
        binding.multiGauge.thirdMaxValue = 100000.0
        binding.multiGauge.thirdValue = 0.0


        return binding.root
    }

   //funzione per impostare la categoria in base al button
    private fun updateCategorySpinner(categories: List<String>) {
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = categoryAdapter
    }

    //selezione data
    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(requireContext(),
            { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
                val selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                binding.dateEditText.setText(selectedDate)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    //registro transazione una volta confermata
    private fun addTransaction() {
        val user = firebaseAuth.currentUser
        if (user != null) {
            val UID = user.uid

            val amount = binding.editTextAmount.text.toString().toDoubleOrNull()
            var isOutgoing = binding.radiogroup.checkedRadioButtonId == R.id.buttonminus
            val category = binding.spinnerCategory.selectedItem.toString()
            val description = binding.editTextDescription.text.toString()
            val date = binding.dateEditText.text.toString()

            val currentDateTime = LocalDateTime.now()
            // Parsa la data dalla stringa 'date' e ottieni un oggetto LocalDateTime
            val parsedDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("d/M/yyyy"))
            // Crea un nuovo oggetto LocalDateTime che contenga la data di 'date' e l'ora, i minuti, i secondi e i millisecondi da 'currentDateTime'
            val newDateTime = LocalDateTime.of(parsedDate, currentDateTime.toLocalTime())
            // Converti il nuovo oggetto LocalDateTime in un oggetto Date
            val formattedDateTime = Date.from(newDateTime.atZone(ZoneId.systemDefault()).toInstant())


            if (amount != null && category.isNotEmpty()) {
                val userTransaction = UserTransaction(
                    uid = UID,
                    amount = amount,
                    outgoing = isOutgoing,
                    category = category,
                    notes = description,
                    date = formattedDateTime
                )

                db.runTransaction { transaction ->
                    val accountRef = db.collection(UID).document("Account")
                    val snapshot = transaction.get(accountRef)
                    val currentBalance = snapshot.getDouble("balance") ?: 0.0

                    val newBalance = if (isOutgoing) {
                        currentBalance - amount
                    } else {
                        currentBalance + amount
                    }
                    transaction.update(accountRef, "balance", newBalance)

                    val transactionRef = accountRef.collection("Transaction").document(formattedDateTime.toString())
                    transaction.set(transactionRef, userTransaction)

                    newBalance
                }.addOnSuccessListener { newBalance ->
                    Toast.makeText(context, "Transaction added. New balance: $newBalance", Toast.LENGTH_SHORT).show()
                    updateGauges()
                }.addOnFailureListener { e ->
                    Log.w(TAG, "Transaction failed.", e)
                    Toast.makeText(context, "Transaction failed. Please try again.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Please enter a valid amount and category.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //funzione per aggiornare grafico
    private fun updateGauges() {
        val user = firebaseAuth.currentUser
        if (user != null) {
            val UID = user.uid
            val selectedMonth = binding.spinnerMonth.selectedItem.toString()
            val selectedYear = binding.spinnerYear.selectedItem.toString()

            val transactionsRef = db.collection(UID).document("Account").collection("Transaction")

            //in base alla scelta nello spinner seleziono su quali transazioni calcolare saldo
            val query = if (selectedMonth == "All" && selectedYear == "All") {
                transactionsRef
            } else if (selectedMonth == "All") {
                val year = selectedYear.toInt()
                transactionsRef.whereGreaterThanOrEqualTo("date", getStartOfYear(year))
                    .whereLessThan("date", getStartOfNextYear(year))
            } else if (selectedYear == "All") {
                val monthIndex = Month.valueOf(selectedMonth.uppercase(Locale.ROOT)).value
                transactionsRef.whereEqualTo("month", monthIndex)
            } else {
                val year = selectedYear.toInt()
                val monthIndex = Month.valueOf(selectedMonth.uppercase(Locale.ROOT)).value
                transactionsRef.whereGreaterThanOrEqualTo("date", getStartOfMonth(monthIndex, year))
                    .whereLessThan("date", getStartOfNextMonth(monthIndex, year))
            }

            query.get().addOnSuccessListener { documents ->
                var totalPositive = 0.0
                var totalNegative = 0.0

                for (document in documents) {
                    val transaction = document.toObject(UserTransaction::class.java)
                    //se outgoing è true aggiungo il valore della transazione a quelle negative viceversa se è false
                    if (transaction.outgoing) {
                        totalNegative += transaction.amount
                    } else {
                        totalPositive += transaction.amount
                    }
                }

                //stampo il valore del amount positivo, negativo e della differenza
                val numberFormat = NumberFormat.getNumberInstance(Locale.ITALY)
                binding.totaleEntrate.text = numberFormat.format(totalPositive)
                binding.totaleUscite.text = numberFormat.format(totalNegative)
                binding.totaleDelta.text = numberFormat.format(totalPositive - totalNegative)

                // Grandezza del cerchio colorato è proporzionale al valore più grande
                binding.multiGauge.value = totalPositive
                binding.multiGauge.secondValue = totalNegative
                binding.multiGauge.thirdValue = totalPositive - totalNegative

                if (totalPositive > totalNegative) {
                    binding.multiGauge.maxValue = totalPositive * 1.1
                    binding.multiGauge.secondMaxValue = totalPositive * 1.1
                    binding.multiGauge.thirdMaxValue = totalPositive * 1.1
                } else {
                    binding.multiGauge.maxValue = totalNegative * 1.1
                    binding.multiGauge.secondMaxValue = totalNegative * 1.1
                    binding.multiGauge.thirdMaxValue = totalNegative * 1.1
                }
            }.addOnFailureListener { e ->
                Toast.makeText(context, "Failed to load transactions: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    //funzioni per la gestione della selezione di mesi e anni
    private fun getStartOfMonth(month: Int, year: Int): Date {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.time
    }

    private fun getStartOfNextMonth(month: Int, year: Int): Date {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.time
    }

    private fun getStartOfYear(year: Int): Date {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, 0)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.time
    }

    private fun getStartOfNextYear(year: Int): Date {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year + 1)
            set(Calendar.MONTH, 0)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.time
    }

    //lista anni
    fun getYearsList(): List<String> {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        return listOf("All") + (currentYear downTo (currentYear - 10)).map { it.toString() }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateGauges()
    }

}

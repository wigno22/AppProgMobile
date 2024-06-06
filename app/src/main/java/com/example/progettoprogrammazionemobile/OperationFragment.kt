package com.example.progettoprogrammazionemobile

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.graphics.Color
import android.icu.text.NumberFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.ekn.gruzer.gaugelibrary.MultiGauge
import com.ekn.gruzer.gaugelibrary.Range
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

    private lateinit var editTextAmount: EditText
    private lateinit var spinnerType: Spinner
    private lateinit var editTextDescription: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var buttonConfirm: Button
    private lateinit var vmultiGauge: MultiGauge
    private lateinit var totEntrate: TextView
    private lateinit var totUscite: TextView
    private lateinit var totDelta: TextView
    private lateinit var spinnerMonth: Spinner
    private lateinit var spinnerYear: Spinner
    lateinit var dateEditText: TextInputEditText



    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Initialize UI components
        val view = inflater.inflate(R.layout.fragment_operation, container, false)

        editTextAmount = view.findViewById(R.id.editTextAmount)
        spinnerType = view.findViewById(R.id.spinnerType)
        editTextDescription = view.findViewById(R.id.editTextDescription)
        spinnerCategory = view.findViewById(R.id.spinnerCategory)
        buttonConfirm = view.findViewById(R.id.buttonConfirm)
        totUscite = view.findViewById(R.id.totaleUscite)
        totEntrate = view.findViewById(R.id.totaleEntrate)
        totDelta = view.findViewById(R.id.totaleDelta)
        spinnerMonth = view.findViewById(R.id.spinnerMonth)
        spinnerYear = view.findViewById(R.id.spinnerYear)
        dateEditText = view.findViewById(R.id.dateEditText)
        vmultiGauge = view.findViewById(R.id.multiGauge)

        // Set up the spinner for transaction type (+/-)
        val types = listOf("+", "-")
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerType.adapter = typeAdapter

        // Set up the spinner with categories
        val categories = listOf("food", "transport", "shopping", "service", "entertainment", "salary", "household expenses", "subscription")
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter


        val months = listOf("All", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        val monthAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMonth.adapter = monthAdapter

        spinnerMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                updateGauges()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }

        val years = getYearsList()
        val yearAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, years)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerYear.adapter =yearAdapter

        spinnerYear.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                updateGauges()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }


        dateEditText.setOnClickListener {
            showDatePickerDialog()
        }


        // Confirm button to add transaction
        buttonConfirm.setOnClickListener {
            addTransaction()
        }

        // Add color ranges to gauge
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
            color = Color.parseColor("#E3E500")
            from = 0.0
            to = 1000000.0
        }

        vmultiGauge.addRange(positiveRange) // Green for positive transactions
        vmultiGauge.addSecondRange(negativeRange) // Red for negative transactions
        vmultiGauge.addThirdRange(deltaRange)

        // Set min, max, and initial values
        vmultiGauge.minValue = 0.0
        vmultiGauge.maxValue = 100000.0
        vmultiGauge.value = 0.0
        vmultiGauge.secondMinValue = 0.0
        vmultiGauge.secondMaxValue = 100000.0
        vmultiGauge.secondValue = 0.0
        vmultiGauge.thirdMinValue = 0.0
        vmultiGauge.thirdMaxValue = 100000.0
        vmultiGauge.thirdValue = 0.0


        return view
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(requireContext(),
            { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
                val selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                dateEditText.setText(selectedDate)
            },
            year,
            month,
            day
        )

        datePickerDialog.show()
    }
    private fun addTransaction() {
        val user = firebaseAuth.currentUser
        if (user != null) {
            val UID = user.uid
            val amount = editTextAmount.text.toString().toDoubleOrNull()
            val type = spinnerType.selectedItem.toString()
            val category = spinnerCategory.selectedItem.toString()
            val description = editTextDescription.text.toString()
            val date = dateEditText.text.toString() // Ottieni la data dal campo dateEditText

            val currentDateTime = LocalDateTime.now()

            // Parsa la data dalla stringa 'date' e ottieni un oggetto LocalDateTime

            val parsedDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("d/M/yyyy"))

            // Crea un nuovo oggetto LocalDateTime che contenga la data di 'date' e l'ora, i minuti, i secondi e i millisecondi da 'currentDateTime'
            val newDateTime = LocalDateTime.of(parsedDate, currentDateTime.toLocalTime())

            // Converti il nuovo oggetto LocalDateTime in un oggetto Date
            val formattedDateTime = Date.from(newDateTime.atZone(ZoneId.systemDefault()).toInstant())


            if (amount != null && category.isNotEmpty()) {
                val isOutgoing = type == "-"
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

                    null
                }.addOnSuccessListener {
                    Toast.makeText(context, "Transaction added successfully", Toast.LENGTH_SHORT).show()
                    resetFields()
                    updateGauges()
                }.addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to add transaction: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Please enter a valid amount and select a category", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateGauges() {
        val user = firebaseAuth.currentUser
        if (user != null) {
            val UID = user.uid
            val selectedMonth = spinnerMonth.selectedItem.toString()
            val selectedYear = spinnerYear.selectedItem.toString().toInt()
            val transactionsRef = db.collection(UID).document("Account").collection("Transaction")

            val query = if (selectedMonth == "All") {
                transactionsRef.whereGreaterThanOrEqualTo("date", getStartOfYear(selectedYear))
                    .whereLessThan("date", getStartOfNextYear(selectedYear))
            } else {
                val monthIndex = Month.valueOf(selectedMonth.uppercase(Locale.ROOT)).value
                transactionsRef.whereGreaterThanOrEqualTo("date", getStartOfMonth(monthIndex, selectedYear))
                    .whereLessThan("date", getStartOfNextMonth(monthIndex, selectedYear))
            }

            query.get().addOnSuccessListener { documents ->
                var totalPositive = 0.0
                var totalNegative = 0.0

                for (document in documents) {
                    val transaction = document.toObject(UserTransaction::class.java)
                    if (transaction.outgoing) {
                        totalNegative += transaction.amount
                    } else {
                        totalPositive += transaction.amount
                    }
                }

                val numberFormat = NumberFormat.getNumberInstance(Locale.ITALY)
                totEntrate.text = numberFormat.format(totalPositive)
                totUscite.text = numberFormat.format(totalNegative)
                totDelta.text = numberFormat.format(totalPositive-totalNegative)


                // Update gauge values proportionally
                vmultiGauge.value = totalPositive
                vmultiGauge.secondValue = totalNegative
                vmultiGauge.thirdValue= totalPositive - totalNegative

                if (totalPositive > totalNegative) {
                    vmultiGauge.maxValue = totalPositive * 1.1
                    vmultiGauge.secondMaxValue = totalPositive * 1.1
                    vmultiGauge.thirdMaxValue = totalPositive * 1.1
                } else {
                    vmultiGauge.maxValue = totalNegative * 1.1
                    vmultiGauge.secondMaxValue = totalNegative * 1.1
                    vmultiGauge.thirdMaxValue = totalNegative * 1.1
                }
            }.addOnFailureListener { e ->
                Toast.makeText(context, "Failed to load transactions: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

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

    fun getYearsList(): List<String> {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        return (currentYear downTo (currentYear - 10)).map { it.toString() }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateGauges()
    }

    private fun resetFields() {
        editTextAmount.text.clear()
        editTextDescription.text.clear()
        spinnerType.setSelection(0)
        spinnerCategory.setSelection(0)
    }
}

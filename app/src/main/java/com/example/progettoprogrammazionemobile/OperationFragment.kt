package com.example.progettoprogrammazionemobile

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.ekn.gruzer.gaugelibrary.HalfGauge
import com.ekn.gruzer.gaugelibrary.MultiGauge
import com.ekn.gruzer.gaugelibrary.Range
import com.example.progettoprogrammazionemobile.databinding.FragmentOperationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

data class UserTransaction(
    val uid: String = "",
    val amount: Double = 0.0,
    val isOutgoing: Boolean = true,
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

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

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

        // Confirm button to add transaction
        buttonConfirm.setOnClickListener {
            addTransaction()
        }


        val range = Range()
        range.color = Color.parseColor("#ce0000")
        range.from = 0.0
        range.to = 1000000.0

        val range2 = Range()
        range2.color = Color.parseColor("#E3E500")
        range2.from = 0.0
        range2.to = 1000000.0

        val range3 = Range()
        range3.color = Color.parseColor("#00b20b")
        range3.from = 0.0
        range3.to = 1000000.0


        val vmultiGauge:MultiGauge = view.findViewById<MultiGauge>(R.id.multiGauge)

        // Add color ranges to gauge
        vmultiGauge.addRange(range)
        //vmultiGauge.addRange(range2)
       // vmultiGauge.addRange(range3)

        //vmultiGauge.addSecondRange(range)
        vmultiGauge.addSecondRange(range2)
       // vmultiGauge.addSecondRange(range3)

        //vmultiGauge.addThirdRange(range)
       // vmultiGauge.addThirdRange(range2)
        vmultiGauge.addThirdRange(range3)

        // Set min, max, and current value
        vmultiGauge.minValue = 0.0
        vmultiGauge.maxValue = 100000.0
        vmultiGauge.value = 21000.0

        vmultiGauge.secondMinValue = 0.0
        vmultiGauge.secondMaxValue = 100000.0
        vmultiGauge.secondValue = 15.0

        vmultiGauge.thirdMinValue = 0.0
        vmultiGauge.thirdMaxValue = 100000.0
        vmultiGauge.thirdValue = 48000.0

        return view
    }

    private fun addTransaction() {
        val user = firebaseAuth.currentUser
        if (user != null) {
            val UID = user.uid
            val amount = editTextAmount.text.toString().toDoubleOrNull()
            val type = spinnerType.selectedItem.toString()
            val category = spinnerCategory.selectedItem.toString()
            val description = editTextDescription.text.toString()
            val date = Date()  // Prende l'ora esatta al momento della creazione

            if (amount != null && category.isNotEmpty()) {
                val isOutgoing = type == "-"
                val userTransaction = UserTransaction(
                    uid = UID,
                    amount = amount,
                    isOutgoing = isOutgoing,
                    category = category,
                    notes = description,
                    date = date
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

                    val transactionRef = accountRef.collection("Transaction").document(date.toString())
                    transaction.set(transactionRef, userTransaction)

                    null
                }.addOnSuccessListener {
                    Toast.makeText(context, "Transaction added successfully", Toast.LENGTH_SHORT).show()
                    resetFields()
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

    private fun resetFields() {
        editTextAmount.text.clear()
        editTextDescription.text.clear()
        spinnerType.setSelection(0)
        spinnerCategory.setSelection(0)
    }
}

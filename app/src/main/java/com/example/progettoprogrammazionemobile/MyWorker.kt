package com.example.progettoprogrammazionemobile

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar
import java.util.Date


//il mio servizio periodico non è altro che la registrazione delle transazioni di entrate/uscite fisse
class MyWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        // Perform the background task here
        val sharedPreferences = applicationContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val UID = sharedPreferences.getString("UID", null)

        return if (UID != null) {
            //chiamo il servizio in background
            performBackgroundTask(UID)
            Result.success()
        } else {
            Log.e(TAG, "UID non trovato.")
            Result.failure()
        }
    }

    private fun performBackgroundTask(UID: String) {
        val db = Firebase.firestore

        val userDocRef = db.collection(UID).document("Account")

        userDocRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val amountIN = document.getDouble("fixed_income")?.toInt() ?: 0
                    val amountOUT = document.getDouble("fixed_expenses")?.toInt() ?: 0

                    //metto le categorie manualmente nelle due transazioni
                    val categoryIN = "Fixed Income"
                    val categoryOUT = "Fixed Outcome"
                    val descriptionIN = "Fixed Income"
                    val descriptionOUT = "Fixed Outcome"

                    val fdate = LocalDateTime.now()
                    val date: Date = Date.from(fdate.atZone(ZoneId.systemDefault()).toInstant())

                    if (amountOUT != 0 && categoryOUT.isNotEmpty()) {
                        val userTransactionOUT = UserTransaction(
                            uid = UID,
                            amount = amountOUT.toDouble(),
                            outgoing = true,
                            category = categoryOUT,
                            notes = descriptionOUT,
                            date = date
                        )

                        db.runTransaction { transaction ->
                            val accountRef = db.collection(UID).document("Account")
                            val snapshot = transaction.get(accountRef)
                            val currentBalance = snapshot.getDouble("balance") ?: 0.0
                            val newBalance = currentBalance - amountOUT

                            transaction.update(accountRef, "balance", newBalance)

                            val currentDate = Date()

                            //per fare in modo che non si crei una sovrapposizione delle due transazioni nella registrazione sul db, aggiungo 1 secondo di ritardo
                            val calendar = Calendar.getInstance()
                            calendar.time = currentDate
                            calendar.add(Calendar.SECOND, 1)

                            val newDate = calendar.time

                            val transactionRef = accountRef.collection("Transaction").document(newDate.toString())
                            transaction.set(transactionRef, userTransactionOUT)

                            newBalance
                        }.addOnSuccessListener {
                            Log.d(TAG, "Transazione completata con successo.")
                        }.addOnFailureListener { e ->
                            Log.e(TAG, "Errore durante la transazione: $e")
                        }
                    }

                    if (amountIN != 0 && categoryIN.isNotEmpty()) {
                        val userTransactionIN = UserTransaction(
                            uid = UID,
                            amount = amountIN.toDouble(),
                            outgoing = false,
                            category = categoryIN,
                            notes = descriptionIN,
                            date = date
                        )

                        db.runTransaction { transaction ->
                            val accountRef = db.collection(UID).document("Account")
                            val snapshot = transaction.get(accountRef)
                            val currentBalance = snapshot.getDouble("balance") ?: 0.0
                            val newBalance = currentBalance + amountIN

                            transaction.update(accountRef, "balance", newBalance)

                            val transactionRef = accountRef.collection("Transaction").document(date.toString())
                            transaction.set(transactionRef, userTransactionIN)

                            newBalance
                        }.addOnSuccessListener {
                            Log.d(TAG, "Transazione completata con successo.")
                        }.addOnFailureListener { e ->
                            Log.e(TAG, "Errore durante la transazione: $e")
                        }
                    }
                } else {
                    Log.e(TAG, "Il documento non esiste o è nullo.")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Errore durante il recupero del documento: $e")
            }
    }

    companion object {
        private const val TAG = "MyWorker"
    }
}

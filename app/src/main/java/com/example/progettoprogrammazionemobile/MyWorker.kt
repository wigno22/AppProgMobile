package com.example.progettoprogrammazionemobile

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar
import java.util.Date

class MyWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        // Perform the background task here
        performBackgroundTask()
        return Result.success()
    }
    private fun performBackgroundTask() {
        val db = Firebase.firestore
        val firebaseAuth = FirebaseAuth.getInstance()

        val user = firebaseAuth.currentUser
        val UID = user?.uid
        if (UID != null) {
            val userDocRef = db.collection(UID).document("Account")

            userDocRef.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val amountIN = document.getDouble("fixed_income")?.toInt() ?: 0
                        val amountOUT = document.getDouble("fixed_expenses")?.toInt() ?: 0

                        val categoryIN = "Fixed Income"
                        val categoryOUT = "Fixed Outcome"
                        val descriptionIN = "Fixed Income"
                        val descriptionOUT = "Fixed Outcome"

                        val fdate = LocalDateTime.now()
                        val date: Date = Date.from(fdate.atZone(ZoneId.systemDefault()).toInstant())

                        if (amountOUT != null && categoryOUT.isNotEmpty()) {
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

                                // Aggiungere un secondo alla data corrente
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

                        if (amountIN != null && categoryIN.isNotEmpty()) {
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
                        triggerActivityReload()

                    }

                    else {
                        Log.e(TAG, "Il documento non esiste o è nullo.")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Errore durante il recupero del documento: $e")
                }
        }


    }
    private fun triggerActivityReload() {
        // Accedi all'Activity corrente attraverso il contesto
        val activity = (applicationContext as? Activity)
        activity?.runOnUiThread {
            // Ricrea l'Activity per ricaricare la pagina
            activity.recreate()
        }
    }

    companion object {
        private const val TAG = "MyWorker"
    }

}

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date

class InvestmentWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val UID = inputData.getString("UID")
        val durationInMonths = inputData.getInt("durationInMonths", 0)

        return if (UID != null) {
            // Gestisci il piano di investimento senza ricalcolare la durata
            handleInvestmentPlan(UID, durationInMonths)
            Result.success()
        } else {
            Log.e(TAG, "UID non trovato.")
            Result.failure()
        }
    }

    private suspend fun handleInvestmentPlan(UID: String, durationInMonths: Int) {
        val db = Firebase.firestore
        val investmentPlanRef = db.collection(UID).document("Account")

        try {
            val document = investmentPlanRef.get().await()
            if (document != null && document.exists()) {
                val startDate = document.getDate("dataInizioPiano")

                if (startDate != null) {
                    val endDate = Calendar.getInstance().apply {
                        time = startDate
                        add(Calendar.MONTH, durationInMonths)
                    }.time

                    if (Date().after(endDate)) {
                        sellAssets(UID, db)
                    }
                }
            } else {
                Log.e(TAG, "Piano di investimento non trovato.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore durante il recupero del piano di investimento: $e")
        }
    }


    private suspend fun sellAssets(uid: String, db: FirebaseFirestore) {
        withContext(Dispatchers.IO) {
            try {
                // Ottieni i documenti delle azioni e criptovalute
                val stocksDoc = db.collection(uid).document("Stocks").get().await()
                val cryptosDoc = db.collection(uid).document("Cryptos").get().await()

                if (stocksDoc != null && stocksDoc.exists()) {
                    // Supponiamo che tu abbia una funzione per vendere tutte le azioni
                    val stocks = stocksDoc.toObject(Stocks::class.java)
                    // Vendita delle azioni (implementa la logica di vendita qui)
                    // esempio: sellAllStocks(stocks)
                } else {
                    Log.e(TAG, "Documenti delle azioni non trovati.")
                }

                if (cryptosDoc != null && cryptosDoc.exists()) {
                    // Supponiamo che tu abbia una funzione per vendere tutte le criptovalute
                    val cryptos = cryptosDoc.toObject(Cryptos::class.java)
                    // Vendita delle criptovalute (implementa la logica di vendita qui)
                    // esempio: sellAllCryptos(cryptos)
                } else {
                    Log.e(TAG, "Documenti delle criptovalute non trovati.")
                }

                Log.d(TAG, "Azioni e criptovalute vendute per l'utente $uid.")
            } catch (e: Exception) {
                Log.e(TAG, "Errore durante la vendita di azioni e criptovalute: $e")
            }
        }
    }

    companion object {
        private const val TAG = "InvestmentWorker"
    }
}

// Classe di esempio per Stocks e Cryptos
data class Stocks(val stockList: List<String>? = null)
data class Cryptos(val cryptoList: List<String>? = null)

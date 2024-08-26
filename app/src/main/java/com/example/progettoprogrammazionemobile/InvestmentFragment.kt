package com.example.progettoprogrammazionemobile

import android.app.AlertDialog
import android.health.connect.datatypes.units.Percentage
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.DecimalFormat
import java.util.*
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import java.util.concurrent.TimeUnit


class InvestmentFragment : Fragment() {

    private lateinit var saldoMedioTextView: TextView
    private lateinit var saldoCifraEditText: TextView
    private lateinit var cifraInvestimentoEditText: EditText
    private lateinit var radioGroupRischio: RadioGroup
    private lateinit var azioniCifraTextView: TextView
    private lateinit var cryptoCifraTextView: TextView
    private lateinit var confirmButton: Button
    private lateinit var periodoSpinner: Spinner
    private lateinit var azioniFattoreRischioTextView: TextView
    private lateinit var CryptoFattoreRischioTextView: TextView
    private lateinit var azioniButton: ImageButton
    private lateinit var cryptoButton: ImageButton
    private lateinit var azioni_valore_attuale: TextView
    private lateinit var crypto_valore_attuale: TextView
    private lateinit var crypto_data_acq: TextView
    private lateinit var crypto_data_attuale: TextView
    private lateinit var azioni_data_attuale: TextView
    private lateinit var azioni_data_acq: TextView
    private lateinit var azioni_perc_rend: TextView
    private lateinit var crypto_perc_rend: TextView
    private val apiKeyA = "cqrngv9r01quefaheobgcqrngv9r01quefaheoc0"
    private val apiKeyC = "6e362c44-bcd8-483d-9db5-a3f880533d6f"

    private lateinit var viewModel: DataViewModel

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var saldoMedio: Double = 0.0
    private var exchangeRate: Double = 1.09


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_investment, container, false)

        // Ottieni il ViewModel
        viewModel = ViewModelProvider(this).get(DataViewModel::class.java)


        saldoMedioTextView = view.findViewById(R.id.saldo_medio)
        saldoCifraEditText = view.findViewById(R.id.saldo_cifra)
        cifraInvestimentoEditText = view.findViewById(R.id.cifra_investimento)
        radioGroupRischio = view.findViewById(R.id.radio_group_rischio)
        azioniCifraTextView = view.findViewById(R.id.azioni_cifra)
        cryptoCifraTextView = view.findViewById(R.id.crypto_cifra)

        confirmButton = view.findViewById(R.id.confirm_button)
        periodoSpinner = view.findViewById(R.id.periodo_spinner)

        azioniButton = view.findViewById(R.id.azioni_button)
        cryptoButton = view.findViewById(R.id.crypto_button)

        azioniFattoreRischioTextView = view.findViewById(R.id.azioni_percentuale_rischio)
        CryptoFattoreRischioTextView = view.findViewById(R.id.crypto_percentuale_rischio)

        azioni_valore_attuale =  view.findViewById(R.id.azioni_valore_attuale)
        crypto_valore_attuale = view.findViewById(R.id.crypto_valore_attuale)

        crypto_data_acq = view.findViewById(R.id.crypto_data_acquisto)
        crypto_data_attuale = view.findViewById(R.id.crypto_data_attuale)
        azioni_data_attuale = view.findViewById(R.id.azioni_data_attuale)
        azioni_data_acq = view.findViewById(R.id.azioni_data_acquisto)


        azioni_perc_rend = view.findViewById(R.id.azioni_percentuale_rendimento)
        crypto_perc_rend = view.findViewById(R.id.crypto_percentuale_rendimento)

        azioniFattoreRischioTextView.text = "<10%"
        CryptoFattoreRischioTextView.text = "<10%"

         val retrofit = Retrofit.Builder()
            .baseUrl("https://pro-api.coinmarketcap.com/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

         val service = retrofit.create(CoinMarketCapApiService::class.java)


        //fetchExchangeRate()
        showUserData()
        setupPeriodoSpinner()
        fetchSaldoMedioTrimestrale()
        fetchSaldoAzioni()
        fetchSaldoCrypto()

        lifecycleScope.launch {
            viewModel.updateAllStockValues(db, FirebaseAuth.getInstance().currentUser, okHttpClient, apiKeyA, exchangeRate)

            viewModel.updateAllCryptoValues(db, FirebaseAuth.getInstance().currentUser, service, apiKeyC, exchangeRate)
        }


        confirmButton.setOnClickListener { onConfirmButtonClick() }

        azioniButton.setOnClickListener { navigatetoFragment("azioni") }
        cryptoButton.setOnClickListener { navigatetoFragment("crypto") }
        return view
    }






    private fun showUserData() {
        val user = auth.currentUser
        val UID = user?.uid
        if (UID != null) {
            val userDocRef = db.collection(UID).document("Account")
            val cryptoinfo = db.collection(UID).document("Account").collection("Criptovalute")
            val azioniinfo = db.collection(UID).document("Account").collection("Azioni")

            userDocRef.get().addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val livelloRischio = document.getLong("livelloRischio")?.toInt() ?: 1
                    val saldoTrimestraleMedio = document.getDouble("saldoTrimestraleMedio") ?: 0.0
                    val periodoInvestimento = document.getLong("periodoInvestimento")?.toInt() ?: 0
                    val cifraInvestimento = document.getDouble("cifraInvestimento") ?: 0.0

                    // Aggiorna le viste con i dati recuperati
                    saldoCifraEditText.setText(saldoTrimestraleMedio.toString())
                    cifraInvestimentoEditText.setText(cifraInvestimento.toString())

                    // Imposta il periodo nello Spinner confrontando direttamente con il valore
                    for (i in 0 until periodoSpinner.count) {
                        val periodo = periodoSpinner.getItemAtPosition(i).toString()
                        if (periodo.startsWith(periodoInvestimento.toString())) {
                            periodoSpinner.setSelection(i)
                            break
                        }
                    }

                    // Imposta il livello di rischio nel RadioGroup
                    when (livelloRischio) {
                        1 -> radioGroupRischio.check(R.id.rischio_basso)
                        2 -> radioGroupRischio.check(R.id.rischio_medio)
                        3 -> radioGroupRischio.check(R.id.rischio_alto)
                    }

                    // Calcola le quote azioni e fondi
                    val quotaazioni = when (livelloRischio) {
                        1 -> cifraInvestimento * 0.6
                        2 -> cifraInvestimento * 0.5
                        3 -> cifraInvestimento * 0.4
                        else -> 0.0
                    }
                    val quotacrypto = cifraInvestimento - quotaazioni

                    azioniCifraTextView.text = quotaazioni.toString()
                    cryptoCifraTextView.text = quotacrypto.toString()

                    azioniinfo.get().addOnSuccessListener { querySnapshot ->
                        if (querySnapshot != null && !querySnapshot.isEmpty) {
                            // Crea un SimpleDateFormat per il parsing della data originale e uno per la formattazione della nuova data
                            val originalFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                            val newFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                            // Itera sui documenti nella collezione
                            for (document in querySnapshot.documents) {
                                // Estrai i dati dai singoli documenti
                                val azioniDataAttualeString = document.getString("dataReal")
                                val azioniDataAcqString = document.getString("dataAcq")

                                // Converti e formatta le date
                                val azioniDataAttuale = azioniDataAttualeString?.let {
                                    val date = originalFormat.parse(it)
                                    newFormat.format(date)
                                }

                                val azioniDataAcq = azioniDataAcqString?.let {
                                    val date = originalFormat.parse(it)
                                    newFormat.format(date)
                                }

                                // Mostra le date formattate nei TextView
                                azioni_data_acq.text = azioniDataAcq
                                azioni_data_attuale.text = azioniDataAttuale
                            }
                        }
                    }

                    cryptoinfo.get().addOnSuccessListener { querySnapshot ->
                        if (querySnapshot != null && !querySnapshot.isEmpty) {
                            // Crea un SimpleDateFormat per il parsing della data originale e uno per la formattazione della nuova data
                            val originalFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                            val newFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                            // Itera sui documenti nella collezione
                            for (document in querySnapshot.documents) {
                                // Estrai i dati dai singoli documenti
                                val cryptoDataAttualeString = document.getString("dataReal")
                                val cryptoDataAcqString = document.getString("dataAcq")

                                // Converti e formatta le date
                                val cryptoDataAttuale = cryptoDataAttualeString?.let {
                                    val date = originalFormat.parse(it)
                                    newFormat.format(date)
                                }

                                val cryptoDataAcq = cryptoDataAcqString?.let {
                                    val date = originalFormat.parse(it)
                                    newFormat.format(date)
                                }

                                // Mostra le date formattate nei TextView
                                crypto_data_acq.text = cryptoDataAcq
                                crypto_data_attuale.text = cryptoDataAttuale
                            }
                        }
                    }




                    // Imposta visibilità dei bottoni
                    azioniButton.visibility = if (quotaazioni > 0) View.VISIBLE else View.INVISIBLE
                    cryptoButton.visibility = if (quotacrypto > 0) View.VISIBLE else View.INVISIBLE


                } else {
                    Toast.makeText(requireContext(), "Nessun dato trovato per l'utente.", Toast.LENGTH_LONG).show()
                    // Se nessun dato trovato, nascondi i bottoni
                    azioniButton.visibility = View.INVISIBLE
                    cryptoButton.visibility = View.INVISIBLE
                }
            }.addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Errore nel recupero dei dati: ${e.message}", Toast.LENGTH_SHORT).show()
                // Gestisci anche i bottoni in caso di errore
                azioniButton.visibility = View.INVISIBLE
                cryptoButton.visibility = View.INVISIBLE
            }
        } else {
            Toast.makeText(requireContext(), "Utente non autenticato.", Toast.LENGTH_SHORT).show()
            // Gestisci anche i bottoni in caso di utente non autenticato
            azioniButton.visibility = View.INVISIBLE
            cryptoButton.visibility = View.INVISIBLE
        }
    }




    private fun setupPeriodoSpinner() {
        val periodi = arrayOf("6 Months", "12 Months", "18 Months", "24 Months", "36 Months")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, periodi)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        periodoSpinner.adapter = adapter
    }

    private fun fetchSaldoAzioni() {
        val user = auth.currentUser
        val UID = user?.uid
        if (UID != null) {
            val userDocRef = db.collection(UID).document("Account")
            val transactionsRef = userDocRef.collection("Azioni")

            transactionsRef.get().addOnSuccessListener { querySnapshot ->
                var saldoAzTotale = 0.0

                for (document in querySnapshot.documents) {
                    var qta = document.getDouble("numeroAzioni") ?: 0.0
                    var amount = document.getDouble("valoreUlt") ?: 0.0

                    saldoAzTotale += (amount * qta)
                }

                val decimalFormat = DecimalFormat("#.0")
                val AzioniFormatted = decimalFormat.format(saldoAzTotale)

                // Imposta il valore formato o 0,0 se nullo
                azioni_valore_attuale.text = if (AzioniFormatted == ",0") "0,0" else AzioniFormatted

                val azioniCifraText = azioniCifraTextView.text.toString()

                if (saldoAzTotale != 0.0 && azioniCifraText.isNotBlank() &&
                    azioniCifraText.matches(Regex("[-+]?\\d*\\.?\\d+"))) {

                    val azioniValoreAcqIniz = azioniCifraText.toDouble()
                    val margineAzioni = ((saldoAzTotale / azioniValoreAcqIniz) - 1)

                    val percentFormat = DecimalFormat("#.#%")
                    azioni_perc_rend.text = percentFormat.format(margineAzioni)
                } else {
                    azioni_perc_rend.text = "0,0%" // Imposta un valore di default se il valore è nullo
                }
            }.addOnFailureListener { exception ->
                Log.e("InvestimentiFragment", "Failed to fetch transactions: ${exception.message}")
            }
        } else {
            Log.e("InvestimentiFragment", "User ID is null")
        }
    }


    private fun fetchSaldoCrypto() {
        val user = auth.currentUser
        val UID = user?.uid
        if (UID != null) {
            val userDocRef = db.collection(UID).document("Account")
            val transactionsRef = userDocRef.collection("Criptovalute")

            transactionsRef.get().addOnSuccessListener { querySnapshot ->
                var saldoCryTotale = 0.0

                for (document in querySnapshot.documents) {
                    var amount = document.getDouble("valoreUlt€") ?: 0.0
                    var qta = document.getDouble("numeroCriptovalute") ?: 0.0
                    saldoCryTotale += amount * qta
                }

                val decimalFormat = DecimalFormat("#.0")
                val CryptoFormatted = decimalFormat.format(saldoCryTotale)

                // Imposta il valore formato o 0,0 se nullo
                crypto_valore_attuale.text = if (CryptoFormatted == ",0") "0,0" else CryptoFormatted

                val cryptoCifraText = cryptoCifraTextView.text.toString()

                if (saldoCryTotale != 0.0 && cryptoCifraText.isNotBlank() &&
                    cryptoCifraText.matches(Regex("[-+]?\\d*\\.?\\d+"))) {

                    val cryptoCifraNumero = cryptoCifraText.toDouble()
                    val margineCrypto = ((saldoCryTotale / cryptoCifraNumero) - 1)

                    val percentFormat = DecimalFormat("#.#%")
                    crypto_perc_rend.text = percentFormat.format(margineCrypto)
                } else {
                    crypto_perc_rend.text = "0,0%" // Imposta un valore di default se il valore è nullo
                }
            }.addOnFailureListener { exception ->
                Log.e("InvestimentiFragment", "Failed to fetch transactions: ${exception.message}")
            }
        } else {
            Log.e("InvestimentiFragment", "User ID is null")
        }
    }




    private fun fetchSaldoMedioTrimestrale() {
        val user = auth.currentUser
        val UID = user?.uid
        if (UID != null) {
            val userDocRef = db.collection(UID).document("Account")
            val threeMonthsAgo = Calendar.getInstance().apply {
                add(Calendar.MONTH, -3)
            }.time

            val transactionsRef = userDocRef.collection("Transaction")
                .whereGreaterThan("date", threeMonthsAgo)
                .orderBy("date", Query.Direction.ASCENDING)

            transactionsRef.get().addOnSuccessListener { querySnapshot ->
                var saldoTotale = 0.0

                Log.d("InvestimentiFragment", "Number of documents: ${querySnapshot.documents.size}")

                for (document in querySnapshot.documents) {
                    var amount = document.getDouble("amount") ?: 0.0
                    val isOutgoing = document.getBoolean("outgoing") ?: false
                    if (isOutgoing) amount *= -1 else amount *= 1

                    saldoTotale += amount

                    Log.d("InvestimentiFragment", "Document: $document, Amount: $amount, Running Total: $saldoTotale")
                }

                saldoMedio = saldoTotale / 3
                val decimalFormat = DecimalFormat("#.0")
                val saldoMedioFormatted = decimalFormat.format(saldoMedio)


                saldoCifraEditText.text = if (saldoMedioFormatted == ",0") "0,0" else saldoMedioFormatted

                Log.d("InvestimentiFragment", "Saldo medio: $saldoMedioFormatted")
            }.addOnFailureListener { exception ->
                Log.e("InvestimentiFragment", "Failed to fetch transactions: ${exception.message}")
            }
        } else {
            Log.e("InvestimentiFragment", "User ID is null")
        }
    }



    private fun onConfirmButtonClick() {
        val user = auth.currentUser
        val UID = user?.uid ?: return

        val cryptoinfo = db.collection(UID).document("Account").collection("Criptovalute")
        val azioniinfo = db.collection(UID).document("Account").collection("Azioni")

        val cifraInvestimento = cifraInvestimentoEditText.text.toString().toDoubleOrNull() ?: return

        if (cifraInvestimento > saldoMedio) {
            Toast.makeText(requireContext(), "La cifra di investimento non può superare il saldo medio trimestrale.", Toast.LENGTH_LONG).show()
            return
        }

        azioniinfo.get().addOnSuccessListener { azioniSnapshot ->
            cryptoinfo.get().addOnSuccessListener { cryptoSnapshot ->
                val hasStocks = !azioniSnapshot.isEmpty
                val hasCryptos = !cryptoSnapshot.isEmpty

                if (hasStocks || hasCryptos) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Conferma Piano di Investimento")
                        .setMessage("Ci sono azioni o criptovalute esistenti. Procedendo con questo piano, verranno vendute. Sei sicuro di voler continuare?")
                        .setPositiveButton("Conferma") { dialog, which ->
                            lifecycleScope.launch {
                                if (hasStocks) {
                                    viewModel.sellAllStocks(db, user)
                                }
                                if (hasCryptos) {
                                    viewModel.sellAllCryptos(db, user)
                                }

                                saveInvestmentPlan(cifraInvestimento)

                                // Ricarica il fragment
                                reloadFragment()
                            }
                        }
                        .setNegativeButton("Annulla") { dialog, which ->
                            dialog.dismiss()
                        }
                        .show()
                } else {
                    saveInvestmentPlan(cifraInvestimento)
                    reloadFragment()
                }
            }
        }
    }



    private fun reloadFragment() {
        showUserData()
        fetchSaldoAzioni()
        fetchSaldoCrypto()

    }


    private fun saveInvestmentPlan(cifraInvestimento: Double) {
        val selectedPeriodo = periodoSpinner.selectedItem.toString()
        val periodo = selectedPeriodo.split(" ")[0].toInt()
        var quotaazioni = 0.0
        var quotacrypto = 0.0
        val currentDate = LocalDateTime.now()
        val formattedDateTime = Date.from(currentDate.atZone(ZoneId.systemDefault()).toInstant())

        val rischioId = radioGroupRischio.checkedRadioButtonId
        val fattoreRischio = when (rischioId) {
            R.id.rischio_basso -> 1
            R.id.rischio_medio -> 2
            R.id.rischio_alto -> 3
            else -> 1
        }
        if (fattoreRischio == 1) {
            quotaazioni = cifraInvestimento * 0.6
            quotacrypto = cifraInvestimento * 0.4
        } else if (fattoreRischio == 2) {
            quotaazioni = cifraInvestimento * 0.5
            quotacrypto = cifraInvestimento * 0.5
        } else {
            quotaazioni = cifraInvestimento * 0.4
            quotacrypto = cifraInvestimento * 0.6
        }

        azioniCifraTextView.text = quotaazioni.toString()
        cryptoCifraTextView.text = quotacrypto.toString()

        val uid = auth.uid
        if (uid != null) {
            val docref = db.collection(uid).document("Account")

            val accountData = hashMapOf(
                "livelloRischio" to fattoreRischio,
                "saldoTrimestraleMedio" to saldoMedio,
                "periodoInvestimento" to periodo,
                "cifraInvestimento" to cifraInvestimento,
                "cifraInAzioni" to quotaazioni,
                "cifraInCrypto" to quotacrypto,
                "exchangeRate" to exchangeRate,
                "dataInizioPiano" to formattedDateTime
            )

            docref.set(accountData, SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Dati salvati con successo!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Errore nel salvataggio: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        azioniButton.visibility = View.VISIBLE
        cryptoButton.visibility = View.VISIBLE
    }


    private fun navigatetoFragment(type: String) {

        // Ottenere il NavController
        val navController = findNavController()

        if (type=="azioni")navController.navigate(R.id.navigation_stock) else if (type=="crypto") navController.navigate(R.id.navigation_crypto)


    }



}





package com.example.progettoprogrammazionemobile

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.BuildConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.DecimalFormat
import java.util.*

class InvestmentFragment : Fragment() {

    private lateinit var saldoMedioTextView: TextView
    private lateinit var saldoCifraEditText: TextView
    private lateinit var cifraInvestimentoEditText: EditText
    private lateinit var radioGroupRischio: RadioGroup
    private lateinit var azioniCifraTextView: TextView
    private lateinit var CryptoCifraTextView: TextView
    private lateinit var confirmButton: Button
    private lateinit var periodoSpinner: Spinner
    private lateinit var azioniFattoreRischioTextView: TextView
    private lateinit var CryptoFattoreRischioTextView: TextView
    private lateinit var azioniButton: Button
    private lateinit var cryptoButton: Button


    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var saldoMedio: Double = 0.0
    private var exchangeRate: Double = 1.09


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_investment, container, false)
        saldoMedioTextView = view.findViewById(R.id.saldo_medio)
        saldoCifraEditText = view.findViewById(R.id.saldo_cifra)
        cifraInvestimentoEditText = view.findViewById(R.id.cifra_investimento)
        radioGroupRischio = view.findViewById(R.id.radio_group_rischio)
        azioniCifraTextView = view.findViewById(R.id.azioni_cifra)
        CryptoCifraTextView = view.findViewById(R.id.crypto_cifra)

        confirmButton = view.findViewById(R.id.confirm_button)
        periodoSpinner = view.findViewById(R.id.periodo_spinner)

        azioniButton = view.findViewById(R.id.azioni_button)
        cryptoButton = view.findViewById(R.id.crypto_button)

        azioniFattoreRischioTextView = view.findViewById(R.id.azioni_fattore_rischio)
        CryptoFattoreRischioTextView = view.findViewById(R.id.crypto_fattore_rischio)


        azioniFattoreRischioTextView.text = "<10%"
        CryptoFattoreRischioTextView.text = "<5%"

        //fetchExchangeRate()
        showUserData()
        setupPeriodoSpinner()
        fetchSaldoMedioTrimestrale()
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
                        1 -> cifraInvestimento * 0.4
                        2 -> cifraInvestimento * 0.5
                        3 -> cifraInvestimento * 0.6
                        else -> 0.0
                    }
                    val quotacrypto = cifraInvestimento - quotaazioni

                    azioniCifraTextView.text = quotaazioni.toString()
                    CryptoCifraTextView.text = quotacrypto.toString()

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
        val periodi = arrayOf("6 mesi", "12 mesi", "18 mesi", "24 mesi", "36 mesi")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, periodi)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        periodoSpinner.adapter = adapter
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

                saldoCifraEditText.setText(saldoMedioFormatted)

                Log.d("InvestimentiFragment", "Saldo medio: $saldoMedioFormatted")
            }.addOnFailureListener { exception ->
                Log.e("InvestimentiFragment", "Failed to fetch transactions: ${exception.message}")
            }
        } else {
            Log.e("InvestimentiFragment", "User ID is null")
        }
    }



    private fun onConfirmButtonClick() {
        val cifraInvestimento = cifraInvestimentoEditText.text.toString().toDoubleOrNull() ?: return

        // Controllo se la cifra di investimento supera il saldo medio trimestrale
        if (cifraInvestimento > saldoMedio) {
            Toast.makeText(requireContext(), "La cifra di investimento non può superare il saldo medio trimestrale.", Toast.LENGTH_LONG).show()
            return
        }

        val selectedPeriodo = periodoSpinner.selectedItem.toString()
        val periodo = selectedPeriodo.split(" ")[0].toInt()
        var quotaazioni = 0.0
        var quotacrypto = 0.0

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
        CryptoCifraTextView.text = quotacrypto.toString()

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
                "exchangeRate" to exchangeRate
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





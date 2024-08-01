package com.example.progettoprogrammazionemobile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.text.DecimalFormat
import java.util.*


class InvestmentFragment : Fragment() {

    private lateinit var saldoMedioTextView: TextView
    private lateinit var saldoCifraEditText: TextView
    private lateinit var cifraInvestimentoEditText: EditText
    private lateinit var radioGroupRischio: RadioGroup
    private lateinit var azioniCifraTextView: TextView
    private lateinit var fondiCifraTextView: TextView
    private lateinit var liquiditaCifraTextView: TextView
    private lateinit var confirmButton: Button
    private lateinit var periodoSpinner: Spinner
    private lateinit var previsioneRendimentoTextView: TextView
    private lateinit var azioniFattoreRischioTextView: TextView
    private lateinit var fondiFattoreRischioTextView: TextView
    private lateinit var liquiditaFattoreRischioTextView: TextView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var saldoMedio: Double = 0.0
    private var azioniFattoreRischio: Int = 0
    private var fondiFattoreRischio: Int = 0
    private var liquiditaFattoreRischio: Int = 0

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
        fondiCifraTextView = view.findViewById(R.id.fondi_cifra)
        liquiditaCifraTextView = view.findViewById(R.id.liquidita_cifra)
        confirmButton = view.findViewById(R.id.confirm_button)
        periodoSpinner = view.findViewById(R.id.periodo_spinner)
        previsioneRendimentoTextView = view.findViewById(R.id.previsione_rendimento)
        azioniFattoreRischioTextView = view.findViewById(R.id.azioni_fattore_rischio)
        fondiFattoreRischioTextView = view.findViewById(R.id.fondi_fattore_rischio)
        liquiditaFattoreRischioTextView = view.findViewById(R.id.liquidita_fattore_rischio)

        setupPeriodoSpinner()
        fetchSaldoMedioTrimestrale()
        fetchFattoriRischioOnline()
        confirmButton.setOnClickListener { onConfirmButtonClick() }


        return view
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

    private fun fetchFattoriRischioOnline() {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=MSFT&apikey=KMD5G7J3GYUXUOLA")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("InvestimentiFragment", "Failed to fetch risk factors: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let {
                    val json = JSONObject(it)
                    val timeSeries = json.getJSONObject("Time Series (Daily)")
                    val latestEntry = timeSeries.keys().next()
                    val latestData = timeSeries.getJSONObject(latestEntry)
                    val closingPrice = latestData.getDouble("4. close")

                    // Simulate realistic risk factors (e.g., percentage-based)
                    azioniFattoreRischio = (closingPrice * 0.02).toInt() // Example calculation
                    fondiFattoreRischio = (closingPrice * 0.01).toInt() // Example calculation
                    liquiditaFattoreRischio = (closingPrice * 0.005).toInt() // Example calculation

                    activity?.runOnUiThread {
                        azioniFattoreRischioTextView.text = "Fattore di rischio: ${azioniFattoreRischio}%"
                        fondiFattoreRischioTextView.text = "Fattore di rischio: ${fondiFattoreRischio}%"
                        liquiditaFattoreRischioTextView.text = "Fattore di rischio: ${liquiditaFattoreRischio}%"
                    }
                }
            }
        })
    }

    private fun onConfirmButtonClick() {
        val cifraInvestimentoStr = cifraInvestimentoEditText.text.toString()
        if (cifraInvestimentoStr.isBlank()) {
            Toast.makeText(requireContext(), "Inserisci una cifra da investire", Toast.LENGTH_SHORT).show()
            return
        }

        val cifraInvestimento = cifraInvestimentoStr.toDouble()
        if (cifraInvestimento > saldoMedio) {
            Toast.makeText(requireContext(), "La cifra da investire non può superare il saldo medio", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedRischioId = radioGroupRischio.checkedRadioButtonId
        if (selectedRischioId == -1) {
            Toast.makeText(requireContext(), "Seleziona un livello di rischio", Toast.LENGTH_SHORT).show()
            return
        }

        val distribuzione = when (selectedRischioId) {
            R.id.radio_basso -> Triple(0.1, 0.2, 0.7)
            R.id.radio_medio -> Triple(0.3, 0.4, 0.3)
            R.id.radio_alto -> Triple(0.5, 0.4, 0.1)
            else -> Triple(0.0, 0.0, 0.0)
        }

        val azioniCifra = cifraInvestimento * distribuzione.first
        val fondiCifra = cifraInvestimento * distribuzione.second
        val liquiditaCifra = cifraInvestimento * distribuzione.third

        azioniCifraTextView.text = azioniCifra.toString()
        fondiCifraTextView.text = fondiCifra.toString()
        liquiditaCifraTextView.text = liquiditaCifra.toString()

        val periodo = periodoSpinner.selectedItem.toString()
        val previsioneRendimento = calcolaRendimento(azioniCifra, fondiCifra, periodo)
        previsioneRendimentoTextView.text = previsioneRendimento
    }

    private fun calcolaRendimento(azioniCifra: Double, fondiCifra: Double, periodo: String): String {
        val rendimentoAzioni = when (periodo) {
            "6 mesi" -> 0.05
            "12 mesi" -> 0.1
            "18 mesi" -> 0.15
            "24 mesi" -> 0.2
            "36 mesi" -> 0.3
            else -> 0.0
        }

        val rendimentoFondi = when (periodo) {
            "6 mesi" -> 0.03
            "12 mesi" -> 0.06
            "18 mesi" -> 0.09
            "24 mesi" -> 0.12
            "36 mesi" -> 0.18
            else -> 0.0
        }

        val rendimentoTotaleAzioni = azioniCifra * (1 + rendimentoAzioni)
        val rendimentoTotaleFondi = fondiCifra * (1 + rendimentoFondi)
        val rendimentoTotale = rendimentoTotaleAzioni + rendimentoTotaleFondi

        val decimalFormat = DecimalFormat("#.00")
        return "Rendimento previsto: €${decimalFormat.format(rendimentoTotale)}"
    }
}

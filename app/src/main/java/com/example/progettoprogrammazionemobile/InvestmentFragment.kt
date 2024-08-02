package com.example.progettoprogrammazionemobile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONArray
import org.json.JSONException
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
    private lateinit var miglioriRecyclerView: RecyclerView
    private lateinit var switchButton: Button

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var saldoMedio: Double = 0.0
    private var azioniFattoreRischio: Int = 0
    private var fondiFattoreRischio: Int = 0
    private var liquiditaFattoreRischio: Int = 0
    private var showingStocks = true

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
        miglioriRecyclerView = view.findViewById(R.id.migliori_recyclerview)
        switchButton = view.findViewById(R.id.switch_button)

        setupPeriodoSpinner()
        setupRecyclerView()
        fetchSaldoMedioTrimestrale()
        confirmButton.setOnClickListener { onConfirmButtonClick() }
        generateAIContent()

        return view
    }

    private fun setupPeriodoSpinner() {
        val periodi = arrayOf("6 mesi", "12 mesi", "18 mesi", "24 mesi", "36 mesi")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, periodi)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        periodoSpinner.adapter = adapter
    }

    private fun setupRecyclerView() {
        miglioriRecyclerView.layoutManager = LinearLayoutManager(requireContext())
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

    private fun generateAIContent() {
        lifecycleScope.launch {
            try {
                val responseText = generateContent("Tell me the 10 most popular singer in europe")
                Log.d("InvestmentFragment", "Generated AI content: $responseText")
            } catch (e: Exception) {
                Log.e("InvestmentFragment", "Failed to generate AI content: ${e.message}")
            }
        }
    }

    private suspend fun generateContent(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val generativeModel = GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = "AIzaSyCe2AHQoV22Njr7MWcPwoEHnXJQpPIlGFw"
            )
            val result = generativeModel.generateContent(prompt)
            result.text ?: "No content generated"
        } catch (e: Exception) {
            Log.e("InvestmentFragment", "Failed to generate content: ${e.message}")
            "Error generating content"
        }
    }

    private fun onConfirmButtonClick() {
        val cifraInvestimento = cifraInvestimentoEditText.text.toString().toDoubleOrNull() ?: return
        val selectedPeriodo = periodoSpinner.selectedItem.toString()
        val periodo = selectedPeriodo.split(" ")[0].toInt()

        val rischioId = radioGroupRischio.checkedRadioButtonId
        val fattoreRischio = when (rischioId) {
            R.id.rischio_basso -> 1
            R.id.rischio_medio -> 2
            R.id.rischio_alto -> 3
            else -> 1
        }

        val rendimentoPrevisto = calcolaRendimentoPrevisto(cifraInvestimento, periodo, fattoreRischio)
        previsioneRendimentoTextView.text = rendimentoPrevisto.toString()
    }

    private fun calcolaRendimentoPrevisto(cifra: Double, periodo: Int, rischio: Int): Double {
        // Placeholder per il calcolo effettivo del rendimento previsto
        return cifra * periodo * rischio / 100.0
    }
}

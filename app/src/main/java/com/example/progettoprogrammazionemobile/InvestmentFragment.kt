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
    private lateinit var fondiCifraTextView: TextView
    private lateinit var liquiditaCifraTextView: TextView
    private lateinit var confirmButton: Button
    private lateinit var periodoSpinner: Spinner
    private lateinit var previsioneRendimentoTextView: TextView
    private lateinit var azioniFattoreRischioTextView: TextView
    private lateinit var fondiFattoreRischioTextView: TextView
    private lateinit var liquiditaFattoreRischioTextView: TextView
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatInput: EditText
    private lateinit var sendButton: Button

    //private val messages = mutableListOf<ChatMessage>()
    //private lateinit var chatAdapter: ChatAdapter

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var saldoMedio: Double = 0.0
    private var exchangeRate: Double = 1.09
    private var azioniFattoreRischio: Int = 0
    private var fondiFattoreRischio: Int = 0
    private var liquiditaFattoreRischio: Int = 0

    //data class ChatMessage(val text: String, val isUser: Boolean)

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

        confirmButton = view.findViewById(R.id.confirm_button)
        periodoSpinner = view.findViewById(R.id.periodo_spinner)

        azioniFattoreRischioTextView = view.findViewById(R.id.azioni_fattore_rischio)
        fondiFattoreRischioTextView = view.findViewById(R.id.fondi_fattore_rischio)


        //chatRecyclerView = view.findViewById(R.id.chat_recyclerview)
        //chatInput = view.findViewById(R.id.chat_input)
        //sendButton = view.findViewById(R.id.send_button)

        azioniFattoreRischioTextView.text = "<10%"
        fondiFattoreRischioTextView.text = "<5%"

        //fetchExchangeRate()
        showUserData()
        setupPeriodoSpinner()
        fetchSaldoMedioTrimestrale()
        confirmButton.setOnClickListener { onConfirmButtonClick() }
        //chatAdapter = ChatAdapter(messages)

        //chatRecyclerView.adapter = chatAdapter
        //chatRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        //sendButton.setOnClickListener { sendMessage() }

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
                    val quotafondi = cifraInvestimento - quotaazioni

                    azioniCifraTextView.text = quotaazioni.toString()
                    fondiCifraTextView.text = quotafondi.toString()

                } else {
                    Toast.makeText(requireContext(), "Nessun dato trovato per l'utente.", Toast.LENGTH_LONG).show()
                }
            }.addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Errore nel recupero dei dati: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Utente non autenticato.", Toast.LENGTH_SHORT).show()
        }
    }


    /* private fun sendMessage() {
        val messageText = chatInput.text.toString()
        if (messageText.isNotBlank()) {
            val userMessage = ChatMessage(messageText, true)
            messages.add(userMessage)
            //chatAdapter.notifyItemInserted(messages.size - 1)
            chatRecyclerView.scrollToPosition(messages.size - 1)
            chatInput.text.clear()
            //lifecycleScope.launch { sendToAI(messageText) }
        }
    }

   private suspend fun sendToAI(messageText: String) = withContext(Dispatchers.IO) {
        try {
            val generativeModel = GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = "AIzaSyCe2AHQoV22Njr7MWcPwoEHnXJQpPIlGFw"
            )
            val chat = generativeModel.startChat(
                history = messages.map {
                    content(role = if (it.isUser) "user" else "model") { text(it.text) }
                }
            )
            val aiResponse = chat.sendMessage(messageText)
            withContext(Dispatchers.Main) {
                val aiMessage = ChatMessage(aiResponse.text ?: "No response", false)
                messages.add(aiMessage)
                chatAdapter.notifyItemInserted(messages.size - 1)
                chatRecyclerView.scrollToPosition(messages.size - 1)
            }
        } catch (e: Exception) {
            Log.e("InvestmentFragment", "Failed to generate AI content: ${e.message}")
        }
    }*/

    private fun fetchExchangeRate() {
        lifecycleScope.launch {
            try {
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://v6.exchangerate-api.com/v6/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val api = retrofit.create(ExchangeRateApiService::class.java)
                val response = api.getExchangeRates("47a1ca6a80-0abeff07df-shwjzm", "EUR", "USD")

                // Verifica che il body non sia null e recupera il valore del tasso di cambio
                exchangeRate = response.conversion_rate
                Log.d("AIIntegrationFragment", "Exchange Rate EUR/USD: $exchangeRate")

            } catch (e: Exception) {
                Log.e("AIIntegrationFragment", "Failed to fetch exchange rate: ${e.message}")
                Toast.makeText(requireContext(), "Errore nel recupero del tasso di cambio.", Toast.LENGTH_SHORT).show()
            }
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

   /* private fun generateAIContent() {
        lifecycleScope.launch {
            try {
                val responseText = generateContent("What are the 5 best stocks to invest in right now?")
                azioniSuggeriteTextView.text = responseText
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

            val response = generativeModel.generateContent(prompt)
            response.text ?: "No response"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }*/


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
        var quotafondi = 0.0

        val rischioId = radioGroupRischio.checkedRadioButtonId
        val fattoreRischio = when (rischioId) {
            R.id.rischio_basso -> 1
            R.id.rischio_medio -> 2
            R.id.rischio_alto -> 3
            else -> 1
        }
        if (fattoreRischio == 1) {
            quotaazioni = cifraInvestimento * 0.4
            quotafondi = cifraInvestimento * 0.6
        } else if (fattoreRischio == 2) {
            quotaazioni = cifraInvestimento * 0.5
            quotafondi = cifraInvestimento * 0.5
        } else {
            quotaazioni = cifraInvestimento * 0.6
            quotafondi = cifraInvestimento * 0.4
        }

        azioniCifraTextView.text = quotaazioni.toString()
        fondiCifraTextView.text = quotafondi.toString()

        val uid = auth.uid
        if (uid != null) {
            val docref = db.collection(uid).document("Account")

            val accountData = hashMapOf(
                "livelloRischio" to fattoreRischio,
                "saldoTrimestraleMedio" to saldoMedio,
                "periodoInvestimento" to periodo,
                "cifraInvestimento" to cifraInvestimento,
                "cifraInAzioni" to quotaazioni,
                "cifraInFondi" to quotafondi,
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
    }


    private fun calcolaRendimentoPrevisto(cifra: Double, periodo: Int, rischio: Int): Double {
        // Placeholder per il calcolo effettivo del rendimento previsto
        return cifra * periodo * rischio / 100.0
    }
}




/*
class ChatAdapter(private val messages: List<InvestmentFragment.ChatMessage>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageTextView: TextView = view.findViewById(R.id.message_text_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]
        holder.messageTextView.text = message.text
        // Configura l'aspetto del messaggio in base al fatto che sia dell'utente o dell'AI
        if (message.isUser) {
            holder.messageTextView.gravity = Gravity.END
            holder.messageTextView.setBackgroundResource(R.drawable.user_message_background)
        } else {
            holder.messageTextView.gravity = Gravity.START
            holder.messageTextView.setBackgroundResource(R.drawable.ai_message_background)
        }
    }

    override fun getItemCount() = messages.size
}*/


package com.example.progettoprogrammazionemobile

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class ChatAIFragment : Fragment() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatInput: EditText
    private lateinit var sendButton: Button
    data class ChatMessage(val text: String, val isUser: Boolean)
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chatai, container, false)

        chatRecyclerView = view.findViewById(R.id.chat_recyclerview)
        chatInput = view.findViewById(R.id.chat_input)
        sendButton = view.findViewById(R.id.send_button)

        chatAdapter = ChatAdapter(messages)

        chatRecyclerView.adapter = chatAdapter
        chatRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        sendButton.setOnClickListener { sendMessage() }

        // Aggiungi il listener per la tastiera
        handleKeyboardVisibility(view)

        return view
    }

    private fun sendMessage() {
        val messageText = chatInput.text.toString()
        if (messageText.isNotBlank()) {
            val userMessage = ChatMessage(messageText, true)
            messages.add(userMessage)
            chatAdapter.notifyItemInserted(messages.size - 1)
            chatRecyclerView.scrollToPosition(messages.size - 1)
            chatInput.text.clear()
            lifecycleScope.launch { sendToAI(messageText) }
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
            Log.e("ChatAIFragment", "Failed to generate AI content: ${e.message}")
        }
    }

    private fun handleKeyboardVisibility(view: View) {
        val bottomNavigationView = requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)

        view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val rect = Rect()
                view.getWindowVisibleDisplayFrame(rect)
                val screenHeight = view.rootView.height
                val keypadHeight = screenHeight - rect.bottom

                if (keypadHeight > screenHeight * 0.15) {
                    // La tastiera è visibile
                    bottomNavigationView.visibility = View.GONE
                } else {
                    // La tastiera è nascosta
                    bottomNavigationView.visibility = View.VISIBLE
                }
            }
        })
    }
}

class ChatAdapter(private val messages: List<ChatAIFragment.ChatMessage>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

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
        if (message.isUser) {
            holder.messageTextView.gravity = Gravity.END
            holder.messageTextView.setBackgroundResource(R.drawable.user_message_background)
        } else {
            holder.messageTextView.gravity = Gravity.START
            holder.messageTextView.setBackgroundResource(R.drawable.ai_message_background)
        }
    }

    override fun getItemCount() = messages.size
}

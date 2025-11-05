package com.example.mycardiacrehab.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycardiacrehab.BuildConfig
import com.example.mycardiacrehab.model.ChatMessage
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatbotViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val outOfScopeResponse = "That is out of my scope. Please consult a healthcare professional."
    private val systemPrompt = """
        Act as a helpful, non-diagnostic AI assistant for a cardiac rehabilitation patient. 
        Your responses must be supportive, educational, and strictly limited to exercise routines, 
        medication schedules, general heart health guidance, and stress management tips. 
        NEVER provide clinical diagnoses, recommend specific dosages, or offer emergency medical advice. 
        If the user asks for out-of-scope advice, or mentions symptoms requiring immediate attention (like severe chest pain), 
        you MUST reply ONLY with: '$outOfScopeResponse'. 
        Keep your answers brief, informative, and encouraging.
    """.trimIndent()

    // The hard-coded API key is GONE. This is secure.
    // The initialization is clean and uses BuildConfig.
    private val generativeModel: GenerativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY, // This will resolve after the final sync.
        systemInstruction = content { text(systemPrompt) }
    )

    var userId: String = ""

    fun loadChatHistory(currentUserId: String) {
        if (currentUserId.isBlank()) return
        userId = currentUserId

        db.collection("mycardiacrehab_chat")
            .whereEqualTo("userId", currentUserId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ChatbotViewModel", "Listen failed.", e)
                    return@addSnapshotListener
                }
                val chatList = snapshot?.toObjects(ChatMessage::class.java) ?: emptyList()
                _messages.value = chatList
            }
    }

    fun sendMessage(userText: String) = viewModelScope.launch {
        if (userText.isBlank() || _isLoading.value) return@launch
        _isLoading.value = true

        val userMessage = ChatMessage(
            userId = userId,
            role = "user",
            text = userText,
            timestamp = Timestamp.now()
        )
        db.collection("mycardiacrehab_chat").add(userMessage)

        var aiResponseText: String
        var isInScope = true

        try {
            val chatHistory = _messages.value.map { msg ->
                content(role = msg.role) { text(msg.text) }
            }

            val chat = generativeModel.startChat(history = chatHistory)
            val response = chat.sendMessage(userText)

            aiResponseText = response.text ?: "I'm sorry, I couldn't generate a coherent response."

            if (aiResponseText.contains(outOfScopeResponse, ignoreCase = true)) {
                isInScope = false
            }

        } catch (e: Exception) {
            aiResponseText = "Service error: Could not connect to AI. Check your API key and network."
            isInScope = false
            Log.e("ChatbotViewModel", "AI API Error", e)
        }

        val aiMessage = ChatMessage(
            userId = userId,
            role = "model",
            text = aiResponseText,
            timestamp = Timestamp.now(),
            isInScope = isInScope
        )
        db.collection("mycardiacrehab_chat").add(aiMessage)

        _isLoading.value = false
    }
}

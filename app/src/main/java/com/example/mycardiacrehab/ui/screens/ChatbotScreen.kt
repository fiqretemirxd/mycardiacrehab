package com.example.mycardiacrehab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mycardiacrehab.R
import com.example.mycardiacrehab.model.ChatMessage
import com.example.mycardiacrehab.viewmodel.AuthViewModel
import com.example.mycardiacrehab.viewmodel.ChatbotViewModel
import kotlinx.coroutines.launch

// Define your custom colors here, at the top of the file
val ColorUserMessage = Color(0xFF03A9F4)  // Light Blue
val ColorModelMessage = Color(0xFF4CAF50) // Green

@Composable
fun ChatbotScreen(
    authViewModel: AuthViewModel = viewModel(),
    chatbotViewModel: ChatbotViewModel = viewModel()
) {
    val authState = authViewModel.authState.collectAsState().value
    val currentUserId = (authState as? AuthViewModel.AuthState.Authenticated)?.userId ?: return

    // Load chat history when the user is authenticated
    LaunchedEffect(currentUserId) {
        chatbotViewModel.loadChatHistory(currentUserId)
    }

    val messages by chatbotViewModel.messages.collectAsState()
    val isLoading by chatbotViewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // AppHeader is currently commented out, as in your original code
        // AppHeader()

        // MessageList takes up the remaining space
        MessageList(
            modifier = Modifier.weight(1f),
            messages = messages,
            isLoading = isLoading
        )

        // Disclaimer text
        Text(
            text = "Disclaimer: This AI is for educational support only and does not replace medical advice.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // MessageInput for user to type and send
        MessageInput(
            isLoading = isLoading,
            onMessageSent = { messageText ->
                chatbotViewModel.sendMessage(messageText)
            }
        )
    }
}


@Composable
fun MessageList(
    modifier: Modifier = Modifier,
    messages: List<ChatMessage>,
    isLoading: Boolean
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 🟢 FINAL FIX: Auto-scroll to the bottom when a new message appears
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty() || isLoading) {
            coroutineScope.launch {
                // Ensure index is valid: go to last index, or 0 if empty
                val targetIndex = if (messages.isNotEmpty()) messages.size - 1 else 0
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    if (messages.isEmpty() && !isLoading) {
        // Empty state message when there are no messages
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Note: Assuming R.drawable.baseline_question_answer_24 is a placeholder and should be an Icon or SVG
            Icon(
                Icons.Default.QuestionAnswer, // Using default icon as placeholder
                contentDescription = "Chatbot Icon",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
            Text(
                text = "Ask a rehabilitation question",
                fontSize = 20.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    } else {
        // The list of messages
        LazyColumn(
            state = listState,
            modifier = modifier.padding(horizontal = 8.dp)
        ) {
            items(messages) { message ->
                ChatMessageItem(message = message)
            }
            if (isLoading) {
                item {
                    LoadingBubble()
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val isUser = message.role == "user"
    val bubbleColor = if (isUser) ColorUserMessage else ColorModelMessage
    val textColor = Color.White // Set text to white for both for better contrast

    val horizontalAlignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = horizontalAlignment
    ) {
        // The chat bubble
        Box(
            modifier = Modifier
                .padding(
                    start = if (isUser) 60.dp else 0.dp,
                    end = if (isUser) 0.dp else 60.dp,
                    top = 8.dp,
                    bottom = 4.dp
                )
                .clip(RoundedCornerShape(16.dp))
                .background(bubbleColor)
                .padding(12.dp)
        ) {
            SelectionContainer {
                Text(
                    text = message.text,
                    color = textColor,
                    fontWeight = FontWeight.W500
                )
            }
        }
        // The "Out of Scope" safety message
        if (!isUser && !message.isInScope) {
            Text(
                text = "Safety Protocol: Out of Scope",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Red.copy(alpha = 0.8f),
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
fun MessageInput(isLoading: Boolean, onMessageSent: (String) -> Unit) {
    var message by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            placeholder = { Text("Ask a question...") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            enabled = !isLoading,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.LightGray
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = {
                if (message.isNotBlank()) {
                    onMessageSent(message)
                    message = ""
                }
            },
            enabled = message.isNotBlank() && !isLoading,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                disabledContainerColor = Color.LightGray
            )
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message")
        }
    }
}

@Composable
fun LoadingBubble() {
    Row(
        modifier = Modifier.padding(start = 0.dp, end = 60.dp, top = 8.dp, bottom = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(ColorModelMessage) // Use the same green for the loading bubble
                .padding(12.dp)
        ) {
            Text(text = "Typing...", color = Color.White.copy(alpha = 0.7f)) // Make text slightly transparent
        }
    }
}

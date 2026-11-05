package com.jarvis.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

private val DeepSpace = Color(0xFF050B18)
private val Panel = Color(0xFF0B1A2D)
private val Cyan = Color(0xFF48D9FF)

class MainActivity : ComponentActivity() {
    private lateinit var vault: SecretVault
    private lateinit var memory: JsonMemoryStore
    private lateinit var openRouter: OpenRouterClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vault = SecretVault(this)
        memory = JsonMemoryStore(this)
        openRouter = OpenRouterClient(vault)

        setContent {
            JarvisTheme {
                var configured by remember { mutableStateOf(vault.hasKey()) }
                var messages by remember { mutableStateOf(memory.loadConversation()) }
                var busy by remember { mutableStateOf(false) }
                var error by remember { mutableStateOf<String?>(null) }

                if (!configured) {
                    ApiSetupScreen(onSave = { key ->
                        vault.saveKey(key)
                        configured = true
                    })
                } else {
                    JarvisHome(
                        messages = messages,
                        busy = busy,
                        error = error,
                        onSend = { prompt ->
                            if (prompt.isNotBlank() && !busy) {
                                val updated = messages + ChatMessage("user", prompt)
                                messages = updated
                                memory.saveConversation(updated)
                                busy = true
                                error = null
                                lifecycleScope.launch {
                                    try {
                                        val answer = openRouter.reply(updated)
                                        messages = messages + ChatMessage("assistant", answer)
                                        memory.saveConversation(messages)
                                    } catch (t: Throwable) {
                                        error = t.message ?: "The request failed."
                                    } finally {
                                        busy = false
                                    }
                                }
                            }
                        },
                        onChangeKey = { vault.clearKey(); configured = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Cyan, background = DeepSpace, surface = Panel,
            onBackground = Color.White, onSurface = Color.White
        ),
        content = content
    )
}

@Composable
private fun ApiSetupScreen(onSave: (String) -> Unit) {
    var key by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var warning by remember { mutableStateOf<String?>(null) }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF07182D), DeepSpace))
        ).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("J.A.R.V.I.S.", color = Cyan, fontSize = 32.sp)
            Text("INITIAL SYSTEM CONFIGURATION", color = Color.LightGray, fontSize = 12.sp)
            OutlinedTextField(
                value = key,
                onValueChange = { key = it.trim(); warning = null },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("OpenRouter API key") },
                visualTransformation = if (visible)
                    androidx.compose.ui.text.input.VisualTransformation.None
                else androidx.compose.ui.text.input.PasswordVisualTransformation()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = visible, onCheckedChange = { visible = it })
                Text("Show key", color = Color.LightGray)
            }
            warning?.let { Text(it, color = Color(0xFFFF8A80)) }
            Button(
                onClick = {
                    if (key.startsWith("sk-or-") && key.length > 20) onSave(key)
                    else warning = "Enter a valid-looking OpenRouter key."
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("SAVE & CONNECT") }
            Text(
                "The key is stored on this device using Android Keystore-backed encrypted preferences.",
                color = Color.Gray, fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun JarvisHome(
    messages: List<ChatMessage>,
    busy: Boolean,
    error: String?,
    onSend: (String) -> Unit,
    onChangeKey: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(DeepSpace).padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("J.A.R.V.I.S.", color = Cyan, fontSize = 25.sp)
                Text("PERSONAL ASSISTANT  •  ONLINE", color = Color.Gray, fontSize = 10.sp)
            }
            TextButton(onClick = onChangeKey) { Text("API SETTINGS") }
        }
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier.fillMaxWidth().height(150.dp)
                .background(Panel, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("◉", color = Cyan, fontSize = 58.sp)
                Text(if (busy) "PROCESSING" else "SYSTEM READY", color = Cyan, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                val isUser = message.role == "user"
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                ) {
                    Text(if (isUser) "CHIEF" else "JARVIS", color = Cyan, fontSize = 10.sp)
                    Surface(color = Panel, shape = RoundedCornerShape(12.dp)) {
                        Text(message.content, Modifier.padding(12.dp), color = Color.White)
                    }
                }
            }
            if (busy) item { Text("JARVIS is processing…", color = Cyan) }
            error?.let { item { Text(it, color = Color(0xFFFF8A80)) } }
        }
        Row(
            Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp)).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f).padding(10.dp),
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                decorationBox = { inner ->
                    if (input.isEmpty()) Text("Speak or type to JARVIS…", color = Color.Gray)
                    inner()
                }
            )
            IconButton(onClick = { /* Voice subsystem will be added in a later implementation. */ }) {
                Icon(Icons.Default.Mic, contentDescription = "Voice input", tint = Cyan)
            }
            IconButton(onClick = {
                val value = input.trim()
                if (value.isNotEmpty()) { onSend(value); input = "" }
            }) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Cyan)
            }
        }
    }
}

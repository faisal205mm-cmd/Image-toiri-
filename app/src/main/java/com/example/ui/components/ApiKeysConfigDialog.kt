package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ApiKeysConfigDialog(
    onDismiss: () -> Unit,
    geminiApiKeyStatus: Pair<Boolean, String>,
    customGeminiKey: String,
    isGeminiTesting: Boolean,
    onSaveGeminiKey: (String) -> Unit,
    onClearGeminiKey: () -> Unit,
    onTestGeminiKey: (String, (Boolean, String) -> Unit) -> Unit,
    pollinationsApiKeyStatus: Pair<Boolean, String>,
    customPollinationsKey: String,
    isPollinationsTesting: Boolean,
    onSavePollinationsKey: (String) -> Unit,
    onClearPollinationsKey: () -> Unit,
    onTestPollinationsKey: (String, (Boolean, String) -> Unit) -> Unit,
    onQuickPromptSelected: ((String) -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Dialog Title & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "API Keys & AI Engines",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Gemini AI ও Pollinations ইমেজ ইঞ্জিন সেটিংস",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Gemini API Key Bar
                GeminiApiKeyBar(
                    apiKeyStatus = geminiApiKeyStatus,
                    customApiKey = customGeminiKey,
                    isApiKeyTesting = isGeminiTesting,
                    onSaveApiKey = onSaveGeminiKey,
                    onClearApiKey = onClearGeminiKey,
                    onTestApiKey = onTestGeminiKey,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Pollinations API Key Bar (Directly below Gemini API Key Bar)
                PollinationsApiKeyBar(
                    apiKeyStatus = pollinationsApiKeyStatus,
                    customApiKey = customPollinationsKey,
                    isApiKeyTesting = isPollinationsTesting,
                    onSaveApiKey = onSavePollinationsKey,
                    onClearApiKey = onClearPollinationsKey,
                    onTestApiKey = onTestPollinationsKey,
                    onQuickPromptSelected = { prompt ->
                        onDismiss()
                        onQuickPromptSelected?.invoke(prompt)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "💡 টিপস: Gemini API কি চ্যাটিং ও পরামর্শের জন্য এবং Pollinations যেকোনো পোশাক ও থিমের দ্রুত ইমেজ তৈরির জন্য ব্যবহৃত হয়।",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickImagePromptDialog(
    onDismiss: () -> Unit,
    onGenerate: (String) -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    val pollinationsAccent = Color(0xFF7C4DFF)
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(scrollState)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = pollinationsAccent.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = pollinationsAccent,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ইমেজ জেনারেটর (AI Visual Engine)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "বাংলা, Banglish বা ইংরেজি প্রম্পট লিখুন",
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

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("ছবির বিবরণ (Prompt)") },
                    placeholder = { Text("যেমন: সাদা background এ একটা কালো cargo pant দেখাও...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("quick_image_prompt_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "💡 রেডিমেড টেস্ট প্রম্পটসমূহ (ট্যাপ করে লোড করুন):",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                val sampleCategories = listOf(
                    "📦 Product Photography" to listOf(
                        "সাদা background এ একটা কালো cargo pant দেখাও",
                        "একটি গোল্ডেন মেটালিক লাক্সারি পারফিউম বোতল, চারকোনা স্কয়ার ফরম্যাট",
                        "সাদা ব্যাকগ্রাউন্ডে আধুনিক ব্ল্যাক স্নিকার্স জুতো"
                    ),
                    "💃 Fashion & Model" to listOf(
                        "নীল রঙের জামদানি শাড়ি পরা একজন বাংলাদেশি নারী মডেল, স্টুডিও লাইটিং",
                        "কালো লেদার জ্যাকেট পরা একজন পুরুষ ফ্যাশন মডেলের পোর্ট্রেট"
                    ),
                    "🔤 Banglish & English" to listOf(
                        "sada background e black shoe dekhao",
                        "ekta red hoodie er front view photo dao",
                        "Front view photorealistic red hoodie on mannequin"
                    ),
                    "✍️ Text in Image" to listOf(
                        "টেবিলে রাখা একটি সাদা কফি মগ যাতে 'DAILY FOCUS' লেখা আছে"
                    )
                )

                sampleCategories.forEach { (categoryName, samples) ->
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        samples.forEach { sug ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (prompt == sug) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, if (prompt == sug) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { prompt = sug }
                            ) {
                                Text(
                                    text = sug,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = if (prompt == sug) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        val clean = prompt.trim()
                        if (clean.isNotBlank()) {
                            onDismiss()
                            onGenerate(clean)
                        }
                    },
                    enabled = prompt.trim().isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = pollinationsAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("confirm_quick_generate_image_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ছবি তৈরি করুন (Generate Visual)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

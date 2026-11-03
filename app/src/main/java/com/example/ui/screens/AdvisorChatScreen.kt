package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.core.content.ContextCompat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.db.ChatMessageEntity
import com.example.data.repository.AdvisorPersona
import com.example.ui.viewmodel.AdvisorViewModel
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvisorChatScreen(
    viewModel: AdvisorViewModel,
    modifier: Modifier = Modifier,
    onOpenHistory: () -> Unit = {}
) {
    val selectedPersona by viewModel.selectedPersona.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isChatLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()
    val chatInput by viewModel.chatInput.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val attachedMedia by viewModel.attachedMedia.collectAsStateWithLifecycle()

    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val speechPartialText by viewModel.speechPartialText.collectAsStateWithLifecycle()
    val speechRmsDb by viewModel.speechRmsDb.collectAsStateWithLifecycle()
    val speechError by viewModel.speechError.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val currentSpeakingMessageId by viewModel.currentSpeakingMessageId.collectAsStateWithLifecycle()
    val autoVoiceReply by viewModel.autoVoiceReply.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    var showClearDialog by remember { mutableStateOf(false) }
    var showMediaAttachMenu by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.startVoiceInput()
        } else {
            Toast.makeText(context, "ভয়েস অপশন ব্যবহারের জন্য মাইক্রোফোন পারমিশন প্রয়োজন", Toast.LENGTH_SHORT).show()
        }
    }

    val onVoiceMicClicked = {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            if (isListening) {
                viewModel.stopVoiceInput()
            } else {
                viewModel.startVoiceInput()
            }
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.attachMediaUri(uri, context)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: android.graphics.Bitmap? ->
        if (bitmap != null) {
            viewModel.attachBitmap(bitmap, context)
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.attachMediaUri(uri, context)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.attachMediaUri(uri, context)
        }
    }

    // Auto scroll to bottom when new messages arrive
    LaunchedEffect(chatMessages.size, isChatLoading) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Conversation") },
            text = { Text("Are you sure you want to clear this conversation history?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCurrentChat()
                        showClearDialog = false
                    },
                    modifier = Modifier.testTag("confirm_clear_chat_button")
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }


    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(start = 10.dp, end = 10.dp, top = 2.dp, bottom = 4.dp)
    ) {
        // Horizontal Persona Chips Capsule Bar directly under top bar (eliminating top blank space)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF04141B),
            border = BorderStroke(1.2.dp, Color(0xFF00E676).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(AdvisorPersona.values()) { persona ->
                        val isSelected = persona == selectedPersona
                        val chipBg = if (isSelected) Color(0xFF00A859) else Color(0xFF0A1B22)
                        val contentColor = Color.White
                        val border = if (isSelected) {
                            BorderStroke(1.5.dp, Color(0xFF00E676))
                        } else {
                            BorderStroke(1.dp, Color(0xFF1B3640))
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = chipBg,
                            border = border,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { viewModel.selectPersona(persona) }
                                .testTag("mode_chip_${persona.name.lowercase()}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                val icon: ImageVector = when (persona) {
                                    AdvisorPersona.GENERAL -> Icons.Default.AutoAwesome
                                    AdvisorPersona.CREATIVE -> Icons.Default.Palette
                                    AdvisorPersona.CODING -> Icons.Default.Code
                                    AdvisorPersona.LEARNING -> Icons.Default.School
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = persona.title,
                                    tint = contentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = persona.title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = contentColor
                                )
                            }
                        }
                    }
                }
            }
        }

        // Futuristic Dark Obsidian Card containing the chat view
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF04141B)),
            border = BorderStroke(1.2.dp, Color(0xFF00E676).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Layer 0: Decorative glowing emerald circles & arcs matching the screenshot!
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Top-left glowing arc
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF00E676).copy(alpha = 0.22f), Color.Transparent),
                            center = Offset(0f, 0f),
                            radius = size.width * 0.65f
                        ),
                        radius = size.width * 0.65f,
                        center = Offset(0f, 0f)
                    )
                    drawCircle(
                        color = Color(0xFF00E676).copy(alpha = 0.35f),
                        center = Offset(0f, 0f),
                        radius = size.width * 0.55f,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFF00E676).copy(alpha = 0.2f),
                        center = Offset(0f, 0f),
                        radius = size.width * 0.7f,
                        style = Stroke(width = 1.2.dp.toPx())
                    )

                    // Bottom-left glowing circle
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF00E676).copy(alpha = 0.28f), Color.Transparent),
                            center = Offset(size.width * 0.1f, size.height * 0.75f),
                            radius = size.width * 0.45f
                        ),
                        radius = size.width * 0.45f,
                        center = Offset(size.width * 0.1f, size.height * 0.75f)
                    )
                    drawCircle(
                        color = Color(0xFF00E676).copy(alpha = 0.35f),
                        center = Offset(size.width * 0.1f, size.height * 0.75f),
                        radius = size.width * 0.4f,
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // Bottom-right concentric glowing arcs
                    drawCircle(
                        color = Color(0xFF00E676).copy(alpha = 0.35f),
                        center = Offset(size.width * 0.95f, size.height * 0.8f),
                        radius = size.width * 0.6f,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFF00E676).copy(alpha = 0.22f),
                        center = Offset(size.width * 0.95f, size.height * 0.8f),
                        radius = size.width * 0.75f,
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }

                // Layer 1: Foreground Column
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Messages Thread
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
            if (chatMessages.isEmpty()) {
                // Empty state greeting with topic suggestions & photo/video upload buttons
                EmptyChatGreeting(
                    persona = selectedPersona,
                    historyCount = allSessions.size,
                    onOpenHistory = onOpenHistory,
                    onPromptClick = { prompt -> viewModel.sendChatMessage(prompt) },
                    onVoiceClick = onVoiceMicClicked,
                    onUploadPhotoClick = {
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onUploadVideoClick = {
                        videoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    },
                    onBrowseFilesClick = {
                        filePickerLauncher.launch("*/*")
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(chatMessages, key = { it.id }) { msg ->
                        val priorUserMsg = if (msg.role == "model") {
                            chatMessages.takeWhile { it.id != msg.id }.lastOrNull { it.role == "user" }
                        } else null

                        ChatMessageBubble(
                            message = msg,
                            persona = selectedPersona,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(msg.message))
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            onEditPrompt = { promptToEdit ->
                                viewModel.onChatInputChange(promptToEdit)
                            },
                            onOpenHistory = onOpenHistory,
                            onRetry = {
                                priorUserMsg?.let { userMsg ->
                                    viewModel.retryMessage(userMsg.message)
                                }
                            },
                            isSpeaking = isSpeaking && (currentSpeakingMessageId == msg.id),
                            onSpeak = { viewModel.speakMessage(msg.id, msg.message) },
                            onStopSpeak = { viewModel.stopSpeaking() }
                        )
                    }

                    if (isChatLoading) {
                        item {
                            AdvisorTypingIndicator(
                                persona = selectedPersona
                            )
                        }
                    }
                }
            }
        }

        // Suggested Prompt Quick Row (if chat is active and no media attached)
        if (chatMessages.isNotEmpty() && !isChatLoading && attachedMedia == null) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            ) {
                items(selectedPersona.suggestedPrompts) { prompt ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { viewModel.sendChatMessage(prompt) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = prompt,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Attached Media Preview Bar (Shown when a photo/video is attached before sending)
        AnimatedVisibility(visible = attachedMedia != null) {
            attachedMedia?.let { media ->
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Thumbnail / Icon
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (media.isVideo) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = "Video attached",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    SubcomposeAsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(File(media.localFilePath))
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Attached preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (media.isVideo) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = if (media.isVideo) "VIDEO" else "PHOTO",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                            color = if (media.isVideo) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = media.fileName,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = if (media.isVideo) "Attached product video ready for review" else "Attached product photo ready for AI vision analysis",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Remove Attachment Button
                            IconButton(
                                onClick = { viewModel.removeAttachedMedia() },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .testTag("remove_attached_media_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Attachment",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Quick action chips for attached product media
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            val productMediaPrompts = if (media.isVideo) {
                                listOf(
                                    "🎥 এই ভিডিওর জন্য আকর্ষনীয় সোশ্যাল মিডিয়া ক্যাপশন ও হ্যাশট্যাগ দাও",
                                    "💰 এই প্রোডাক্টের জন্য পাইকারি ও খুচরা বিক্রয় মূল্য কত রাখা যায়?",
                                    "📈 ভিডিও দেখে পোশাকটির মার্কেটিং স্ট্র্যাটেজি কেমন হওয়া উচিত?"
                                )
                            } else {
                                listOf(
                                    "🔍 এই কাপড়ের ফ্যাব্রিক কোয়ালিটি ও সেলাই ফিনিশিং কেমন?",
                                    "💰 কাপড়টির আনুমানিক উৎপাদন খরচ ও বিক্রয় মূল্য কত হতে পারে?",
                                    "✨ ফেসবুক ও ইনস্টাগ্রামে বিক্রির জন্য একটি ক্যাপশন লিখে দাও",
                                    "🎨 এই কাপড়ের সাথে কী ধরনের ম্যাচিং বা ডিজাইন আরও ভালো লাগবে?"
                                )
                            }

                            items(productMediaPrompts) { quickPrompt ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { viewModel.sendChatMessage(quickPrompt) }
                                ) {
                                    Text(
                                        text = quickPrompt,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Modal Bottom Sheet for Add Options
        if (showMediaAttachMenu) {
            ModalBottomSheet(
                onDismissRequest = { showMediaAttachMenu = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .padding(bottom = 24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "যুক্ত করুন (Add Options)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "ছবি, ভিডিও বা AI ডিজাইন নির্বাচন করুন",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { showMediaAttachMenu = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 1. Camera Option
                    Surface(
                        onClick = {
                            showMediaAttachMenu = false
                            cameraLauncher.launch(null)
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("sheet_take_photo")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "📸 ক্যামেরা দিয়ে ছবি তুলুন (Camera)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "ক্যামেরা দিয়ে কাপড়ের লাইভ ছবি তুলে পাঠান",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // 2. Photo Gallery Option
                    Surface(
                        onClick = {
                            showMediaAttachMenu = false
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("sheet_pick_photo")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🖼️ গ্যালারি থেকে ছবি (Photo Gallery)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "পোশাক বা ফ্যাব্রিকের ছবি আপলোড করুন",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // 3. Video Option
                    Surface(
                        onClick = {
                            showMediaAttachMenu = false
                            videoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("sheet_pick_video")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🎥 ভিডিও যুক্ত করুন (Video)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "কাপড়ের ট্রায়াল বা ফ্যাব্রিকেশনের ভিডিও দিন",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // 4. File Browser Option
                    Surface(
                        onClick = {
                            showMediaAttachMenu = false
                            filePickerLauncher.launch("*/*")
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("sheet_browse_files")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "📁 ফাইল ব্রাউজার (Browse Files)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "ডিভাইসের মেমোরি কার্ড বা ফাইল স্টোরেজ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Chat Input Bar & Voice Controls inside the dark card
        Surface(
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // AI Speaking banner
                AnimatedVisibility(
                    visible = isSpeaking,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    VoiceSpeakingBanner(
                        onStopSpeaking = { viewModel.stopSpeaking() }
                    )
                }

                // Voice Listening waveform & live preview card
                AnimatedVisibility(
                    visible = isListening,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    VoiceListeningCard(
                        partialText = speechPartialText,
                        rmsDb = speechRmsDb,
                        onStopAndSend = { viewModel.stopVoiceInput() },
                        onCancel = { viewModel.cancelVoiceInput() }
                    )
                }

                // Voice Error notice
                if (speechError != null && !isListening) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = speechError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp)
                ) {
                    // Left Green Circular Add Button (+)
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00C853))
                            .clickable { showMediaAttachMenu = true }
                            .testTag("add_option_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (attachedMedia != null) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = "Add Option",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Rounded Capsule Text Field with Mic inside trailing edge
                    OutlinedTextField(
                        value = chatInput,
                        onValueChange = { viewModel.onChatInputChange(it) },
                        placeholder = {
                            Text(
                                text = if (attachedMedia != null) {
                                    "কাপড়ের বিস্তারিত লিখুন বা সরাসরি পাঠান..."
                                } else {
                                    "লিখুন বা মাইকে কথা বলুন..."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        trailingIcon = {
                            Box(
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF102630))
                                    .border(1.dp, Color(0xFF00E676).copy(alpha = 0.45f), CircleShape)
                                    .clickable { onVoiceMicClicked() }
                                    .testTag("voice_mic_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = if (isListening) "Stop Voice Listening" else "Voice Input",
                                    tint = if (isListening) Color(0xFFEF4444) else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        shape = RoundedCornerShape(26.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E676),
                            unfocusedBorderColor = Color(0xFF00E676).copy(alpha = 0.5f),
                            focusedContainerColor = Color(0xFF071B21),
                            unfocusedContainerColor = Color(0xFF071B21),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFF00E676)
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Right Green Circular Send Button (>)
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00C853))
                            .clickable { viewModel.sendChatMessage() }
                            .testTag("send_chat_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChatLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Message",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
            }
        }
    }
}
}





@Composable
fun EmptyChatGreeting(
    persona: AdvisorPersona,
    historyCount: Int = 0,
    onOpenHistory: () -> Unit = {},
    onPromptClick: (String) -> Unit = {},
    onVoiceClick: () -> Unit = {},
    onUploadPhotoClick: () -> Unit = {},
    onUploadVideoClick: () -> Unit = {},
    onBrowseFilesClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("empty_chat_greeting"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            // Glowing Green & Lime Orb
            Box(
                modifier = Modifier
                    .size(176.dp),
                contentAlignment = Alignment.Center
            ) {
                // Soft outer ambient glow
                Box(
                    modifier = Modifier
                        .size(176.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF00E676).copy(alpha = 0.35f),
                                    Color(0xFF00E676).copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Central Gradient Badge: Green (#00C853) to Lime (#CDDC39)
                Box(
                    modifier = Modifier
                        .size(138.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF00C853),
                                    Color(0xFF00E676),
                                    Color(0xFFAEEA00),
                                    Color(0xFFCDDC39)
                                ),
                                start = Offset(0f, 138f),
                                end = Offset(138f, 0f)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Sparkle",
                        tint = Color.White,
                        modifier = Modifier.size(68.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Greeting Headline from Screenshot: কিভাবে আপনাকে সাহায্য করতে পারি?
            Text(
                text = "কিভাবে আপনাকে সাহায্য করতে পারি?",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Greeting Subtitle from Screenshot: আপনার প্রশ্ন লিখুন, আমি আছি আপনার সাথে
            Text(
                text = "আপনার প্রশ্ন লিখুন, আমি আছি আপনার সাথে",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp
                ),
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Rounded Green Indicator Accent Bar
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(3.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF00E676))
            )
        }
    }
}

/**
 * Unified Dispatcher for Chat Message Bubbles
 */
@Composable
fun ChatMessageBubble(
    message: ChatMessageEntity,
    persona: AdvisorPersona,
    onCopy: () -> Unit,
    onEditPrompt: (String) -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onRetry: () -> Unit = {},
    isSpeaking: Boolean = false,
    onSpeak: () -> Unit = {},
    onStopSpeak: () -> Unit = {}
) {
    val isUser = message.role == "user"
    val timeFormatted = remember(message.timestamp) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.format(Date(message.timestamp))
    }
    val context = LocalContext.current

    if (isUser) {
        UserMessageBubble(
            text = message.message,
            imageUrl = message.imageUrl,
            videoUrl = message.videoUrl,
            mediaType = message.mediaType,
            mediaFileName = message.mediaFileName,
            timestamp = timeFormatted,
            onVideoClick = { videoPath ->
                com.example.data.util.MediaUtils.openMediaFile(context, videoPath, "video/*")
            }
        )
    } else {
        AdvisorMessageBubble(
            text = message.message,
            timestamp = timeFormatted,
            persona = persona,
            onCopy = onCopy,
            onOpenHistory = onOpenHistory,
            onRetry = onRetry,
            isSpeaking = isSpeaking,
            onSpeak = onSpeak,
            onStopSpeak = onStopSpeak
        )
    }
}

/**
 * Modern, High-Contrast User Message Bubble Supporting Photos & Videos
 */
@Composable
fun UserMessageBubble(
    text: String,
    imageUrl: String? = null,
    videoUrl: String? = null,
    mediaType: String? = null,
    mediaFileName: String? = null,
    timestamp: String,
    onVideoClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = 20.dp,
                    bottomEnd = 4.dp
                ),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    // --- Render Attached Product Image in User Bubble ---
                    if (imageUrl != null && mediaType != "VIDEO") {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    com.example.data.util.MediaUtils.openMediaFile(context, imageUrl, "image/*")
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            ) {
                                SubcomposeAsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(if (imageUrl.startsWith("/")) File(imageUrl) else imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Attached Product Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                // Tag badge
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.Black.copy(alpha = 0.65f),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddPhotoAlternate,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Product Photo",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // --- Render Attached Product Video in User Bubble ---
                    if (videoUrl != null || mediaType == "VIDEO") {
                        val activeVideoPath = videoUrl ?: imageUrl ?: ""
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.25f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onVideoClick(activeVideoPath) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play Video",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mediaFileName ?: "Product Video",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "ভিডিও চালাতে ট্যাপ করুন",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text(
                    text = timestamp,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = "Sent",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/**
 * Modern, Distinctive AI Advisor Message Bubble with Rich Markdown
 */
@Composable
fun AdvisorMessageBubble(
    text: String,
    timestamp: String,
    persona: AdvisorPersona,
    onCopy: () -> Unit,
    onOpenHistory: () -> Unit = {},
    onRetry: () -> Unit = {},
    isSpeaking: Boolean = false,
    onSpeak: () -> Unit = {},
    onStopSpeak: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var copiedState by remember { mutableStateOf(false) }

    val isErrorOrNotice = text.contains("⚠️ **Gemini") || text.contains("⚠️ **Connection Error")
    val isApiKeyRelated = isErrorOrNotice && (text.contains("API Key") || text.contains("API key"))
    val isRateLimit = isErrorOrNotice && (text.contains("429") || text.contains("Rate Limit") || text.contains("Quota"))

    LaunchedEffect(copiedState) {
        if (copiedState) {
            delay(2000)
            copiedState = false
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        // AI Avatar
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = if (isErrorOrNotice) {
                            listOf(
                                MaterialTheme.colorScheme.errorContainer,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        } else {
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    )
                )
                .border(
                    1.dp,
                    if (isErrorOrNotice) MaterialTheme.colorScheme.error.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isErrorOrNotice) Icons.Default.Warning else Icons.Default.AutoAwesome,
                contentDescription = "AI Assistant",
                tint = if (isErrorOrNotice) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 4.dp,
                    topEnd = 20.dp,
                    bottomStart = 20.dp,
                    bottomEnd = 20.dp
                ),
                color = if (isErrorOrNotice) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    if (isErrorOrNotice) MaterialTheme.colorScheme.error.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant
                ),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header row with Persona tag & Action buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${persona.title} AI",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (isSpeaking) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "বলছে...",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Listen / Stop Speaker Button
                            IconButton(
                                onClick = {
                                    if (isSpeaking) onStopSpeak() else onSpeak()
                                },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("speak_message_button")
                            ) {
                                Icon(
                                    imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = if (isSpeaking) "Stop Voice" else "Listen Voice",
                                    tint = if (isSpeaking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(2.dp))

                            IconButton(
                                onClick = {
                                    onCopy()
                                    copiedState = true
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (copiedState) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = "Copy Response",
                                    tint = if (copiedState) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Formatted AI message content
                    FormattedMarkdownContent(
                        rawText = text,
                        textColor = MaterialTheme.colorScheme.onSurface
                    )

                    // If Rate Limit (429) or API Key error or Notice occurred, provide action buttons
                    if (isErrorOrNotice) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onRetry,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isRateLimit) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("retry_request_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Retry (পুনরায় চেষ্টা)", style = MaterialTheme.typography.labelSmall)
                            }

                            OutlinedButton(
                                onClick = onOpenHistory,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("fix_api_key_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("API Key Settings", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = timestamp,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

/**
 * Parses and formats markdown-like text with bold tokens, headers, and bullet points
 */
@Composable
fun FormattedMarkdownContent(
    rawText: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val lines = remember(rawText) { rawText.lines() }

    Column(modifier = modifier) {
        lines.forEachIndexed { _, line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
            } else if (trimmed.startsWith("### ") || trimmed.startsWith("## ")) {
                val headerText = trimmed.removePrefix("### ").removePrefix("## ")
                Text(
                    text = headerText,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else if (trimmed.startsWith("* ") || trimmed.startsWith("- ") || trimmed.startsWith("• ")) {
                val bulletContent = trimmed.removePrefix("* ").removePrefix("- ").removePrefix("• ")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 7.dp, end = 8.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = buildAnnotatedStringWithBold(bulletContent, textColor),
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp)
                    )
                }
            } else {
                Text(
                    text = buildAnnotatedStringWithBold(trimmed, textColor),
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}

/**
 * Builds an AnnotatedString parsing inline **bold** text
 */
fun buildAnnotatedStringWithBold(text: String, defaultColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        val regex = Regex("\\*\\*(.*?)\\*\\*")
        val matches = regex.findAll(text)

        for (match in matches) {
            val start = match.range.first
            val end = match.range.last + 1

            if (start > currentIndex) {
                withStyle(SpanStyle(color = defaultColor)) {
                    append(text.substring(currentIndex, start))
                }
            }

            val boldContent = match.groupValues[1]
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = defaultColor
                )
            ) {
                append(boldContent)
            }

            currentIndex = end
        }

        if (currentIndex < text.length) {
            withStyle(SpanStyle(color = defaultColor)) {
                append(text.substring(currentIndex))
            }
        }
    }
}

/**
 * Modern Pulsing Dot Animation for AI Typing State
 */
@Composable
fun AdvisorTypingIndicator(
    persona: AdvisorPersona
) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_dots")

    val dot1Scale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )

    val dot2Scale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 180, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )

    val dot3Scale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 360, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Surface(
            shape = RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 18.dp,
                bottomStart = 18.dp,
                bottomEnd = 18.dp
            ),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 1.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // 3 Pulsing Animated Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .scale(dot1Scale)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .scale(dot2Scale)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .scale(dot3Scale)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "AI is thinking...",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Animated Listening Waveform & Preview Card for Speech-To-Text
 */
@Composable
fun VoiceListeningCard(
    partialText: String,
    rmsDb: Float,
    onStopAndSend: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voice_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        tonalElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .scale(pulseScale)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "🎙️ আপনার কথা শুনছি...",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "স্পষ্ট করে বাংলায় বা ইংরেজিতে বলুন",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel Voice",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Recognized text live preview
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (partialText.isNotBlank()) {
                        Text(
                            text = "\"$partialText\"",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "মুখে বলুন, যেমন: \"আজকের মোট বিক্রি কত?\" বা \"জর্জেট কাপড়ের দাম কেমন?\"...",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("বাতিল")
                }

                Button(
                    onClick = onStopAndSend,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("পাঠান ও শুনুন", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Animated Banner when AI is Speaking
 */
@Composable
fun VoiceSpeakingBanner(
    onStopSpeaking: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "speaking_wave")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_scale"
    )

    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
        tonalElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .scale(waveScale)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "🔊 AI উত্তর শোনাচ্ছে...",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "কথা শেষ হলে বন্ধ হবে, বা সরাসরি থামাতে পারেন",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                    )
                }
            }

            FilledTonalButton(
                onClick = onStopSpeaking,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("থামান", fontWeight = FontWeight.Bold)
            }
        }
    }
}

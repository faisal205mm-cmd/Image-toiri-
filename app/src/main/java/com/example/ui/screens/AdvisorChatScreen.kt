package com.example.ui.screens

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
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
    val isImageMode by viewModel.isImageMode.collectAsStateWithLifecycle()
    val selectedAspectRatio by viewModel.selectedAspectRatio.collectAsStateWithLifecycle()
    val attachedMedia by viewModel.attachedMedia.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    var showClearDialog by remember { mutableStateOf(false) }
    var showMediaAttachMenu by remember { mutableStateOf(false) }
    var viewingImageUrl by remember { mutableStateOf<String?>(null) }
    var viewingImagePrompt by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

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

    // Fullscreen Image Preview Dialog
    if (viewingImageUrl != null) {
        FullscreenImageViewerDialog(
            imageUrl = viewingImageUrl!!,
            prompt = viewingImagePrompt,
            onDismiss = { viewingImageUrl = null },
            onShare = { path -> shareImage(context, path) },
            onEditPrompt = { promptToEdit ->
                viewModel.toggleImageMode(true)
                viewModel.onChatInputChange(promptToEdit)
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Mode Selection Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isImageMode) "Mode:" else "AI Mode:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isImageMode) "🎨 Image Generator (Gemini & Pollinations AI)" else selectedPersona.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isImageMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (chatMessages.isNotEmpty()) {
                            IconButton(
                                onClick = { showClearDialog = true },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("clear_chat_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Clear Chat",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Horizontal Persona / Mode Chips & Image Mode Chip
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    // Image Mode Chip
                    item {
                        FilterChip(
                            selected = isImageMode,
                            onClick = { viewModel.toggleImageMode() },
                            label = {
                                Text(
                                    text = "🎨 Image Generator",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isImageMode) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Image Generation",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ),
                            modifier = Modifier.testTag("image_mode_toggle_chip")
                        )
                    }

                    items(AdvisorPersona.values()) { persona ->
                        val isSelected = persona == selectedPersona && !isImageMode
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isImageMode) viewModel.toggleImageMode(false)
                                viewModel.selectPersona(persona)
                            },
                            label = {
                                Text(
                                    text = persona.title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            leadingIcon = {
                                val icon: ImageVector = when (persona) {
                                    AdvisorPersona.GENERAL -> Icons.Default.AutoAwesome
                                    AdvisorPersona.CREATIVE -> Icons.Default.Palette
                                    AdvisorPersona.CODING -> Icons.Default.Code
                                    AdvisorPersona.LEARNING -> Icons.Default.School
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = persona.title,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("mode_chip_${persona.name.lowercase()}")
                        )
                    }
                }
            }
        }

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
                    isImageMode = isImageMode,
                    historyCount = allSessions.size,
                    onOpenHistory = onOpenHistory,
                    onPromptClick = { prompt -> viewModel.sendChatMessage(prompt) },
                    onActivateImageMode = { viewModel.toggleImageMode(true) },
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
                            onImageClick = { url, prompt ->
                                viewingImageUrl = url
                                viewingImagePrompt = prompt
                            },
                            onEditPrompt = { promptToEdit ->
                                viewModel.toggleImageMode(true)
                                viewModel.onChatInputChange(promptToEdit)
                            },
                            onOpenHistory = onOpenHistory,
                            onRetry = {
                                priorUserMsg?.let { userMsg ->
                                    viewModel.retryMessage(userMsg.message, userMsg.isImagePrompt)
                                }
                            }
                        )
                    }

                    if (isChatLoading) {
                        item {
                            AdvisorTypingIndicator(
                                persona = selectedPersona,
                                isImageGenerating = isImageMode || chatInput.contains("image", ignoreCase = true)
                            )
                        }
                    }
                }
            }
        }

        // Image Mode Settings Banner (Aspect Ratio & Quick Prompts)
        AnimatedVisibility(visible = isImageMode) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Aspect Ratio:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val ratios = listOf(
                                "1:1" to "Square (1:1)",
                                "16:9" to "Wide (16:9)",
                                "9:16" to "Story (9:16)",
                                "4:3" to "Standard (4:3)"
                            )
                            ratios.forEach { (ratioKey, _) ->
                                val isRatioSelected = selectedAspectRatio == ratioKey
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isRatioSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, if (isRatioSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { viewModel.setSelectedAspectRatio(ratioKey) }
                                        .testTag("ratio_$ratioKey")
                                ) {
                                    Text(
                                        text = ratioKey,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isRatioSelected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick image prompt suggestions
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        val imageSuggestions = listOf(
                            "🏪 একটি আধুনিক বাংলাদেশের কাপড়ের দোকানের interior তৈরি করো",
                            "📸 একটি realistic fashion product photo তৈরি করো",
                            "👗 White background-এ একটি সম্পূর্ণ dress-এর product photography তৈরি করো",
                            "✨ প্রিমিয়াম সিল্ক জামদানি শাড়ির স্টুডিও লুকবুক ফটো",
                            "👔 হ্যান্ডলুম খাদি পাঞ্জাবির মিনিমালিস্ট ক্যাটালগ ফটো",
                            "🧥 Oversized streetwear hoodie mockup with graffiti print"
                        )
                        items(imageSuggestions) { suggestion ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { 
                                        val clean = suggestion.substringAfter(" ")
                                        viewModel.sendChatMessage(clean, forceImageMode = true) 
                                    }
                            ) {
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Suggested Prompt Quick Row (if chat is active and not image mode and no media attached)
        if (chatMessages.isNotEmpty() && !isChatLoading && !isImageMode && attachedMedia == null) {
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

                    // 5. AI Garment Designer
                    Surface(
                        onClick = {
                            showMediaAttachMenu = false
                            viewModel.toggleImageMode(true)
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("sheet_ai_design")
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
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "✨ AI পোশাক ডিজাইন (AI Designer)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "প্রম্পট দিয়ে নতুন পোশাকের ইমেজ তৈরি করুন",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Chat Input Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                // Dedicated High-Visibility Add (+) Option Button on Left side of Text Bar
                IconButton(
                    onClick = { showMediaAttachMenu = true },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (attachedMedia != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            }
                        )
                        .testTag("add_option_button")
                ) {
                    Icon(
                        imageVector = if (attachedMedia != null) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = "Add Option",
                        tint = if (attachedMedia != null) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = chatInput,
                    onValueChange = { viewModel.onChatInputChange(it) },
                    placeholder = {
                        Text(
                            text = if (attachedMedia != null) {
                                "কাপড়ের বিস্তারিত লিখুন বা সরাসরি পাঠান..."
                            } else if (isImageMode) {
                                "টেক্সট প্রম্পট লিখুন (যেমন: White background-এ dress এর ছবি)..."
                            } else {
                                "কাপড়ের বিবরণ লিখুন বা '+' চেপে যুক্ত করুন..."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isImageMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                FloatingActionButton(
                    onClick = { viewModel.sendChatMessage() },
                    shape = CircleShape,
                    containerColor = if (isImageMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    contentColor = if (isImageMode) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("send_chat_button")
                ) {
                    if (isChatLoading) {
                        CircularProgressIndicator(
                            color = if (isImageMode) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (isImageMode) Icons.Default.AutoAwesome else Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyChatGreeting(
    persona: AdvisorPersona,
    isImageMode: Boolean = false,
    historyCount: Int = 0,
    onOpenHistory: () -> Unit = {},
    onPromptClick: (String) -> Unit,
    onActivateImageMode: () -> Unit = {},
    onUploadPhotoClick: () -> Unit = {},
    onUploadVideoClick: () -> Unit = {},
    onBrowseFilesClick: () -> Unit = {}
) {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = if (isImageMode) {
                                listOf(
                                    MaterialTheme.colorScheme.tertiaryContainer,
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
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isImageMode) Icons.Default.Image else Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = if (isImageMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isImageMode) "🎨 AI Image Generator" else "Cloth & Fashion Store Assistant",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isImageMode) "Gemini 2.5 Flash Image দিয়ে যে কোনো পোশাকের ছবি, মডেল লুকবুক ও স্টুডিও ফটোগ্রাফি তৈরি করুন" else "পোশাকের ছবি বা ভিডিও আপলোড করুন এবং ফ্যাব্রিক, প্রাইসিং ও মার্কেটিং বিশ্লেষণ পান",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Upload Photo / Video / Browse Files
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                FilledTonalButton(
                    onClick = onUploadPhotoClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("upload_photo_greeting_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ছবি আপলোড",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                FilledTonalButton(
                    onClick = onUploadVideoClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    modifier = Modifier.testTag("upload_video_greeting_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ভিডিও আপলোড",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            if (!isImageMode) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onActivateImageMode,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("switch_to_image_mode_banner_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI দিয়ে পোশাক ডিজাইন করুন (Generate Design)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            if (historyCount > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onOpenHistory() }
                        .testTag("open_history_from_empty_greeting")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "View $historyCount saved conversation${if (historyCount > 1) "s" else ""}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (isImageMode) "Popular Visual Prompts:" else "জনপ্রিয় প্রশ্ন ও কমান্ড:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isImageMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))
        }

        val promptsToShow = if (isImageMode) {
            listOf(
                "🏪 একটি আধুনিক বাংলাদেশের কাপড়ের দোকানের interior তৈরি করো",
                "📸 একটি realistic fashion product photo তৈরি করো",
                "👗 White background-এ একটি সম্পূর্ণ dress-এর product photography তৈরি করো",
                "✨ প্রিমিয়াম সিল্ক জামদানি শাড়ির স্টুডিও লুকবুক ফটো",
                "👔 হ্যান্ডলুম খাদি পাঞ্জাবির মিনিমালিস্ট ক্যাটালগ ফটো",
                "🧥 Oversized streetwear hoodie mockup with futuristic cyberpunk print"
            )
        } else {
            listOf(
                "📊 আমার দোকানের মোট বিক্রির হিসাব ও লাভ দেখাও",
                "☁️ ক্লাউডে সংরক্ষিত বিক্রির ডাটা ও কাস্টমার বাকি পর্যালোচনা করো",
                "💵 এই মাসের মোট বিক্রি, খরচ এবং নিট ব্যালেন্স কত?",
                "👕 এই কাপড়ের কোয়ালিটি ও দাম কত রাখা উচিত?",
                "👗 ফেসবুক পেইজে পোশাক বিক্রির আকর্ষণীয় ক্যাপশন লেখো",
                "🧵 কাপড়ের জিএসএম (GSM) এবং ফ্যাব্রিক টাইপ বোঝার নিয়ম কি?",
                "📈 কাপড়ের দোকানের জন্য কার্যকরী সোশ্যাল মিডিয়া মার্কেটিং প্ল্যান"
            )
        }

        items(promptsToShow) { prompt ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { 
                        val clean = if (isImageMode) prompt.substringAfter(" ") else prompt
                        onPromptClick(clean) 
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Icon(
                        imageVector = if (isImageMode) Icons.Default.Image else Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = if (isImageMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
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
    onImageClick: (String, String) -> Unit = { _, _ -> },
    onEditPrompt: (String) -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onRetry: () -> Unit = {}
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
            isImagePrompt = message.isImagePrompt,
            onImageClick = { url -> onImageClick(url, message.message) },
            onVideoClick = { videoPath ->
                com.example.data.util.MediaUtils.openMediaFile(context, videoPath, "video/*")
            }
        )
    } else {
        AdvisorMessageBubble(
            text = message.message,
            imageUrl = message.imageUrl,
            timestamp = timeFormatted,
            persona = persona,
            onCopy = onCopy,
            onImageClick = { url -> onImageClick(url, message.message) },
            onEditPrompt = onEditPrompt,
            onOpenHistory = onOpenHistory,
            onRetry = onRetry
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
    isImagePrompt: Boolean = false,
    onImageClick: (String) -> Unit = {},
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
                color = if (isImagePrompt) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (isImagePrompt) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Image Request",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // --- Render Attached Product Image in User Bubble ---
                    if (imageUrl != null && mediaType != "VIDEO") {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onImageClick(imageUrl) }
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
                        color = if (isImagePrompt) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary
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
 * Modern, Distinctive AI Advisor Message Bubble with Rich Markdown & Image Rendering
 */
@Composable
fun AdvisorMessageBubble(
    text: String,
    imageUrl: String?,
    timestamp: String,
    persona: AdvisorPersona,
    onCopy: () -> Unit,
    onImageClick: (String) -> Unit = {},
    onEditPrompt: (String) -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var copiedState by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val isErrorOrNotice = text.contains("⚠️ **Gemini") || text.contains("⚠️ **Connection Error") || text.contains("⚠️ **Image Generation Error")
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
                        } else if (imageUrl != null) {
                            listOf(
                                MaterialTheme.colorScheme.tertiaryContainer,
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
                imageVector = when {
                    isErrorOrNotice -> Icons.Default.Warning
                    imageUrl != null -> Icons.Default.Image
                    else -> Icons.Default.AutoAwesome
                },
                contentDescription = "AI Assistant",
                tint = when {
                    isErrorOrNotice -> MaterialTheme.colorScheme.error
                    imageUrl != null -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                },
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
                color = when {
                    isErrorOrNotice -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                    imageUrl != null -> MaterialTheme.colorScheme.surface
                    else -> MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    1.dp,
                    when {
                        isErrorOrNotice -> MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
                        imageUrl != null -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
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
                                text = if (imageUrl != null) "🎨 Gemini 3.1 Flash Image" else "${persona.title} AI",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = if (imageUrl != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
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

                    // --- Render Generated Image if Available ---
                    if (imageUrl != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                .clickable { onImageClick(imageUrl) }
                                .testTag("generated_image_preview")
                        ) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(if (imageUrl.startsWith("/")) File(imageUrl) else imageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Generated AI Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp)),
                                loading = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(32.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                },
                                error = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "⚠️ Image preview unavailable",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            )

                            // Zoom overlay badge on top-right of image
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fullscreen,
                                        contentDescription = "View Fullscreen",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Expand",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Action row for Image (Fullscreen / Edit / Save / Share)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { onImageClick(imageUrl) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Expand", style = MaterialTheme.typography.labelSmall)
                            }

                            FilledTonalButton(
                                onClick = { 
                                    val extractedPrompt = if (text.contains("Prompt:")) {
                                        text.substringAfter("Prompt:").substringBefore("\n").trim().removeSurrounding("*")
                                    } else {
                                        text.take(80)
                                    }
                                    onEditPrompt(extractedPrompt)
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Edit", style = MaterialTheme.typography.labelSmall)
                            }

                            FilledTonalButton(
                                onClick = {
                                    val (success, msg) = com.example.data.util.MediaUtils.saveImageToGallery(context, imageUrl)
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Save to Gallery",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Save", style = MaterialTheme.typography.labelSmall)
                            }

                            FilledTonalButton(
                                onClick = { shareImage(context, imageUrl) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Share", style = MaterialTheme.typography.labelSmall)
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
 * Fullscreen Interactive Image Viewer Dialog
 */
@Composable
fun FullscreenImageViewerDialog(
    imageUrl: String,
    prompt: String,
    onDismiss: () -> Unit,
    onShare: (String) -> Unit,
    onEditPrompt: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.95f),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Generated AI Image (Gemini 2.5 Flash)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // High-resolution image viewport
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(if (imageUrl.startsWith("/")) File(imageUrl) else imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = prompt,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp)),
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Prompt description and action buttons
                if (prompt.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(12.dp),
                            maxLines = 3
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(prompt))
                            Toast.makeText(context, "Prompt copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy")
                    }

                    if (onEditPrompt != null && prompt.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onEditPrompt(prompt)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit")
                        }
                    }

                    FilledTonalButton(
                        onClick = {
                            val (success, msg) = com.example.data.util.MediaUtils.saveImageToGallery(context, imageUrl)
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = "Save to Gallery", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }

                    Button(
                        onClick = { onShare(imageUrl) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }
                }
            }
        }
    }
}

/**
 * Share Image Intent Helper
 */
private fun shareImage(context: Context, imagePath: String) {
    try {
        if (imagePath.startsWith("/")) {
            val file = File(imagePath)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share AI Generated Image"))
        } else {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Generated with Gemini Image AI")
            }
            context.startActivity(Intent.createChooser(intent, "Share Image"))
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to share image: ${e.message}", Toast.LENGTH_SHORT).show()
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
    persona: AdvisorPersona,
    isImageGenerating: Boolean = false
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
                .background(if (isImageGenerating) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isImageGenerating) Icons.Default.Image else Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = if (isImageGenerating) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
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
                            .background(if (isImageGenerating) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .scale(dot2Scale)
                            .clip(CircleShape)
                            .background(if (isImageGenerating) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .scale(dot3Scale)
                            .clip(CircleShape)
                            .background(if (isImageGenerating) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = if (isImageGenerating) "Gemini 2.5 Flash Image দিয়ে ছবি তৈরি হচ্ছে..." else "AI is thinking...",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.AdvisorPersona
import com.example.ui.viewmodel.AdvisorViewModel
import com.example.ui.viewmodel.VoiceCallState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCallScreen(
    viewModel: AdvisorViewModel,
    onCloseScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val callState by viewModel.voiceCallState.collectAsStateWithLifecycle()
    val isCallActive by viewModel.isCallActive.collectAsStateWithLifecycle()
    val durationSeconds by viewModel.callDurationSeconds.collectAsStateWithLifecycle()
    val callUserSpeech by viewModel.callUserSpeech.collectAsStateWithLifecycle()
    val callAiSpeech by viewModel.callAiSpeech.collectAsStateWithLifecycle()
    val callTranscript by viewModel.callTranscript.collectAsStateWithLifecycle()
    val isMuted by viewModel.isCallMuted.collectAsStateWithLifecycle()
    val isSpeakerOn by viewModel.isCallSpeakerOn.collectAsStateWithLifecycle()
    val speechRmsDb by viewModel.speechRmsDb.collectAsStateWithLifecycle()
    val speechPartialText by viewModel.speechPartialText.collectAsStateWithLifecycle()
    val selectedPersona by viewModel.selectedPersona.collectAsStateWithLifecycle()

    var showTranscriptSheet by remember { mutableStateOf(false) }

    // Monospace call duration format mm:ss
    val formattedDuration = remember(durationSeconds) {
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    // Infinite animation transition for pulsing wave rings
    val infiniteTransition = rememberInfiniteTransition(label = "call_waves")
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale_1"
    )
    val pulseAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha_1"
    )
    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, delayMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale_2"
    )
    val pulseAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, delayMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha_2"
    )
    val rotateAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "thinking_spin"
    )

    // Dynamic wave expansion factor based on user voice RMS level
    val micDynamicScale = remember(speechRmsDb, callState) {
        if (callState == VoiceCallState.LISTENING) {
            1f + (speechRmsDb / 10f) * 0.4f
        } else 1f
    }

    // Deep immersive background gradient
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F172A), // Slate 900
            Color(0xFF090D16), // Dark Midnight
            Color(0xFF020617)  // Deep Charcoal Black
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("voice_call_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- TOP BAR: Back, Call Status Pill, Timer, Speaker Toggle ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back / Minimize Button
                IconButton(
                    onClick = onCloseScreen,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                        .testTag("call_minimize_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "মিনিমাইজ করুন",
                        tint = Color.White
                    )
                }

                // Call Status & Timer Badge
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = when (callState) {
                            VoiceCallState.CONNECTING -> Color(0xFFEAB308).copy(alpha = 0.18f)
                            VoiceCallState.LISTENING -> Color(0xFF10B981).copy(alpha = 0.18f)
                            VoiceCallState.SPEAKING -> Color(0xFF3B82F6).copy(alpha = 0.18f)
                            VoiceCallState.THINKING -> Color(0xFF8B5CF6).copy(alpha = 0.18f)
                            VoiceCallState.MUTED -> Color(0xFFEF4444).copy(alpha = 0.18f)
                            VoiceCallState.IDLE -> Color.White.copy(alpha = 0.1f)
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when (callState) {
                                VoiceCallState.CONNECTING -> Color(0xFFEAB308).copy(alpha = 0.5f)
                                VoiceCallState.LISTENING -> Color(0xFF10B981).copy(alpha = 0.5f)
                                VoiceCallState.SPEAKING -> Color(0xFF3B82F6).copy(alpha = 0.5f)
                                VoiceCallState.THINKING -> Color(0xFF8B5CF6).copy(alpha = 0.5f)
                                VoiceCallState.MUTED -> Color(0xFFEF4444).copy(alpha = 0.5f)
                                VoiceCallState.IDLE -> Color.White.copy(alpha = 0.2f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Active blinking dot
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        when (callState) {
                                            VoiceCallState.CONNECTING -> Color(0xFFEAB308)
                                            VoiceCallState.LISTENING -> Color(0xFF10B981)
                                            VoiceCallState.SPEAKING -> Color(0xFF3B82F6)
                                            VoiceCallState.THINKING -> Color(0xFF8B5CF6)
                                            VoiceCallState.MUTED -> Color(0xFFEF4444)
                                            VoiceCallState.IDLE -> Color.Gray
                                        },
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "১:১ ভয়েস কল",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = formattedDuration,
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                // Speakerphone Toggle Button
                IconButton(
                    onClick = { viewModel.toggleCallSpeaker() },
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (isSpeakerOn) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                            CircleShape
                        )
                        .testTag("call_speaker_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                        contentDescription = if (isSpeakerOn) "স্পিকার চালু" else "ইয়ারপিস চালু",
                        tint = if (isSpeakerOn) Color(0xFF60A5FA) else Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- CENTER STAGE: Concentric Glowing Rings & Persona Avatar ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(240.dp)
                ) {
                    // Outer Ripple Ring 2
                    if (callState == VoiceCallState.LISTENING || callState == VoiceCallState.SPEAKING) {
                        Box(
                            modifier = Modifier
                                .size(170.dp)
                                .scale(pulseScale2 * micDynamicScale)
                                .alpha(pulseAlpha2)
                                .border(
                                    2.dp,
                                    if (callState == VoiceCallState.LISTENING) Color(0xFF10B981) else Color(0xFF3B82F6),
                                    CircleShape
                                )
                        )
                    }

                    // Middle Ripple Ring 1
                    if (callState == VoiceCallState.LISTENING || callState == VoiceCallState.SPEAKING) {
                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .scale(pulseScale1 * micDynamicScale)
                                .alpha(pulseAlpha1)
                                .border(
                                    2.5.dp,
                                    if (callState == VoiceCallState.LISTENING) Color(0xFF34D399) else Color(0xFF60A5FA),
                                    CircleShape
                                )
                        )
                    }

                    // Rotating Shimmer Ring for Thinking State
                    if (callState == VoiceCallState.THINKING) {
                        Box(
                            modifier = Modifier
                                .size(145.dp)
                                .rotate(rotateAngle)
                                .border(
                                    3.dp,
                                    Brush.sweepGradient(
                                        listOf(
                                            Color(0xFF8B5CF6),
                                            Color(0xFFEC4899),
                                            Color(0xFF3B82F6),
                                            Color.Transparent
                                        )
                                    ),
                                    CircleShape
                                )
                        )
                    }

                    // Ambient Background Glow
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        when (callState) {
                                            VoiceCallState.LISTENING -> Color(0xFF10B981).copy(alpha = 0.35f)
                                            VoiceCallState.SPEAKING -> Color(0xFF3B82F6).copy(alpha = 0.35f)
                                            VoiceCallState.THINKING -> Color(0xFF8B5CF6).copy(alpha = 0.35f)
                                            VoiceCallState.MUTED -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                            else -> Color.White.copy(alpha = 0.15f)
                                        },
                                        Color.Transparent
                                    )
                                ),
                                CircleShape
                            )
                    )

                    // Core AI Avatar Orb
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(116.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = when (callState) {
                                        VoiceCallState.LISTENING -> listOf(Color(0xFF059669), Color(0xFF10B981))
                                        VoiceCallState.SPEAKING -> listOf(Color(0xFF2563EB), Color(0xFF38BDF8))
                                        VoiceCallState.THINKING -> listOf(Color(0xFF7C3AED), Color(0xFFC084FC))
                                        VoiceCallState.MUTED -> listOf(Color(0xFFB91C1C), Color(0xFFEF4444))
                                        else -> listOf(Color(0xFF475569), Color(0xFF64748B))
                                    }
                                )
                            )
                            .border(
                                width = 3.dp,
                                color = Color.White.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                            .clickable {
                                // Tap avatar to toggle speak/interrupt
                                if (callState == VoiceCallState.SPEAKING) {
                                    viewModel.interruptAiAndSpeak()
                                }
                            }
                    ) {
                        val avatarIcon: ImageVector = when (callState) {
                            VoiceCallState.SPEAKING -> Icons.Default.GraphicEq
                            VoiceCallState.LISTENING -> Icons.Default.Mic
                            VoiceCallState.THINKING -> Icons.Default.AutoAwesome
                            VoiceCallState.MUTED -> Icons.Default.MicOff
                            else -> when (selectedPersona) {
                                AdvisorPersona.GENERAL -> Icons.Default.AutoAwesome
                                AdvisorPersona.CREATIVE -> Icons.Default.Palette
                                AdvisorPersona.CODING -> Icons.Default.Code
                                AdvisorPersona.LEARNING -> Icons.Default.School
                            }
                        }

                        Icon(
                            imageVector = avatarIcon,
                            contentDescription = "AI অবতার",
                            tint = Color.White,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Persona Name
                Text(
                    text = selectedPersona.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Call Status Text
                Text(
                    text = when (callState) {
                        VoiceCallState.CONNECTING -> "এআই এর সাথে কল সংযোগ হচ্ছে..."
                        VoiceCallState.LISTENING -> "🎙️ আপনার কথা শুনছি... (বলুন)"
                        VoiceCallState.THINKING -> "⚡ এআই চিন্তা করছে..."
                        VoiceCallState.SPEAKING -> "🔊 এআই উত্তর দিচ্ছে..."
                        VoiceCallState.MUTED -> "🔇 আপনার মাইক মিউট করা আছে"
                        VoiceCallState.IDLE -> "কল নিষ্ক্রিয়"
                    },
                    color = when (callState) {
                        VoiceCallState.LISTENING -> Color(0xFF34D399)
                        VoiceCallState.SPEAKING -> Color(0xFF60A5FA)
                        VoiceCallState.THINKING -> Color(0xFFC084FC)
                        VoiceCallState.MUTED -> Color(0xFFFCA5A5)
                        else -> Color.White.copy(alpha = 0.7f)
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // --- Real-Time Live Transcript Preview Card ---
                val activeSpeechText = when {
                    callState == VoiceCallState.LISTENING && speechPartialText.isNotBlank() -> speechPartialText
                    callState == VoiceCallState.SPEAKING && callAiSpeech.isNotBlank() -> callAiSpeech
                    callState == VoiceCallState.THINKING && callUserSpeech.isNotBlank() -> "আপনি বলেছেন: \"$callUserSpeech\""
                    callAiSpeech.isNotBlank() -> callAiSpeech
                    else -> "কথা বলা শুরু করতে সরাসরি কথা বলুন..."
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp, max = 130.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = activeSpeechText,
                            color = Color.White.copy(alpha = 0.95f),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 20.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // View Full Transcript Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showTranscriptSheet = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("open_call_transcript_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "আলাপ দেখুন",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "পুরো আলাপ দেখুন (${callTranscript.size} বার্তা)",
                        color = Color.White.copy(alpha = 0.65f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // --- BOTTOM CONTROLS BAR: Mute, Interrupt / Talk Now, End Call ---
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = Color.White.copy(alpha = 0.09f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Mute / Unmute Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { viewModel.toggleCallMute() }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (isMuted) Color(0xFFEF4444).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.12f))
                                .border(
                                    1.dp,
                                    if (isMuted) Color(0xFFEF4444) else Color.White.copy(alpha = 0.2f),
                                    CircleShape
                                )
                                .testTag("call_mute_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = if (isMuted) "আনমিউট" else "মিউট",
                                tint = if (isMuted) Color(0xFFFCA5A5) else Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isMuted) "আনমিউট" else "মিউট",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    // 2. Interrupt AI / Speak Now Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            if (callState == VoiceCallState.SPEAKING) {
                                viewModel.interruptAiAndSpeak()
                            } else if (callState == VoiceCallState.MUTED) {
                                viewModel.toggleCallMute()
                            }
                        }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    if (callState == VoiceCallState.SPEAKING) Color(0xFFF59E0B).copy(alpha = 0.25f)
                                    else Color.White.copy(alpha = 0.12f)
                                )
                                .border(
                                    1.dp,
                                    if (callState == VoiceCallState.SPEAKING) Color(0xFFF59E0B) else Color.White.copy(alpha = 0.2f),
                                    CircleShape
                                )
                                .testTag("call_interrupt_button")
                        ) {
                            Icon(
                                imageVector = if (callState == VoiceCallState.SPEAKING) Icons.Default.Stop else Icons.Default.RecordVoiceOver,
                                contentDescription = "কথা বলুন",
                                tint = if (callState == VoiceCallState.SPEAKING) Color(0xFFFCD34D) else Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (callState == VoiceCallState.SPEAKING) "থামুন ও বলুন" else "কথা বলুন",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    // 3. End Call Button (Big Red Hang Up)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            viewModel.endVoiceCall()
                            onCloseScreen()
                        }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDC2626))
                                .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                .testTag("call_end_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "কল শেষ করুন",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "কল কাটুন",
                            color = Color(0xFFFCA5A5),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }

    // --- Modal Bottom Sheet: Full Call Transcript ---
    if (showTranscriptSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTranscriptSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF0F172A),
            modifier = Modifier.testTag("call_transcript_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📞 কল ট্রান্সক্রিপ্ট (কথোপকথন)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = formattedDuration,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF60A5FA)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (callTranscript.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "এখনও কোনো বার্তা আদান-প্রদান হয়নি।",
                            color = Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(callTranscript) { (role, message) ->
                            val isUser = role == "User"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isUser) 16.dp else 4.dp,
                                        bottomEnd = if (isUser) 4.dp else 16.dp
                                    ),
                                    color = if (isUser) Color(0xFF1D4ED8) else Color(0xFF1E293B),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isUser) Color(0xFF3B82F6) else Color.White.copy(alpha = 0.1f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = if (isUser) "আপনি" else "এআই সহকারী",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isUser) Color(0xFF93C5FD) else Color(0xFF34D399)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White
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

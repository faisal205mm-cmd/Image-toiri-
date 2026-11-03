package com.example

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.ChatSessionEntity
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.example.ui.components.ApiKeysConfigDialog
import com.example.ui.components.ChatHistoryDrawerContent
import com.example.ui.screens.AdvisorChatScreen
import com.example.ui.screens.FirebaseDatabaseScreen
import com.example.ui.screens.NewSaleScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AdvisorViewModel
import com.example.ui.viewmodel.MainAppTab
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: AdvisorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContainer(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(viewModel: AdvisorViewModel) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val currentSessionId by viewModel.currentSessionId.collectAsStateWithLifecycle()
    val customApiKey by viewModel.customApiKey.collectAsStateWithLifecycle()
    val apiKeyStatus by viewModel.apiKeyStatus.collectAsStateWithLifecycle()
    val isApiKeyTesting by viewModel.apiKeyTesting.collectAsStateWithLifecycle()
    val selectedMainTab by viewModel.selectedMainTab.collectAsStateWithLifecycle()
    val allFinances by viewModel.allFinances.collectAsStateWithLifecycle()

    var showApiKeysDialog by remember { mutableStateOf(false) }
    var showTopMoreMenu by remember { mutableStateOf(false) }

    val currentSession = allSessions.find { it.id == currentSessionId }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                ChatHistoryDrawerContent(
                    sessions = allSessions,
                    currentSessionId = currentSessionId,
                    onSelectSession = { sessionId ->
                        viewModel.selectSession(sessionId)
                    },
                    onNewChat = {
                        viewModel.startNewSession()
                    },
                    onRenameSession = { sessionId, newTitle ->
                        viewModel.renameSession(sessionId, newTitle)
                    },
                    onDeleteSession = { sessionId ->
                        viewModel.deleteSession(sessionId)
                    },
                    onClearAllHistory = {
                        viewModel.clearAllHistory()
                    },
                    onCloseDrawer = {
                        scope.launch { drawerState.close() }
                    },
                    apiKeyStatus = apiKeyStatus,
                    customApiKey = customApiKey,
                    isApiKeyTesting = isApiKeyTesting,
                    onSaveApiKey = { key ->
                        viewModel.saveCustomApiKey(key)
                    },
                    onClearApiKey = {
                        viewModel.clearCustomApiKey()
                    },
                    onTestApiKey = { key, onComplete ->
                        viewModel.testApiKey(key, onComplete)
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (selectedMainTab == MainAppTab.AI_CHAT) {
                    // Header row right at the very top of the screen with no blank space above
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 4.dp, vertical = 0.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            },
                            modifier = Modifier.testTag("open_history_drawer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Chat History",
                                tint = Color(0xFF1E293B)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    showApiKeysDialog = true
                                },
                                modifier = Modifier.testTag("top_bar_api_key_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = "API Key",
                                    tint = Color(0xFF00C853)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.startNewSession() },
                                modifier = Modifier.testTag("top_bar_new_chat_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New Chat",
                                    tint = Color(0xFF00C853)
                                )
                            }

                            // 3-dot overflow menu
                            Box {
                                IconButton(
                                    onClick = { showTopMoreMenu = true },
                                    modifier = Modifier.testTag("top_bar_more_menu_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options menu",
                                        tint = Color(0xFF1E293B)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showTopMoreMenu,
                                    onDismissRequest = { showTopMoreMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    "API Keys Settings",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    "Gemini API Key",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.VpnKey,
                                                contentDescription = null,
                                                tint = Color(0xFF00C853)
                                            )
                                        },
                                        onClick = {
                                            showTopMoreMenu = false
                                            showApiKeysDialog = true
                                        },
                                        modifier = Modifier.testTag("menu_api_keys_item")
                                    )

                                    HorizontalDivider()

                                    DropdownMenuItem(
                                        text = { Text("Start New Chat") },
                                        leadingIcon = {
                                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color(0xFF00C853))
                                        },
                                        onClick = {
                                            showTopMoreMenu = false
                                            viewModel.startNewSession()
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = { Text("Clear Current Chat") },
                                        leadingIcon = {
                                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                                        },
                                        onClick = {
                                            showTopMoreMenu = false
                                            viewModel.clearCurrentChat()
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    CenterAlignedTopAppBar(
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                    }
                                },
                                modifier = Modifier.testTag("open_history_drawer_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Chat History",
                                    tint = Color(0xFF1E293B)
                                )
                            }
                        },
                        title = {
                            Text(
                                text = if (selectedMainTab == MainAppTab.NEW_SALE) "মাসিক ফাইন্যান্স (Sale)" else "Firebase Firestore",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF1E293B)
                            )
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    showApiKeysDialog = true
                                },
                                modifier = Modifier.testTag("top_bar_api_key_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = "API Key",
                                    tint = Color(0xFF00C853)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.startNewSession() },
                                modifier = Modifier.testTag("top_bar_new_chat_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New Chat",
                                    tint = Color(0xFF00C853)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.White
                        )
                    )
                }
            },
            bottomBar = {
                Surface(
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(top = 10.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. AI Chat Tab
                        val isAiChat = selectedMainTab == MainAppTab.AI_CHAT
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { viewModel.selectMainTab(MainAppTab.AI_CHAT) }
                                .padding(horizontal = 16.dp)
                                .testTag("nav_item_ai_chat")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Chat",
                                tint = if (isAiChat) Color(0xFF00C853) else Color(0xFF64748B),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "AI চ্যাট",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isAiChat) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                ),
                                color = if (isAiChat) Color(0xFF00C853) else Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (isAiChat) {
                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0xFF00C853))
                                )
                            } else {
                                Spacer(modifier = Modifier.height(3.dp))
                            }
                        }

                        // 2. Sale Tab
                        val isSale = selectedMainTab == MainAppTab.NEW_SALE
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { viewModel.selectMainTab(MainAppTab.NEW_SALE) }
                                .padding(horizontal = 16.dp)
                                .testTag("nav_item_new_sale")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = "Sale",
                                tint = if (isSale) Color(0xFF00C853) else Color(0xFF64748B),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Sale",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSale) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                ),
                                color = if (isSale) Color(0xFF00C853) else Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (isSale) {
                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0xFF00C853))
                                )
                            } else {
                                Spacer(modifier = Modifier.height(3.dp))
                            }
                        }

                        // 3. Fashion / DB Tab
                        val isDb = selectedMainTab == MainAppTab.FIREBASE_DB
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { viewModel.selectMainTab(MainAppTab.FIREBASE_DB) }
                                .padding(horizontal = 16.dp)
                                .testTag("nav_item_firebase_db")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                                contentDescription = "Fashion Records",
                                tint = if (isDb) Color(0xFF00C853) else Color(0xFF64748B),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ক্যাশনরোলেন",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isDb) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                ),
                                color = if (isDb) Color(0xFF00C853) else Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (isDb) {
                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0xFF00C853))
                                )
                            } else {
                                Spacer(modifier = Modifier.height(3.dp))
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedMainTab) {
                    MainAppTab.AI_CHAT -> {
                        AdvisorChatScreen(
                            viewModel = viewModel,
                            onOpenHistory = {
                                scope.launch { drawerState.open() }
                            }
                        )
                    }
                    MainAppTab.NEW_SALE -> {
                        NewSaleScreen(
                            viewModel = viewModel
                        )
                    }
                    MainAppTab.FIREBASE_DB -> {
                        FirebaseDatabaseScreen(
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }

    if (showApiKeysDialog) {
        ApiKeysConfigDialog(
            onDismiss = { showApiKeysDialog = false },
            geminiApiKeyStatus = apiKeyStatus,
            customGeminiKey = customApiKey,
            isGeminiTesting = isApiKeyTesting,
            onSaveGeminiKey = { viewModel.saveCustomApiKey(it) },
            onClearGeminiKey = { viewModel.clearCustomApiKey() },
            onTestGeminiKey = { key, callback -> viewModel.testApiKey(key, callback) }
        )
    }
}

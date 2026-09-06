package com.example

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Storage
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
                Column {
                    CenterAlignedTopAppBar(
                        navigationIcon = {
                            if (selectedMainTab == MainAppTab.AI_CHAT) {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                        }
                                    },
                                    modifier = Modifier.testTag("open_history_drawer_button")
                                ) {
                                    if (allSessions.isNotEmpty()) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                                ) {
                                                    Text("${allSessions.size}")
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Menu,
                                                contentDescription = "Chat History",
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Menu,
                                            contentDescription = "Chat History",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            } else {
                                IconButton(
                                    onClick = { viewModel.selectMainTab(MainAppTab.AI_CHAT) },
                                    modifier = Modifier.testTag("nav_to_chat_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "AI Chat",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (selectedMainTab) {
                                                    MainAppTab.AI_CHAT -> MaterialTheme.colorScheme.primaryContainer
                                                    MainAppTab.NEW_SALE -> MaterialTheme.colorScheme.secondaryContainer
                                                    MainAppTab.FIREBASE_DB -> MaterialTheme.colorScheme.tertiaryContainer
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (selectedMainTab) {
                                                MainAppTab.AI_CHAT -> Icons.Default.AutoAwesome
                                                MainAppTab.NEW_SALE -> Icons.Default.PointOfSale
                                                MainAppTab.FIREBASE_DB -> Icons.Default.Storage
                                            },
                                            contentDescription = null,
                                            tint = when (selectedMainTab) {
                                                MainAppTab.AI_CHAT -> MaterialTheme.colorScheme.onPrimaryContainer
                                                MainAppTab.NEW_SALE -> MaterialTheme.colorScheme.onSecondaryContainer
                                                MainAppTab.FIREBASE_DB -> MaterialTheme.colorScheme.onTertiaryContainer
                                            },
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when (selectedMainTab) {
                                            MainAppTab.AI_CHAT -> "AI Assistant"
                                            MainAppTab.NEW_SALE -> "মাসিক ফাইন্যান্স (Sale)"
                                            MainAppTab.FIREBASE_DB -> "Firebase Firestore"
                                        },
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.3.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (selectedMainTab == MainAppTab.AI_CHAT && currentSession != null) {
                                    Text(
                                        text = currentSession.title,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else if (selectedMainTab == MainAppTab.NEW_SALE) {
                                    Text(
                                        text = "Monthly Sale • Expense • DEBT • Product Buy",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else if (selectedMainTab == MainAppTab.FIREBASE_DB) {
                                    Text(
                                        text = "Cloud Firestore • sales_records",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                    }
                                },
                                modifier = Modifier.testTag("top_bar_api_key_button")
                            ) {
                                if (!apiKeyStatus.first) {
                                    BadgedBox(
                                        badge = {
                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.error,
                                                contentColor = MaterialTheme.colorScheme.onError
                                            ) {
                                                Text("!")
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Key,
                                            contentDescription = "Gemini API Key Required",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = "Gemini API Key Active",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            if (selectedMainTab == MainAppTab.AI_CHAT) {
                                IconButton(
                                    onClick = { viewModel.startNewSession() },
                                    modifier = Modifier.testTag("top_bar_new_chat_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "New Chat",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // TabRow for immediate 1-tap tab switching
                    TabRow(
                        selectedTabIndex = when (selectedMainTab) {
                            MainAppTab.AI_CHAT -> 0
                            MainAppTab.NEW_SALE -> 1
                            MainAppTab.FIREBASE_DB -> 2
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Tab(
                            selected = selectedMainTab == MainAppTab.AI_CHAT,
                            onClick = { viewModel.selectMainTab(MainAppTab.AI_CHAT) },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "AI চ্যাট",
                                        fontWeight = if (selectedMainTab == MainAppTab.AI_CHAT) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                            },
                            modifier = Modifier.testTag("tab_ai_chat_top")
                        )

                        Tab(
                            selected = selectedMainTab == MainAppTab.NEW_SALE,
                            onClick = { viewModel.selectMainTab(MainAppTab.NEW_SALE) },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PointOfSale,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Sale",
                                        fontWeight = if (selectedMainTab == MainAppTab.NEW_SALE) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                    if (allFinances.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(
                                            color = if (selectedMainTab == MainAppTab.NEW_SALE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                text = "${allFinances.size}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = if (selectedMainTab == MainAppTab.NEW_SALE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.testTag("tab_new_sale_top")
                        )

                        Tab(
                            selected = selectedMainTab == MainAppTab.FIREBASE_DB,
                            onClick = { viewModel.selectMainTab(MainAppTab.FIREBASE_DB) },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Storage,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "ফায়ারবেস",
                                        fontWeight = if (selectedMainTab == MainAppTab.FIREBASE_DB) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                            },
                            modifier = Modifier.testTag("tab_firebase_top")
                        )
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    NavigationBarItem(
                        selected = selectedMainTab == MainAppTab.AI_CHAT,
                        onClick = { viewModel.selectMainTab(MainAppTab.AI_CHAT) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Chat"
                            )
                        },
                        label = {
                            Text(
                                text = "AI চ্যাট",
                                fontWeight = if (selectedMainTab == MainAppTab.AI_CHAT) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("nav_item_ai_chat")
                    )

                    NavigationBarItem(
                        selected = selectedMainTab == MainAppTab.NEW_SALE,
                        onClick = { viewModel.selectMainTab(MainAppTab.NEW_SALE) },
                        icon = {
                            if (allFinances.isNotEmpty()) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ) {
                                            Text("${allFinances.size}")
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PointOfSale,
                                        contentDescription = "Sale"
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PointOfSale,
                                    contentDescription = "Sale"
                                )
                            }
                        },
                        label = {
                            Text(
                                text = "Sale",
                                fontWeight = if (selectedMainTab == MainAppTab.NEW_SALE) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("nav_item_new_sale")
                    )

                    NavigationBarItem(
                        selected = selectedMainTab == MainAppTab.FIREBASE_DB,
                        onClick = { viewModel.selectMainTab(MainAppTab.FIREBASE_DB) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = "Firebase DB"
                            )
                        },
                        label = {
                            Text(
                                text = "ফায়ারবেস",
                                fontWeight = if (selectedMainTab == MainAppTab.FIREBASE_DB) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("nav_item_firebase_db")
                    )
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
}

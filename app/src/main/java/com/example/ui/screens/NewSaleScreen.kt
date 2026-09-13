package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.MonthlyFinanceEntity
import com.example.ui.viewmodel.AdvisorViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun NewSaleScreen(
    viewModel: AdvisorViewModel
) {
    val context = LocalContext.current

    // Observe financial flows
    val monthlySales by viewModel.monthlySales.collectAsState()
    val monthlyExpenses by viewModel.monthlyExpenses.collectAsState()
    val monthlyDebtPays by viewModel.monthlyDebtPays.collectAsState()
    val monthlyProductBuys by viewModel.monthlyProductBuys.collectAsState()

    val totalSale by viewModel.totalMonthlySale.collectAsState()
    val totalExpense by viewModel.totalMonthlyExpense.collectAsState()
    val totalDebtPay by viewModel.totalMonthlyDebtPay.collectAsState()
    val totalProductBuy by viewModel.totalMonthlyProductBuy.collectAsState()
    val netBalance by viewModel.netBalance.collectAsState()

    val syncStatus by viewModel.firestoreSyncStatus.collectAsStateWithLifecycle()
    var isSyncing by remember { mutableStateOf(false) }

    // State for Add Dialog
    var activeAddType by remember { mutableStateOf<String?>(null) } // "SALE", "EXPENSE", "DEBT_PAY", "PRODUCT_BUY"

    // Expanded states for the 4 bars
    var isSaleExpanded by remember { mutableStateOf(true) }
    var isExpenseExpanded by remember { mutableStateOf(true) }
    var isDebtExpanded by remember { mutableStateOf(true) }
    var isProductBuyExpanded by remember { mutableStateOf(true) }

    val selectedMonthYear by viewModel.selectedMonthYear.collectAsState()
    var showCalendarPicker by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Top Financial Overview & Net Balance Card ---
            item {
                MonthlyFinancialSummaryCard(
                    monthYear = selectedMonthYear,
                    totalSale = totalSale,
                    totalExpense = totalExpense,
                    totalDebtPay = totalDebtPay,
                    totalProductBuy = totalProductBuy,
                    netBalance = netBalance,
                    onOpenCalendar = { showCalendarPicker = true },
                    onPreviousMonth = { viewModel.selectPreviousMonth() },
                    onNextMonth = { viewModel.selectNextMonth() },
                    onAskAiAudit = {
                        viewModel.askAiAboutFinances("ALL")
                        Toast.makeText(context, "AI আর্থিক অডিট শুরু হচ্ছে...", Toast.LENGTH_SHORT).show()
                    },
                    onSyncCloud = {
                        isSyncing = true
                        viewModel.syncAllSalesToFirestore { _, msg ->
                            isSyncing = false
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    isSyncing = isSyncing,
                    syncStatus = syncStatus
                )
            }

            // --- AI Advisor Action Quick Triggers ---
            item {
                AiFinancialActionsBar(
                    totalSale = totalSale,
                    totalExpense = totalExpense,
                    totalDebtPay = totalDebtPay,
                    totalProductBuy = totalProductBuy,
                    onTriggerAi = { actionType ->
                        viewModel.askAiAboutFinances(actionType)
                        Toast.makeText(context, "AI পরামর্শ লোড হচ্ছে...", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // --- Section Header: 4 Dedicated Financial Bars ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "মাসিক হিসাবের বারসমূহ",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "ডাটাবেজে সংরক্ষিত",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // ==========================================
            // BAR 1: Monthly Sale (মাসিক বিক্রি)
            // ==========================================
            item {
                FinanceCategoryBar(
                    title = "Monthly Sale (মাসিক বিক্রি)",
                    bnSubtitle = "সব ধরণের বিক্রয় ও আয়ের হিসাব",
                    totalAmount = totalSale,
                    itemCount = monthlySales.size,
                    barColor = Color(0xFF2E7D32), // Emerald Green
                    containerColor = Color(0xFFE8F5E9),
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    isExpanded = isSaleExpanded,
                    onToggleExpand = { isSaleExpanded = !isSaleExpanded },
                    onAddClick = { activeAddType = "SALE" },
                    items = monthlySales,
                    onDeleteItem = { item ->
                        viewModel.deleteMonthlyFinance(item.id, item.type)
                        Toast.makeText(context, "বিক্রির রেকর্ড ডিলিট করা হয়েছে", Toast.LENGTH_SHORT).show()
                    },
                    testTag = "bar_monthly_sale"
                )
            }

            // ==========================================
            // BAR 2: Monthly Expense (মাসিক খরচ)
            // ==========================================
            item {
                FinanceCategoryBar(
                    title = "Monthly Expense (মাসিক খরচ)",
                    bnSubtitle = "দোকান ভাড়া, বেতন, বিদ্যুৎ বিল ও বিবিধ ব্যয়",
                    totalAmount = totalExpense,
                    itemCount = monthlyExpenses.size,
                    barColor = Color(0xFFE65100), // Vibrant Amber/Orange
                    containerColor = Color(0xFFFFF3E0),
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    isExpanded = isExpenseExpanded,
                    onToggleExpand = { isExpenseExpanded = !isExpenseExpanded },
                    onAddClick = { activeAddType = "EXPENSE" },
                    items = monthlyExpenses,
                    onDeleteItem = { item ->
                        viewModel.deleteMonthlyFinance(item.id, item.type)
                        Toast.makeText(context, "খরচের রেকর্ড ডিলিট করা হয়েছে", Toast.LENGTH_SHORT).show()
                    },
                    aiWarning = if (totalSale > 0 && (totalExpense / totalSale) > 0.45) {
                        "সতর্কতা: খরচ মোট বিক্রির ${(totalExpense / totalSale * 100).toInt()}% পৌঁছেছে! খরচ নিয়ন্ত্রণে AI পরামর্শ নিন।"
                    } else null,
                    testTag = "bar_monthly_expense"
                )
            }

            // ==========================================
            // BAR 3: Monthly DEBT Pay (মাসিক ঋণ পরিশোধ)
            // ==========================================
            item {
                FinanceCategoryBar(
                    title = "Monthly DEBT Pay (মাসিক ঋণ পরিশোধ)",
                    bnSubtitle = "ব্যাংক লোন কিস্তি, মহাজন ও দেনা পরিশোধের কিস্তি",
                    totalAmount = totalDebtPay,
                    itemCount = monthlyDebtPays.size,
                    barColor = Color(0xFFC2185B), // Deep Rose/Crimson
                    containerColor = Color(0xFFFCE4EC),
                    icon = Icons.Default.CreditCard,
                    isExpanded = isDebtExpanded,
                    onToggleExpand = { isDebtExpanded = !isDebtExpanded },
                    onAddClick = { activeAddType = "DEBT_PAY" },
                    items = monthlyDebtPays,
                    onDeleteItem = { item ->
                        viewModel.deleteMonthlyFinance(item.id, item.type)
                        Toast.makeText(context, "ঋণ পরিশোধের রেকর্ড ডিলিট করা হয়েছে", Toast.LENGTH_SHORT).show()
                    },
                    aiWarning = if (totalSale > 0 && (totalDebtPay / totalSale) > 0.30) {
                        "নজর দিন: বিক্রির ${(totalDebtPay / totalSale * 100).toInt()}% ঋণ পরিশোধে যাচ্ছে। ঋণমুক্তির রোডম্যাপ দেখুন।"
                    } else null,
                    testTag = "bar_monthly_debt"
                )
            }

            // ==========================================
            // BAR 4: Monthly Product Buy (মাসিক পণ্য ক্রয়)
            // ==========================================
            item {
                FinanceCategoryBar(
                    title = "Monthly Product Buy (মাসিক পণ্য ক্রয়)",
                    bnSubtitle = "থান কাপড়, রেডিমেড স্টক, ম্যাটেরিয়াল ও প্যাকেজিং ক্রয়",
                    totalAmount = totalProductBuy,
                    itemCount = monthlyProductBuys.size,
                    barColor = Color(0xFF1565C0), // Royal Blue
                    containerColor = Color(0xFFE3F2FD),
                    icon = Icons.Default.Inventory2,
                    isExpanded = isProductBuyExpanded,
                    onToggleExpand = { isProductBuyExpanded = !isProductBuyExpanded },
                    onAddClick = { activeAddType = "PRODUCT_BUY" },
                    items = monthlyProductBuys,
                    onDeleteItem = { item ->
                        viewModel.deleteMonthlyFinance(item.id, item.type)
                        Toast.makeText(context, "পণ্য ক্রয়ের রেকর্ড ডিলিট করা হয়েছে", Toast.LENGTH_SHORT).show()
                    },
                    testTag = "bar_monthly_product_buy"
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Add Dialog
        activeAddType?.let { type ->
            AddFinanceEntryDialog(
                type = type,
                monthYear = selectedMonthYear,
                onDismiss = { activeAddType = null },
                onSave = { title, amount, category, note ->
                    viewModel.addMonthlyFinance(
                        type = type,
                        title = title,
                        amount = amount,
                        category = category,
                        note = note,
                        monthYear = selectedMonthYear
                    ) {
                        Toast.makeText(context, "ডাটাবেজে সফলভাবে সংরক্ষণ করা হয়েছে", Toast.LENGTH_SHORT).show()
                    }
                    activeAddType = null
                }
            )
        }

        // Calendar Month-Year Picker Dialog
        if (showCalendarPicker) {
            MonthYearCalendarPickerDialog(
                currentMonthYear = selectedMonthYear,
                onDismiss = { showCalendarPicker = false },
                onSelectMonthYear = { chosenMonthYear ->
                    viewModel.selectMonthYear(chosenMonthYear)
                    Toast.makeText(context, "$chosenMonthYear এর হিসাব সিলেক্ট করা হয়েছে", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

// -------------------------------------------------------------
// Component: Top Summary & Net Balance Card
// -------------------------------------------------------------
@Composable
fun MonthlyFinancialSummaryCard(
    monthYear: String,
    totalSale: Double,
    totalExpense: Double,
    totalDebtPay: Double,
    totalProductBuy: Double,
    netBalance: Double,
    onOpenCalendar: () -> Unit = {},
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    onAskAiAudit: () -> Unit,
    onSyncCloud: () -> Unit = {},
    isSyncing: Boolean = false,
    syncStatus: String? = null
) {
    val isPositive = netBalance >= 0
    val totalOutflow = totalExpense + totalDebtPay + totalProductBuy
    val profitMargin = if (totalSale > 0) ((netBalance / totalSale) * 100).toInt() else 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_monthly_summary"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with interactive Calendar month/year & navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Calendar & Month Selector Pill
                Surface(
                    onClick = onOpenCalendar,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.testTag("btn_select_month_year_calendar")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "ক্যালেন্ডার খুলুন",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = monthYear,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "ক্যালেন্ডার থেকে মাস নির্বাচন করুন",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Month Nav controls & Profit badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPreviousMonth,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("btn_prev_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                            contentDescription = "পূর্ববর্তী মাস",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(
                        onClick = onNextMonth,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("btn_next_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = "পরবর্তী মাস",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        color = if (isPositive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = if (isPositive) "মুনাফা" else "ঘাটতি",
                            color = if (isPositive) Color(0xFF2E7D32) else Color(0xFFC62828),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Large Net Cash Balance
            Text(
                text = "নিট ব্যালেন্স (Net Balance)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "৳ ${formatTaka(netBalance)}",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = if (isPositive) Color(0xFF2E7D32) else Color(0xFFC62828)
                )

                if (totalSale > 0) {
                    Text(
                        text = "মার্জিন: $profitMargin%",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isPositive) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Quick 4 metrics mini-grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MiniMetric(
                    label = "মোট বিক্রি (+)",
                    amount = totalSale,
                    color = Color(0xFF2E7D32)
                )
                MiniMetric(
                    label = "খরচ (-)",
                    amount = totalExpense,
                    color = Color(0xFFE65100)
                )
                MiniMetric(
                    label = "ঋণ শোধ (-)",
                    amount = totalDebtPay,
                    color = Color(0xFFC2185B)
                )
                MiniMetric(
                    label = "পণ্য ক্রয় (-)",
                    amount = totalProductBuy,
                    color = Color(0xFF1565C0)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dual Action Buttons: Cloud Sync & AI Audit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSyncCloud,
                    enabled = !isSyncing,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_cloud_sync_new_sale"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("সিঙ্ক হচ্ছে...", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ক্লাউডে সিঙ্ক", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onAskAiAudit,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_ai_audit"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI অডিট রিপোর্ট",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            if (!syncStatus.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = syncStatus,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MiniMetric(label: String, amount: Double, color: Color) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "৳${formatTaka(amount)}",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

// -------------------------------------------------------------
// Component: AI Financial Actions Bar
// -------------------------------------------------------------
@Composable
fun AiFinancialActionsBar(
    totalSale: Double,
    totalExpense: Double,
    totalDebtPay: Double,
    totalProductBuy: Double,
    onTriggerAi: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "AI ফাইন্যান্সিয়াল অ্যাডভাইজর একশন",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AiActionChip(
                    text = "💡 খরচ কমানোর উপায়",
                    modifier = Modifier.weight(1f),
                    onClick = { onTriggerAi("EXPENSE_REDUCE") }
                )
                AiActionChip(
                    text = "💳 ঋণমুক্তির রোডম্যাপ",
                    modifier = Modifier.weight(1f),
                    onClick = { onTriggerAi("DEBT_STRATEGY") }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AiActionChip(
                    text = "📦 পণ্য ক্রয় বাজেট পরামর্শ",
                    modifier = Modifier.weight(1f),
                    onClick = { onTriggerAi("PRODUCT_BUY_STRATEGY") }
                )
                AiActionChip(
                    text = "📊 লাভ বাড়ানোর কৌশল",
                    modifier = Modifier.weight(1f),
                    onClick = { onTriggerAi("ALL") }
                )
            }
        }
    }
}

@Composable
fun AiActionChip(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// -------------------------------------------------------------
// Component: Dedicated Finance Category Bar (Expandable)
// -------------------------------------------------------------
@Composable
fun FinanceCategoryBar(
    title: String,
    bnSubtitle: String,
    totalAmount: Double,
    itemCount: Int,
    barColor: Color,
    containerColor: Color,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAddClick: () -> Unit,
    items: List<MonthlyFinanceEntity>,
    onDeleteItem: (MonthlyFinanceEntity) -> Unit,
    aiWarning: String? = null,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, barColor.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Main clickable bar header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(containerColor.copy(alpha = 0.45f))
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(barColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = bnSubtitle,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Total amount badge & Expand Icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "৳ ${formatTaka(totalAmount)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = barColor
                        )
                        Text(
                            text = "$itemCount টি এন্ট্রি",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = barColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Optional AI Insight banner for this specific bar
            aiWarning?.let { warningText ->
                Surface(
                    color = Color(0xFFFFF8E1),
                    border = BorderStroke(1.dp, Color(0xFFFFD54F)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFF57F17),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = warningText,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF5D4037)
                        )
                    }
                }
            }

            // Expanded content (Action + List of items)
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    // "Add" bar action button
                    Button(
                        onClick = onAddClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("${testTag}_add_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = barColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "নতুন ${title.substringBefore(" (")} যোগ করুন (+ Add)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // List of items in this bar
                    if (items.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "এই বারে এখনও কোনো রেকর্ড যুক্ত করা হয়নি। উপরের বাটনে ট্যাপ করে যোগ করুন।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items.forEach { item ->
                                FinanceItemRow(
                                    item = item,
                                    barColor = barColor,
                                    onDelete = { onDeleteItem(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Single Finance Item Row
// -------------------------------------------------------------
@Composable
fun FinanceItemRow(
    item: MonthlyFinanceEntity,
    barColor: Color,
    onDelete: () -> Unit
) {
    val dateStr = remember(item.timestamp) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(item.timestamp))
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (item.category.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = barColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = item.category,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = barColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (item.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.note,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "৳ ${formatTaka(item.amount)}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = barColor
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Add Entry Dialog
// -------------------------------------------------------------
@Composable
fun AddFinanceEntryDialog(
    type: String,
    monthYear: String,
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, category: String, note: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val dialogTitle = when (type) {
        "SALE" -> "Monthly Sale (বিক্রি যোগ করুন)"
        "EXPENSE" -> "Monthly Expense (খরচ যোগ করুন)"
        "DEBT_PAY" -> "Monthly DEBT Pay (ঋণ পরিশোধ যোগ করুন)"
        "PRODUCT_BUY" -> "Monthly Product Buy (পণ্য ক্রয় যোগ করুন)"
        else -> "আর্থিক এন্ট্রি যোগ করুন"
    }

    val defaultCategories = when (type) {
        "SALE" -> listOf("শপ খুচরা বিক্রি", "পাইকারি অর্ডার", "অনলাইন অর্ডার", "অন্যান্য")
        "EXPENSE" -> listOf("দোকান ভাড়া", "কর্মচারীর বেতন", "বিদ্যুৎ ও ইউটিলিটি", "মার্কেটিং ও বিজ্ঞাপন", "অন্যান্য")
        "DEBT_PAY" -> listOf("ব্যাংক লোন কিস্তি", "সাপ্লায়ার বাকী শোধ", "ব্যক্তিগত ঋণ", "অন্যান্য")
        "PRODUCT_BUY" -> listOf("থান কাপড় / ফেব্রিক", "রেডিমেড পোশাক", "সুতা ও এক্সেসরিজ", "প্যাকেজিং বক্স/ব্যাগ")
        else -> emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = dialogTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "মাস: $monthYear • ডাটাবেজে সংরক্ষিত হবে",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { char -> char.isDigit() || char == '.' }
                        errorMsg = null
                    },
                    label = { Text("টাকার পরিমাণ (৳ Amount) *") },
                    placeholder = { Text("e.g. 5000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_finance_amount"),
                    singleLine = true
                )

                // Title / Description
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("বিবরণ / শিরোনাম *") },
                    placeholder = {
                        Text(
                            when (type) {
                                "SALE" -> "e.g. আজকের শপ সেল বা গ্রাহকের অর্ডার"
                                "EXPENSE" -> "e.g. চলতি মাসের দোকান ভাড়া"
                                "DEBT_PAY" -> "e.g. ইসলাম ফ্যাশন সাপ্লায়ার দেনা শোধ"
                                "PRODUCT_BUY" -> "e.g. ৫০ গজ কটন থান কাপড় ক্রয়"
                                else -> "বিবরণ লিখুন"
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_finance_title"),
                    singleLine = true
                )

                // Category Quick selection
                Text(
                    text = "ক্যাটাগরি নির্বাচন করুন:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    defaultCategories.take(3).forEach { cat ->
                        val selected = category == cat
                        Surface(
                            onClick = { category = if (selected) "" else cat },
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Note Field (Optional)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("মন্তব্য / নোট (ঐচ্ছিক)") },
                    placeholder = { Text("অতিরিক্ত কোনো তথ্য থাকলে লিখুন") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                errorMsg?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        errorMsg = "সঠিক টাকার পরিমাণ দিন"
                        return@Button
                    }
                    if (title.isBlank()) {
                        title = when (type) {
                            "SALE" -> "বিক্রির এন্ট্রি"
                            "EXPENSE" -> "খরচের এন্ট্রি"
                            "DEBT_PAY" -> "ঋণ পরিশোধের এন্ট্রি"
                            "PRODUCT_BUY" -> "পণ্য ক্রয়ের এন্ট্রি"
                            else -> "হিসাব"
                        }
                    }
                    onSave(title, amount, category, note)
                },
                modifier = Modifier.testTag("btn_confirm_save_finance")
            ) {
                Text("সংরক্ষণ করুন (Save)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

fun formatTaka(amount: Double): String {
    return NumberFormat.getNumberInstance(Locale.US).format(amount.toLong())
}

// -------------------------------------------------------------
// Component: Month & Year Calendar Picker Dialog
// -------------------------------------------------------------
@Composable
fun MonthYearCalendarPickerDialog(
    currentMonthYear: String,
    onDismiss: () -> Unit,
    onSelectMonthYear: (String) -> Unit
) {
    val sdf = remember { SimpleDateFormat("MMMM yyyy", Locale.ENGLISH) }
    val initialDate = remember(currentMonthYear) {
        try {
            sdf.parse(currentMonthYear) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }
    val initialCal = remember(initialDate) {
        Calendar.getInstance().apply { time = initialDate }
    }

    var selectedYear by remember { mutableStateOf(initialCal.get(Calendar.YEAR)) }
    var selectedMonthIndex by remember { mutableStateOf(initialCal.get(Calendar.MONTH)) } // 0 = Jan .. 11 = Dec

    val months = listOf(
        Pair("January", "জানুয়ারি"),
        Pair("February", "ফেব্রুয়ারি"),
        Pair("March", "মার্চ"),
        Pair("April", "এপ্রিল"),
        Pair("May", "মে"),
        Pair("June", "জুন"),
        Pair("July", "জুলাই"),
        Pair("August", "আগস্ট"),
        Pair("September", "সেপ্টেম্বর"),
        Pair("October", "অক্টোবর"),
        Pair("November", "নভেম্বর"),
        Pair("December", "ডিসেম্বর")
    )

    AlertDialog(
        modifier = Modifier.testTag("dialog_month_year_calendar"),
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "মাস ও বছর ক্যালেন্ডার",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "হিসাব দেখতে যেকোনো মাস ও বছর সিলেক্ট করুন",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Year selector controls
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { selectedYear-- },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("btn_cal_prev_year")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "পূর্ববর্তী বছর",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$selectedYear সাল",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "বছর পরিবর্তন করতে ডানে-বামে ট্যাপ করুন",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { selectedYear++ },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("btn_cal_next_year")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "পরবর্তী বছর",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 12 Months Grid (3 columns x 4 rows)
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (row in 0 until 4) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (col in 0 until 3) {
                                val monthIdx = row * 3 + col
                                val (enName, bnName) = months[monthIdx]
                                val isSelected = monthIdx == selectedMonthIndex

                                Surface(
                                    onClick = {
                                        selectedMonthIndex = monthIdx
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("month_item_$monthIdx")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = bnName,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = enName.take(3),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Short-cuts (Current Month & All Months)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            val nowCal = Calendar.getInstance()
                            selectedYear = nowCal.get(Calendar.YEAR)
                            selectedMonthIndex = nowCal.get(Calendar.MONTH)
                            val chosenMonthYear = "${months[selectedMonthIndex].first} $selectedYear"
                            onSelectMonthYear(chosenMonthYear)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_cal_current_month")
                    ) {
                        Text(text = "চলতি মাস", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = {
                            onSelectMonthYear("সকল মাস")
                            onDismiss()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_cal_all_months")
                    ) {
                        Text(text = "সকল মাস (All)", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val chosenMonthYear = "${months[selectedMonthIndex].first} $selectedYear"
                    onSelectMonthYear(chosenMonthYear)
                    onDismiss()
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_cal_confirm")
            ) {
                Text("সেট করুন (${months[selectedMonthIndex].first.take(3)} $selectedYear)")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_cal_cancel")
            ) {
                Text("বাতিল")
            }
        }
    )
}

package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.BrandProfileEntity
import com.example.data.db.ChatMessageEntity
import com.example.data.db.ChatSessionEntity
import com.example.data.db.CollectionEntity
import com.example.data.db.PricingSkuEntity
import com.example.data.db.SaleRecordEntity
import com.example.data.db.MonthlyFinanceEntity
import com.example.data.repository.AdvisorPersona
import com.example.data.repository.AdvisorRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


enum class MainAppTab(val title: String, val bnTitle: String) {
    AI_CHAT("AI Chat", "AI চ্যাট"),
    NEW_SALE("Sale", "Sale"),
    FIREBASE_DB("Firebase DB", "ফায়ারবেস DB")
}

enum class AppTab(val title: String, val icon: String) {
    ADVISOR_CHAT("AI Advisor", "chat"),
    PRICING_CALCULATOR("Costing & Margins", "calculator"),
    COLLECTIONS_DROPS("Drops & Planning", "collections"),
    PRODUCTION_SIZING("Size Curve & Sourcing", "sizing"),
    BRAND_DIAGNOSTIC("Brand Audit", "analytics")
}

data class FabricInfo(
    val name: String,
    val gsmWeight: String,
    val composition: String,
    val bestFor: String,
    val drapeFeel: String,
    val shrinkageAllowance: String,
    val proTip: String
)

class AdvisorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AdvisorRepository
    private val prefs = application.getSharedPreferences("gemini_api_prefs", android.content.Context.MODE_PRIVATE)

    // Gemini API Key Management
    private val _customApiKey = MutableStateFlow(prefs.getString("custom_gemini_api_key", "") ?: "")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _apiKeyStatus = MutableStateFlow(Pair(false, ""))
    val apiKeyStatus: StateFlow<Pair<Boolean, String>> = _apiKeyStatus.asStateFlow()

    private val _apiKeyTesting = MutableStateFlow(false)
    val apiKeyTesting: StateFlow<Boolean> = _apiKeyTesting.asStateFlow()

    // Firebase Firestore Sync Status (declared before init to prevent NPE during initial collect)
    private val _firestoreSyncStatus = MutableStateFlow<String?>("সংযুক্ত (Connected)")
    val firestoreSyncStatus: StateFlow<String?> = _firestoreSyncStatus.asStateFlow()

    // Business Diagnostic state (declared before init to prevent NPE during loadInitialProfile)
    private val _isGeneratingDiagnostic = MutableStateFlow(false)
    val isGeneratingDiagnostic: StateFlow<Boolean> = _isGeneratingDiagnostic.asStateFlow()
    private val _diagnosticReport = MutableStateFlow<String?>(null)
    val diagnosticReport: StateFlow<String?> = _diagnosticReport.asStateFlow()

    // Voice Assistant (Speech-To-Text & Text-To-Speech)
    private val voiceAssistant = com.example.data.util.VoiceAssistantManager(application)
    val isListening: StateFlow<Boolean> = voiceAssistant.isListening
    val speechPartialText: StateFlow<String> = voiceAssistant.speechPartialText
    val speechRmsDb: StateFlow<Float> = voiceAssistant.speechRmsDb
    val speechError: StateFlow<String?> = voiceAssistant.speechError
    val isSpeaking: StateFlow<Boolean> = voiceAssistant.isSpeaking
    val currentSpeakingMessageId: StateFlow<Long?> = voiceAssistant.currentSpeakingMessageId

    // Auto Voice Reply preference (persists across launches)
    private val _autoVoiceReply = MutableStateFlow(prefs.getBoolean("auto_voice_reply", true))
    val autoVoiceReply: StateFlow<Boolean> = _autoVoiceReply.asStateFlow()

    fun toggleAutoVoiceReply(enabled: Boolean? = null) {
        val updated = enabled ?: !_autoVoiceReply.value
        _autoVoiceReply.value = updated
        prefs.edit().putBoolean("auto_voice_reply", updated).apply()
    }

    fun startVoiceInput(onCompletedText: ((String) -> Unit)? = null) {
        voiceAssistant.startListening { recognizedText ->
            if (recognizedText.isNotBlank()) {
                _chatInput.value = recognizedText
                if (onCompletedText != null) {
                    onCompletedText(recognizedText)
                } else {
                    // Send directly as voice query so AI speaks back its answer
                    sendChatMessage(prompt = recognizedText, isVoiceQuery = true)
                }
            }
        }
    }

    fun stopVoiceInput() {
        voiceAssistant.stopListening()
    }

    fun cancelVoiceInput() {
        voiceAssistant.cancelListening()
    }

    fun speakMessage(messageId: Long, text: String) {
        voiceAssistant.speak(text, messageId)
    }

    fun stopSpeaking() {
        voiceAssistant.stopSpeaking()
    }



    init {
        val db = AppDatabase.getDatabase(application)
        repository = AdvisorRepository(db, application.applicationContext)
        val savedKey = prefs.getString("custom_gemini_api_key", "") ?: ""
        if (savedKey.isNotBlank()) {
            repository.setCustomApiKey(savedKey)
        }
        updateApiKeyStatus()
        loadInitialProfile()
        seedSampleDataIfEmpty()
        viewModelScope.launch {
            com.example.data.firebase.FirebaseAuthManager.currentUser.collect { user ->
                if (user != null) {
                    testFirestoreConnectionAndSync()
                    autoRestoreFromCloudIfEmpty()
                } else {
                    _firestoreSyncStatus.value = "Google অ্যাকাউন্ট সংযুক্ত নেই (লগইন করুন)"
                }
            }
        }
    }

    private fun autoRestoreFromCloudIfEmpty() {
        val user = com.example.data.firebase.FirebaseAuthManager.currentUser.value ?: return
        viewModelScope.launch {
            try {
                val localSales = repository.getAllSalesDirect()
                val localFinances = repository.getAllFinancesDirect()
                if (localSales.isEmpty() && localFinances.isEmpty()) {
                    _firestoreSyncStatus.value = "ক্লাউড থেকে ডাটা সিঙ্ক হচ্ছে..."
                    val (restoredSales, restoredFinances) = repository.restoreAllFromFirestore()
                    if (restoredSales > 0 || restoredFinances > 0) {
                        _firestoreSyncStatus.value = "ক্লাউড থেকে $restoredSales টি বিক্রি ও $restoredFinances টি হিসাব পুনরুদ্ধার হয়েছে"
                    } else {
                        _firestoreSyncStatus.value = "সংযুক্ত (Connected)"
                    }
                }
            } catch (e: Exception) {
                Log.w("AdvisorViewModel", "Auto cloud sync notice: ${e.message}")
            }
        }
    }

    private fun updateApiKeyStatus() {
        _apiKeyStatus.value = repository.getApiKeyInfo()
    }

    fun saveCustomApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString("custom_gemini_api_key", trimmed).apply()
        _customApiKey.value = trimmed
        repository.setCustomApiKey(trimmed.ifBlank { null })
        updateApiKeyStatus()
    }

    fun clearCustomApiKey() {
        prefs.edit().remove("custom_gemini_api_key").apply()
        _customApiKey.value = ""
        repository.setCustomApiKey(null)
        updateApiKeyStatus()
    }

    fun testApiKey(keyToTest: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _apiKeyTesting.value = true
            val effective = if (keyToTest.isNotBlank()) keyToTest else repository.getEffectiveApiKey()
            val result = repository.validateApiKey(effective)
            _apiKeyTesting.value = false
            onComplete(result.first, result.second)
        }
    }



    // Navigation
    private val _selectedMainTab = MutableStateFlow(MainAppTab.AI_CHAT)
    val selectedMainTab: StateFlow<MainAppTab> = _selectedMainTab.asStateFlow()

    fun selectMainTab(tab: MainAppTab) {
        _selectedMainTab.value = tab
    }

    private val _currentTab = MutableStateFlow(AppTab.ADVISOR_CHAT)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    // --- Sales Records ---
    val allSales: StateFlow<List<SaleRecordEntity>> = repository.getAllSales()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveNewSale(sale: SaleRecordEntity, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = repository.saveSale(sale)
            onComplete?.invoke(id)
        }
    }

    fun deleteSale(id: Long) {
        viewModelScope.launch {
            repository.deleteSale(id)
        }
    }

    fun askAiAboutSale(sale: SaleRecordEntity) {
        val prompt = "আমি একটি নতুন বিক্রির হিসাব করেছি:\n" +
                "ইনভয়েস: ${sale.invoiceNumber}\n" +
                "ক্রেতা: ${if (sale.customerName.isNotBlank()) sale.customerName else "সাধারণ কাস্টমার"}\n" +
                "আইটেম: ${sale.itemsSummary}\n" +
                "মোট টাকা: ৳${sale.totalAmount.toInt()}, পেইড: ৳${sale.paidAmount.toInt()}, বাকি: ৳${sale.dueAmount.toInt()}\n" +
                "পেমেন্ট মেথড: ${sale.paymentMethod}\n" +
                "এই বিক্রির ওপর ভিত্তি করে লাভ বাড়ানো এবং আরও বিক্রি বৃদ্ধির জন্য ৩টি প্রফেশনাল পরামর্শ দিন।"
        selectMainTab(MainAppTab.AI_CHAT)
        sendChatMessage(prompt)
    }

    // --- Firebase Firestore ---


    fun testFirestoreConnection(onResult: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            _firestoreSyncStatus.value = "ফায়ারবেস ক্লাউড ডাটাবেজ যাচাই করা হচ্ছে..."
            val result = repository.testFirestoreConnection()
            result.onSuccess { (salesCount, financesCount) ->
                val msg = "সফলভাবে সংযুক্ত! ($salesCount টি বিক্রি ও $financesCount টি আর্থিক রেকর্ড ক্লাউডে সক্রিয়)"
                _firestoreSyncStatus.value = msg
                onResult?.invoke(true, msg)
            }.onFailure { ex ->
                val errorMsg = "ডাটাবেজ সংযোগ ত্রুটি: ${ex.localizedMessage ?: "সংযোগ ব্যর্থ"}"
                _firestoreSyncStatus.value = errorMsg
                onResult?.invoke(false, errorMsg)
            }
        }
    }

    fun testFirestoreConnectionAndSync(onResult: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            _firestoreSyncStatus.value = "ক্লাউড ডাটাবেজ যাচাই ও সিঙ্ক হচ্ছে..."
            val testRes = repository.testFirestoreConnection()
            testRes.onSuccess {
                try {
                    val (salesCount, financesCount) = repository.syncAllToFirestore()
                    val msg = "ডাটাবেজ সংযুক্ত ও সিঙ্ক সম্পন্ন ($salesCount টি বিক্রি, $financesCount টি আর্থিক হিসাব)"
                    _firestoreSyncStatus.value = msg
                    onResult?.invoke(true, msg)
                } catch (e: Exception) {
                    val errorMsg = "সিঙ্ক ত্রুটি: ${e.localizedMessage ?: "ত্রুটি"}"
                    _firestoreSyncStatus.value = errorMsg
                    onResult?.invoke(false, errorMsg)
                }
            }.onFailure { ex ->
                val errorMsg = "ডাটাবেজ সংযোগ ত্রুটি: ${ex.localizedMessage ?: "সংযোগ ব্যর্থ"}"
                _firestoreSyncStatus.value = errorMsg
                onResult?.invoke(false, errorMsg)
            }
        }
    }

    fun syncAllSalesToFirestore(onResult: ((Int, String) -> Unit)? = null) {
        viewModelScope.launch {
            _firestoreSyncStatus.value = "ক্লাউডে সিঙ্ক হচ্ছে..."
            try {
                val (salesCount, financesCount) = repository.syncAllToFirestore()
                val total = salesCount + financesCount
                val msg = "সফলভাবে $salesCount টি বিক্রির রেকর্ড ও $financesCount টি আর্থিক হিসাব ক্লাউডে সিঙ্ক হয়েছে"
                _firestoreSyncStatus.value = msg
                onResult?.invoke(total, msg)
            } catch (e: Exception) {
                val errorMsg = "সিঙ্ক ত্রুটি: ${e.localizedMessage ?: "Firebase সমস্যা"}"
                _firestoreSyncStatus.value = errorMsg
                onResult?.invoke(0, errorMsg)
            }
        }
    }

    fun restoreAllFromFirestore(onResult: ((Int, String) -> Unit)? = null) {
        viewModelScope.launch {
            _firestoreSyncStatus.value = "ক্লাউড থেকে রিস্টোর হচ্ছে..."
            try {
                val (salesCount, financesCount) = repository.restoreAllFromFirestore()
                val total = salesCount + financesCount
                val msg = if (total > 0) {
                    "ক্লাউড থেকে $salesCount টি বিক্রির রেকর্ড ও $financesCount টি আর্থিক হিসাব সফলভাবে রিস্টোর হয়েছে"
                } else {
                    "ক্লাউডে কোনো সংরক্ষিত ডাটা পাওয়া যায়নি"
                }
                _firestoreSyncStatus.value = msg
                onResult?.invoke(total, msg)
            } catch (e: Exception) {
                val errorMsg = "রিস্টোর ত্রুটি: ${e.localizedMessage ?: "ক্লাউড সংযোগ ত্রুটি"}"
                _firestoreSyncStatus.value = errorMsg
                onResult?.invoke(0, errorMsg)
            }
        }
    }

    // --- Monthly Finances (Sale, Expense, DEBT Pay, Product Buy) & Calendar Filtering ---
    private val currentDefaultMonthYear = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(Date())
    private val _selectedMonthYear = MutableStateFlow(currentDefaultMonthYear)
    val selectedMonthYear: StateFlow<String> = _selectedMonthYear.asStateFlow()

    fun selectMonthYear(monthYear: String) {
        _selectedMonthYear.value = monthYear
    }

    fun selectPreviousMonth() {
        _selectedMonthYear.value = shiftMonth(_selectedMonthYear.value, -1)
    }

    fun selectNextMonth() {
        _selectedMonthYear.value = shiftMonth(_selectedMonthYear.value, 1)
    }

    private fun shiftMonth(current: String, delta: Int): String {
        return try {
            val sdf = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
            val date = sdf.parse(current) ?: Date()
            val cal = Calendar.getInstance().apply {
                time = date
                add(Calendar.MONTH, delta)
            }
            sdf.format(cal.time)
        } catch (e: Exception) {
            current
        }
    }

    val allFinances: StateFlow<List<MonthlyFinanceEntity>> = repository.getAllFinances()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val filteredFinances: StateFlow<List<MonthlyFinanceEntity>> = combine(allFinances, _selectedMonthYear) { list, selectedMonth ->
        if (selectedMonth == "ALL" || selectedMonth == "সকল মাস") {
            list
        } else {
            val sdf = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
            list.filter { item ->
                if (item.monthYear.isNotBlank()) {
                    item.monthYear.equals(selectedMonth, ignoreCase = true)
                } else {
                    try {
                        val itemDateMonth = sdf.format(Date(item.timestamp))
                        itemDateMonth.equals(selectedMonth, ignoreCase = true)
                    } catch (e: Exception) {
                        false
                    }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlySales: StateFlow<List<MonthlyFinanceEntity>> = filteredFinances.map { list ->
        list.filter { it.type == "SALE" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyExpenses: StateFlow<List<MonthlyFinanceEntity>> = filteredFinances.map { list ->
        list.filter { it.type == "EXPENSE" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyDebtPays: StateFlow<List<MonthlyFinanceEntity>> = filteredFinances.map { list ->
        list.filter { it.type == "DEBT_PAY" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyProductBuys: StateFlow<List<MonthlyFinanceEntity>> = filteredFinances.map { list ->
        list.filter { it.type == "PRODUCT_BUY" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalMonthlySale: StateFlow<Double> = monthlySales.map { list ->
        list.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalMonthlyExpense: StateFlow<Double> = monthlyExpenses.map { list ->
        list.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalMonthlyDebtPay: StateFlow<Double> = monthlyDebtPays.map { list ->
        list.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalMonthlyProductBuy: StateFlow<Double> = monthlyProductBuys.map { list ->
        list.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val netBalance: StateFlow<Double> = filteredFinances.map { list ->
        val sale = list.filter { it.type == "SALE" }.sumOf { it.amount }
        val exp = list.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val debt = list.filter { it.type == "DEBT_PAY" }.sumOf { it.amount }
        val buy = list.filter { it.type == "PRODUCT_BUY" }.sumOf { it.amount }
        sale - (exp + debt + buy)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun addMonthlyFinance(
        type: String, // "SALE", "EXPENSE", "DEBT_PAY", "PRODUCT_BUY"
        title: String,
        amount: Double,
        category: String = "",
        note: String = "",
        monthYear: String? = null,
        onComplete: ((Long) -> Unit)? = null
    ) {
        val effectiveMonthYear = monthYear?.ifBlank { null } ?: _selectedMonthYear.value.let {
            if (it == "ALL" || it == "সকল মাস") SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(Date()) else it
        }
        viewModelScope.launch {
            val entity = MonthlyFinanceEntity(
                type = type,
                title = title.ifBlank {
                    when (type) {
                        "SALE" -> "মাসিক বিক্রি"
                        "EXPENSE" -> "মাসিক খরচ"
                        "DEBT_PAY" -> "ঋণ পরিশোধ"
                        "PRODUCT_BUY" -> "পণ্য ক্রয়"
                        else -> "আর্থিক এন্ট্রি"
                    }
                },
                amount = amount,
                category = category,
                monthYear = effectiveMonthYear,
                note = note
            )
            val id = repository.saveFinance(entity)
            onComplete?.invoke(id)
        }
    }

    fun deleteMonthlyFinance(id: Long, type: String) {
        viewModelScope.launch {
            repository.deleteFinance(id, type)
        }
    }

    fun askAiAboutFinances(analysisType: String = "ALL") {
        val sale = totalMonthlySale.value
        val expense = totalMonthlyExpense.value
        val debt = totalMonthlyDebtPay.value
        val buy = totalMonthlyProductBuy.value
        val net = sale - (expense + debt + buy)
        val profitMargin = if (sale > 0) ((net / sale) * 100).toInt() else 0

        val salesList = monthlySales.value.take(3).joinToString { "${it.title}: ৳${it.amount.toInt()}" }
        val expensesList = monthlyExpenses.value.take(3).joinToString { "${it.title}: ৳${it.amount.toInt()}" }
        val debtList = monthlyDebtPays.value.take(3).joinToString { "${it.title}: ৳${it.amount.toInt()}" }
        val buyList = monthlyProductBuys.value.take(3).joinToString { "${it.title}: ৳${it.amount.toInt()}" }

        val currentMonthName = _selectedMonthYear.value
        val prompt = when (analysisType) {
            "EXPENSE_REDUCE" -> """
                আমার $currentMonthName মাসের খরচের বিশদ হিসাব:
                - মোট বিক্রি (Monthly Sale): ৳${sale.toInt()}
                - মোট খরচ (Monthly Expense): ৳${expense.toInt()} ${if (expensesList.isNotBlank()) "($expensesList)" else ""}
                - পণ্যের ক্রয় (Product Buy): ৳${buy.toInt()}
                - নিট ব্যালেন্স: ৳${net.toInt()}

                আমার এই খরচের তথ্য পর্যালোচনা করে কীভাবে অহেতুক খরচ কমিয়ে নিট প্রফিট বাড়ানো যায়, তার ৫টি বাস্তবসম্মত কৌশল বাংলায় পয়েন্ট আকারে দিন।
            """.trimIndent()

            "DEBT_STRATEGY" -> """
                আমার $currentMonthName মাসের দেনা ও ঋণ পরিশোধের চিত্র:
                - মোট বিক্রি (Monthly Sale): ৳${sale.toInt()}
                - এই মাসে ঋণ পরিশোধ (Monthly DEBT Pay): ৳${debt.toInt()} ${if (debtList.isNotBlank()) "($debtList)" else ""}
                - বর্তমান ক্যাশ ব্যালেন্স: ৳${net.toInt()}

                আমার এই বিক্রির আয়ের সাথে ঋণ পরিশোধের অনুপাত বিশ্লেষণ করে কীভাবে দ্রুত ও ঝুঁকিমুক্তভাবে ঋণমুক্ত হওয়া যায় এবং ব্যবসার ক্যাশফ্লো ঠিক রাখা যায়, তার একটি কার্যকর রোডম্যাপ দিন।
            """.trimIndent()

            "PRODUCT_BUY_STRATEGY" -> """
                আমার $currentMonthName মাসের পণ্য ক্রয় ও ইনভেন্টরির হিসাব:
                - মোট বিক্রি (Monthly Sale): ৳${sale.toInt()}
                - পণ্য ক্রয় (Product Buy): ৳${buy.toInt()} ${if (buyList.isNotBlank()) "($buyList)" else ""}
                - বর্তমান নিট ক্যাশ: ৳${net.toInt()}

                বিক্রির কত শতাংশ পরবর্তী মাসের জন্য নতুন পণ্য বা স্টক ক্রয়ে রি-ইনভেস্ট করা উচিত এবং কীভাবে ডেড স্টক এড়িয়ে সর্বোচ্চ লাভ নিশ্চিত করা যায়, তা পরামর্শ দিন।
            """.trimIndent()

            else -> """
                আমার পোশাক/ব্যবসার $currentMonthName মাসের সার্বিক আর্থিক প্রতিবেদন নিম্নরূপ:
                1. 💰 মাসিক বিক্রি (Monthly Sale): ৳${sale.toInt()} ${if (salesList.isNotBlank()) "[$salesList]" else ""}
                2. 📉 মাসিক খরচ (Monthly Expense): ৳${expense.toInt()} ${if (expensesList.isNotBlank()) "[$expensesList]" else ""}
                3. 💳 ঋণ পরিশোধ (Monthly DEBT Pay): ৳${debt.toInt()} ${if (debtList.isNotBlank()) "[$debtList]" else ""}
                4. 📦 পণ্য ক্রয় (Monthly Product Buy): ৳${buy.toInt()} ${if (buyList.isNotBlank()) "[$buyList]" else ""}
                ------------------------------------------
                📊 নিট প্রফিট / ব্যালেন্স (Net Balance): ৳${net.toInt()}
                📈 নিট প্রফিট মার্জিন: $profitMargin%

                এই তথ্যের ভিত্তিতে একজন বিশেষজ্ঞ বিজনেস ও ফাইন্যান্সিয়াল কনসালটেন্ট হিসেবে:
                ১. আমার ব্যবসার বর্তমান আর্থিক স্বাস্থ্য বিশ্লেষণ করুন (Financial Health Audit)
                ২. খরচ ও ঋণ পরিশোধের ভারসাম্য কেমন আছে তা জানান
                ৩. আগামী মাসে বিক্রি দ্বিগুণ করতে এবং লাভ বাড়াতে সুনির্দিষ্ট ৩টি অ্যাকশন প্ল্যান দিন।
            """.trimIndent()
        }

        selectMainTab(MainAppTab.AI_CHAT)
        sendChatMessage(prompt)
    }

    // --- Chat Section & Sessions ---
    private val _selectedPersona = MutableStateFlow(AdvisorPersona.GENERAL)
    val selectedPersona: StateFlow<AdvisorPersona> = _selectedPersona.asStateFlow()

    private val _currentSessionId = MutableStateFlow<Long?>(null)
    val currentSessionId: StateFlow<Long?> = _currentSessionId.asStateFlow()

    val allSessions: StateFlow<List<ChatSessionEntity>> = repository.getAllSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    private val _chatInput = MutableStateFlow("")
    val chatInput: StateFlow<String> = _chatInput.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val chatMessages: StateFlow<List<ChatMessageEntity>> = _currentSessionId
        .flatMapLatest { sessionId ->
            if (sessionId == null) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            } else {
                repository.getMessagesForSession(sessionId)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectPersona(persona: AdvisorPersona) {
        _selectedPersona.value = persona
    }

    fun onChatInputChange(newText: String) {
        _chatInput.value = newText
    }

    fun startNewSession(persona: AdvisorPersona = _selectedPersona.value) {
        _selectedPersona.value = persona
        _currentSessionId.value = null
        _chatInput.value = ""
    }

    fun selectSession(sessionId: Long) {
        _currentSessionId.value = sessionId
        // Sync persona if session entity has it
        viewModelScope.launch {
            val session = allSessions.value.find { it.id == sessionId }
            if (session != null) {
                val matchingPersona = AdvisorPersona.values().find { it.id == session.persona }
                    ?: AdvisorPersona.GENERAL
                _selectedPersona.value = matchingPersona
            }
        }
    }

    fun renameSession(sessionId: Long, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            repository.renameSession(sessionId, newTitle)
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                _currentSessionId.value = null
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
            _currentSessionId.value = null
        }
    }

    data class AttachedMediaInfo(
        val uriString: String,
        val localFilePath: String,
        val fileName: String,
        val mimeType: String,
        val isVideo: Boolean,
        val sizeBytes: Long
    )

    private val _attachedMedia = MutableStateFlow<AttachedMediaInfo?>(null)
    val attachedMedia: StateFlow<AttachedMediaInfo?> = _attachedMedia.asStateFlow()

    fun attachMediaUri(uri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val result = com.example.data.util.MediaUtils.saveUriToInternalStorage(context, uri)
            if (result != null) {
                _attachedMedia.value = AttachedMediaInfo(
                    uriString = uri.toString(),
                    localFilePath = result.file.absolutePath,
                    fileName = result.fileName,
                    mimeType = result.mimeType,
                    isVideo = result.isVideo,
                    sizeBytes = result.sizeBytes
                )
            }
        }
    }

    fun attachBitmap(bitmap: android.graphics.Bitmap, context: android.content.Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val result = com.example.data.util.MediaUtils.saveBitmapToInternalStorage(context, bitmap)
            if (result != null) {
                _attachedMedia.value = AttachedMediaInfo(
                    uriString = result.file.toURI().toString(),
                    localFilePath = result.file.absolutePath,
                    fileName = result.fileName,
                    mimeType = result.mimeType,
                    isVideo = false,
                    sizeBytes = result.sizeBytes
                )
            }
        }
    }

    fun removeAttachedMedia() {
        _attachedMedia.value = null
    }

    fun sendChatMessage(
        prompt: String? = null,
        isVoiceQuery: Boolean = false
    ) {
        val media = _attachedMedia.value
        val textToSend = (prompt ?: _chatInput.value).trim()
        if ((textToSend.isBlank() && media == null) || _isChatLoading.value) return

        if (prompt == null) {
            _chatInput.value = ""
        }
        _attachedMedia.value = null

        viewModelScope.launch {
            _isChatLoading.value = true
            try {
                val currentHistory = chatMessages.value
                val (createdOrActiveId, modelResponseText) = repository.sendChatMessage(
                    sessionId = _currentSessionId.value,
                    persona = _selectedPersona.value,
                    userPrompt = textToSend,
                    history = currentHistory,
                    attachedFilePath = media?.localFilePath,
                    attachedMimeType = media?.mimeType,
                    attachedFileName = media?.fileName,
                    attachedIsVideo = media?.isVideo ?: false
                )
                if (_currentSessionId.value == null) {
                    _currentSessionId.value = createdOrActiveId
                }

                // If user asked with voice or auto voice reply is turned on, speak the answer
                if ((isVoiceQuery || _autoVoiceReply.value) && modelResponseText.isNotBlank()) {
                    kotlinx.coroutines.delay(250)
                    voiceAssistant.speak(modelResponseText, System.currentTimeMillis())
                }
            } catch (e: Exception) {
                Log.e("AdvisorViewModel", "Failed to send chat message: ${e.message}", e)
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    fun retryMessage(prompt: String) {
        sendChatMessage(prompt = prompt)
    }

    fun clearCurrentChat() {
        val activeId = _currentSessionId.value
        if (activeId != null) {
            deleteSession(activeId)
        } else {
            _chatInput.value = ""
        }
    }

    // --- Pricing & Costing Section ---
    val savedSkus: StateFlow<List<PricingSkuEntity>> = repository.getAllSkus()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isAuditingPricing = MutableStateFlow(false)
    val isAuditingPricing: StateFlow<Boolean> = _isAuditingPricing.asStateFlow()

    private val _latestPricingAudit = MutableStateFlow<String?>(null)
    val latestPricingAudit: StateFlow<String?> = _latestPricingAudit.asStateFlow()

    fun savePricingSku(
        name: String,
        category: String,
        fabric: Double,
        labor: Double,
        trims: Double,
        shipping: Double,
        overhead: Double,
        retail: Double,
        wholesale: Double
    ) {
        viewModelScope.launch {
            val totalCogs = fabric + labor + trims + shipping + overhead
            val profit = retail - totalCogs
            val margin = if (retail > 0) (profit / retail) * 100.0 else 0.0

            val entity = PricingSkuEntity(
                itemName = name,
                category = category,
                fabricCost = fabric,
                laborCost = labor,
                trimsPackagingCost = trims,
                shippingDutiesCost = shipping,
                overheadBuffer = overhead,
                retailPrice = retail,
                wholesalePrice = wholesale,
                profitPerUnit = profit,
                marginPercentage = margin,
                aiCritique = _latestPricingAudit.value ?: ""
            )
            repository.saveSku(entity)
        }
    }

    fun deletePricingSku(sku: PricingSkuEntity) {
        viewModelScope.launch {
            repository.deleteSku(sku)
        }
    }

    fun auditCurrentPricing(
        name: String,
        category: String,
        fabric: Double,
        labor: Double,
        trims: Double,
        shipping: Double,
        overhead: Double,
        retail: Double,
        wholesale: Double
    ) {
        val totalCogs = fabric + labor + trims + shipping + overhead
        val profit = retail - totalCogs
        val margin = if (retail > 0) (profit / retail) * 100.0 else 0.0

        val tempSku = PricingSkuEntity(
            itemName = name.ifBlank { "Unsaved Garment" },
            category = category,
            fabricCost = fabric,
            laborCost = labor,
            trimsPackagingCost = trims,
            shippingDutiesCost = shipping,
            overheadBuffer = overhead,
            retailPrice = retail,
            wholesalePrice = wholesale,
            profitPerUnit = profit,
            marginPercentage = margin
        )

        viewModelScope.launch {
            _isAuditingPricing.value = true
            try {
                val auditResult = repository.auditPricing(tempSku)
                _latestPricingAudit.value = auditResult
            } finally {
                _isAuditingPricing.value = false
            }
        }
    }

    fun clearPricingAudit() {
        _latestPricingAudit.value = null
    }

    // --- Collections & Drops Section ---
    val collections: StateFlow<List<CollectionEntity>> = repository.getAllCollections()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isGeneratingDropStrategy = MutableStateFlow(false)
    val isGeneratingDropStrategy: StateFlow<Boolean> = _isGeneratingDropStrategy.asStateFlow()

    private val _latestDropStrategy = MutableStateFlow<String?>(null)
    val latestDropStrategy: StateFlow<String?> = _latestDropStrategy.asStateFlow()

    fun createCollection(
        title: String,
        season: String,
        launchDate: String,
        pieceCount: Int,
        budget: Double,
        projectedRevenue: Double,
        notes: String
    ) {
        viewModelScope.launch {
            val entity = CollectionEntity(
                title = title,
                season = season,
                targetLaunchDate = launchDate,
                pieceCount = pieceCount,
                budget = budget,
                projectedRevenue = projectedRevenue,
                status = "PLANNING",
                notes = notes,
                aiStrategyNotes = _latestDropStrategy.value ?: ""
            )
            repository.saveCollection(entity)
            _latestDropStrategy.value = null
        }
    }

    fun updateCollectionStatus(collection: CollectionEntity, newStatus: String) {
        viewModelScope.launch {
            repository.updateCollection(collection.copy(status = newStatus))
        }
    }

    fun deleteCollection(collection: CollectionEntity) {
        viewModelScope.launch {
            repository.deleteCollection(collection)
        }
    }

    fun generateDropStrategy(
        title: String,
        season: String,
        budget: Double,
        pieceCount: Int,
        notes: String
    ) {
        viewModelScope.launch {
            _isGeneratingDropStrategy.value = true
            try {
                val strategy = repository.generateDropStrategy(
                    title = title.ifBlank { "Untitled Collection" },
                    season = season,
                    budget = budget,
                    pieceCount = pieceCount,
                    notes = notes
                )
                _latestDropStrategy.value = strategy
            } finally {
                _isGeneratingDropStrategy.value = false
            }
        }
    }

    // --- Size Curve & Sourcing ---
    private val _sizeCurveAdvice = MutableStateFlow<String?>(null)
    val sizeCurveAdvice: StateFlow<String?> = _sizeCurveAdvice.asStateFlow()

    private val _isCalculatingSize = MutableStateFlow(false)
    val isCalculatingSize: StateFlow<Boolean> = _isCalculatingSize.asStateFlow()

    fun calculateSizeCurve(totalUnits: Int, fitType: String) {
        viewModelScope.launch {
            _isCalculatingSize.value = true
            try {
                val advice = repository.getQuickSizeAdvice(totalUnits, fitType)
                _sizeCurveAdvice.value = advice
            } finally {
                _isCalculatingSize.value = false
            }
        }
    }

    // --- Brand Diagnostic & Growth ---
    val brandProfile: StateFlow<BrandProfileEntity?> = repository.getBrandProfileFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun saveProfile(profile: BrandProfileEntity) {
        viewModelScope.launch {
            repository.saveBrandProfile(profile)
        }
    }

    fun runBusinessDiagnostic(profile: BrandProfileEntity) {
        viewModelScope.launch {
            _isGeneratingDiagnostic.value = true
            try {
                val report = repository.generateBusinessDiagnostic(profile)
                _diagnosticReport.value = report
                repository.saveBrandProfile(
                    profile.copy(lastDiagnosticReport = report, lastUpdated = System.currentTimeMillis())
                )
            } finally {
                _isGeneratingDiagnostic.value = false
            }
        }
    }

    private fun loadInitialProfile() {
        viewModelScope.launch {
            val p = repository.getBrandProfile()
            if (p.lastDiagnosticReport.isNotBlank()) {
                _diagnosticReport.value = p.lastDiagnosticReport
            }
        }
    }

    private fun seedSampleDataIfEmpty() {
        viewModelScope.launch {
            val skus = repository.getAllSkus()
            // Sample seed collections if empty
            val initialProfile = repository.getBrandProfile()
            if (initialProfile.lastDiagnosticReport.isBlank()) {
                // profile ready
            }
        }
    }

    // Curated Fabric Guide Reference Data
    val fabricLibrary: List<FabricInfo> = listOf(
        FabricInfo(
            name = "Heavyweight Combed Cotton",
            gsmWeight = "240 - 280 GSM",
            composition = "100% Combed Ring-Spun Cotton",
            bestFor = "Luxury Streetwear Boxy T-shirts & Graphic Tees",
            drapeFeel = "Structured, minimal clinging, substantial vintage drape",
            shrinkageAllowance = "Pre-shrunk (allow 3-5% for garment wash)",
            proTip = "Use a 1x1 tight ribbed collar with double-needle 7/8 inch coverstitching to prevent neckline stretching."
        ),
        FabricInfo(
            name = "Loopback French Terry",
            gsmWeight = "380 - 450 GSM",
            composition = "100% Organic Cotton (Loopback knit)",
            bestFor = "Four-season Hoodies, Crewnecks & Relaxed Sweatpants",
            drapeFeel = "Cozy interior loops, clean smooth exterior face, zero lint shedding",
            shrinkageAllowance = "5-7% length shrinkage unless silicone enzyme pre-washed",
            proTip = "French Terry maintains structure better than brushed fleece in warmer climates."
        ),
        FabricInfo(
            name = "Brushed Back Heavy Fleece",
            gsmWeight = "460 - 500+ GSM",
            composition = "80% Cotton / 20% Polyester or 100% Cotton",
            bestFor = "Winter Statement Hoodies, Puffer Liners & Tracksuits",
            drapeFeel = "Ultra plush, heavyweight warmth, boxy architectural silhouette",
            shrinkageAllowance = "Minimal (if poly blend) / 4% (100% cotton)",
            proTip = "Add double-layer self-fabric hood to make the hood stand upright without collapsing."
        ),
        FabricInfo(
            name = "Mercerized Pima Cotton",
            gsmWeight = "180 - 200 GSM",
            composition = "100% Extra Long Staple (ELS) Pima Cotton",
            bestFor = "Elevated Basics, Minimalist Polo Shirts, High-end Undershirts",
            drapeFeel = "Silky luster, cool to the touch, zero pilling",
            shrinkageAllowance = "Very low (< 2%)",
            proTip = "Mercerization increases dye uptake by 25%, making deep blacks and rich colors stay vibrant after 50+ washes."
        ),
        FabricInfo(
            name = "Linen-Cotton Slub Blend",
            gsmWeight = "160 - 210 GSM",
            composition = "55% Linen / 45% Cotton",
            bestFor = "Summer Resort Shirts, Camp Collar Tops, Relaxed Trousers",
            drapeFeel = "Textured slub weave, breathable, softens significantly with wear",
            shrinkageAllowance = "5-8% (Must specify garment garment wash before cut)",
            proTip = "Blending with cotton reduces pure linen wrinkles while maintaining breathable thermal regulation."
        ),
        FabricInfo(
            name = "Nylon Ripstop & Tactel",
            gsmWeight = "110 - 150 GSM",
            composition = "100% High-Density Polyamide / Nylon with DWR Coating",
            bestFor = "Technical Windbreakers, Cargo Pants, Utility Vests",
            drapeFeel = "Crisp, lightweight, water-repellent, tear-resistant grid pattern",
            shrinkageAllowance = "0%",
            proTip = "Specify taped sealed seams if marketing as fully waterproof."
        )
    )

    override fun onCleared() {
        super.onCleared()
        voiceAssistant.shutdown()
    }
}

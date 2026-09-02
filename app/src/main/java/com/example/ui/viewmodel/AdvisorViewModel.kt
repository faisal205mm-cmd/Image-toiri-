package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.BrandProfileEntity
import com.example.data.db.ChatMessageEntity
import com.example.data.db.ChatSessionEntity
import com.example.data.db.CollectionEntity
import com.example.data.db.PricingSkuEntity
import com.example.data.repository.AdvisorPersona
import com.example.data.repository.AdvisorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    // API Key Management
    private val _customApiKey = MutableStateFlow(prefs.getString("custom_gemini_api_key", "") ?: "")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _apiKeyStatus = MutableStateFlow(Pair(false, ""))
    val apiKeyStatus: StateFlow<Pair<Boolean, String>> = _apiKeyStatus.asStateFlow()

    private val _apiKeyTesting = MutableStateFlow(false)
    val apiKeyTesting: StateFlow<Boolean> = _apiKeyTesting.asStateFlow()

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
    private val _currentTab = MutableStateFlow(AppTab.ADVISOR_CHAT)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
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

    private val _isImageMode = MutableStateFlow(false)
    val isImageMode: StateFlow<Boolean> = _isImageMode.asStateFlow()

    private val _selectedAspectRatio = MutableStateFlow("1:1")
    val selectedAspectRatio: StateFlow<String> = _selectedAspectRatio.asStateFlow()

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

    fun toggleImageMode(enabled: Boolean? = null) {
        _isImageMode.value = enabled ?: !_isImageMode.value
    }

    fun setSelectedAspectRatio(ratio: String) {
        _selectedAspectRatio.value = ratio
    }

    fun sendChatMessage(prompt: String? = null, forceImageMode: Boolean? = null) {
        val media = _attachedMedia.value
        val textToSend = (prompt ?: _chatInput.value).trim()
        if ((textToSend.isBlank() && media == null) || _isChatLoading.value) return

        if (prompt == null) {
            _chatInput.value = ""
        }
        _attachedMedia.value = null

        val effectiveImageMode = forceImageMode ?: _isImageMode.value

        viewModelScope.launch {
            _isChatLoading.value = true
            try {
                val currentHistory = chatMessages.value
                val (createdOrActiveId, _) = repository.sendChatMessage(
                    sessionId = _currentSessionId.value,
                    persona = _selectedPersona.value,
                    userPrompt = textToSend,
                    history = currentHistory,
                    isImageMode = effectiveImageMode,
                    aspectRatio = _selectedAspectRatio.value,
                    attachedFilePath = media?.localFilePath,
                    attachedMimeType = media?.mimeType,
                    attachedFileName = media?.fileName,
                    attachedIsVideo = media?.isVideo ?: false
                )
                if (_currentSessionId.value == null) {
                    _currentSessionId.value = createdOrActiveId
                }
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    fun retryMessage(prompt: String, isImagePrompt: Boolean = false) {
        sendChatMessage(prompt = prompt, forceImageMode = if (isImagePrompt) true else null)
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

    private val _isGeneratingDiagnostic = MutableStateFlow(false)
    val isGeneratingDiagnostic: StateFlow<Boolean> = _isGeneratingDiagnostic.asStateFlow()

    private val _diagnosticReport = MutableStateFlow<String?>(null)
    val diagnosticReport: StateFlow<String?> = _diagnosticReport.asStateFlow()

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
}

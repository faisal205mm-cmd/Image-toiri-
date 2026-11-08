package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.SeasonalTrendEntity
import com.example.data.model.DesignDetail
import com.example.data.model.EntrepreneurEconomics
import com.example.data.model.FabricRecommendation
import com.example.data.model.GoToMarketStep
import com.example.data.model.SeasonalTrendAnalysisResult
import com.example.data.model.SeasonalTrendRequest
import com.example.data.model.SilhouetteRecommendation
import com.example.data.model.TrendColor
import com.example.ui.viewmodel.AdvisorViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BrandGreen = Color(0xFF00C853)
private val BrandDarkGreen = Color(0xFF007A3D)
private val NeutralSlate = Color(0xFF1E293B)
private val SubtitleGray = Color(0xFF64748B)
private val CardBorderColor = Color(0xFFE2E8F0)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SeasonalTrendsScreen(
    viewModel: AdvisorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentResult by viewModel.seasonalTrendResult.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzingTrends.collectAsStateWithLifecycle()
    val analysisError by viewModel.trendAnalysisError.collectAsStateWithLifecycle()
    val savedTrends by viewModel.savedTrends.collectAsStateWithLifecycle()

    // Form inputs state
    var selectedSeason by remember { mutableStateOf("Summer 2026") }
    var selectedCategory by remember { mutableStateOf("Streetwear & Oversized Tees") }
    var selectedTargetMarket by remember { mutableStateOf("Gen-Z & Urban Youth") }
    var selectedPriceTier by remember { mutableStateOf("Mid-Range (৳1,200 - ৳2,200)") }
    var selectedProductionScale by remember { mutableStateOf("Growth (100 - 300 pcs)") }
    var customFocusQuery by remember { mutableStateOf("") }

    var showSavedTrendsDialog by remember { mutableStateOf(false) }
    var isFullReportExpanded by remember { mutableStateOf(false) }
    var isInputFormExpanded by remember { mutableStateOf(currentResult == null) }

    val seasons = listOf(
        "Summer 2026",
        "Festive & Eid 2026",
        "Monsoon 2026",
        "Autumn / Puja 2026",
        "Winter 2026/27",
        "Spring 2027"
    )

    val categories = listOf(
        "Streetwear & Oversized Tees",
        "Panjabi & Ethnic Kurta",
        "Resort & Linen Shirts",
        "Hoodies & Fleece",
        "Cargo & Utility Pants",
        "Polo & Knitted Tops",
        "Modest Wear & Abaya"
    )

    val targetMarkets = listOf(
        "Gen-Z & Urban Youth",
        "Corporate & Office Casual",
        "Premium Boutique Shoppers",
        "Mass Market Online Retail"
    )

    val priceTiers = listOf(
        "Budget Friendly (৳650 - ৳1,000)",
        "Mid-Range (৳1,200 - ৳2,200)",
        "Premium Boutique (৳2,500 - ৳5,500)"
    )

    val productionScales = listOf(
        "Small Batch (30-50 pcs)",
        "Growth (100-300 pcs)",
        "Bulk Sourcing (500+ pcs)"
    )

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label কপি হয়েছে: $text", Toast.LENGTH_SHORT).show()
    }

    fun shareReport(result: SeasonalTrendAnalysisResult) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Fashion Trend Report: ${result.season} - ${result.category}")
            putExtra(
                Intent.EXTRA_TEXT,
                "🌟 ${result.season} - ${result.category} ফ্যাশন ট্রেন্ড বিশ্লেষণ\n\n" +
                        "সংক্ষেপ: ${result.headlineSummary}\n\n" +
                        "🎨 কালার প্যালেট:\n" +
                        result.colorPalette.joinToString("\n") { "• ${it.name} (${it.hexCode}) - ${it.description}" } +
                        "\n\n🧵 প্রস্তাবিত ফ্যাব্রিক:\n" +
                        result.fabrics.joinToString("\n") { "• ${it.fabricName} (${it.gsmWeight}) - সোর্সিং: ${it.sourcingHub}" } +
                        "\n\n💰 উদ্যোক্তা বাজেট ও প্রফিট:\n" +
                        "• আনুমানিক খরচ: ${result.economics.estimatedCogs}\n" +
                        "• প্রস্তাবিত বিক্রয়মূল্য: ${result.economics.recommendedMSRP}\n" +
                        "• গ্রস মার্জিন: ${result.economics.grossMargin}\n\n" +
                        "বিস্তারিত জানতে Apparel Advisor অ্যাপটি ব্যবহার করুন।"
            )
        }
        context.startActivity(Intent.createChooser(shareIntent, "ট্রেন্ড রিপোর্ট শেয়ার করুন"))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Header Banner & Quick Actions ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorderColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFE8F5E9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = BrandGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "মৌসুমি ফ্যাশন ট্রেন্ড",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = NeutralSlate
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFE0F2FE)
                                    ) {
                                        Text(
                                            text = "Gemini 3.8",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF0284C7)
                                        )
                                    }
                                }
                                Text(
                                    text = "উদ্যোক্তাদের জন্য বাণিজ্যিক ফ্যাব্রিক, সিলুয়েট ও কস্ট গাইড",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SubtitleGray
                                )
                            }
                        }

                        // Saved Library Button
                        OutlinedButton(
                            onClick = { showSavedTrendsDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CardBorderColor),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("view_saved_trends_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = "Saved Trends",
                                tint = BrandDarkGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "সংরক্ষিত (${savedTrends.size})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = BrandDarkGreen
                            )
                        }
                    }
                }
            }
        }

        // --- 2. Interactive Input / Parameters Card ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, if (isInputFormExpanded) BrandGreen.copy(alpha = 0.5f) else CardBorderColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isInputFormExpanded = !isInputFormExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Style,
                                contentDescription = null,
                                tint = BrandGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "নতুন ট্রেন্ড বিশ্লেষণ পরামিতি",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = NeutralSlate
                            )
                        }
                        IconButton(
                            onClick = { isInputFormExpanded = !isInputFormExpanded },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isInputFormExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isInputFormExpanded) "সংকোচন করুন" else "প্রসারিত করুন",
                                tint = SubtitleGray
                            )
                        }
                    }

                    if (isInputFormExpanded) {
                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Presets
                        Text(
                            text = "দ্রুত প্রিসেট (Quick Presets):",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = SubtitleGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                OutlinedButton(
                                    onClick = {
                                        selectedSeason = "Festive & Eid 2026"
                                        selectedCategory = "Panjabi & Ethnic Kurta"
                                        selectedTargetMarket = "Premium Boutique Shoppers"
                                        selectedPriceTier = "Premium Boutique (৳2,500 - ৳5,500)"
                                        selectedProductionScale = "Growth (100 - 300 pcs)"
                                        customFocusQuery = "রিসাইকেলড কটন জ্যাকার্ড ও এলিগেন্ট মেটাল বাটন ফিনিশ"
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("preset_eid_panjabi")
                                ) {
                                    Text("✨ ঈদ প্রিমিয়াম পাঞ্জাবি", fontSize = 12.sp, color = NeutralSlate)
                                }
                            }
                            item {
                                OutlinedButton(
                                    onClick = {
                                        selectedSeason = "Summer 2026"
                                        selectedCategory = "Streetwear & Oversized Tees"
                                        selectedTargetMarket = "Gen-Z & Urban Youth"
                                        selectedPriceTier = "Mid-Range (৳1,200 - ৳2,200)"
                                        selectedProductionScale = "Growth (100 - 300 pcs)"
                                        customFocusQuery = "হেভিওয়েট ২৪০ জিএসএম ড্রপ শোল্ডার মিনিমাল পাফ প্রিন্ট"
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("preset_streetwear_tee")
                                ) {
                                    Text("👕 ড্রপ শোল্ডার টি-শার্ট", fontSize = 12.sp, color = NeutralSlate)
                                }
                            }
                            item {
                                OutlinedButton(
                                    onClick = {
                                        selectedSeason = "Winter 2026/27"
                                        selectedCategory = "Hoodies & Fleece"
                                        selectedTargetMarket = "Gen-Z & Urban Youth"
                                        selectedPriceTier = "Mid-Range (৳1,200 - ৳2,200)"
                                        selectedProductionScale = "Growth (100 - 300 pcs)"
                                        customFocusQuery = "৪২০+ জিএসএম ফ্রেঞ্চ টেরি ডাবল লেয়ার হুড"
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("preset_winter_hoodie")
                                ) {
                                    Text("🧥 উইন্টার হেভি হুডি", fontSize = 12.sp, color = NeutralSlate)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Season Selection
                        Text(
                            text = "১. সিজন বা মৌসুম নির্বাচন করুন:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = NeutralSlate
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            seasons.forEach { season ->
                                val isSelected = selectedSeason == season
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedSeason = season },
                                    label = { Text(season, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrandGreen.copy(alpha = 0.15f),
                                        selectedLabelColor = BrandDarkGreen
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = CardBorderColor,
                                        selectedBorderColor = BrandGreen
                                    ),
                                    modifier = Modifier.testTag("season_chip_${season.replace(" ", "_")}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Category Selection
                        Text(
                            text = "২. পোশাকের ক্যাটাগরি:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = NeutralSlate
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            categories.forEach { category ->
                                val isSelected = selectedCategory == category
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCategory = category },
                                    label = { Text(category, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrandGreen.copy(alpha = 0.15f),
                                        selectedLabelColor = BrandDarkGreen
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = CardBorderColor,
                                        selectedBorderColor = BrandGreen
                                    ),
                                    modifier = Modifier.testTag("category_chip_${category.replace(" ", "_")}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Target Market
                        Text(
                            text = "৩. টার্গেট কাস্টমার ও মার্কেট:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = NeutralSlate
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            targetMarkets.forEach { tm ->
                                val isSelected = selectedTargetMarket == tm
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedTargetMarket = tm },
                                    label = { Text(tm, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrandGreen.copy(alpha = 0.15f),
                                        selectedLabelColor = BrandDarkGreen
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = CardBorderColor,
                                        selectedBorderColor = BrandGreen
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Production Scale & Price Tier Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "৪. প্রাইস রেঞ্জ:",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = NeutralSlate
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                priceTiers.forEach { pt ->
                                    val isSelected = selectedPriceTier == pt
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) Color(0xFFE8F5E9) else Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, if (isSelected) BrandGreen else CardBorderColor),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .clickable { selectedPriceTier = pt }
                                    ) {
                                        Text(
                                            text = pt,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) BrandDarkGreen else NeutralSlate,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "৫. প্রোডাকশন স্কেল:",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = NeutralSlate
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                productionScales.forEach { ps ->
                                    val isSelected = selectedProductionScale == ps
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) Color(0xFFE8F5E9) else Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, if (isSelected) BrandGreen else CardBorderColor),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .clickable { selectedProductionScale = ps }
                                    ) {
                                        Text(
                                            text = ps,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) BrandDarkGreen else NeutralSlate,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Custom Query input
                        OutlinedTextField(
                            value = customFocusQuery,
                            onValueChange = { customFocusQuery = it },
                            label = { Text("বিশেষ কোনো ফোকাস বা ডিজাইন আইডিয়া (ঐচ্ছিক)") },
                            placeholder = { Text("যেমন: জাপানিজ মিনিমালিজম, ওভারসাইজড ফিট, পাফ প্রিন্ট...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("trend_custom_focus_input"),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Primary Action Button
                        Button(
                            onClick = {
                                isInputFormExpanded = false
                                val request = SeasonalTrendRequest(
                                    season = selectedSeason,
                                    category = selectedCategory,
                                    targetMarket = selectedTargetMarket,
                                    priceTier = selectedPriceTier,
                                    productionScale = selectedProductionScale,
                                    specialFocus = customFocusQuery.ifBlank { "High breathability, structured drape, commercial appeal" }
                                )
                                viewModel.analyzeSeasonalTrends(request)
                            },
                            enabled = !isAnalyzing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandGreen,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("analyze_trends_button")
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Gemini 3.8 Flash বাজার বিশ্লেষণ করছে...",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AI ট্রেন্ড ও সোর্সিং রিপোর্ট তৈরি করুন",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Error Banner if any ---
        if (analysisError != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFDC2626)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = analysisError ?: "ত্রুটি ঘটেছে",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF991B1B)
                        )
                    }
                }
            }
        }

        // --- Loading State Display ---
        if (isAnalyzing) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorderColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = BrandGreen,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Gemini 3.8 Flash ফ্যাশন ডাটা বিশ্লেষণ করছে",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = NeutralSlate,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "আন্তর্জাতিক সিলুয়েট, দেশীয় ফ্যাব্রিক হাব (নারায়ণগঞ্জ, ইসলামপুর) ও ইউনিট কস্ট হিসাব তৈরি হচ্ছে...",
                            style = MaterialTheme.typography.bodySmall,
                            color = SubtitleGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // --- Active Result Details ---
        val result = currentResult
        if (result != null && !isAnalyzing) {
            // A. Executive Headline & Actions Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFE8F5E9)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = BrandDarkGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${result.season} • ${result.category}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = BrandDarkGreen
                                    )
                                }
                            }

                            Row {
                                // Save Button
                                IconButton(
                                    onClick = {
                                        viewModel.saveCurrentTrend {
                                            Toast.makeText(context, "ট্রেন্ড রিপোর্ট লাইব্রেরিতে সেভ হয়েছে!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.testTag("save_trend_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BookmarkBorder,
                                        contentDescription = "Save Report",
                                        tint = BrandDarkGreen
                                    )
                                }

                                // Share Button
                                IconButton(
                                    onClick = { shareReport(result) },
                                    modifier = Modifier.testTag("share_trend_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share Report",
                                        tint = NeutralSlate
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = result.headlineSummary,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                lineHeight = 22.sp
                            ),
                            color = NeutralSlate
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "টার্গেট কাস্টমার: ${result.targetMarket}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SubtitleGray
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick action: Ask AI Chat
                        OutlinedButton(
                            onClick = { viewModel.sendTrendToChat(result) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BrandGreen),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandDarkGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ask_ai_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "এই কালেকশন নিয়ে AI চ্যাটে মার্কেটিং ও লঞ্চ প্ল্যান তৈরি করুন",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // B. Curated Color Palette
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorderColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ColorLens,
                                contentDescription = null,
                                tint = BrandGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ট্রেন্ডিং কালার প্যালেট (Color Palette)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = NeutralSlate
                            )
                        }
                        Text(
                            text = "হেক্স কোডে ট্যাপ করে কপি করুন এবং ডায়িং ল্যাব বা ফ্যাব্রিক ডিলারের সাথে শেয়ার করুন।",
                            style = MaterialTheme.typography.bodySmall,
                            color = SubtitleGray,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(result.colorPalette) { colorItem ->
                                val parsedColor = remember(colorItem.hexCode) {
                                    parseHexColor(colorItem.hexCode)
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, CardBorderColor),
                                    modifier = Modifier
                                        .width(130.dp)
                                        .clickable {
                                            copyToClipboard("Color Hex", colorItem.hexCode)
                                        }
                                        .testTag("color_chip_${colorItem.name.replace(" ", "_")}")
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(55.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(parsedColor)
                                                .border(1.dp, Color.Black.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        ) {
                                            if (colorItem.isPrimary) {
                                                Surface(
                                                    shape = RoundedCornerShape(bottomEnd = 6.dp),
                                                    color = Color.Black.copy(alpha = 0.7f),
                                                    modifier = Modifier.align(Alignment.TopStart)
                                                ) {
                                                    Text(
                                                        text = "KEY",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = colorItem.name,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = NeutralSlate,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = colorItem.hexCode,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                    fontSize = 11.sp
                                                ),
                                                color = BrandDarkGreen
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy Hex",
                                                tint = SubtitleGray,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = colorItem.description,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                            color = SubtitleGray,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // C. Actionable Fabric Specifications & Sourcing Hubs
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorderColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = BrandGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "প্রস্তাবিত ফ্যাব্রিক স্পেসিফিকেশন ও সোর্সিং হাব",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = NeutralSlate
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        result.fabrics.forEachIndexed { index, fabric ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, CardBorderColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${index + 1}. ${fabric.fabricName}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = NeutralSlate
                                        )

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFE8F5E9)
                                        ) {
                                            Text(
                                                text = fabric.gsmWeight,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = BrandDarkGreen
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "উপাদান (Composition):",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                color = SubtitleGray
                                            )
                                            Text(
                                                text = fabric.composition,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                color = NeutralSlate
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "সুতার কাউন্ট (Yarn Count):",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                color = SubtitleGray
                                            )
                                            Text(
                                                text = fabric.yarnCount,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                color = NeutralSlate
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = Color(0xFFE11D48),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "সোর্সিং হাব: ${fabric.sourcingHub}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = Color(0xFF9F1239)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "ড্রেপ/ফিল: ${fabric.drapeFeel} | সংকোচন: ${fabric.shrinkageExpectation}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = SubtitleGray
                                    )

                                    if (fabric.proTip.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFFEF3C7),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(6.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Lightbulb,
                                                    contentDescription = null,
                                                    tint = Color(0xFFB45309),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = fabric.proTip,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                    color = Color(0xFF92400E)
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

            // D. Silhouettes & Surface Details
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorderColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = BrandGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ট্রেন্ডিং সিলুয়েট ও কাটিং প্যাটার্ন",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = NeutralSlate
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        result.silhouettes.forEach { silhouette ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, CardBorderColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = silhouette.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = NeutralSlate
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = silhouette.cutDetails,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SubtitleGray
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "বাণিজ্যিক কাট ও ফিট: ${silhouette.commercialAppeal}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                                        color = BrandDarkGreen
                                    )
                                }
                            }
                        }

                        if (result.designDetails.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "সারফেস ডিজাইন ও ট্রিমস (Trims & Prints):",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = NeutralSlate
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            result.designDetails.forEach { detail ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("• ", color = BrandGreen, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${detail.feature}: ${detail.recommendation} (${detail.executionNotes})",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = NeutralSlate
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // E. Unit Economics & Pricing Calculator for Entrepreneurs
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = BrandDarkGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "উদ্যোক্তা ইউনিট কস্ট ও প্রফিট মার্জিন",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = BrandDarkGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFDCFCE7)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "প্রতি পিস খরচ (COGS)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = SubtitleGray
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = result.economics.estimatedCogs,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = NeutralSlate
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFDCFCE7)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "প্রস্তাবিত খুচরা মূল্য (MSRP)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = SubtitleGray
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = result.economics.recommendedMSRP,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = BrandDarkGreen
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFDCFCE7)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "টার্গেট গ্রস মার্জিন",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = SubtitleGray
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = result.economics.grossMargin,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF047857)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFDCFCE7)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "ব্রেক-ইভেন ভলিউম",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = SubtitleGray
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = result.economics.breakEvenUnits,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = NeutralSlate
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "💡 লাভজনকতা পরামর্শ: ${result.economics.roiInsight}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF166534)
                        )
                    }
                }
            }

            // F. 3-Phase Go-To-Market Roadmap
            if (result.goToMarket.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, CardBorderColor),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = BrandGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "গো-টু-মার্কেট টাইমলাইন (৩ ধাপের লঞ্চ প্ল্যান)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = NeutralSlate
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            result.goToMarket.forEachIndexed { idx, step ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(BrandGreen.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${idx + 1}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = BrandDarkGreen
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = step.phase,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = NeutralSlate
                                            )
                                            Text(
                                                text = step.timeframe,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = BrandDarkGreen
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = step.actionItems.joinToString(" • "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SubtitleGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // G. Full In-Depth Markdown Report (Collapsible)
            if (result.fullReportMarkdown.isNotBlank()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, CardBorderColor),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isFullReportExpanded = !isFullReportExpanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "সম্পূর্ণ ফ্যাশন ও ম্যানুফ্যাকচারিং রিপোর্ট",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = NeutralSlate
                                )
                                IconButton(
                                    onClick = { isFullReportExpanded = !isFullReportExpanded },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFullReportExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = SubtitleGray
                                    )
                                }
                            }

                            if (isFullReportExpanded) {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = result.fullReportMarkdown,
                                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                    color = NeutralSlate
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Saved Trends Dialog ---
    if (showSavedTrendsDialog) {
        AlertDialog(
            onDismissRequest = { showSavedTrendsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = BrandDarkGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("সংরক্ষিত ট্রেন্ড লাইব্রেরি")
                }
            },
            text = {
                if (savedTrends.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            tint = SubtitleGray,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "কোনো সংরক্ষিত রিপোর্ট নেই",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SubtitleGray
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(savedTrends) { item ->
                            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(item.timestamp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, CardBorderColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.loadSavedTrendIntoView(item)
                                        showSavedTrendsDialog = false
                                        Toast.makeText(context, "রিপোর্ট লোড করা হয়েছে", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${item.season} • ${item.category}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = NeutralSlate
                                        )
                                        Text(
                                            text = item.headlineSummary,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SubtitleGray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "তারিখ: $dateStr",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = SubtitleGray
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteSavedTrend(item.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSavedTrendsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                ) {
                    Text("বন্ধ করুন")
                }
            }
        )
    }
}

/**
 * Safely parses hex color strings (e.g. "#7D9D8B" or "7D9D8B") into Compose Color.
 */
private fun parseHexColor(hexString: String): Color {
    val clean = hexString.replace("#", "").trim()
    return try {
        when (clean.length) {
            6 -> {
                val colorInt = clean.toLong(16)
                Color(0xFF000000 or colorInt)
            }
            8 -> {
                val colorInt = clean.toLong(16)
                Color(colorInt)
            }
            else -> Color(0xFF7D9D8B)
        }
    } catch (_: Exception) {
        Color(0xFF7D9D8B)
    }
}

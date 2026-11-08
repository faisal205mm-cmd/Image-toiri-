package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request parameters for analyzing seasonal fashion trends and fabric specifications.
 */
data class SeasonalTrendRequest(
    val season: String = "Summer 2026",
    val category: String = "Graphic Tees & Streetwear",
    val targetMarket: String = "Bangladesh Urban DTC",
    val priceTier: String = "Mid-Premium DTC (৳1,200 - ৳2,200)",
    val productionScale: String = "Micro-Batch Drop (150-300 pcs)",
    val specialFocus: String = "High breathability, structured drape, viral aesthetic"
)

data class TrendColor(
    val name: String,
    val hexCode: String,
    val description: String,
    val isPrimary: Boolean = true
)

data class SilhouetteRecommendation(
    val title: String,
    val cutDetails: String,
    val commercialAppeal: String
)

data class FabricRecommendation(
    val fabricName: String,
    val gsmWeight: String,
    val yarnCount: String,
    val composition: String,
    val drapeFeel: String,
    val shrinkageExpectation: String,
    val sourcingHub: String,
    val proTip: String
)

data class DesignDetail(
    val feature: String,
    val recommendation: String,
    val executionNotes: String
)

data class EntrepreneurEconomics(
    val estimatedCogs: String,
    val recommendedMSRP: String,
    val grossMargin: String,
    val breakEvenUnits: String,
    val roiInsight: String
)

data class GoToMarketStep(
    val phase: String,
    val timeframe: String,
    val actionItems: List<String>
)

data class SeasonalTrendAnalysisResult(
    val headlineSummary: String,
    val season: String,
    val category: String,
    val targetMarket: String,
    val colorPalette: List<TrendColor>,
    val silhouettes: List<SilhouetteRecommendation>,
    val fabrics: List<FabricRecommendation>,
    val designDetails: List<DesignDetail>,
    val economics: EntrepreneurEconomics,
    val goToMarket: List<GoToMarketStep>,
    val fullReportMarkdown: String,
    val timestamp: Long = System.currentTimeMillis()
)

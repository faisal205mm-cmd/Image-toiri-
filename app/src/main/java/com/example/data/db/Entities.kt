package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val persona: String = "GENERAL",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val messageCount: Int = 0,
    val lastSnippet: String = ""
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long = 0,
    val role: String, // "user" or "model"
    val message: String,
    val persona: String = "GENERAL",
    val imageUrl: String? = null, // Local file path, URI, or base64 data for image
    val videoUrl: String? = null, // Local file path or URI for video
    val mediaType: String? = null, // "IMAGE", "VIDEO", null
    val mediaFileName: String? = null,
    val isImagePrompt: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val season: String, // e.g. "SS26", "FW26", "Capsule Drop"
    val targetLaunchDate: String,
    val pieceCount: Int,
    val budget: Double,
    val projectedRevenue: Double,
    val status: String, // "PLANNING", "SAMPLING", "PRODUCTION", "LAUNCHED"
    val notes: String,
    val aiStrategyNotes: String = "",
    val imageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "pricing_skus")
data class PricingSkuEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemName: String,
    val category: String, // "Tees & Tops", "Hoodies & Fleece", "Pants & Denim", "Dresses & Outerwear", "Accessories"
    val fabricCost: Double,
    val laborCost: Double,
    val trimsPackagingCost: Double,
    val shippingDutiesCost: Double,
    val overheadBuffer: Double,
    val retailPrice: Double,
    val wholesalePrice: Double,
    val profitPerUnit: Double,
    val marginPercentage: Double,
    val imageUrl: String? = null,
    val aiCritique: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "brand_profile")
data class BrandProfileEntity(
    @PrimaryKey val id: Int = 1, // Single profile row
    val brandName: String = "My Apparel Co.",
    val niche: String = "Streetwear & Lifestyle", // e.g., "Streetwear", "Sustainable Basics", "Luxury Athleisure", "Vintage Denim", "Boutique Dresses"
    val businessModel: String = "DTC (Shopify) & Pop-ups", // "DTC E-commerce", "Wholesale & Retailers", "Custom / Made-to-Order", "Hybrid"
    val currentStage: String = "Emerging ($10k-$50k/mo)", // "Idea / Pre-launch", "Early (<$10k/mo)", "Emerging ($10k-$50k/mo)", "Scaling ($50k+/mo)"
    val monthlyRevenue: Double = 15000.0,
    val primaryGoal: String = "Increase repeat purchase rate & launch winter drop",
    val primaryPainPoint: String = "Factory MOQs and pricing margins calculation",
    val lastDiagnosticReport: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "sales_records")
data class SaleRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val customerName: String = "",
    val customerPhone: String = "",
    val itemsSummary: String, // e.g. "Cotton Sharee (Free Size) x 1, Panjabi (L) x 2"
    val itemsCount: Int = 1,
    val subtotal: Double,
    val discount: Double = 0.0,
    val totalAmount: Double,
    val paidAmount: Double,
    val dueAmount: Double = 0.0,
    val paymentMethod: String = "CASH", // "CASH", "BKASH", "NAGAD", "CARD", "DUE"
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "monthly_finances")
data class MonthlyFinanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "SALE", "EXPENSE", "DEBT_PAY", "PRODUCT_BUY"
    val title: String, // e.g. "মাসিক বিক্রির আয়", "দোকান ভাড়া", "সাপ্লায়ার দেনা পরিশোধ", "পাইকারি থান কাপড় ক্রয়"
    val amount: Double,
    val category: String = "", // e.g. "Retail", "Rent", "EMI", "Raw Materials"
    val monthYear: String = "", // e.g. "September 2026"
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)


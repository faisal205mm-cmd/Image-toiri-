package com.example.data.image

import java.util.Locale

/**
 * Image Generation Mode
 */
enum class ImageMode {
    PRODUCT,         // E-commerce, catalog, isolated items
    FASHION_MODEL,   // Human apparel, clothing on model, editorial
    LIFESTYLE,       // Real-world contextual scenes, ambient
    PORTRAIT,        // Human portrait, face/upper body
    LANDSCAPE,       // Scenery, wide panoramic vistas
    ARTISTIC         // Stylized illustrations, digital art
}

/**
 * Detected Input Language
 */
enum class DetectedLanguage {
    BENGALI,
    BANGLISH,
    ENGLISH,
    MIXED
}

/**
 * Structured Image Prompt representation capturing all visual dimensions
 */
data class StructuredImagePrompt(
    val rawPrompt: String,
    val language: DetectedLanguage,
    val mode: ImageMode,
    val subject: String,
    val product: String?,
    val poseComposition: String,
    val background: String,
    val lighting: String,
    val camera: String,
    val style: String,
    val materialTexture: String,
    val color: String?,
    val detailLevel: String,
    val aspectRatio: String, // "1:1", "16:9", "9:16", "4:3", "3:4"
    val orientationName: String,
    val textInImage: String?,
    val negativePrompt: String,
    val optimizedPrompt: String,
    val bengaliAnalysis: String
)

/**
 * Preset Test Prompt Item for verification & UI testing
 */
data class ImageTestPromptItem(
    val id: String,
    val category: String,
    val prompt: String,
    val description: String,
    val expectedMode: ImageMode,
    val expectedAspectRatio: String
)

/**
 * Intelligent Multi-lingual Image Prompt Understanding & Enhancement Engine
 */
object ImagePromptEngine {

    // Negative prompt presets to guarantee high quality and prevent common generation artifacts
    const val DEFAULT_NEGATIVE_PROMPT =
        "blurry, distorted, duplicate objects, malformed object, unwanted background objects, cropped product, cut off edges, extra limbs, bad anatomy, deformed fingers, unrealistic proportions, low quality, pixelated, watermark, artifact, noisy, oversaturated, amateur photography"

    const val PRODUCT_NEGATIVE_PROMPT =
        "blurry, distorted, duplicate objects, malformed object, unwanted background objects, cropped product, cut off edges, missing parts, human hands, dirty background, cluttered scene, low resolution, bad lighting, watermark, signature, artifacts"

    const val FASHION_NEGATIVE_PROMPT =
        "extra limbs, deformed hands, missing fingers, bad anatomy, distorted face, unrealistic body proportions, cropped head, cropped feet, blurry fabric, low resolution, amateur shot, poorly rendered clothing, oversaturated, artifacts"

    /**
     * Complete test suite containing at least 15 comprehensive prompts covering all required modes
     */
    val TEST_PROMPT_SUITE: List<ImageTestPromptItem> = listOf(
        ImageTestPromptItem(
            id = "test_1",
            category = "বাংলা Product Prompt",
            prompt = "সাদা background এ একটা কালো cargo pant দেখাও",
            description = "সাদা ব্যাকগ্রাউন্ডে কালো কার্গো প্যান্টের প্রোডাক্ট ফটোগ্রাফি",
            expectedMode = ImageMode.PRODUCT,
            expectedAspectRatio = "1:1"
        ),
        ImageTestPromptItem(
            id = "test_2",
            category = "Banglish Prompt",
            prompt = "sada background e ekta black leather shoe dekhao",
            description = "Banglish প্রম্পটে কালো চামড়ার জুতো রেন্ডারিং",
            expectedMode = ImageMode.PRODUCT,
            expectedAspectRatio = "1:1"
        ),
        ImageTestPromptItem(
            id = "test_3",
            category = "English Prompt",
            prompt = "A minimalist stainless steel matte black thermos water bottle on white background, studio shot",
            description = "English commercial catalog product shot",
            expectedMode = ImageMode.PRODUCT,
            expectedAspectRatio = "1:1"
        ),
        ImageTestPromptItem(
            id = "test_4",
            category = "Mixed Language Prompt",
            prompt = "Ekta white sneakers er cinematic product photo, high detail, studio lighting",
            description = "বাংলা-ইংলিশ মিশ্রিত প্রম্পট স্নিকার্স ফটোগ্রাফি",
            expectedMode = ImageMode.PRODUCT,
            expectedAspectRatio = "1:1"
        ),
        ImageTestPromptItem(
            id = "test_5",
            category = "Clothing Product",
            prompt = "Royal blue denim jacket, front view, uncropped, authentic stitching and brass buttons, clean studio backdrop",
            description = "পোশাকের আকৃতি ও ডিটেইলস অবিকল রেখে প্রোডাক্ট ছবি",
            expectedMode = ImageMode.PRODUCT,
            expectedAspectRatio = "1:1"
        ),
        ImageTestPromptItem(
            id = "test_6",
            category = "Model Fashion Photo",
            prompt = "লাল রঙের জামদানি শাড়ি পরা বাংলাদেশি নারী মডেল, ফুল বডি ফ্যাশন ফটোগ্রাফি",
            description = "মডেলের পরিহিত শাড়ির ডিজাইন ও ফুল বডি কম্পোজিশন",
            expectedMode = ImageMode.FASHION_MODEL,
            expectedAspectRatio = "9:16"
        ),
        ImageTestPromptItem(
            id = "test_7",
            category = "White Background Product Photo",
            prompt = "স্মার্ট ওয়াচ, পিওর সাদা ব্যাকগ্রাউন্ডে, ডিসপ্লে অন, প্রিমিয়াম লুক",
            description = "পিওর সাদা ব্যাকগ্রাউন্ড ও নিখুঁত আলোতে স্মার্টওয়াচ",
            expectedMode = ImageMode.PRODUCT,
            expectedAspectRatio = "1:1"
        ),
        ImageTestPromptItem(
            id = "test_8",
            category = "Lifestyle Photo",
            prompt = "একটি আধুনিক কফি মগ ও ল্যাপটপ কাঠের ক্যাফে টেবিলে, মর্নিং সানলাইট",
            description = "বাস্তবধর্মী লাইফস্টাইল দৃশ্য ও প্রাকৃতিক আলো",
            expectedMode = ImageMode.LIFESTYLE,
            expectedAspectRatio = "16:9"
        ),
        ImageTestPromptItem(
            id = "test_9",
            category = "Portrait",
            prompt = "কালো ব্লেজার পরা একজন প্রফেশনাল পুরুষ উদ্যোক্তার কর্পোরেট পোট্রেট ছবি",
            description = "আই-লেভেল পোর্ট্রেট ও প্রফেশনাল কর্পোরেট লাইটিং",
            expectedMode = ImageMode.PORTRAIT,
            expectedAspectRatio = "3:4"
        ),
        ImageTestPromptItem(
            id = "test_10",
            category = "Landscape",
            prompt = "সবুজ চা বাগান ও কুয়াশাচ্ছন্ন পাহাড়ের মনোরম ল্যান্ডস্কেপ দৃশ্য",
            description = "ওয়াইড-অ্যাঙ্গেল প্রাকৃতিক প্যানোরামা",
            expectedMode = ImageMode.LANDSCAPE,
            expectedAspectRatio = "16:9"
        ),
        ImageTestPromptItem(
            id = "test_11",
            category = "Square",
            prompt = "একটি গোল্ডেন মেটালিক লাক্সারি পারফিউম বোতল, চারকোনা স্কয়ার ফরম্যাট, সফট শ্যাডো",
            description = "চারকোনা ১:১ অনুপাতের লাক্সারি পারফিউম ছবি",
            expectedMode = ImageMode.PRODUCT,
            expectedAspectRatio = "1:1"
        ),
        ImageTestPromptItem(
            id = "test_12",
            category = "Text-in-Image",
            prompt = "A black cotton hoodie with crisp white printed text 'AI STUDIO 2026' on chest",
            description = "টি-শার্ট বা হুডিতে নির্দিষ্ট টেক্সট সঠিকভাবে প্রিন্ট",
            expectedMode = ImageMode.PRODUCT,
            expectedAspectRatio = "1:1"
        ),
        ImageTestPromptItem(
            id = "test_13",
            category = "Multi-language Clothing Prompt",
            prompt = "কালো রঙের hoodie পরা smart boy, urban street style aesthetic",
            description = "ফ্যাশন মডেল ও স্ট্রিট স্টাইল লাইফস্টাইল",
            expectedMode = ImageMode.FASHION_MODEL,
            expectedAspectRatio = "9:16"
        ),
        ImageTestPromptItem(
            id = "test_14",
            category = "Banglish Tech Product",
            prompt = "ekta sleek silver wireless headphone wooden table er upor cinematic angle e",
            description = "টেক গ্যাজেট প্রোডাক্ট শুট ও কাঠের ব্যাকগ্রাউন্ড",
            expectedMode = ImageMode.PRODUCT,
            expectedAspectRatio = "1:1"
        ),
        ImageTestPromptItem(
            id = "test_15",
            category = "Artistic / Studio Styled",
            prompt = "Vintage mechanical wrist watch with visible golden gears and dials, macro luxury catalog photography",
            description = "ম্যাক্রো ডিটেইলস ও মেকানিক্যাল ইন্টারনালস ফটোগ্রাফি",
            expectedMode = ImageMode.PRODUCT,
            expectedAspectRatio = "1:1"
        )
    )

    /**
     * Primary Pipeline: Parses user prompt and produces a structured, optimized visual prompt
     */
    fun analyzeAndOptimize(
        userPrompt: String,
        overrideAspectRatio: String? = null
    ): StructuredImagePrompt {
        val cleanInput = cleanPrefixes(userPrompt)
        val language = detectLanguage(cleanInput)
        val textInImage = extractTextInImage(cleanInput)
        val detectedColors = extractColors(cleanInput)
        val detectedProduct = extractProduct(cleanInput)
        val isHumanModel = detectHumanModel(cleanInput)
        val isLandscape = detectLandscape(cleanInput)
        val isPortrait = detectPortrait(cleanInput)
        val isArtistic = detectArtistic(cleanInput)

        // Determine Mode
        val mode = when {
            isArtistic -> ImageMode.ARTISTIC
            isHumanModel && (detectedProduct != null || cleanInput.contains("fashion", ignoreCase = true) || cleanInput.contains("শাড়ি") || cleanInput.contains("dress", ignoreCase = true)) -> ImageMode.FASHION_MODEL
            isHumanModel -> ImageMode.PORTRAIT
            isLandscape -> ImageMode.LANDSCAPE
            cleanInput.contains("cafe", ignoreCase = true) || cleanInput.contains("ক্যাফে") || cleanInput.contains("lifestyle", ignoreCase = true) || cleanInput.contains("লাইফস্টাইল") || (cleanInput.contains("মগ") && cleanInput.contains("টেবিল")) -> ImageMode.LIFESTYLE
            detectedProduct != null -> ImageMode.PRODUCT
            else -> ImageMode.PRODUCT
        }

        // Determine Aspect Ratio & Orientation
        val (aspectRatio, orientationName) = determineAspectRatio(cleanInput, mode, overrideAspectRatio)

        // Determine Background
        val background = determineBackground(cleanInput, mode)

        // Determine Lighting
        val lighting = determineLighting(cleanInput, mode)

        // Determine Composition & Camera
        val (poseComposition, camera) = determineCompositionAndCamera(cleanInput, mode, isHumanModel)

        // Determine Material & Texture
        val materialTexture = determineMaterialTexture(cleanInput, detectedProduct)

        // Determine Style & Detail
        val (style, detailLevel) = determineStyleAndDetail(mode)

        // Synthesize Subject description
        val primaryColor = detectedColors.firstOrNull() ?: ""
        val subject = buildSubjectDescription(cleanInput, detectedProduct, primaryColor, isHumanModel, mode)

        // Build Negative Prompt
        val negativePrompt = when (mode) {
            ImageMode.FASHION_MODEL -> FASHION_NEGATIVE_PROMPT
            ImageMode.PRODUCT -> PRODUCT_NEGATIVE_PROMPT
            else -> DEFAULT_NEGATIVE_PROMPT
        }

        // Synthesize the Final Optimized Visual Prompt
        val optimizedPrompt = buildOptimizedPrompt(
            mode = mode,
            subject = subject,
            poseComposition = poseComposition,
            background = background,
            lighting = lighting,
            camera = camera,
            style = style,
            materialTexture = materialTexture,
            color = primaryColor,
            detailLevel = detailLevel,
            textInImage = textInImage,
            aspectRatio = aspectRatio,
            negativePrompt = negativePrompt
        )

        // Generate Bengali Understanding Breakdown for user transparency
        val bengaliAnalysis = buildBengaliAnalysis(
            mode = mode,
            subject = subject,
            background = background,
            lighting = lighting,
            composition = poseComposition,
            textInImage = textInImage,
            aspectRatio = aspectRatio
        )

        return StructuredImagePrompt(
            rawPrompt = cleanInput,
            language = language,
            mode = mode,
            subject = subject,
            product = detectedProduct,
            poseComposition = poseComposition,
            background = background,
            lighting = lighting,
            camera = camera,
            style = style,
            materialTexture = materialTexture,
            color = primaryColor.ifBlank { null },
            detailLevel = detailLevel,
            aspectRatio = aspectRatio,
            orientationName = orientationName,
            textInImage = textInImage,
            negativePrompt = negativePrompt,
            optimizedPrompt = optimizedPrompt,
            bengaliAnalysis = bengaliAnalysis
        )
    }

    private fun cleanPrefixes(prompt: String): String {
        var p = prompt.trim()
        val prefixes = listOf(
            "/image", "/img", "/draw", "/generate_image",
            "generate image of", "create an image of", "generate an image of",
            "create image of", "generate photo of", "create photo of",
            "ছবি তৈরি করো", "ছবি বানাও", "ছবি দেখাও", "ইমেজ তৈরি করো",
            "ekta chobi toiri koro", "ekta image dao", "chobi banao"
        )
        for (prefix in prefixes) {
            if (p.startsWith(prefix, ignoreCase = true)) {
                p = p.substring(prefix.length).trim().removePrefix(":").removePrefix("-").trim()
            }
        }
        return if (p.isNotBlank()) p else prompt.trim()
    }

    private fun detectLanguage(text: String): DetectedLanguage {
        val hasBengaliChar = text.any { it in '\u0980'..'\u09FF' }
        val lower = text.lowercase(Locale.ROOT)
        val banglishTokens = listOf("ekta", "kalo", "sada", "lal", "neel", "nil", "shobuj", "er", "porer", "pora", "chobi", "dekhao", "banao", "toiri", "ronger", "lagbe", "pant", "shotti")
        val hasBanglish = banglishTokens.any { lower.contains(it) }

        return when {
            hasBengaliChar && hasBanglish -> DetectedLanguage.MIXED
            hasBengaliChar -> DetectedLanguage.BENGALI
            hasBanglish -> DetectedLanguage.BANGLISH
            else -> DetectedLanguage.ENGLISH
        }
    }

    private fun extractTextInImage(text: String): String? {
        // Look for quoted text: 'TEXT' or "TEXT"
        val singleQuoteRegex = Regex("['\"]([^'\"]{1,50})['\"]")
        val quoteMatch = singleQuoteRegex.find(text)
        if (quoteMatch != null) {
            return quoteMatch.groupValues[1].trim()
        }
        // Look for "লেখা থাকবে XYZ" or "text XYZ"
        val patternBengali = Regex("(?:লেখা|টেক্সট|লেখা থাকবে)\\s+[:]?\\s*([a-zA-Z0-9\\s]{2,30})")
        val matchBengali = patternBengali.find(text)
        if (matchBengali != null) {
            return matchBengali.groupValues[1].trim()
        }
        val patternEnglish = Regex("(?:text|words|written)\\s+[:]?\\s*([a-zA-Z0-9\\s]{2,30})", RegexOption.IGNORE_CASE)
        val matchEnglish = patternEnglish.find(text)
        if (matchEnglish != null) {
            return matchEnglish.groupValues[1].trim()
        }
        return null
    }

    private fun extractColors(text: String): List<String> {
        val lower = text.lowercase(Locale.ROOT)
        val colorMap = linkedMapOf(
            "কালো" to "black", "black" to "black", "kalo" to "black",
            "সাদা" to "white", "white" to "white", "sada" to "white",
            "লাল" to "red", "red" to "red", "lal" to "red",
            "নীল" to "blue", "blue" to "blue", "nil" to "blue", "neel" to "blue",
            "সবুজ" to "green", "green" to "green", "shobuj" to "green", "sobuj" to "green",
            "হলুদ" to "yellow", "yellow" to "yellow", "holud" to "yellow",
            "গোলাপি" to "pink", "pink" to "pink", "golapi" to "pink",
            "বেগুনি" to "purple", "purple" to "purple", "beguni" to "purple",
            "ধূসর" to "grey", "grey" to "grey", "gray" to "grey", "dhusor" to "grey",
            "সোনালী" to "gold", "গোল্ডেন" to "gold", "golden" to "gold", "sonali" to "gold",
            "রূপালী" to "silver", "সিলভার" to "silver", "silver" to "silver", "rupali" to "silver",
            "বাদামী" to "brown", "brown" to "brown", "badami" to "brown",
            "কমলা" to "orange", "orange" to "orange", "komla" to "orange",
            "খয়েরি" to "maroon", "maroon" to "maroon", "khoyeri" to "maroon",
            "আসমানী" to "sky blue", "sky blue" to "sky blue",
            "emerald" to "emerald green", "navy" to "navy blue", "beige" to "beige"
        )
        val found = mutableListOf<String>()
        for ((key, value) in colorMap) {
            if (lower.contains(key) && !found.contains(value)) {
                found.add(value)
            }
        }
        return found
    }

    private fun extractProduct(text: String): String? {
        val lower = text.lowercase(Locale.ROOT)
        val products = listOf(
            "cargo pant" to "cargo pants",
            "cargo pants" to "cargo pants",
            "কার্গো প্যান্ট" to "cargo pants",
            "প্যান্ট" to "pants",
            "pant" to "pants",
            "pants" to "pants",
            "জিন্স" to "denim jeans",
            "jeans" to "denim jeans",
            "t-shirt" to "t-shirt",
            "tshirt" to "t-shirt",
            "টি-শার্ট" to "t-shirt",
            "shirt" to "shirt",
            "শার্ট" to "shirt",
            "hoodie" to "hoodie",
            "হুডি" to "hoodie",
            "jacket" to "jacket",
            "জ্যাকেট" to "jacket",
            "denim jacket" to "denim jacket",
            "ব্লেজার" to "tailored blazer",
            "blazer" to "tailored blazer",
            "শাড়ি" to "traditional saree",
            "saree" to "traditional saree",
            "sharee" to "traditional saree",
            "jamdani" to "Jamdani saree",
            "জামদানি" to "Jamdani saree",
            "panjabi" to "Panjabi kurta",
            "পাঞ্জাবি" to "Panjabi kurta",
            "dress" to "designer dress",
            "জুতো" to "shoes",
            "shoe" to "shoes",
            "shoes" to "shoes",
            "leather shoe" to "leather shoes",
            "sneakers" to "sneakers",
            "স্নিকার্স" to "sneakers",
            "ঘড়ি" to "wrist watch",
            "watch" to "wrist watch",
            "smart watch" to "smart watch",
            "স্মার্ট ওয়াচ" to "smart watch",
            "headphone" to "wireless headphones",
            "হেডফোন" to "wireless headphones",
            "water bottle" to "stainless steel water bottle",
            "বোতল" to "bottle",
            "thermos" to "thermos flask",
            "মগ" to "coffee mug",
            "mug" to "coffee mug",
            "পারফিউম" to "luxury perfume bottle",
            "perfume" to "luxury perfume bottle",
            "ব্যাগ" to "bag",
            "backpack" to "backpack",
            "সানগ্লাস" to "sunglasses",
            "sunglasses" to "sunglasses"
        )
        val sortedProducts = products.sortedByDescending { it.first.length }
        for ((key, value) in sortedProducts) {
            if (lower.contains(key)) {
                return value
            }
        }
        return null
    }

    private fun detectHumanModel(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        val humanKeywords = listOf(
            "model", "মডেল", "নারী", "পুরুষ", "ছেলে", "মেয়ে", "মেয়ে",
            "man", "woman", "person", "guy", "girl", "boy", "lady",
            "পরা", "পরে আছে", "পড়া", "wearing", "wears", "dressed in",
            "বডি", "full body", "ফুল বডি", "portrait", "পোট্রেট", "face"
        )
        return humanKeywords.any { lower.contains(it) }
    }

    private fun detectLandscape(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        val landscapeKeywords = listOf(
            "landscape", "ল্যান্ডস্কেপ", "পাহাড়", "চা বাগান", "সমুদ্র",
            "mountain", "scenery", "nature", "garden", "sunset", "vista",
            "ওয়াইড", "wide view", "panoramic", "আড়াআড়ি"
        )
        return landscapeKeywords.any { lower.contains(it) }
    }

    private fun detectPortrait(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        val portraitKeywords = listOf(
            "portrait", "পোট্রেট", "close up", "হাতে", "face", "মুখ", "headshot"
        )
        return portraitKeywords.any { lower.contains(it) }
    }

    private fun detectArtistic(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        val artisticKeywords = listOf(
            "drawing", "illustration", "anime", "sketch", "আঁকা", "কার্টুন", "পেইন্টিং", "painting", "vector"
        )
        return artisticKeywords.any { lower.contains(it) }
    }

    private fun determineAspectRatio(
        text: String,
        mode: ImageMode,
        overrideAspectRatio: String?
    ): Pair<String, String> {
        if (!overrideAspectRatio.isNullOrBlank()) {
            val name = when (overrideAspectRatio) {
                "16:9" -> "Landscape (16:9)"
                "9:16" -> "Portrait / Story (9:16)"
                "4:3" -> "Standard Landscape (4:3)"
                "3:4" -> "Fashion Portrait (3:4)"
                else -> "Square (1:1)"
            }
            return Pair(overrideAspectRatio, name)
        }

        val lower = text.lowercase(Locale.ROOT)
        return when {
            lower.contains("9:16") || lower.contains("story") || lower.contains("স্টোরি") || lower.contains("রিল") || lower.contains("reel") || lower.contains("ফুল বডি") || lower.contains("full body") -> {
                Pair("9:16", "Full-Body Vertical / Story (9:16)")
            }
            lower.contains("16:9") || lower.contains("landscape") || lower.contains("ল্যান্ডস্কেপ") || lower.contains("ওয়াইড") || lower.contains("wide") || lower.contains("banner") -> {
                Pair("16:9", "Wide Landscape (16:9)")
            }
            lower.contains("3:4") || lower.contains("portrait") || lower.contains("পোট্রেট") -> {
                Pair("3:4", "Portrait (3:4)")
            }
            lower.contains("4:3") -> {
                Pair("4:3", "Classic Landscape (4:3)")
            }
            lower.contains("1:1") || lower.contains("square") || lower.contains("চারকোনা") || lower.contains("স্কয়ার") -> {
                Pair("1:1", "Square (1:1)")
            }
            mode == ImageMode.FASHION_MODEL -> {
                Pair("9:16", "Fashion Vertical (9:16)")
            }
            mode == ImageMode.LANDSCAPE || mode == ImageMode.LIFESTYLE -> {
                Pair("16:9", "Atmospheric Landscape (16:9)")
            }
            mode == ImageMode.PORTRAIT -> {
                Pair("3:4", "Editorial Portrait (3:4)")
            }
            else -> {
                Pair("1:1", "E-Commerce Square (1:1)")
            }
        }
    }

    private fun determineBackground(text: String, mode: ImageMode): String {
        val lower = text.lowercase(Locale.ROOT)
        return when {
            lower.contains("সাদা") || lower.contains("white") || lower.contains("sada") || lower.contains("clean background") || lower.contains("pure white") -> {
                "seamless pure white studio background with soft natural contact drop-shadow, distraction-free commercial catalog backdrop"
            }
            lower.contains("কাঠ") || lower.contains("wooden table") || lower.contains("wood") -> {
                "authentic warm polished rustic wooden table surface with shallow organic depth of field"
            }
            lower.contains("ক্যাফে") || lower.contains("cafe") -> {
                "modern minimalist cafe interior with blurred warm ambient bokeh background"
            }
            lower.contains("পাহাড়") || lower.contains("চা বাগান") || lower.contains("nature") -> {
                "lush green tea gardens stretching across misty undulating mountain slopes under soft morning atmosphere"
            }
            lower.contains("street") || lower.contains("urban") -> {
                "stylish contemporary urban street with blurred architectural backdrop"
            }
            mode == ImageMode.PRODUCT -> {
                "clean professional neutral studio cyclorama background with subtle balanced contact shadow"
            }
            mode == ImageMode.FASHION_MODEL -> {
                "minimalist high-fashion editorial studio setting with subtle warm neutral gradient backdrop"
            }
            else -> {
                "tastefully balanced professional backdrop complementing the subject"
            }
        }
    }

    private fun determineLighting(text: String, mode: ImageMode): String {
        val lower = text.lowercase(Locale.ROOT)
        return when {
            lower.contains("morning") || lower.contains("সূর্য") || lower.contains("sunlight") || lower.contains("মর্নিং") || lower.contains("সানলাইট") -> {
                "warm cinematic golden morning sunlight casting soft natural rays and gentle ambient glow"
            }
            lower.contains("cinematic") || lower.contains("সিনেমাটিক") -> {
                "dramatic cinematic lighting with balanced key light, subtle colored rim light, and high dynamic range"
            }
            mode == ImageMode.PRODUCT -> {
                "commercial product photography lighting, dual diffused softboxes, gentle rim highlight, even neutral illumination without glare"
            }
            mode == ImageMode.FASHION_MODEL || mode == ImageMode.PORTRAIT -> {
                "high-end fashion studio beauty lighting, large octabox key light, soft fill, accentuating natural contours and garment drape"
            }
            else -> {
                "balanced natural diffused lighting with crisp, lifelike clarity"
            }
        }
    }

    private fun determineCompositionAndCamera(
        text: String,
        mode: ImageMode,
        isHuman: Boolean
    ): Pair<String, String> {
        val lower = text.lowercase(Locale.ROOT)
        val isFullBody = lower.contains("full body") || lower.contains("ফুল বডি") || lower.contains("মাথা থেকে পা")
        val isMacro = lower.contains("macro") || lower.contains("ম্যাক্রো") || lower.contains("close up") || lower.contains("ডিটেইল")

        val composition = when {
            mode == ImageMode.PRODUCT -> {
                "perfectly centered hero composition, complete product 100% visible from edge to edge with generous negative breathing space, uncropped silhouette, symmetrical catalog presentation"
            }
            mode == ImageMode.FASHION_MODEL && isFullBody -> {
                "full-length head-to-toe fashion shot, model standing gracefully showing entire outfit uncropped, natural high-fashion pose, balanced vertical framing"
            }
            mode == ImageMode.FASHION_MODEL -> {
                "three-quarter fashion editorial framing, highlighting garment fit, drape and silhouette, elegant natural poise"
            }
            mode == ImageMode.PORTRAIT -> {
                "intimate eye-level chest-up portrait framing, confident natural expression, rule-of-thirds composition"
            }
            mode == ImageMode.LANDSCAPE -> {
                "expansive panoramic wide-angle composition with leading lines and layered atmospheric depth"
            }
            else -> {
                "centered eye-level hero shot with crisp visual focus"
            }
        }

        val camera = when {
            isMacro -> "Hasselblad H6D-100c, 120mm macro lens, f/8, ultra-sharp focus on fine micro-textures"
            mode == ImageMode.PRODUCT -> "Sony A7R V, 90mm f/2.8 Macro lens, tack-sharp focal plane, razor-clean edges"
            mode == ImageMode.FASHION_MODEL || mode == ImageMode.PORTRAIT -> "Canon EOS R5, 85mm f/1.4 L lens, creamy shallow depth of field, natural facial rendering"
            mode == ImageMode.LANDSCAPE -> "Nikon Z9, 24-70mm f/2.8 lens, f/11 hyperfocal distance, corner-to-corner sharpness"
            else -> "Professional full-frame camera with prime commercial lens"
        }

        return Pair(composition, camera)
    }

    private fun determineMaterialTexture(text: String, product: String?): String {
        val lower = text.lowercase(Locale.ROOT)
        return when {
            lower.contains("leather") || lower.contains("চামড়া") -> {
                "authentic premium full-grain leather texture with tactile grain, realistic sheen, and reinforced tonal stitching"
            }
            lower.contains("denim") || lower.contains("জিন্স") -> {
                "heavyweight diagonal twill denim weave, authentic indigo wash fading, copper rivets, and heavy contrast stitching"
            }
            lower.contains("cargo") || lower.contains("কার্গো") -> {
                "durable rugged cotton-twill weave, reinforced pocket flaps, matte military-spec zippers, authentic seam contours"
            }
            lower.contains("jamdani") || lower.contains("জামদানি") || lower.contains("saree") -> {
                "intricate hand-loomed sheer cotton-silk fabric, ornate traditional zari floral motifs, delicate sheer drape"
            }
            lower.contains("stainless") || lower.contains("metal") || lower.contains("স্টিল") -> {
                "brushed stainless steel with ultra-clean specular metallic reflections and condensation-free satin finish"
            }
            lower.contains("cotton") || lower.contains("হুডি") || lower.contains("hoodie") -> {
                "heavyweight 400gsm combed fleece cotton texture, ribbed cuffs, clean drawstrings, authentic fabric knit"
            }
            product == "smart watch" || product == "wireless headphones" -> {
                "anodized aircraft-grade aluminum casing, responsive OLED glass display reflection, and premium matte silicone straps"
            }
            else -> {
                "hyper-realistic tactile surface texture, authentic micro-details, true-to-life material reflections"
            }
        }
    }

    private fun determineStyleAndDetail(mode: ImageMode): Pair<String, String> {
        val style = when (mode) {
            ImageMode.FASHION_MODEL -> "High-end Vogue & Harper's Bazaar fashion editorial standard, authentic human skin tones, natural elegant aesthetic"
            ImageMode.PRODUCT -> "Luxury commercial e-commerce advertising photography, Apple and Amazon premium catalog tier"
            ImageMode.LIFESTYLE -> "Kinfolk magazine editorial lifestyle aesthetic, organic warm tones, authentic real-world charm"
            ImageMode.LANDSCAPE -> "National Geographic award-winning landscape standard, rich dynamic range, vibrant natural hues"
            ImageMode.ARTISTIC -> "High-end concept art illustration, artistic rendering, rich colors and expressive brushwork"
            else -> "Photorealistic commercial photography standard"
        }
        val detailLevel = "8k UHD, masterpiece quality, razor-sharp focus, ray-traced ambient occlusion, zero noise, pixel-perfect fidelity"
        return Pair(style, detailLevel)
    }

    private fun buildSubjectDescription(
        text: String,
        product: String?,
        color: String,
        isHuman: Boolean,
        mode: ImageMode
    ): String {
        val lower = text.lowercase(Locale.ROOT)
        val colorPrefix = if (color.isNotBlank()) "$color " else ""

        return when {
            mode == ImageMode.FASHION_MODEL && (lower.contains("jamdani") || lower.contains("জামদানি") || lower.contains("শাড়ি") || lower.contains("saree")) -> {
                "A graceful South Asian female fashion model elegantly wearing a ${colorPrefix}handcrafted Jamdani saree with intricate gold zari embroidery"
            }
            mode == ImageMode.FASHION_MODEL && (product != null || lower.contains("hoodie") || lower.contains("dress") || lower.contains("blazer")) -> {
                val garment = product ?: "apparel"
                "A professional fashion model showcasing a stylish ${colorPrefix}$garment with flawless tailored fit and authentic silhouette"
            }
            mode == ImageMode.FASHION_MODEL -> {
                "A high-fashion model elegantly presenting ${colorPrefix}contemporary attire"
            }
            product == "cargo pants" -> {
                "A pair of premium ${colorPrefix}cargo pants with deep functional utility pockets, clean pocket flaps, tailored tapered cuffs, and metal drawstring hardware"
            }
            product == "leather shoes" || product == "shoes" || product == "sneakers" -> {
                "A pristine pair of ${colorPrefix}${product}, luxury footwear styling with clean laces and immaculate soles"
            }
            product == "smart watch" -> {
                "A state-of-the-art ${colorPrefix}smartwatch with high-resolution active touch screen display, metallic bezel, and clean modern strap"
            }
            product != null -> {
                "A premium ${colorPrefix}$product, brand-new commercial sample with immaculate finish"
            }
            else -> {
                // Fallback using cleaned text
                "${colorPrefix}${text.take(60)}"
            }
        }
    }

    private fun buildOptimizedPrompt(
        mode: ImageMode,
        subject: String,
        poseComposition: String,
        background: String,
        lighting: String,
        camera: String,
        style: String,
        materialTexture: String,
        color: String,
        detailLevel: String,
        textInImage: String?,
        aspectRatio: String,
        negativePrompt: String
    ): String {
        val promptBuilder = StringBuilder()

        // Core visual hook
        promptBuilder.append("Commercial photography of ")
        promptBuilder.append(subject)
        promptBuilder.append(". ")

        // Text in image instruction if requested
        if (!textInImage.isNullOrBlank()) {
            promptBuilder.append("With clear, sharp, perfectly legible printed text \"$textInImage\" placed accurately on the subject, authentic typography, correct spelling. ")
        }

        // Composition & framing
        promptBuilder.append("Framing: ").append(poseComposition).append(". ")

        // Background
        promptBuilder.append("Background: ").append(background).append(". ")

        // Lighting
        promptBuilder.append("Lighting: ").append(lighting).append(". ")

        // Material & Textures
        promptBuilder.append("Materials: ").append(materialTexture).append(". ")

        // Camera & Lens specs
        promptBuilder.append("Camera: ").append(camera).append(". ")

        // Style & detail
        promptBuilder.append("Style: ").append(style).append(", ").append(detailLevel).append(". ")

        // Aspect ratio tag
        promptBuilder.append("--ar ").append(aspectRatio).append(" ")

        // Embedded negative constraint
        promptBuilder.append("[Negative: ").append(negativePrompt).append("]")

        return promptBuilder.toString().trim()
    }

    private fun buildBengaliAnalysis(
        mode: ImageMode,
        subject: String,
        background: String,
        lighting: String,
        composition: String,
        textInImage: String?,
        aspectRatio: String
    ): String {
        val modeBangla = when (mode) {
            ImageMode.PRODUCT -> "🛍️ প্রোডাক্ট ফটোগ্রাফি (Product Mode)"
            ImageMode.FASHION_MODEL -> "👗 ফ্যাশন মডেল (Fashion Model Mode)"
            ImageMode.PORTRAIT -> "👤 পোর্ট্রেট ফটোগ্রাফি (Portrait Mode)"
            ImageMode.LIFESTYLE -> "☕ লাইফস্টাইল দৃশ্য (Lifestyle Mode)"
            ImageMode.LANDSCAPE -> "🌄 ল্যান্ডস্কেপ (Landscape Mode)"
            ImageMode.ARTISTIC -> "🎨 ডিজিটাল আর্ট (Artistic Mode)"
        }

        val textNote = if (!textInImage.isNullOrBlank()) {
            "\n- **টেক্সট (Text):** \"$textInImage\" (উচ্চ রেজুলেশন টাইপোগ্রাফি নিশ্চিত করা হয়েছে)"
        } else ""

        return """
**ভিজ্যুয়াল অপটিমাইজেশন বিবরণ:**
- **ক্যাটেগরি:** $modeBangla
- **মূল বিষয়:** $subject
- **ব্যাকগ্রাউন্ড:** $background
- **আলো (Lighting):** $lighting
- **কম্পোজিশন:** $composition
- **অনুপাত (Aspect Ratio):** $aspectRatio$textNote
- **ফিল্টার:** ব্লার, বিকৃতি বা অতিরিক্ত বস্তু স্বয়ংক্রিয়ভাবে ফিল্টার করা হয়েছে
""".trimIndent()
    }
}

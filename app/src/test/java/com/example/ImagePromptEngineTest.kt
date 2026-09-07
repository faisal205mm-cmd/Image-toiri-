package com.example

import com.example.data.image.ImageMode
import com.example.data.image.ImagePromptEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verification Test Suite for the Intelligent Image Generation Engine
 * Tests at least 15 required real-world scenarios across Bengali, Banglish, English,
 * and Mixed Language inputs.
 */
class ImagePromptEngineTest {

    @Test
    fun test1_bengaliProductPrompt() {
        val input = "সাদা background এ একটা কালো cargo pant দেখাও"
        val result = ImagePromptEngine.analyzeAndOptimize(input)

        assertEquals(ImageMode.PRODUCT, result.mode)
        assertEquals("1:1", result.aspectRatio)
        assertEquals("black", result.color)
        assertEquals("cargo pants", result.product)
        assertTrue(result.background.contains("white", ignoreCase = true))
        assertTrue(result.poseComposition.contains("centered", ignoreCase = true))
        assertTrue(result.optimizedPrompt.contains("cargo pants", ignoreCase = true))
        assertTrue(result.optimizedPrompt.contains("black", ignoreCase = true))
        assertTrue(result.optimizedPrompt.contains("white", ignoreCase = true))
        assertTrue(result.negativePrompt.contains("blurry", ignoreCase = true))
    }

    @Test
    fun test2_banglishPrompt() {
        val input = "sada background e ekta black leather shoe dekhao"
        val result = ImagePromptEngine.analyzeAndOptimize(input)

        assertEquals(ImageMode.PRODUCT, result.mode)
        assertEquals("1:1", result.aspectRatio)
        assertEquals("black", result.color)
        assertTrue(result.background.contains("white", ignoreCase = true))
        assertTrue(result.materialTexture.contains("leather", ignoreCase = true))
        assertTrue(result.optimizedPrompt.contains("leather", ignoreCase = true))
    }

    @Test
    fun test3_englishProductPrompt() {
        val input = "A minimalist stainless steel matte black thermos water bottle on white background, studio shot"
        val result = ImagePromptEngine.analyzeAndOptimize(input)

        assertEquals(ImageMode.PRODUCT, result.mode)
        assertEquals("1:1", result.aspectRatio)
        assertEquals("black", result.color)
        assertTrue(result.background.contains("white", ignoreCase = true))
        assertTrue(result.materialTexture.contains("stainless", ignoreCase = true))
    }

    @Test
    fun test4_mixedLanguagePrompt() {
        val input = "Ekta white sneakers er cinematic product photo, high detail, studio lighting"
        val result = ImagePromptEngine.analyzeAndOptimize(input)

        assertEquals(ImageMode.PRODUCT, result.mode)
        assertEquals("1:1", result.aspectRatio)
        assertEquals("white", result.color)
        assertEquals("sneakers", result.product)
        assertTrue(result.optimizedPrompt.contains("sneakers", ignoreCase = true))
        assertTrue(result.optimizedPrompt.contains("white", ignoreCase = true))
    }

    @Test
    fun test5_clothingProductPrompt() {
        val input = "Royal blue denim jacket, front view, uncropped, authentic stitching and brass buttons, clean studio backdrop"
        val result = ImagePromptEngine.analyzeAndOptimize(input)

        assertEquals(ImageMode.PRODUCT, result.mode)
        assertEquals("1:1", result.aspectRatio)
        assertEquals("blue", result.color)
        assertTrue(result.materialTexture.contains("denim", ignoreCase = true))
        assertTrue(result.poseComposition.contains("uncropped", ignoreCase = true))
    }

    @Test
    fun test6_modelFashionPhotoPrompt() {
        val input = "লাল রঙের জামদানি শাড়ি পরা বাংলাদেশি নারী মডেল, ফুল বডি ফ্যাশন ফটোগ্রাফি"
        val result = ImagePromptEngine.analyzeAndOptimize(input)

        assertEquals(ImageMode.FASHION_MODEL, result.mode)
        assertEquals("9:16", result.aspectRatio) // Full body vertical framing
        assertEquals("red", result.color)
        assertTrue(result.materialTexture.contains("Jamdani", ignoreCase = true) || result.subject.contains("Jamdani", ignoreCase = true))
        assertTrue(result.poseComposition.contains("full-length", ignoreCase = true) || result.poseComposition.contains("head-to-toe", ignoreCase = true))
        assertTrue(result.negativePrompt.contains("extra limbs", ignoreCase = true))
    }

    @Test
    fun test7_whiteBackgroundProductPhotoPrompt() {
        val input = "স্মার্ট ওয়াচ, পিওর সাদা ব্যাকগ্রাউন্ডে, ডিসপ্লে অন, প্রিমিয়াম লুক"
        val result = ImagePromptEngine.analyzeAndOptimize(input)

        assertEquals(ImageMode.PRODUCT, result.mode)
        assertEquals("1:1", result.aspectRatio)
        assertEquals("smart watch", result.product)
        assertTrue(result.background.contains("white", ignoreCase = true))
        assertTrue(result.lighting.contains("diffused", ignoreCase = true) || result.lighting.contains("commercial", ignoreCase = true))
    }

    @Test
    fun test8_lifestylePhotoPrompt() {
        val input = "একটি আধুনিক কফি মগ ও ল্যাপটপ কাঠের ক্যাফে টেবিলে, মর্নিং সানলাইট"
        val result = ImagePromptEngine.analyzeAndOptimize(input)

        assertEquals(ImageMode.LIFESTYLE, result.mode)
        assertEquals("16:9", result.aspectRatio)
        assertTrue(result.background.contains("cafe", ignoreCase = true) || result.background.contains("wooden", ignoreCase = true))
        assertTrue(result.lighting.contains("morning", ignoreCase = true) || result.lighting.contains("sunlight", ignoreCase = true))
    }

    @Test
    fun test9_portraitPrompt() {
        val input = "কালো ব্লেজার পরা একজন প্রফেশনাল পুরুষ উদ্যোক্তার কর্পোরেট পোট্রেট ছবি"
        val result = ImagePromptEngine.analyzeAndOptimize(input)

        assertTrue(result.mode == ImageMode.PORTRAIT || result.mode == ImageMode.FASHION_MODEL)
        assertEquals("3:4", result.aspectRatio)
        assertEquals("black", result.color)
        assertTrue(result.poseComposition.contains("portrait", ignoreCase = true) || result.poseComposition.contains("framing", ignoreCase = true))
    }

    @Test
    fun test10_landscapePrompt() {
        val input = "সবুজ চা বাগান ও কুয়াশাচ্ছন্ন পাহাড়ের মনোরম ল্যান্ডস্কেপ দৃশ্য"
        val result = ImagePromptEngine.analyzeAndOptimize(input)

        assertEquals(ImageMode.LANDSCAPE, result.mode)
        assertEquals("16:9", result.aspectRatio)
        assertEquals("green", result.color)
        assertTrue(result.background.contains("tea", ignoreCase = true) || result.background.contains("mountain", ignoreCase = true))
    }

    @Test
    fun test11_squareFormatPrompt() {
        val input = "একটি গোল্ডেন মেটালিক লাক্সারি পারফিউম বোতল, চারকোনা স্কয়ার ফরম্যাট, সফট শ্যাডো"
        val result = ImagePromptEngine.analyzeAndOptimize(input)

        assertEquals(ImageMode.PRODUCT, result.mode)
        assertEquals("1:1", result.aspectRatio)
        assertEquals("gold", result.color)
        assertEquals("luxury perfume bottle", result.product)
    }

    @Test
    fun test12_textInImagePrompt() {
        val input = "A black cotton hoodie with crisp white printed text 'AI STUDIO 2026' on chest"
        val result = ImagePromptEngine.analyzeAndOptimize(input)

        assertEquals(ImageMode.PRODUCT, result.mode)
        assertEquals("1:1", result.aspectRatio)
        assertEquals("black", result.color)
        assertNotNull(result.textInImage)
        assertEquals("AI STUDIO 2026", result.textInImage)
        assertTrue(result.optimizedPrompt.contains("\"AI STUDIO 2026\"", ignoreCase = true))
    }

    @Test
    fun test13_multiLanguageClothingPrompt() {
        val input = "কালো রঙের hoodie পরা smart boy, urban street style aesthetic"
        val result = ImagePromptEngine.analyzeAndOptimize(input)

        assertEquals(ImageMode.FASHION_MODEL, result.mode)
        assertEquals("black", result.color)
        assertTrue(result.background.contains("urban", ignoreCase = true) || result.background.contains("street", ignoreCase = true))
    }

    @Test
    fun test14_banglishTechProductPrompt() {
        val input = "ekta sleek silver wireless headphone wooden table er upor cinematic angle e"
        val result = ImagePromptEngine.analyzeAndOptimize(input)

        assertEquals(ImageMode.PRODUCT, result.mode)
        assertEquals("silver", result.color)
        assertEquals("wireless headphones", result.product)
        assertTrue(result.background.contains("wood", ignoreCase = true))
    }

    @Test
    fun test15_artisticStudioStyledPrompt() {
        val input = "Vintage mechanical wrist watch with visible golden gears and dials, macro luxury catalog photography"
        val result = ImagePromptEngine.analyzeAndOptimize(input)

        assertEquals(ImageMode.PRODUCT, result.mode)
        assertEquals("gold", result.color)
        assertEquals("wrist watch", result.product)
        assertTrue(result.camera.contains("macro", ignoreCase = true))
        assertTrue(result.optimizedPrompt.contains("wrist watch", ignoreCase = true))
    }

    @Test
    fun testPromptSuiteIntegrity() {
        assertEquals(15, ImagePromptEngine.TEST_PROMPT_SUITE.size)
        ImagePromptEngine.TEST_PROMPT_SUITE.forEach { testItem ->
            val result = ImagePromptEngine.analyzeAndOptimize(testItem.prompt)
            assertNotNull(result.optimizedPrompt)
            assertTrue(result.optimizedPrompt.isNotBlank())
            assertTrue(result.negativePrompt.isNotBlank())
        }
    }
}

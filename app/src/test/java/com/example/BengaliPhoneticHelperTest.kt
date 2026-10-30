package com.example

import com.example.data.repository.AdvisorPersona
import com.example.data.util.BengaliPhoneticHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BengaliPhoneticHelperTest {

    @Test
    fun testContainsBengali_detectsBengaliCharacters() {
        assertTrue(BengaliPhoneticHelper.containsBengali("পোশাক"))
        assertTrue(BengaliPhoneticHelper.containsBengali("আজকের বিক্রি"))
        assertFalse(BengaliPhoneticHelper.containsBengali("Hello world 123!"))
        assertFalse(BengaliPhoneticHelper.containsBengali("Fashion Store POS"))
    }

    @Test
    fun testCommonWordsMapping() {
        val result = BengaliPhoneticHelper.toPhoneticBangla("ধন্যবাদ")
        assertEquals("Dhonnobad", result)
    }

    @Test
    fun testBengaliNumeralsConversion() {
        val result = BengaliPhoneticHelper.toPhoneticBangla("১২৩৪৫")
        assertEquals("12345", result)
    }

    @Test
    fun testMixedEnglishBengaliPreservation() {
        val result = BengaliPhoneticHelper.toPhoneticBangla("AI সহকারী")
        assertTrue(result.contains("AI"))
        assertTrue(result.contains("shohokari"))
    }

    @Test
    fun testAdvisorPersonaIntegrity() {
        val personas = AdvisorPersona.values()
        assertEquals(4, personas.size)
        assertTrue(personas.contains(AdvisorPersona.GENERAL))
        assertTrue(personas.contains(AdvisorPersona.CREATIVE))
        assertTrue(personas.contains(AdvisorPersona.CODING))
        assertTrue(personas.contains(AdvisorPersona.LEARNING))

        personas.forEach { persona ->
            assertNotNull(persona.title)
            assertTrue(persona.title.isNotEmpty())
            assertNotNull(persona.systemPrompt)
            assertTrue(persona.systemPrompt.isNotEmpty())
        }
    }
}

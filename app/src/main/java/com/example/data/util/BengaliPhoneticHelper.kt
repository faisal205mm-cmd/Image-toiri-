package com.example.data.util

/**
 * High-performance phonetics engine for Bengali text.
 * When an Android device lacks native Bengali (বাংলা) TTS voice packages,
 * passing raw Bengali Unicode results in silence or skipped words.
 *
 * This engine maps Bengali sentences into natural, fluent phonetic
 * transliterated speech that standard Android & Google TTS engines pronounce
 * clearly with natural tone, pitch, and cadence.
 */
object BengaliPhoneticHelper {

    private val commonWords = mapOf(
        "আসসালামু" to "Assalamu",
        "আলাইকুম" to "alaikum",
        "আসসালামু আলাইকুম" to "Assalamu alaikum",
        "আলহামদুলিল্লাহ" to "Alhamdulillah",
        "ধন্যবাদ" to "Dhonnobad",
        "স্বাগতম" to "Swagotom",
        "হ্যালো" to "Hello",
        "হাই" to "Hi",
        "নমস্কার" to "Nomoshkar",
        "পোশাক" to "poshakk",
        "পোশাকের" to "poshakk-er",
        "গার্মেন্টস" to "garments",
        "দোকান" to "dokan",
        "দোকানে" to "dokane",
        "দোকানের" to "dokaner",
        "ব্যবসা" to "bebsha",
        "ব্যবসার" to "bebshar",
        "সহকারী" to "shohokari",
        "এআই" to "AI",
        "আজকে" to "aajke",
        "আজকের" to "aajker",
        "আজ" to "aaj",
        "গতকাল" to "gotokal",
        "আগামীকাল" to "agamikal",
        "বিক্রি" to "bikri",
        "বিক্রয়" to "bikroy",
        "মোট" to "mot",
        "খরচ" to "khoroch",
        "বকেয়া" to "baki",
        "বাকি" to "baki",
        "লাভ" to "labh",
        "নিট" to "neat",
        "টাকা" to "taka",
        "টাকার" to "takar",
        "কাস্টমার" to "customer",
        "কাস্টমারদের" to "customer-der",
        "ক্রেতা" to "kreta",
        "স্টক" to "stock",
        "হিসাব" to "hishab",
        "হিসেব" to "hisheb",
        "কেমন" to "kemon",
        "আছেন" to "achhen",
        "আছি" to "achhi",
        "ভালো" to "bhalo",
        "বলুন" to "bolun",
        "শুনুন" to "shunoon",
        "কীভাবে" to "kibhabe",
        "কিভাবে" to "kibhabe",
        "সাহায্য" to "sahajjo",
        "করতে" to "korte",
        "পারি" to "pari",
        "পারেন" to "paren",
        "হয়েছে" to "hoyechhe",
        "হবে" to "hobe",
        "আছে" to "achhe",
        "নেই" to "nei",
        "হলো" to "holo",
        "হচ্ছে" to "hochhe",
        "করুন" to "korun",
        "করবেন" to "korben",
        "এক" to "ek",
        "দুই" to "dui",
        "তিন" to "teen",
        "চার" to "char",
        "পাঁচ" to "paanch",
        "ছয়" to "chhoy",
        "সাত" to "saat",
        "আট" to "aat",
        "নয়" to "noy",
        "দশ" to "dosh",
        "বিশ" to "beesh",
        "তিরিশ" to "tirish",
        "চল্লিশ" to "chollish",
        "পঞ্চাশ" to "ponchash",
        "শত" to "shoto",
        "হাজার" to "hajar",
        "লাখ" to "lakh",
        "কোটি" to "koti",
        "শার্ট" to "shirt",
        "প্যান্ট" to "pant",
        "পাঞ্জাবি" to "panjabi",
        "পাঞ্জাবী" to "panjabi",
        "পায়জামা" to "payjama",
        "টি-শার্ট" to "t-shirt",
        "টি শার্ট" to "t-shirt",
        "শাড়ি" to "sharee",
        "থ্রি-পিস" to "three piece",
        "থ্রিপিস" to "three piece",
        "বোরকা" to "borka",
        "হিজাব" to "hijab",
        "ফেব্রিক" to "fabric",
        "কাপড়" to "kapor",
        "কাপড়ের" to "kaporer",
        "জি" to "Ji",
        "হ্যাঁ" to "Hya",
        "না" to "Na",
        "অবশ্যই" to "Oboshshoi",
        "দুঃখিত" to "Dukkhito",
        "দারুণ" to "Darun",
        "খুব" to "khoob",
        "অনেক" to "onek",
        "বেশি" to "beshi",
        "কম" to "kom",
        "আদায়" to "aaday"
    )

    // Bengali numbers mapping
    private val digitMap = mapOf(
        '০' to "0", '১' to "1", '২' to "2", '৩' to "3", '৪' to "4",
        '৫' to "5", '৬' to "6", '৭' to "7", '৮' to "8", '৯' to "9"
    )

    private val vowels = mapOf(
        'অ' to "o", 'আ' to "a", 'ই' to "i", 'ঈ' to "ee", 'উ' to "u",
        'ঊ' to "oo", 'ঋ' to "ri", 'এ' to "e", 'ঐ' to "oi", 'ও' to "o", 'ঔ' to "ou"
    )

    private val vowelSigns = mapOf(
        'া' to "a", 'ি' to "i", 'ী' to "ee", 'ু' to "u", 'ূ' to "oo",
        'ৃ' to "ri", 'ে' to "e", 'ৈ' to "oi", 'ো' to "o", 'ৌ' to "ou"
    )

    private val consonants = mapOf(
        'ক' to "k", 'খ' to "kh", 'গ' to "g", 'ঘ' to "gh", 'ঙ' to "ng",
        'চ' to "ch", 'ছ' to "chh", 'জ' to "j", 'ঝ' to "jh", 'ঞ' to "n",
        'ট' to "t", 'ঠ' to "th", 'ড' to "d", 'ঢ' to "dh", 'ণ' to "n",
        'ত' to "t", 'থ' to "th", 'দ' to "d", 'ধ' to "dh", 'ন' to "n",
        'প' to "p", 'ফ' to "f", 'ব' to "b", 'ভ' to "bh", 'ম' to "m",
        'য' to "j", 'র' to "r", 'ল' to "l", 'শ' to "sh", 'ষ' to "sh",
        'স' to "s", 'হ' to "h", 'ড়' to "r", 'ঢ়' to "rh", 'য়' to "y",
        'ৎ' to "t", 'ং' to "ng", 'ঃ' to "h", 'ঁ' to ""
    )

    /**
     * Checks if the text contains any Bengali characters.
     */
    fun containsBengali(text: String): Boolean {
        return text.any { it in '\u0980'..'\u09FF' }
    }

    /**
     * Converts a Bengali text string into natural sounding Romanized Bengali.
     * Preserves English words, punctuation, and digits.
     */
    fun toPhoneticBangla(input: String): String {
        if (!containsBengali(input)) return input

        var processed = input
        // Replace Bengali numerals
        digitMap.forEach { (bn, en) ->
            processed = processed.replace(bn.toString(), en)
        }

        // Tokenize and match common words first
        val words = processed.split(Regex("([\\s,।!?;:]+)"))
        val delimiters = Regex("([\\s,।!?;:]+)").findAll(processed).map { it.value }.toList()

        val sb = StringBuilder()
        var delimIdx = 0

        for (word in words) {
            if (word.isNotBlank()) {
                val cleanWord = word.trim()
                val mapped = commonWords[cleanWord]
                if (mapped != null) {
                    sb.append(mapped)
                } else if (containsBengali(cleanWord)) {
                    sb.append(transliterateWord(cleanWord))
                } else {
                    sb.append(cleanWord)
                }
            }
            if (delimIdx < delimiters.size) {
                val delim = delimiters[delimIdx]
                // Replace Bengali Dari (।) with period (.)
                sb.append(delim.replace('।', '.'))
                delimIdx++
            }
        }

        return sb.toString().replace(Regex("\\s{2,}"), " ").trim()
    }

    private fun transliterateWord(word: String): String {
        val sb = StringBuilder()
        var i = 0
        val len = word.length

        while (i < len) {
            val c = word[i]
            when {
                // Vowel at start or after another vowel
                c in vowels -> {
                    sb.append(vowels[c])
                }
                // Consonant
                c in consonants -> {
                    val consStr = consonants[c] ?: ""
                    sb.append(consStr)

                    // Look ahead for virama (hasant) or vowel sign
                    val next = if (i + 1 < len) word[i + 1] else null
                    if (next == '্') {
                        // Virama: skip inherent vowel and skip the virama char
                        i++
                    } else if (next != null && next in vowelSigns) {
                        // Explicit vowel sign
                        sb.append(vowelSigns[next])
                        i++
                    } else if (next != null && next in consonants) {
                        // Inherent vowel between consonants (usually 'o' in Bengali)
                        sb.append("o")
                    }
                }
                c in vowelSigns -> {
                    sb.append(vowelSigns[c])
                }
                else -> {
                    sb.append(c)
                }
            }
            i++
        }

        return sb.toString()
    }
}

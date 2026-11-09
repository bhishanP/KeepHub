package com.keephub.core.data.util

import java.text.Normalizer

/**
 * Produces canonical normalized forms and candidate keys for robust duplicate detection:
 *  - Lower-case and diacritics stripping (café -> cafe, résumé -> resume)
 *  - Grammatical prefix removal ("to ", "a ", "an ", "the ")
 *  - Separator and punctuation collapsing ("well-being" == "well being" == "wellbeing")
 *  - Smart English inflection and silent 'e' reduction (making -> make, baked -> bake, studies -> study)
 */
class WordNormalizer {

    fun normalizeForKey(raw: String): String {
        val cleaned = cleanAndCollapse(raw)
        return reduceInflection(cleaned)
    }

    /**
     * Generates all candidate keys that could match an existing word in the database.
     * For instance, "making" generates ["make", "mak", "making"].
     */
    fun candidateKeys(raw: String): List<String> {
        val cleaned = cleanAndCollapse(raw)
        if (cleaned.isBlank()) return emptyList()

        val candidates = linkedSetOf<String>()
        val reduced = reduceInflection(cleaned)

        candidates.add(reduced)
        candidates.add(cleaned)

        // Silent 'e' variants: if reduced ends in consonant, also check 'e' variant
        // e.g. "mak" -> "make", "writ" -> "write", "danc" -> "dance"
        if (reduced.length >= 3 && !reduced.endsWith("e")) {
            candidates.add(reduced + "e")
        }

        // If reduced ends in 'i', also check 'y' variant (e.g. "happi" -> "happy")
        if (reduced.endsWith("i")) {
            candidates.add(reduced.dropLast(1) + "y")
        }

        return candidates.toList()
    }

    private fun cleanAndCollapse(raw: String): String {
        var s = raw.trim().lowercase()
        s = stripDiacritics(s)

        // Trim leading and trailing non-alphanumeric punctuation (e.g., quotes, parentheses, brackets, ellipses)
        s = s.replace(Regex("""^[^\p{L}\p{N}]+|[^\p{L}\p{N}]+$"""), "")

        // Strip grammatical prefixes if followed by at least 2 characters
        s = stripGrammaticalPrefixes(s)

        // Collapse hyphens, apostrophes, and internal whitespace into a continuous key
        // e.g., "well-being" -> "wellbeing", "ice cream" -> "icecream", "user's" -> "users"
        s = s.replace("'", "")
            .replace("’", "")
            .replace("`", "")
            .replace("-", "")
            .replace("_", "")
            .replace(Regex("""\s+"""), "")

        return s
    }

    private fun stripDiacritics(s: String): String {
        val n = Normalizer.normalize(s, Normalizer.Form.NFKD)
        return n.replace(Regex("\\p{M}+"), "")
    }

    private fun stripGrammaticalPrefixes(s: String): String {
        val prefixes = listOf("to ", "a ", "an ", "the ")
        for (prefix in prefixes) {
            if (s.startsWith(prefix) && s.length > prefix.length + 1) {
                return s.substring(prefix.length).trim()
            }
        }
        return s
    }

    /**
     * Reduces English inflections (plurals, verb tenses, participles, comparatives, adverbs)
     * to a canonical base lemma.
     */
    private fun reduceInflection(s: String): String {
        if (s.length < 3) return s

        // Words that naturally end in -ing and should NOT be stripped as verbs
        val nonGerunds = setOf(
            "morning", "evening", "ceiling", "lightning", "building", "feeling",
            "meaning", "during", "spring", "string", "bring", "ring", "sing",
            "king", "wing", "thing", "something", "nothing", "anything", "everything"
        )
        if (s in nonGerunds || s.endsWith("being") || s.endsWith("thing")) {
            return s
        }

        // 1. Present participle / gerund (-ing)
        if (s.endsWith("ing") && s.length > 4) {
            val base = s.removeSuffix("ing")
            return when {
                // Doubled consonants: running -> run, swimming -> swim, stopping -> stop
                base.length > 2 && base.last() == base[base.length - 2] && isConsonant(base.last()) -> {
                    base.dropLast(1)
                }
                // Silent 'e' dropped before -ing: making -> make, dancing -> dance, writing -> write
                hasSilentEDropped(base) -> {
                    base + "e"
                }
                else -> base
            }
        }

        // 2. Past tense / past participle (-ied, -ed)
        if (s.endsWith("ied") && s.length > 4) return s.dropLast(3) + "y" // tried -> try
        if (s.endsWith("ed") && s.length > 3) {
            val base = s.removeSuffix("ed")
            return when {
                // Doubled consonants: stopped -> stop, planned -> plan
                base.length > 2 && base.last() == base[base.length - 2] && isConsonant(base.last()) -> {
                    base.dropLast(1)
                }
                // Silent 'e' + d: baked -> bake, liked -> like, created -> create
                hasSilentEDropped(base) -> {
                    base + "e"
                }
                else -> base
            }
        }

        // 3. Adverbs (-ily, -ly)
        if (s.endsWith("ily") && s.length > 4) return s.dropLast(3) + "y" // happily -> happy
        if (s.endsWith("ly") && s.length > 4 && !s.endsWith("ally")) {
            val base = s.removeSuffix("ly")
            if (base.length >= 3) return base // quickly -> quick
        }

        // 4. Comparatives and superlatives (-ier, -iest, -er, -est)
        if (s.endsWith("iest") && s.length > 5) return s.dropLast(4) + "y" // happiest -> happy
        if (s.endsWith("ier") && s.length > 4) return s.dropLast(3) + "y"  // happier -> happy
        if (s.endsWith("est") && s.length > 5) return s.removeSuffix("est") // quickest -> quick
        if (s.endsWith("er") && s.length > 4) {
            val base = s.removeSuffix("er")
            if (hasSilentEDropped(base)) return base + "e" // wider -> wide
            if (base.length > 2 && base.last() == base[base.length - 2]) return base.dropLast(1) // bigger -> big
            return base // quicker -> quick
        }

        // 5. Plurals & 3rd person singular (-ies, -es, -s)
        if (s.endsWith("ies") && s.length > 4) return s.dropLast(3) + "y" // studies -> study
        if (s.endsWith("ves") && s.length > 4) return s.dropLast(3) + "f" // leaves -> leaf, wolves -> wolf
        if (s.endsWith("ses") || s.endsWith("xes") || s.endsWith("zes") || s.endsWith("shes") || s.endsWith("ches")) {
            return s.dropLast(2) // boxes -> box, buses -> bus, watches -> watch
        }
        if (s.endsWith("es") && s.length > 4) {
            val withoutEs = s.removeSuffix("es")
            if (withoutEs.endsWith("o")) return withoutEs // heroes -> hero, tomatoes -> tomato
            return withoutEs + "e" // places -> place
        }
        if (s.endsWith("s") && s.length > 3 && !s.endsWith("ss") && !s.endsWith("us") && !s.endsWith("is")) {
            return s.dropLast(1) // cats -> cat, books -> book
        }

        return s
    }

    private fun isConsonant(c: Char): Boolean = c in 'a'..'z' && c !in "aeiouy"

    /**
     * Determines whether a root word likely dropped a silent 'e'
     * (e.g. "mak" in making -> make, "danc" in dancing -> dance, "writ" in writing -> write).
     */
    private fun hasSilentEDropped(base: String): Boolean {
        if (base.length < 3) return false
        val lastChar = base.last()
        val secondLast = base[base.length - 2]

        // Words ending in soft 'c' or soft 'g' (danc -> dance, chanc -> chance, chang -> change, manag -> manage)
        if (lastChar == 'c' || lastChar == 'g') return true

        // Multi-syllable verbs ending in -ate (creat -> create, motivat -> motivate, isolat -> isolate)
        if (base.endsWith("at") && base.length >= 5) return true

        // If it ends in common consonant clusters (lk, mp, sk, ch, sh, th, ng, ck, ct, nt, st, rt, lt),
        // it almost never had a silent 'e' (walk, jump, ask, watch, wish, think, lock, act, hunt, fast, part, melt)
        val clusters = setOf("lk", "mp", "sk", "ch", "sh", "th", "ng", "ck", "ct", "nt", "st", "rt", "lt", "nd", "nk")
        if (base.length >= 2 && clusters.contains(base.takeLast(2))) return false

        // If preceded by double vowels (look, feel, read, meet, wait), it usually doesn't end in 'e'
        if (base.length >= 3 && !isConsonant(secondLast) && !isConsonant(base[base.length - 3])) {
            return false
        }

        // Single consonant preceded by a single vowel (e.g. m-a-k, t-a-k, w-r-i-t, h-o-p, c-r-e-a-t)
        return isConsonant(lastChar) && !isConsonant(secondLast)
    }

    fun levenshteinDistance(s1: String, s2: String): Int {
        val a = s1.trim().lowercase()
        val b = s2.trim().lowercase()
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val dp = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..b.length) {
                val temp = dp[j]
                dp[j] = if (a[i - 1] == b[j - 1]) prev else 1 + minOf(prev, dp[j], dp[j - 1])
                prev = temp
            }
        }
        return dp[b.length]
    }

    fun isMinorTypo(input: String, target: String): Boolean {
        val a = cleanAndCollapse(input)
        val b = cleanAndCollapse(target)
        if (a == b) return true
        val dist = levenshteinDistance(a, b)
        val maxLen = maxOf(a.length, b.length)
        return when {
            maxLen <= 4 -> dist == 0
            maxLen <= 7 -> dist <= 1
            else -> dist <= 2
        }
    }
}

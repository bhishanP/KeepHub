package com.keephub.core.data.util

import java.text.Normalizer

/**
 * Produces a canonical normalized form for duplicate detection:
 *  - lower-case
 *  - Unicode NFKD + strip diacritics
 *  - trim punctuation
 *  - reduce common English inflections to a base-ish form (lightweight; offline)
 */
class WordNormalizer {
    fun normalizeForKey(raw: String): String {
        val t = raw.trim()
            .lowercase()
            .let(::stripDiacritics)
            .replace(Regex("""^[^\p{L}\p{N}]+|[^\p{L}\p{N}]+$"""), "") // trim non-alnum edges
        return reduceInflection(t)
    }

    private fun stripDiacritics(s: String): String {
        val n = Normalizer.normalize(s, Normalizer.Form.NFKD)
        return n.replace(Regex("\\p{M}+"), "")
    }

    // Very small stemmer for common cases (cats -> cat, studies -> study, running -> run, tried -> try)
    // Intentionally conservative to avoid over-stemming.
    private fun reduceInflection(s: String): String {
        if (s.length < 3) return s
        // gerunds / present participle
        if (s.endsWith("ing") && s.length > 4) {
            val base = s.removeSuffix("ing")
            // e.g., running -> run, making -> make
            return when {
                base.endsWith(base.last().toString() + base.last()) && base.length > 2 -> base.dropLast(1)
                else -> if (base.endsWith("k") || base.endsWith("v") || base.endsWith("c")) base else base
            }
        }
        // past tense / past participle
        if (s.endsWith("ied") && s.length > 4) return s.dropLast(3) + "y"      // tried -> try
        if (s.endsWith("ed") && s.length > 3) return s.removeSuffix("ed")      // walked -> walk

        // simple plurals
        if (s.endsWith("ies") && s.length > 4) return s.dropLast(3) + "y"      // studies -> study
        if (s.endsWith("ses") || s.endsWith("xes") || s.endsWith("zes")) return s.dropLast(2) // buses->bus
        if (s.endsWith("es") && s.length > 3) return s.dropLast(2)             // boxes -> box
        if (s.endsWith("s") && s.length > 3 && !s.endsWith("ss")) return s.dropLast(1) // cats -> cat

        return s
    }
}

package com.keephub.core.data.net

import com.keephub.core.data.repo.*

class MockDictionaryService : DictionaryService {
    override suspend fun lookup(term: String, lang: String): DictionaryLookup {
        val cap = term.trim().replaceFirstChar { it.uppercaseChar() }
        val sense = LookupSense(
            pos = "noun",
            definition = "Mock definition of \"$cap\" for quick offline testing.",
            ipa = "/${term.lowercase().take(4)}/",
            examples = listOf("This is a mock example sentence for $cap."),
            synonyms = listOf("sample", "placeholder"),
            antonyms = listOf("real")
        )
        return DictionaryLookup(senses = listOf(sense), ipa = sense.ipa)
    }
}

class MockTranslateService : TranslateService {
    override suspend fun translate(text: String, target: String, source: String): String =
        "[$target] $text"
}

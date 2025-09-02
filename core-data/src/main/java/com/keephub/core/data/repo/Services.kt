package com.keephub.core.data.repo

data class LookupSense(
    val pos: String?,
    val definition: String,
    val ipa: String? = null,
    val examples: List<String> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val audioUrls: List<String> = emptyList()
)

data class DictionaryLookup(
    val senses: List<LookupSense> = emptyList(),
    val ipa: String? = null                 // top-level best IPA (optional convenience)
)

interface DictionaryService {
    /** Returns senses for the headword (language is ISO like "en"). */
    suspend fun lookup(term: String, lang: String = "en"): DictionaryLookup
}

interface TranslateService {
    /** Translates text from [source] to [target]. Source may be "auto". */
    suspend fun translate(text: String, target: String, source: String = "en"): String
}

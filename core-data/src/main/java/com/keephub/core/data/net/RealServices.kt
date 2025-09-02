package com.keephub.core.data.net

import com.keephub.core.data.repo.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FreeDictionaryApiService(
    private val client: OkHttpClient,
    private val baseUrl: String = "https://api.dictionaryapi.dev",
    private val json: Json = Json { ignoreUnknownKeys = true }
) : DictionaryService {

    @Serializable
    private data class ApiDefinition(
        val definition: String,
        val example: String? = null,
        val synonyms: List<String> = emptyList(),
        val antonyms: List<String> = emptyList()
    )
    @Serializable private data class ApiMeaning(
        @SerialName("partOfSpeech") val pos: String? = null,
        val definitions: List<ApiDefinition> = emptyList()
    )
    @Serializable private data class ApiPhonetic(
        val text: String? = null,
        val audio: String? = null
    )
    @Serializable private data class ApiEntry(
        val word: String,
        val phonetics: List<ApiPhonetic> = emptyList(),
        val meanings: List<ApiMeaning> = emptyList()
    )

    override suspend fun lookup(term: String, lang: String): DictionaryLookup = withContext(Dispatchers.IO) {
        val url = "$baseUrl/api/v2/entries/$lang/${term.trim()}"
        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext DictionaryLookup()
            val body = resp.body.string()
            val entries = json.decodeFromString<List<ApiEntry>>(body)
            val audio = entries.flatMap { it -> it.phonetics.mapNotNull { p -> p.audio?.takeIf { it.isNotBlank() } } }
            val allSenses = entries.flatMap { entry ->
                entry.meanings.flatMap { m ->
                    m.definitions.map { d ->
                        LookupSense(
                            pos = m.pos,
                            definition = d.definition,
                            ipa = entry.phonetics.firstOrNull { !it.text.isNullOrBlank() }?.text,
                            examples = listOfNotNull(d.example),
                            synonyms = d.synonyms,
                            antonyms = d.antonyms,
                            audioUrls = audio
                        )
                    }
                }
            }
            val topIpa = entries.firstOrNull()?.phonetics?.firstOrNull { !it.text.isNullOrBlank() }?.text
            return@withContext DictionaryLookup(allSenses, ipa = topIpa)
        }
    }
}

class LibreTranslateService(
    private val client: OkHttpClient,
    private val baseUrl: String = "https://libretranslate.com",
    private val apiKey: String? = null,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : TranslateService {

    @Serializable private data class Req(val q: String, val source: String, val target: String, val format: String = "text", val apikey: String? = null)
    @Serializable private data class Res(val translatedText: String)

    override suspend fun translate(text: String, target: String, source: String): String = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) return@withContext text
        val payload = Req(text, source.ifBlank { "auto" }, target, apikey = apiKey)
        val media = "application/json; charset=utf-8".toMediaType()
        val body = json.encodeToString(Req.serializer(), payload).toRequestBody(media)
        val req = Request.Builder().url("$baseUrl/translate").post(body).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext text
            val out = resp.body.string()
            return@withContext json.decodeFromString(Res.serializer(), out).translatedText
        }
    }
}

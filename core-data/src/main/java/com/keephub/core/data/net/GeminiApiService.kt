package com.keephub.core.data.net

import com.keephub.core.data.repo.DictionaryLookup
import com.keephub.core.data.repo.LookupSense
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class AiEnrichmentResult(
    val lookup: DictionaryLookup,
    val mnemonic: String? = null
)

@Serializable
data class SemanticEvaluation(
    val isAcceptable: Boolean = false,
    val verdict: String = "INCORRECT", // SYNONYM, TYPO, INCORRECT
    val feedback: String = ""
)

sealed class GeminiTestResult {
    data class Success(val modelUsed: String) : GeminiTestResult()
    data class Failure(val message: String, val statusCode: Int? = null) : GeminiTestResult()
}

class GeminiApiService(
    private val client: OkHttpClient,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }
) {
    companion object {
        val CANDIDATE_MODELS = listOf(
            "gemini-3.5-flash-lite",
            "gemini-3.8-flash",
            "gemini-3.6-flash",
            "gemini-3.1-flash-lite",
            "gemini-flash-latest",
            "gemini-4-argon",
            "gemini-2.5-flash-lite"
        )
    }

    @Volatile
    private var workingModel: String = CANDIDATE_MODELS.first()

    @Serializable
    private data class GeminiPart(val text: String)

    @Serializable
    private data class GeminiContent(val parts: List<GeminiPart>)

    @Serializable
    private data class GeminiGenerationConfig(
        @SerialName("responseMimeType") val responseMimeType: String = "application/json"
    )

    @Serializable
    private data class GeminiRequest(
        val contents: List<GeminiContent>,
        val generationConfig: GeminiGenerationConfig? = null
    )

    @Serializable
    private data class CandidateContent(val parts: List<GeminiPart> = emptyList())

    @Serializable
    private data class Candidate(val content: CandidateContent? = null)

    @Serializable
    private data class GeminiResponse(val candidates: List<Candidate> = emptyList())

    @Serializable
    private data class GeminiApiErrorDetails(
        val code: Int? = null,
        val message: String? = null,
        val status: String? = null
    )

    @Serializable
    private data class GeminiApiErrorEnvelope(
        val error: GeminiApiErrorDetails? = null
    )

    @Serializable
    data class AiSenseSchema(
        val pos: String? = null,
        val definition: String = "",
        val examples: List<String> = emptyList(),
        val synonyms: List<String> = emptyList(),
        val antonyms: List<String> = emptyList()
    )

    @Serializable
    data class AiWordSchema(
        val ipa: String? = null,
        val mnemonic: String? = null,
        val senses: List<AiSenseSchema> = emptyList()
    )

    fun sanitizeApiKey(key: String): String =
        key.trim().removeSurrounding("\"").removeSurrounding("'").trim()

    private sealed class RawCallResult {
        data class Success(val text: String) : RawCallResult()
        data class HttpError(val code: Int, val errorBody: String?) : RawCallResult()
        data class NetworkError(val exception: Exception) : RawCallResult()
    }

    private fun executeGeminiRaw(
        apiKey: String,
        model: String,
        prompt: String,
        jsonMime: Boolean
    ): RawCallResult {
        return try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val payload = GeminiRequest(
                contents = listOf(GeminiContent(parts = listOf(GeminiPart(prompt)))),
                generationConfig = if (jsonMime) GeminiGenerationConfig("application/json") else null
            )
            val media = "application/json; charset=utf-8".toMediaType()
            val reqBody = json.encodeToString(GeminiRequest.serializer(), payload).toRequestBody(media)
            val req = Request.Builder()
                .url(url)
                .addHeader("x-goog-api-key", apiKey)
                .post(reqBody)
                .build()

            client.newCall(req).execute().use { resp ->
                val bodyString = resp.body.string()
                if (!resp.isSuccessful) {
                    return RawCallResult.HttpError(resp.code, bodyString)
                }
                val geminiResp = json.decodeFromString<GeminiResponse>(bodyString)
                val text = geminiResp.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                if (!text.isNullOrBlank()) {
                    RawCallResult.Success(text)
                } else {
                    RawCallResult.HttpError(resp.code, "Empty candidate content in Gemini response")
                }
            }
        } catch (e: Exception) {
            RawCallResult.NetworkError(e)
        }
    }

    private fun formatHttpError(code: Int, errorBody: String?): String {
        val rawMsg = errorBody?.let { extractErrorMessage(it) }
        if (!rawMsg.isNullOrBlank()) {
            val cleanMsg = rawMsg.lines().firstOrNull()?.take(140) ?: rawMsg.take(140)
            return "Gemini error (HTTP $code): $cleanMsg"
        }
        return when (code) {
            400 -> "Invalid request or API key format (HTTP 400)."
            401 -> "Unauthorized: API key is invalid or expired (HTTP 401)."
            403 -> "Access forbidden: Check API key permissions in Google AI Studio (HTTP 403)."
            404 -> "Model not supported for this API key tier (HTTP 404)."
            429 -> "Rate limit or quota exceeded (HTTP 429). Please wait a moment."
            in 500..599 -> "Google Gemini server error (HTTP $code). Please try again later."
            else -> "Connection failed with HTTP $code."
        }
    }

    private fun extractErrorMessage(body: String): String? {
        return try {
            val envelope = json.decodeFromString<GeminiApiErrorEnvelope>(body)
            envelope.error?.message?.trim()?.ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun callGemini(apiKey: String, prompt: String, jsonMime: Boolean): String? = withContext(Dispatchers.IO) {
        val cleanKey = sanitizeApiKey(apiKey)
        if (cleanKey.isBlank()) return@withContext null

        val modelsToTry = listOf(workingModel) + (CANDIDATE_MODELS - workingModel)
        for (model in modelsToTry) {
            when (val result = executeGeminiRaw(cleanKey, model, prompt, jsonMime)) {
                is RawCallResult.Success -> {
                    workingModel = model
                    return@withContext result.text
                }
                is RawCallResult.HttpError -> {
                    if (result.code == 404) {
                        continue // Model not found in this account, try next candidate
                    } else {
                        return@withContext null
                    }
                }
                is RawCallResult.NetworkError -> {
                    return@withContext null
                }
            }
        }
        null
    }

    /**
     * Performs a detailed connectivity test against Gemini, trying candidate models and
     * returning specific, human-readable diagnostics on failure.
     */
    suspend fun testConnectionDetailed(apiKey: String): GeminiTestResult = withContext(Dispatchers.IO) {
        val cleanKey = sanitizeApiKey(apiKey)
        if (cleanKey.isBlank()) {
            return@withContext GeminiTestResult.Failure("API key cannot be empty.")
        }

        val modelsToTry = listOf(workingModel) + (CANDIDATE_MODELS - workingModel)
        var lastError: GeminiTestResult.Failure? = null

        for (model in modelsToTry) {
            val result = executeGeminiRaw(cleanKey, model, "Reply with 'OK' in one word.", jsonMime = false)
            when (result) {
                is RawCallResult.Success -> {
                    workingModel = model
                    return@withContext GeminiTestResult.Success(modelUsed = model)
                }
                is RawCallResult.HttpError -> {
                    if (result.code == 404) {
                        lastError = GeminiTestResult.Failure(
                            message = "Model $model not available (HTTP 404). Trying next...",
                            statusCode = 404
                        )
                        continue
                    } else {
                        val readable = formatHttpError(result.code, result.errorBody)
                        return@withContext GeminiTestResult.Failure(readable, statusCode = result.code)
                    }
                }
                is RawCallResult.NetworkError -> {
                    return@withContext GeminiTestResult.Failure(
                        "Network error: Unable to reach Google servers (${result.exception.localizedMessage ?: "check connection"})."
                    )
                }
            }
        }

        lastError ?: GeminiTestResult.Failure("Failed to connect to any Gemini model.")
    }

    /**
     * Pings Gemini with a minimal prompt to verify API key validity.
     */
    suspend fun testConnection(apiKey: String): Boolean =
        testConnectionDetailed(apiKey) is GeminiTestResult.Success

    /**
     * Enriches a vocabulary term with context-aware sense, mnemonic, and modern examples.
     */
    suspend fun enrichWord(
        apiKey: String,
        term: String,
        contextSentence: String? = null,
        baseLang: String = "en"
    ): AiEnrichmentResult? = withContext(Dispatchers.IO) {
        val cleanKey = sanitizeApiKey(apiKey)
        if (cleanKey.isBlank() || term.isBlank()) return@withContext null
        try {
            val prompt = buildPrompt(term, contextSentence, baseLang)
            val rawText = callGemini(cleanKey, prompt, jsonMime = true) ?: return@withContext null

            val cleanJson = rawText
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val parsed = json.decodeFromString<AiWordSchema>(cleanJson)
            if (parsed.senses.isEmpty()) return@withContext null

            val senses = parsed.senses.map { s ->
                LookupSense(
                    pos = s.pos,
                    definition = s.definition,
                    ipa = parsed.ipa,
                    examples = s.examples,
                    synonyms = s.synonyms,
                    antonyms = s.antonyms,
                    audioUrls = emptyList()
                )
            }

            AiEnrichmentResult(
                lookup = DictionaryLookup(senses = senses, ipa = parsed.ipa),
                mnemonic = parsed.mnemonic
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Explains why a user's answer was incorrect in 2-3 concise, encouraging sentences.
     */
    suspend fun explainMistake(
        apiKey: String,
        term: String,
        definition: String,
        userAnswer: String,
        contextSentence: String? = null
    ): String? = withContext(Dispatchers.IO) {
        val cleanKey = sanitizeApiKey(apiKey)
        if (cleanKey.isBlank()) return@withContext null
        val contextPart = if (!contextSentence.isNullOrBlank()) "Question Context: \"$contextSentence\"\n" else ""
        val prompt = """
            You are an expert language learning tutor.
            Target Word: "$term"
            Correct Definition: "$definition"
            ${contextPart}Student Answer: "$userAnswer"

            In 2 to 3 concise, friendly, and instructive sentences:
            1. Explain why "$term" is the correct answer.
            2. Explain why "$userAnswer" does not fit or how its meaning differs.
            3. Provide a quick tip or mnemonic to remember the difference.
            Do not output conversational filler. Give only the explanation.
        """.trimIndent()
        callGemini(cleanKey, prompt, jsonMime = false)
    }

    /**
     * Evaluates whether a typed answer is an acceptable synonym or minor typo.
     */
    suspend fun evaluateSemanticAnswer(
        apiKey: String,
        targetTerm: String,
        targetDefinition: String,
        userTyped: String
    ): SemanticEvaluation? = withContext(Dispatchers.IO) {
        val cleanKey = sanitizeApiKey(apiKey)
        if (cleanKey.isBlank()) return@withContext null
        val prompt = """
            You are an intelligent language quiz grader.
            Target Word: "$targetTerm"
            Target Meaning: "$targetDefinition"
            Student Typed: "$userTyped"

            Determine whether the student's input should be accepted:
            - Accept as SYNONYM if it is a legitimate synonym fitting the meaning.
            - Accept as TYPO if it is clearly the target word with 1-2 minor misspellings.
            - Reject as INCORRECT if it is wrong, unrelated, or an antonym.

            Output strict JSON matching:
            {
              "isAcceptable": true | false,
              "verdict": "SYNONYM" | "TYPO" | "INCORRECT",
              "feedback": "Concise 1-sentence note for the student"
            }
        """.trimIndent()

        try {
            val raw = callGemini(cleanKey, prompt, jsonMime = true) ?: return@withContext null
            val clean = raw
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            json.decodeFromString<SemanticEvaluation>(clean)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Generates a 100-150 word micro-story incorporating the given vocabulary terms.
     */
    suspend fun generateStory(
        apiKey: String,
        words: List<String>
    ): String? = withContext(Dispatchers.IO) {
        val cleanKey = sanitizeApiKey(apiKey)
        if (cleanKey.isBlank() || words.isEmpty()) return@withContext null
        val wordList = words.distinct().take(10).joinToString(", ")
        val prompt = """
            You are a creative writer and language learning coach.
            Write an engaging, cohesive, modern 100-140 word micro-story that naturally uses these vocabulary words: [$wordList].
            
            Guidelines:
            - Format each target vocabulary word in **bold** (e.g. **ephemeral**).
            - Keep the tone entertaining, clear, and easy to follow (CEFR B2).
            - End with a single-sentence takeaway or moral.
            Output only the story text and moral.
        """.trimIndent()
        callGemini(cleanKey, prompt, jsonMime = false)
    }

    fun buildPrompt(term: String, contextSentence: String?, baseLang: String): String {
        val contextClause = if (!contextSentence.isNullOrBlank()) {
            "The learner encountered this word in this sentence: \"$contextSentence\". Prioritize this context for the primary sense."
        } else {
            "Provide the most useful contemporary senses for language learners."
        }
        return """
            You are an expert lexicographer and language learning tutor.
            Analyze the vocabulary word: "$term" (language: $baseLang).
            $contextClause

            Output strict JSON matching this exact schema:
            {
              "ipa": "/IPA phonetic transcription/",
              "mnemonic": "A short, memorable 1-sentence mnemonic or etymology anchor to help remember the word",
              "senses": [
                {
                  "pos": "verb | noun | adjective | adverb | ...",
                  "definition": "Clear, concise learner-friendly definition in plain English",
                  "examples": [
                    "A natural, contemporary example sentence.",
                    "A second example showing different usage context."
                  ],
                  "synonyms": ["synonym1", "synonym2"],
                  "antonyms": ["antonym1"]
                }
              ]
            }
        """.trimIndent()
    }
}

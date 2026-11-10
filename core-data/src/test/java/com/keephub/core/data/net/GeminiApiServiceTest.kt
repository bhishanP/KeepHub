package com.keephub.core.data.net

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class GeminiApiServiceTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false }

    @Test
    fun testBuildPromptIncludesContextSentence() {
        val service = GeminiApiService(OkHttpClient(), json)
        val prompt = service.buildPrompt("ephemeral", "The morning dew was ephemeral, gone by noon.", "en")

        assertTrue(prompt.contains("ephemeral"))
        assertTrue(prompt.contains("The morning dew was ephemeral"))
        assertTrue(prompt.contains("mnemonic"))
    }

    @Test
    fun testBuildPromptWithoutContextSentence() {
        val service = GeminiApiService(OkHttpClient(), json)
        val prompt = service.buildPrompt("resilient", null, "en")

        assertTrue(prompt.contains("resilient"))
        assertTrue(prompt.contains("useful contemporary senses"))
    }

    @Test
    fun testEnrichWordWithMarkdownCodeFences() = runBlocking {
        val fakeJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "```json\n{\n  \"ipa\": \"/ɪˈfɛmərəl/\",\n  \"mnemonic\": \"E-fem-eral: think of female fashion trends that last only a short time.\",\n  \"senses\": [\n    {\n      \"pos\": \"adjective\",\n      \"definition\": \"Lasting for a very short time.\",\n      \"examples\": [\"Fashions are ephemeral.\"],\n      \"synonyms\": [\"transient\", \"fleeting\"],\n      \"antonyms\": [\"permanent\"]\n    }\n  ]\n}\n```"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(fakeJson.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val service = GeminiApiService(mockClient, json)
        val result = service.enrichWord(apiKey = "test_api_key", term = "ephemeral")

        assertNotNull(result)
        assertEquals("/ɪˈfɛmərəl/", result?.lookup?.ipa)
        assertTrue(result?.mnemonic?.contains("E-fem-eral") == true)
        assertEquals(1, result?.lookup?.senses?.size)

        val firstSense = result?.lookup?.senses?.first()
        assertEquals("adjective", firstSense?.pos)
        assertEquals("Lasting for a very short time.", firstSense?.definition)
        assertEquals(listOf("Fashions are ephemeral."), firstSense?.examples)
        assertEquals(listOf("transient", "fleeting"), firstSense?.synonyms)
        assertEquals(listOf("permanent"), firstSense?.antonyms)
    }

    @Test
    fun testBlankApiKeyReturnsNull() = runBlocking {
        val service = GeminiApiService(OkHttpClient(), json)
        val result = service.enrichWord(apiKey = "", term = "test")
        assertNull(result)
    }

    @Test
    fun testBlankApiKeyTestConnectionReturnsFalse() = runBlocking {
        val service = GeminiApiService(OkHttpClient(), json)
        val result = service.testConnection(apiKey = "")
        assertFalse(result)
    }

    @Test
    fun testNetworkErrorFallsBackGracefully() = runBlocking {
        val errorClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(401)
                    .message("Unauthorized")
                    .body("{}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val service = GeminiApiService(errorClient, json)
        val result = service.enrichWord(apiKey = "invalid_key", term = "test")
        assertNull(result)
    }

    @Test
    fun testExplainMistakeReturnsText() = runBlocking {
        val mockText = "Ephemeral means lasting for a very short time, like morning dew. Eternal means lasting forever, which is the exact opposite antonym. Remember: Ephemeral starts with E like Exit (leaving quickly)."
        val fakeResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [{ "text": "$mockText" }]
                  }
                }
              ]
            }
        """.trimIndent()

        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(fakeResponse.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val service = GeminiApiService(mockClient, json)
        val explanation = service.explainMistake(
            apiKey = "valid_key",
            term = "ephemeral",
            definition = "lasting for a very short time",
            userAnswer = "eternal"
        )

        assertNotNull(explanation)
        assertTrue(explanation!!.contains("lasting for a very short time"))
    }

    @Test
    fun testEvaluateSemanticAnswerAcceptsSynonym() = runBlocking {
        val fakeJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "```json\n{\n  \"isAcceptable\": true,\n  \"verdict\": \"SYNONYM\",\n  \"feedback\": \"Accepted as valid synonym: target was beneficent.\"\n}\n```"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(fakeJson.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val service = GeminiApiService(mockClient, json)
        val eval = service.evaluateSemanticAnswer(
            apiKey = "valid_key",
            targetTerm = "beneficent",
            targetDefinition = "doing good or causing good to be done",
            userTyped = "benevolent"
        )

        assertNotNull(eval)
        assertTrue(eval!!.isAcceptable)
        assertEquals("SYNONYM", eval.verdict)
        assertEquals("Accepted as valid synonym: target was beneficent.", eval.feedback)
    }

    @Test
    fun testGenerateStoryReturnsMarkdownStory() = runBlocking {
        val mockStory = "The morning brought an **ephemeral** mist over the valley. Sarah felt **resilient** as she hiked upward. Takeaway: Beauty is fleeting, but strength endures."
        val fakeResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [{ "text": "$mockStory" }]
                  }
                }
              ]
            }
        """.trimIndent()

        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(fakeResponse.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val service = GeminiApiService(mockClient, json)
        val story = service.generateStory(apiKey = "valid_key", words = listOf("ephemeral", "resilient"))

        assertNotNull(story)
        assertTrue(story!!.contains("**ephemeral**"))
        assertTrue(story.contains("**resilient**"))
    }

    @Test
    fun testSanitizeApiKeyStripsQuotesAndWhitespace() {
        val service = GeminiApiService(OkHttpClient(), json)
        assertEquals("AIzaSy12345", service.sanitizeApiKey("  AIzaSy12345  "))
        assertEquals("AIzaSy12345", service.sanitizeApiKey("\"AIzaSy12345\""))
        assertEquals("AIzaSy12345", service.sanitizeApiKey("'AIzaSy12345'"))
        assertEquals("AIzaSy12345", service.sanitizeApiKey(" \"AIzaSy12345\" "))
    }

    @Test
    fun testTestConnectionDetailedSuccess() = runBlocking {
        val fakeOkResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [{ "text": "OK" }]
                  }
                }
              ]
            }
        """.trimIndent()

        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                // Verify x-goog-api-key header is set
                assertEquals("test_key", chain.request().header("x-goog-api-key"))
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(fakeOkResponse.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val service = GeminiApiService(mockClient, json)
        val result = service.testConnectionDetailed("test_key")
        assertTrue(result is GeminiTestResult.Success)
        assertEquals("gemini-3.5-flash-lite", (result as GeminiTestResult.Success).modelUsed)
    }

    @Test
    fun testTestConnectionDetailed400ParsesErrorMessage() = runBlocking {
        val fakeErrorResponse = """
            {
              "error": {
                "code": 400,
                "message": "API key not valid. Please pass a valid API key.",
                "status": "INVALID_ARGUMENT"
              }
            }
        """.trimIndent()

        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(400)
                    .message("Bad Request")
                    .body(fakeErrorResponse.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val service = GeminiApiService(mockClient, json)
        val result = service.testConnectionDetailed("invalid_key")
        assertTrue(result is GeminiTestResult.Failure)
        val failure = result as GeminiTestResult.Failure
        assertEquals(400, failure.statusCode)
        assertTrue(failure.message.contains("API key not valid"))
    }

    @Test
    fun testTestConnectionDetailed404FallsBackToNextModel() = runBlocking {
        val fakeOkResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [{ "text": "OK" }]
                  }
                }
              ]
            }
        """.trimIndent()

        var callCount = 0
        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                callCount++
                val requestUrl = chain.request().url.toString()
                if (requestUrl.contains("gemini-3.5-flash-lite")) {
                    // First model returns 404
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(404)
                        .message("Not Found")
                        .body("{}".toResponseBody("application/json".toMediaType()))
                        .build()
                } else {
                    // Subsequent model succeeds
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(fakeOkResponse.toResponseBody("application/json".toMediaType()))
                        .build()
                }
            }
            .build()

        val service = GeminiApiService(mockClient, json)
        val result = service.testConnectionDetailed("valid_key")
        assertTrue(result is GeminiTestResult.Success)
        val success = result as GeminiTestResult.Success
        assertEquals("gemini-3.8-flash", success.modelUsed)
        assertEquals(2, callCount)
    }

    @Test
    fun testGenerateDailyWordsParsesSuccessfully() = runBlocking {
        val fakeResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "```json\n{\n  \"words\": [\n    {\n      \"term\": \"lucid\",\n      \"pos\": \"adjective\",\n      \"ipa\": \"/ˈluːsɪd/\",\n      \"definition\": \"Expressed clearly; easy to understand.\",\n      \"example\": \"She gave a lucid explanation of the complex theorem.\",\n      \"mnemonic\": \"Lucid sounds like liquid light—crystal clear.\"\n    },\n    {\n      \"term\": \"meticulous\",\n      \"pos\": \"adjective\",\n      \"ipa\": \"/məˈtɪkjələs/\",\n      \"definition\": \"Showing great attention to detail.\",\n      \"example\": \"He was meticulous about his code formatting.\",\n      \"mnemonic\": \"Meticulous meets every tiny detail.\"\n    },\n    {\n      \"term\": \"pragmatic\",\n      \"pos\": \"adjective\",\n      \"ipa\": \"/præɡˈmætɪk/\",\n      \"definition\": \"Dealing with things sensibly and realistically.\",\n      \"example\": \"We need a pragmatic approach to the problem.\",\n      \"mnemonic\": \"Pragmatic means practical.\"\n    },\n    {\n      \"term\": \"resilient\",\n      \"pos\": \"adjective\",\n      \"ipa\": \"/rɪˈzɪliənt/\",\n      \"definition\": \"Able to recover quickly from difficulties.\",\n      \"example\": \"She proved remarkably resilient.\",\n      \"mnemonic\": \"Resilient like a spring that bounces back.\"\n    },\n    {\n      \"term\": \"tangible\",\n      \"pos\": \"adjective\",\n      \"ipa\": \"/ˈtændʒəbl/\",\n      \"definition\": \"Perceptible by touch; clear and definite.\",\n      \"example\": \"We saw tangible results in two weeks.\",\n      \"mnemonic\": \"Tangible: you can touch it.\"\n    }\n  ]\n}\n```"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(fakeResponse.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val service = GeminiApiService(mockClient, json)
        val result = service.generateDailyWords("fake_key", "Intermediate (B1-B2)")

        assertNotNull(result)
        assertEquals(5, result?.size)
        assertEquals("lucid", result?.first()?.term)
        assertEquals("adjective", result?.first()?.pos)
        assertEquals("Expressed clearly; easy to understand.", result?.first()?.definition)
    }
}

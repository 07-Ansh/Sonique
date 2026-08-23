package com.sonique.lyrics.ai

import com.sonique.domain.data.model.metadata.Line
import com.sonique.domain.data.model.metadata.Lyrics
import com.sonique.ktorext.getEngine
import com.sonique.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

enum class AIHost(val value: String) {
    GEMINI("gemini"),
    OPENAI("openai"),
    CUSTOM_OPENAI("custom_openai");

    companion object {
        fun fromString(value: String): AIHost = when (value) {
            GEMINI.value -> GEMINI
            OPENAI.value -> OPENAI
            CUSTOM_OPENAI.value -> CUSTOM_OPENAI
            else -> GEMINI
        }
    }
}

@Serializable
private data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val response_format: ResponseFormat? = ResponseFormat("json_object"),
)

@Serializable
private data class ChatMessage(
    val role: String,
    val content: String,
)

@Serializable
private data class ResponseFormat(
    val type: String,
)

@Serializable
private data class ChatCompletionResponse(
    val choices: List<ChatChoice> = emptyList(),
)

@Serializable
private data class ChatChoice(
    val message: ChatMessageContent? = null,
)

@Serializable
private data class ChatMessageContent(
    val content: String? = null,
)

@Serializable
private data class TranslationResponse(
    val translations: Map<String, String> = emptyMap(),
)

class AiLyricsTranslator {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }

    private val httpClient =
        HttpClient(getEngine()) {
            install(ContentNegotiation) {
                json(
                    Json {
                        prettyPrint = false
                        isLenient = true
                        ignoreUnknownKeys = true
                        explicitNulls = false
                        encodeDefaults = true
                    },
                )
            }
        }

    suspend fun translateLyrics(
        inputLyrics: Lyrics,
        targetLanguage: String,
        host: AIHost,
        apiKey: String,
        customModelId: String? = null,
        customBaseUrl: String? = null,
        customHeaders: String? = null,
    ): Result<Lyrics> =
        runCatching {
            val lines = inputLyrics.lines ?: throw IllegalStateException("No lyrics lines to translate")

            val indexToWords = mutableMapOf<String, String>()
            lines.forEachIndexed { index, line ->
                val words = line.words.trim()
                if (words.isNotEmpty() && words != "♫") {
                    indexToWords[index.toString()] = words
                }
            }

            if (indexToWords.isEmpty()) {
                throw IllegalStateException("No translatable lyrics lines found")
            }

            val inputJson = json.encodeToString(MapSerializer(String.serializer(), String.serializer()), indexToWords)

            val endpointUrl =
                when (host) {
                    AIHost.GEMINI -> "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"
                    AIHost.OPENAI -> "https://api.openai.com/v1/chat/completions"
                    AIHost.CUSTOM_OPENAI -> {
                        val base = customBaseUrl?.trim()?.trimEnd('/') ?: "https://api.openai.com/v1"
                        if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
                    }
                }

            val model =
                if (!customModelId.isNullOrBlank()) {
                    customModelId
                } else {
                    when (host) {
                        AIHost.GEMINI -> "gemini-2.0-flash"
                        AIHost.OPENAI, AIHost.CUSTOM_OPENAI -> "gpt-4o"
                    }
                }

            val systemPrompt =
                "You are a song lyrics translation assistant.\n" +
                    "\n" +
                    "TASK:\n" +
                    "- You will receive a JSON object where keys are line indices and values are lyrics text.\n" +
                    "- FIRST, detect the dominant language of the input lyrics.\n" +
                    "- If the detected language is the SAME as the target language code, return an EMPTY \"translations\" object. Do NOT translate. Do NOT paraphrase.\n" +
                    "- Otherwise, translate ONLY the values to the target language.\n" +
                    "- When translating: keep ALL keys exactly the same, output MUST have the EXACT same number of entries as the input, do NOT merge/split/add/remove any entries, and preserve the song's meaning, tone, and emotion.\n" +
                    "\n" +
                    "OUTPUT:\n" +
                    "- A JSON object with the \"translations\" field containing the same keys mapped to translated values (or an empty object when the input is already in the target language)."

            val request =
                ChatCompletionRequest(
                    model = model,
                    messages =
                        listOf(
                            ChatMessage(role = "system", content = systemPrompt),
                            ChatMessage(
                                role = "user",
                                content = "Target language: $targetLanguage\nInput lyrics: $inputJson",
                            ),
                        ),
                    response_format = ResponseFormat("json_object"),
                )

            val httpResponse =
                httpClient.post(endpointUrl) {
                    header("Authorization", "Bearer $apiKey")
                    contentType(ContentType.Application.Json)

                    if (!customHeaders.isNullOrBlank()) {
                        try {
                            val parsed = json.decodeFromString<JsonObject>(customHeaders)
                            parsed.forEach { (key, value) ->
                                header(key, value.jsonPrimitive.content)
                            }
                        } catch (e: Exception) {
                            Logger.w(TAG, "Failed to parse custom headers as JSON: ${e.message}")
                        }
                    }

                    setBody(request)
                }

            val responseBody = httpResponse.bodyAsText()
            if (!httpResponse.status.isSuccess()) {
                throw IllegalStateException("AI service error (${httpResponse.status.value}): $responseBody")
            }

            val completion = json.decodeFromString<ChatCompletionResponse>(responseBody)
            val jsonContent =
                completion.choices.firstOrNull()?.message?.content
                    ?: throw IllegalStateException("No response message from AI provider")

            val cleanedJson =
                Regex("```json\\s*([\\s\\S]*?)```").find(jsonContent)?.groups?.get(1)?.value
                    ?: jsonContent.replace("```json", "").replace("```", "").trim()

            val translationResponse = json.decodeFromString<TranslationResponse>(cleanedJson)
            val translatedMap = translationResponse.translations
            if (translatedMap.isEmpty()) {
                throw IllegalStateException("Input lyrics are already in target language ($targetLanguage)")
            }

            val translatedLines =
                lines.mapIndexed { index, originalLine ->
                    val translatedWords = translatedMap[index.toString()]
                    if (translatedWords != null) {
                        Line(
                            startTimeMs = originalLine.startTimeMs,
                            endTimeMs = originalLine.endTimeMs,
                            words = translatedWords,
                            syllables = null,
                        )
                    } else {
                        Line(
                            startTimeMs = originalLine.startTimeMs,
                            endTimeMs = originalLine.endTimeMs,
                            words = originalLine.words,
                            syllables = originalLine.syllables,
                        )
                    }
                }

            val originalWords = inputLyrics.lines?.map { it.words } ?: emptyList()
            val transWords = translatedLines.map { it.words }
            val unchangedCount = originalWords.zip(transWords).count { (orig, trans) -> orig == trans }
            val translatableCount = originalWords.count { it.trim().isNotEmpty() && it.trim() != "♫" }

            if (translatableCount > 0 && unchangedCount.toFloat() / translatableCount > 0.8f) {
                throw IllegalStateException("Translation failed or returned same language")
            }

            Lyrics(
                error = false,
                lines = translatedLines,
                syncType = inputLyrics.syncType,
            )
        }

    companion object {
        private const val TAG = "AiLyricsTranslator"
    }
}

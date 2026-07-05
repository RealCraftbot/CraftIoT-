package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import com.example.BuildConfig
import android.util.Log

@JsonClass(generateAdapter = true)
data class GeminiContentRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiContentRequest
    ): GeminiResponse
}

object GeminiApiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val service: GeminiApiService = retrofit.create(GeminiApiService::class.java)

    suspend fun generateAssistantResponse(prompt: String, telemetryContext: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Developer Setup Alert: Gemini API key is missing. Please add your real GEMINI_API_KEY in the Secrets panel in AI Studio."
        }

        val systemPrompt = """
            You are CraftIoT AI, the premium virtual engineer and assistant for the CraftIoT platform.
            The user owns ESP32 edge devices (Robot Cars, Climate nodes, Drones, Smart Hydroponics).
            We have gathered the following real-time sensor metrics:
            $telemetryContext
            
            Answer the user's queries concisely, providing IoT diagnostic recommendations, C++ code improvements for ESP32/Arduino, automation flow advice, or custom actions.
            Always maintain a helpful, technical but highly approachable engineering persona. Speak directly and cleanly.
        """.trimIndent()

        val request = GeminiContentRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            ),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt)))
        )

        return try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "CraftIoT Error: Failed to receive an actionable response from the AI engine."
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "API failure", e)
            "Connectivity Error: ${e.localizedMessage ?: "Failed to reach the AI servers. Check your internet connection."}"
        }
    }
}

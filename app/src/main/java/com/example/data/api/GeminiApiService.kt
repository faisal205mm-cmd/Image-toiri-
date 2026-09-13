package com.example.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

enum class GeminiErrorType {
    MISSING_API_KEY,
    INVALID_API_KEY,
    PERMISSION_DENIED,
    QUOTA_EXCEEDED,
    NOT_FOUND,
    BAD_REQUEST,
    SERVER_ERROR,
    NETWORK_ERROR,
    UNKNOWN
}

data class GeminiErrorInfo(
    val errorType: GeminiErrorType,
    val title: String,
    val message: String,
    val httpCode: Int? = null,
    val rawMessage: String? = null
)

object GeminiErrorParser {
    fun parse(throwable: Throwable): GeminiErrorInfo {
        if (throwable is HttpException) {
            val code = throwable.code()
            val rawErrorBody = try {
                throwable.response()?.errorBody()?.string() ?: ""
            } catch (_: Exception) {
                ""
            }

            var serverMessage: String? = null
            try {
                if (rawErrorBody.contains("\"message\"")) {
                    val match = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(rawErrorBody)
                    serverMessage = match?.groupValues?.getOrNull(1)
                }
            } catch (_: Exception) {}

            return when (code) {
                400 -> {
                    if (serverMessage?.contains("API key not valid", ignoreCase = true) == true ||
                        serverMessage?.contains("API_KEY_INVALID", ignoreCase = true) == true ||
                        rawErrorBody.contains("API_KEY_INVALID", ignoreCase = true) ||
                        rawErrorBody.contains("INVALID_ARGUMENT", ignoreCase = true) && rawErrorBody.contains("key", ignoreCase = true)
                    ) {
                        GeminiErrorInfo(
                            errorType = GeminiErrorType.INVALID_API_KEY,
                            title = "Invalid API Key",
                            message = "The Gemini API key provided is invalid or malformed. Please verify your API key from Google AI Studio.",
                            httpCode = 400,
                            rawMessage = serverMessage ?: rawErrorBody
                        )
                    } else {
                        GeminiErrorInfo(
                            errorType = GeminiErrorType.BAD_REQUEST,
                            title = "Bad Request (HTTP 400)",
                            message = serverMessage ?: "Invalid parameters sent to Gemini API.",
                            httpCode = 400,
                            rawMessage = serverMessage ?: rawErrorBody
                        )
                    }
                }
                403 -> {
                    GeminiErrorInfo(
                        errorType = GeminiErrorType.PERMISSION_DENIED,
                        title = "Permission Denied (HTTP 403)",
                        message = "Access to Gemini API was denied. Ensure Generative Language API is enabled and your API key has appropriate permissions in Google AI Studio.",
                        httpCode = 403,
                        rawMessage = serverMessage ?: rawErrorBody
                    )
                }
                404 -> {
                    GeminiErrorInfo(
                        errorType = GeminiErrorType.NOT_FOUND,
                        title = "Model Not Found (HTTP 404)",
                        message = "The requested Gemini model is currently not available or endpoint moved.",
                        httpCode = 404,
                        rawMessage = serverMessage ?: rawErrorBody
                    )
                }
                429 -> {
                    GeminiErrorInfo(
                        errorType = GeminiErrorType.QUOTA_EXCEEDED,
                        title = "Rate Limit / Quota Exceeded (HTTP 429)",
                        message = serverMessage ?: "Google AI Studio rate limit reached (RESOURCE_EXHAUSTED). Free tier allows limited requests per minute.",
                        httpCode = 429,
                        rawMessage = serverMessage ?: rawErrorBody
                    )
                }
                in 500..599 -> {
                    GeminiErrorInfo(
                        errorType = GeminiErrorType.SERVER_ERROR,
                        title = "Server Error (HTTP $code)",
                        message = "Google Gemini servers are temporarily busy or unavailable. Please try again in a few moments.",
                        httpCode = code,
                        rawMessage = serverMessage ?: rawErrorBody
                    )
                }
                else -> {
                    GeminiErrorInfo(
                        errorType = GeminiErrorType.UNKNOWN,
                        title = "API Error (HTTP $code)",
                        message = serverMessage ?: "Received unexpected HTTP response from Gemini API.",
                        httpCode = code,
                        rawMessage = serverMessage ?: rawErrorBody
                    )
                }
            }
        } else if (throwable is UnknownHostException || throwable is ConnectException || throwable is SocketTimeoutException || throwable is IOException) {
            return GeminiErrorInfo(
                errorType = GeminiErrorType.NETWORK_ERROR,
                title = "Network Connection Error",
                message = "Unable to connect to Google Gemini. Please check your internet connection and try again.",
                httpCode = null,
                rawMessage = throwable.localizedMessage ?: "Network error"
            )
        } else {
            return GeminiErrorInfo(
                errorType = GeminiErrorType.UNKNOWN,
                title = "Unexpected Error",
                message = throwable.localizedMessage ?: "An unexpected error occurred while communicating with Gemini API.",
                httpCode = null,
                rawMessage = throwable.localizedMessage ?: "Unknown error"
            )
        }
    }
}

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    const val MODEL_FLASH = "gemini-3.5-flash"
    const val MODEL_PRO = "gemini-3.1-pro-preview"
    const val MODEL_FLASH_LITE = "gemini-3.1-flash-lite-preview"
    const val MODEL_IMAGE_PRIMARY = "gemini-2.5-flash-image"
    const val MODEL_IMAGE_LATEST = "gemini-3.1-flash-image-preview"
    const val MODEL_IMAGE_PRO = "gemini-3-pro-image-preview"
    const val MODEL_IMAGE_FALLBACK = "gemini-2.5-flash-image"
    const val MODEL_IMAGE = "gemini-2.5-flash-image"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}


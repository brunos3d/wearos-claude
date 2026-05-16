package com.brunos3d.wearosclaude.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Thin HTTP client targeting the local Fastify backend.
 *
 * Two endpoints matter:
 *  - `/ping`: anonymous liveness probe, used to disentangle "server is
 *     unreachable" from "server rejected my token".
 *  - `/usage`: the real payload, sent with the optional shared-secret header.
 */
class UsageApi(
    private val baseUrlProvider: () -> String,
    private val authTokenProvider: () -> String?,
) {

    /** Result of an `/usage` call along with the HTTP status code we saw. */
    sealed interface UsageResult {
        data class Ok(val payload: UsagePayload, val httpStatus: Int) : UsageResult
        data class Unauthorized(val httpStatus: Int) : UsageResult
        data class Failed(val message: String, val httpStatus: Int? = null) : UsageResult
    }

    /** Result of an anonymous `/ping`. */
    sealed interface PingResult {
        data class Ok(val authRequired: Boolean, val httpStatus: Int) : PingResult
        data class Failed(val message: String, val httpStatus: Int? = null) : PingResult
    }

    @Serializable
    private data class PingPayload(val pong: Boolean = false, val authRequired: Boolean = false)

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 6_000
            connectTimeoutMillis = 4_000
            socketTimeoutMillis = 6_000
        }
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 2)
            retryOnException(maxRetries = 2, retryOnTimeout = true)
            exponentialDelay(base = 2.0, maxDelayMs = 1500)
        }
        expectSuccess = false   // we want to inspect 401 ourselves
    }

    suspend fun fetchUsage(): UsageResult = try {
        val response: HttpResponse = client.get(urlFor("/usage")) {
            authTokenProvider()?.takeIf { it.isNotBlank() }?.let {
                header("x-wearos-claude-token", it)
            }
        }
        when {
            response.status == HttpStatusCode.Unauthorized ->
                UsageResult.Unauthorized(response.status.value)
            response.status.isSuccess() ->
                UsageResult.Ok(response.body<UsagePayload>(), response.status.value)
            else ->
                UsageResult.Failed("http ${response.status.value}", response.status.value)
        }
    } catch (t: Throwable) {
        UsageResult.Failed(t.classifyNetworkError())
    }

    suspend fun ping(): PingResult = try {
        val response: HttpResponse = client.get(urlFor("/ping"))
        if (response.status.isSuccess()) {
            val body = response.body<PingPayload>()
            PingResult.Ok(authRequired = body.authRequired, httpStatus = response.status.value)
        } else {
            PingResult.Failed("http ${response.status.value}", response.status.value)
        }
    } catch (t: Throwable) {
        PingResult.Failed(t.classifyNetworkError())
    }

    private fun urlFor(path: String): String {
        val base = baseUrlProvider().trimEnd('/')
        return "$base$path"
    }
}

/**
 * Maps the ratty exception messages OkHttp tosses out into something the
 * watch's tiny status strip can show without overflowing.
 */
private fun Throwable.classifyNetworkError(): String {
    val msg = this::class.simpleName.orEmpty().lowercase() + " " + (message ?: "")
    return when {
        msg.contains("timeout") -> "timeout"
        msg.contains("unreachable") -> "unreachable"
        msg.contains("refused") -> "refused"
        msg.contains("unknownhost") || msg.contains("unable to resolve") -> "dns"
        msg.contains("ssl") || msg.contains("trust") -> "tls"
        msg.contains("permission") -> "blocked"
        else -> "neterr"
    }
}

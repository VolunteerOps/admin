package com.example.admin.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.*

/** The platform Firebase SDK owns token refresh and secure storage. No tokens are retained here. */
fun interface FirebaseTokenProvider { suspend fun idToken(): String? }
data class ApiFailure(val status: Int, val code: String, val requestId: String?, val retryAfterSeconds: Long?) : Exception(code)
data class ApiEndpoint<T>(val method: HttpMethod, val path: String, val query: Map<String,String> = emptyMap(), val headers: Map<String,String> = emptyMap(), val rawText: Boolean = false, val decode: (JsonElement) -> T)
interface ApiTransport { suspend fun <T> execute(endpoint: ApiEndpoint<T>, body: JsonElement? = null): T }

class LiveApiTransport(
    private val tokens: FirebaseTokenProvider,
    origin: String = BackendConnection.apiOrigin,
    private val client: HttpClient = HttpClient { followRedirects = false; install(HttpTimeout) { requestTimeoutMillis = 15000 } },
) : ApiTransport {
    private val base = origin.trimEnd('/')
    init {
        require(Regex("^https://[A-Za-z0-9.-]+(?::[0-9]+)?$").matches(base) || Regex("^http://(?:localhost|127\\.0\\.0\\.1):[0-9]+$").matches(base))
    }
    override suspend fun <T> execute(endpoint: ApiEndpoint<T>, body: JsonElement?): T {
        require(endpoint.path.startsWith("/api/") && !endpoint.path.contains("..") && !endpoint.path.contains('?') && !endpoint.path.contains('#'))
        val token = tokens.idToken()
        val response = client.request(base + endpoint.path) {
            method = endpoint.method
            if (endpoint.method == HttpMethod.Post && listOf("/planning/weather", "/planning/resources", "/agent/chat").any { endpoint.path.endsWith(it) }) {
                timeout { requestTimeoutMillis = 135000; connectTimeoutMillis = 15000 }
            }
            endpoint.query.forEach { (key,value) -> require(Regex("^[a-z_]+$").matches(key)); parameter(key,value) }
            endpoint.headers.forEach { (key,value) -> require(key == "Idempotency-Key" && Regex("^[0-9a-fA-F-]{36}$").matches(value)); header(key,value) }
            header("Accept", "application/json")
            header("X-Requested-With", "VolunteerOps")
            if (token != null) { require(!token.contains('\r') && !token.contains('\n')); header("Authorization", "Bearer " + token) }
            if (body != null) { header("Content-Type", "application/json"); setBody(body.toString()) }
        }
        val text = response.bodyAsText()
        val json = if (text.isBlank()) JsonNull else runCatching { Json.parseToJsonElement(text) }.getOrElse { JsonNull }
        if (response.status.value !in 200..299) {
            val code = (json as? JsonObject)?.get("error")?.jsonPrimitive?.contentOrNull ?: "REQUEST_FAILED"
            throw ApiFailure(response.status.value, code, response.headers["X-Request-ID"], response.headers["Retry-After"]?.toLongOrNull())
        }
        return endpoint.decode(if(endpoint.rawText) JsonPrimitive(text) else json)
    }
    fun close() = client.close()
}

enum class RepositoryMode { LIVE, TEST }
interface FoundationRepository { val mode: RepositoryMode; suspend fun health(): Boolean }
class LiveFoundationRepository(private val api: ApiTransport) : FoundationRepository {
    override val mode = RepositoryMode.LIVE
    override suspend fun health() = api.execute(ApiEndpoint(HttpMethod.Get, "/api/health") { (it as? JsonObject)?.get("ok")?.jsonPrimitive?.booleanOrNull == true })
}
class TestFoundationRepository(private val available: Boolean) : FoundationRepository {
    override val mode = RepositoryMode.TEST
    override suspend fun health() = available
}
/** Explicit selection only; failures in LIVE propagate to the caller. */
fun foundationRepository(mode: RepositoryMode, live: () -> ApiTransport): FoundationRepository = when (mode) {
    RepositoryMode.LIVE -> LiveFoundationRepository(live())
    RepositoryMode.TEST -> TestFoundationRepository(false)
}

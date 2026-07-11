package com.apilium.aingle

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * Client for the AIngle Cortex REST API, the verifiable memory cortex for AI
 * agents. All methods are `suspend` functions and run the underlying HTTP call
 * off the calling thread.
 *
 * Example:
 * ```kotlin
 * val client = AIngleClient()
 * val id = client.remember("note", data = JsonPrimitive("hello"))
 * val hits = client.recall(text = "hello")
 * ```
 *
 * @param baseUrl base URL of the Cortex server. Defaults to `http://127.0.0.1:19090`.
 * @param token optional bearer token, sent as `Authorization: Bearer <token>`.
 * @param timeout per-request timeout. Defaults to 30 seconds.
 */
class AIngleClient(
    baseUrl: String = "http://127.0.0.1:19090",
    private val token: String? = null,
    private val timeout: Duration = 30.seconds
) {
    private val base: String = baseUrl.trimEnd('/')

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(timeout.toJavaDuration())
        .build()

    // -----------------------------------------------------------------------
    // Health & stats
    // -----------------------------------------------------------------------

    suspend fun health(): Health = get("/api/v1/health")

    suspend fun stats(): Stats = get("/api/v1/stats")

    // -----------------------------------------------------------------------
    // Memory
    // -----------------------------------------------------------------------

    suspend fun remember(
        entryType: String,
        data: JsonElement,
        tags: List<String> = emptyList(),
        importance: Double = 0.0,
        embedding: List<Double>? = null
    ): RememberResponse = post(
        "/api/v1/memory/remember",
        RememberRequest(entryType, data, tags, importance, embedding)
    )

    suspend fun recall(
        text: String? = null,
        tags: List<String> = emptyList(),
        entryType: String? = null,
        minImportance: Double? = null,
        limit: Int? = null
    ): List<RecallResult> = post(
        "/api/v1/memory/recall",
        RecallRequest(text, tags, entryType, minImportance, limit)
    )

    suspend fun search(
        embedding: List<Double>,
        k: Int,
        minSimilarity: Double = 0.0,
        entryType: String? = null,
        tags: List<String>? = null
    ): List<RecallResult> = post(
        "/api/v1/memory/search",
        VectorSearchRequest(embedding, k, minSimilarity, entryType, tags)
    )

    suspend fun memoryStats(): MemoryStats = get("/api/v1/memory/stats")

    suspend fun forget(id: String) {
        delete("/api/v1/memory/${encode(id)}")
    }

    // -----------------------------------------------------------------------
    // Triples
    // -----------------------------------------------------------------------

    suspend fun createTriple(subject: String, predicate: String, `object`: Value): Triple = post(
        "/api/v1/triples",
        CreateTripleRequest(subject, predicate, `object`)
    )

    suspend fun listTriples(
        subject: String? = null,
        predicate: String? = null,
        `object`: String? = null,
        limit: Int? = null,
        offset: Int? = null
    ): ListTriplesResponse {
        val query = buildQuery(
            "subject" to subject,
            "predicate" to predicate,
            "object" to `object`,
            "limit" to limit?.toString(),
            "offset" to offset?.toString()
        )
        return get("/api/v1/triples$query")
    }

    suspend fun getTriple(id: String): Triple = get("/api/v1/triples/${encode(id)}")

    suspend fun deleteTriple(id: String) {
        delete("/api/v1/triples/${encode(id)}")
    }

    suspend fun createTriples(triples: List<CreateTripleRequest>): BatchCreateResponse = post(
        "/api/v1/triples/batch",
        BatchCreateRequest(triples)
    )

    // -----------------------------------------------------------------------
    // Query
    // -----------------------------------------------------------------------

    suspend fun query(
        subject: String? = null,
        predicate: String? = null,
        `object`: Value? = null,
        limit: Int? = null
    ): QueryResponse = post(
        "/api/v1/query",
        QueryRequest(subject, predicate, `object`, limit)
    )

    suspend fun subjects(predicate: String? = null, limit: Int? = null): SubjectsResponse {
        val query = buildQuery("predicate" to predicate, "limit" to limit?.toString())
        return get("/api/v1/query/subjects$query")
    }

    suspend fun predicates(subject: String? = null, limit: Int? = null): PredicatesResponse {
        val query = buildQuery("subject" to subject, "limit" to limit?.toString())
        return get("/api/v1/query/predicates$query")
    }

    // -----------------------------------------------------------------------
    // HTTP plumbing
    // -----------------------------------------------------------------------

    private suspend inline fun <reified T> get(path: String): T =
        send(newRequest(path).GET().build(), serializer())

    private suspend inline fun <reified B, reified T> post(path: String, body: B): T {
        val payload = json.encodeToString(serializer<B>(), body)
        val request = newRequest(path)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
            .build()
        return send(request, serializer<T>())
    }

    private suspend fun delete(path: String) {
        val request = newRequest(path).DELETE().build()
        sendRaw(request)
    }

    private fun newRequest(path: String): HttpRequest.Builder {
        val builder = HttpRequest.newBuilder()
            .uri(URI.create("$base$path"))
            .timeout(timeout.toJavaDuration())
            .header("Accept", "application/json")
        if (token != null) {
            builder.header("Authorization", "Bearer $token")
        }
        return builder
    }

    private suspend fun <T> send(request: HttpRequest, deserializer: KSerializer<T>): T {
        val response = sendRaw(request)
        val body = response.body()
        return if (body.isBlank()) {
            json.decodeFromString(deserializer, "null")
        } else {
            json.decodeFromString(deserializer, body)
        }
    }

    private suspend fun sendRaw(request: HttpRequest): HttpResponse<String> {
        val response = withContext(Dispatchers.IO) {
            http.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .await()
        }
        val status = response.statusCode()
        if (status !in 200..299) {
            throw AIngleException(status, extractError(response.body(), status))
        }
        return response
    }

    private fun extractError(body: String, status: Int): String {
        if (body.isBlank()) return "HTTP $status"
        return try {
            val parsed = json.decodeFromString(ErrorBody.serializer(), body)
            parsed.message ?: parsed.error ?: body
        } catch (_: Exception) {
            body
        }
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")

    private fun buildQuery(vararg params: Pair<String, String?>): String {
        val parts = params.mapNotNull { (key, value) ->
            value?.let { "${encode(key)}=${encode(it)}" }
        }
        return if (parts.isEmpty()) "" else "?" + parts.joinToString("&")
    }
}

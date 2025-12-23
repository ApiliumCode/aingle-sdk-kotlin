package com.apilium.aingle

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for AIngle client.
 */
data class AIngleClientConfig(
    /** Node URL */
    val nodeUrl: String = "http://localhost:8080",
    /** WebSocket URL */
    val wsUrl: String = "ws://localhost:8081",
    /** Request timeout */
    val timeout: Duration = 30.seconds,
    /** Enable debug logging */
    val debug: Boolean = false
)

/**
 * AIngle Client for interacting with AIngle nodes.
 *
 * Example:
 * ```kotlin
 * val client = AIngleClient()
 *
 * // Create an entry
 * val hash = client.createEntry(mapOf("sensor" to "temp", "value" to 23.5))
 *
 * // Retrieve an entry
 * val entry = client.getEntry(hash)
 * ```
 */
class AIngleClient(
    private val config: AIngleClientConfig = AIngleClientConfig()
) : AutoCloseable {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = config.timeout.inWholeMilliseconds
        }
        install(WebSockets)
    }

    /**
     * Create a new entry in the DAG.
     *
     * @param data Entry payload
     * @return Hash of the created entry
     */
    suspend fun createEntry(data: Any): EntryHash {
        val response = httpClient.post("${config.nodeUrl}/api/v1/entries") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("data" to data))
        }

        val result: CreateEntryResponse = response.body()
        return result.hash
    }

    /**
     * Retrieve an entry by hash.
     *
     * @param hash Entry hash
     * @return Entry if found, null otherwise
     */
    suspend fun getEntry(hash: EntryHash): Entry? {
        val response = httpClient.get("${config.nodeUrl}/api/v1/entries/$hash")

        return when (response.status) {
            HttpStatusCode.NotFound -> null
            HttpStatusCode.OK -> response.body()
            else -> throw AIngleException(
                ErrorCode.NETWORK_ERROR,
                "Unexpected status: ${response.status}"
            )
        }
    }

    /**
     * Get node information.
     *
     * @return Node information
     */
    suspend fun getNodeInfo(): NodeInfo {
        val response = httpClient.get("${config.nodeUrl}/api/v1/info")
        return response.body()
    }

    /**
     * Subscribe to real-time updates.
     *
     * @return Flow of entries
     */
    fun subscribe(): Flow<Entry> = flow {
        httpClient.webSocket(config.wsUrl) {
            while (isActive) {
                when (val frame = incoming.receive()) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        val entry = json.decodeFromString<Entry>(text)
                        emit(entry)
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Close the client.
     */
    override fun close() {
        httpClient.close()
    }
}

@kotlinx.serialization.Serializable
private data class CreateEntryResponse(val hash: EntryHash)

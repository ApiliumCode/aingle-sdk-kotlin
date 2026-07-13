package com.apilium.aingle

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * A triple's `object` is an untagged JSON union. It serializes and deserializes
 * as the raw JSON value with no wrapper:
 *
 * - [Value.Text]    -> `"hello"`
 * - [Value.Integer] -> `42`
 * - [Value.Float]   -> `4.2`
 * - [Value.Bool]    -> `true`
 * - [Value.Node]    -> `{ "node": "http://example.org/thing" }`
 */
@Serializable(with = ValueSerializer::class)
sealed class Value {
    @Serializable
    data class Text(val value: String) : Value()

    @Serializable
    data class Integer(val value: Long) : Value()

    @Serializable
    data class Float(val value: Double) : Value()

    @Serializable
    data class Bool(val value: Boolean) : Value()

    /** A node reference (IRI), serialized as `{ "node": "<iri>" }`. */
    @Serializable
    data class Node(val node: String) : Value()

    companion object {
        fun of(value: String): Value = Text(value)
        fun of(value: Long): Value = Integer(value)
        fun of(value: Int): Value = Integer(value.toLong())
        fun of(value: Double): Value = Float(value)
        fun of(value: Boolean): Value = Bool(value)
        fun node(iri: String): Value = Node(iri)
    }
}

/**
 * Untagged serializer for [Value]. Reads and writes the raw JSON value with no
 * discriminator. Requires a JSON format (uses [JsonDecoder] / [JsonEncoder]).
 */
object ValueSerializer : KSerializer<Value> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.apilium.aingle.Value", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Value) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: error("Value can only be serialized with a JSON format")
        val element: JsonElement = when (value) {
            is Value.Text -> JsonPrimitive(value.value)
            is Value.Integer -> JsonPrimitive(value.value)
            is Value.Float -> JsonPrimitive(value.value)
            is Value.Bool -> JsonPrimitive(value.value)
            is Value.Node -> buildJsonObject { put("node", JsonPrimitive(value.node)) }
        }
        jsonEncoder.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): Value {
        val jsonDecoder = decoder as? JsonDecoder
            ?: error("Value can only be deserialized with a JSON format")
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonObject -> {
                val node = element["node"]?.jsonPrimitive?.contentOrNullSafe()
                    ?: error("Unsupported object Value: $element")
                Value.Node(node)
            }
            is JsonPrimitive -> primitiveToValue(element)
            else -> error("Unsupported Value: $element")
        }
    }

    private fun primitiveToValue(p: JsonPrimitive): Value {
        if (p.isString) return Value.Text(p.content)
        p.booleanOrNull?.let { return Value.Bool(it) }
        p.longOrNull?.let { return Value.Integer(it) }
        p.doubleOrNull?.let { return Value.Float(it) }
        // Fallback: treat as text (e.g. very large numbers).
        return Value.Text(p.content)
    }

    private fun JsonPrimitive.contentOrNullSafe(): String? =
        if (this is kotlinx.serialization.json.JsonNull) null else content
}

// ---------------------------------------------------------------------------
// Health & stats
// ---------------------------------------------------------------------------

@Serializable
data class ComponentHealth(
    val status: String,
    val message: String? = null
)

@Serializable
data class HealthComponents(
    val graph: ComponentHealth,
    val logic: ComponentHealth
)

@Serializable
data class Health(
    val status: String,
    val components: HealthComponents
)

@Serializable
data class GraphStats(
    @SerialName("triple_count") val tripleCount: Long,
    @SerialName("subject_count") val subjectCount: Long,
    @SerialName("predicate_count") val predicateCount: Long,
    @SerialName("object_count") val objectCount: Long
)

@Serializable
data class ServerStats(
    @SerialName("connected_clients") val connectedClients: Long,
    @SerialName("uptime_seconds") val uptimeSeconds: Long,
    val version: String
)

@Serializable
data class Stats(
    val graph: GraphStats,
    val server: ServerStats
)

// ---------------------------------------------------------------------------
// Memory
// ---------------------------------------------------------------------------

@Serializable
data class RememberRequest(
    @SerialName("entry_type") val entryType: String,
    val data: JsonElement,
    val tags: List<String> = emptyList(),
    val importance: Double = 0.0,
    val embedding: List<Double>? = null
)

@Serializable
data class RememberResponse(
    val id: String
)

@Serializable
data class RecallRequest(
    val text: String? = null,
    val tags: List<String> = emptyList(),
    @SerialName("entry_type") val entryType: String? = null,
    @SerialName("min_importance") val minImportance: Double? = null,
    val limit: Int? = null
)

@Serializable
data class VectorSearchRequest(
    val embedding: List<Double>,
    val k: Int,
    @SerialName("min_similarity") val minSimilarity: Double = 0.0,
    @SerialName("entry_type") val entryType: String? = null,
    val tags: List<String>? = null
)

@Serializable
data class RecallResult(
    val id: String,
    @SerialName("entry_type") val entryType: String,
    val data: JsonElement,
    val tags: List<String> = emptyList(),
    val importance: Double,
    val relevance: Double,
    val source: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("last_accessed") val lastAccessed: String,
    @SerialName("access_count") val accessCount: Long
)

@Serializable
data class MemoryStats(
    @SerialName("stm_count") val stmCount: Long,
    @SerialName("stm_capacity") val stmCapacity: Long,
    @SerialName("ltm_entity_count") val ltmEntityCount: Long,
    @SerialName("ltm_link_count") val ltmLinkCount: Long,
    @SerialName("total_memory_bytes") val totalMemoryBytes: Long
)

// ---------------------------------------------------------------------------
// Triples
// ---------------------------------------------------------------------------

@Serializable
data class Triple(
    val id: String? = null,
    val subject: String,
    val predicate: String,
    val `object`: Value,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class CreateTripleRequest(
    val subject: String,
    val predicate: String,
    val `object`: Value
)

@Serializable
data class ListTriplesResponse(
    val triples: List<Triple>,
    val total: Long,
    val limit: Int,
    val offset: Int
)

@Serializable
data class BatchCreateRequest(
    val triples: List<CreateTripleRequest>
)

@Serializable
data class BatchCreateResponse(
    val inserted: List<Triple>,
    val total: Long,
    val duplicates: Long
)

// ---------------------------------------------------------------------------
// Query
// ---------------------------------------------------------------------------

@Serializable
data class QueryRequest(
    val subject: String? = null,
    val predicate: String? = null,
    val `object`: Value? = null,
    val limit: Int? = null
)

@Serializable
data class QueryResponse(
    val matches: List<Triple>,
    val total: Long,
    val pattern: JsonElement? = null
)

@Serializable
data class SubjectsResponse(
    val subjects: List<String>,
    val total: Long
)

@Serializable
data class PredicatesResponse(
    val predicates: List<String>,
    val total: Long
)

// ---------------------------------------------------------------------------
// Error
// ---------------------------------------------------------------------------

/**
 * Thrown when the AIngle Cortex API returns a non-2xx response. [status] is the
 * HTTP status code and [message] is the server-provided error text when present.
 */
class AIngleException(
    val status: Int,
    override val message: String
) : RuntimeException("AIngle API error $status: $message")

/** Shape of the JSON error body returned on non-2xx responses. */
@Serializable
internal data class ErrorBody(
    val message: String? = null,
    val error: String? = null
)

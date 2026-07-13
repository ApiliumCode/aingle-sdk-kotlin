# AIngle SDK for Kotlin

Official Kotlin SDK for [AIngle](https://apilium.com), the verifiable memory cortex for AI agents.

AIngle Cortex is a semantic graph plus vector memory served over a REST API. This
SDK is a lightweight, idiomatic Kotlin client for that API. It uses the JDK 11+
`java.net.http.HttpClient` with kotlinx.coroutines and kotlinx.serialization, so
it stays dependency light.

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("com.apilium:aingle-sdk:0.2.0")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'com.apilium:aingle-sdk:0.2.0'
}
```

### Maven

```xml
<dependency>
    <groupId>com.apilium</groupId>
    <artifactId>aingle-sdk</artifactId>
    <version>0.2.0</version>
</dependency>
```

## Quick Start

Give an agent a memory, then recall it. Both calls are `suspend` functions, so
run them inside a coroutine.

```kotlin
import com.apilium.aingle.AIngleClient
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun main() = runBlocking {
    val client = AIngleClient() // defaults to http://127.0.0.1:19090

    // Remember something.
    val remembered = client.remember(
        entryType = "note",
        data = buildJsonObject {
            put("text", "The user prefers dark mode.")
            put("channel", "settings")
        },
        tags = listOf("preference", "ui"),
        importance = 0.8
    )
    println("Remembered as ${remembered.id}")

    // Recall it later by text and tags.
    val hits = client.recall(text = "dark mode", tags = listOf("preference"), limit = 5)
    hits.forEach { hit ->
        println("[${hit.relevance}] ${hit.entryType}: ${hit.data}")
    }
}
```

## Configuration

```kotlin
import com.apilium.aingle.AIngleClient
import kotlin.time.Duration.Companion.seconds

val client = AIngleClient(
    baseUrl = "http://127.0.0.1:19090",
    token = System.getenv("AINGLE_TOKEN"), // optional bearer token
    timeout = 30.seconds                   // optional per-request timeout
)
```

If a namespace token is set on the server, pass it as `token` and the SDK sends
`Authorization: Bearer <token>` on every request.

## Semantic graph (triples)

A triple is `subject`, `predicate`, `object`. The object is an untagged union
modeled by the `Value` sealed class, so a value can be a string, integer, float,
boolean, or a node reference (IRI).

```kotlin
import com.apilium.aingle.AIngleClient
import com.apilium.aingle.Value
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val client = AIngleClient()

    client.createTriple(
        subject = "http://example.org/ada",
        predicate = "http://example.org/knows",
        `object` = Value.node("http://example.org/babbage")
    )
    client.createTriple("http://example.org/ada", "http://example.org/born", Value.of(1815))

    val page = client.listTriples(subject = "http://example.org/ada", limit = 20)
    println("${page.total} triples")

    val matches = client.query(predicate = "http://example.org/knows")
    println("${matches.total} matches")
}
```

## API Reference

### Health and stats

| Method | Description |
|--------|-------------|
| `health()` | Server and component health. |
| `stats()` | Graph and server statistics. |

### Memory

| Method | Description |
|--------|-------------|
| `remember(entryType, data, tags, importance, embedding)` | Store a memory, returns `{ id }`. |
| `recall(text, tags, entryType, minImportance, limit)` | Recall memories, returns `List<RecallResult>`. |
| `search(embedding, k, minSimilarity, entryType, tags)` | Vector search, returns `List<RecallResult>`. |
| `memoryStats()` | Short-term and long-term memory counters. |
| `forget(id)` | Delete a memory by id. |

### Triples

| Method | Description |
|--------|-------------|
| `createTriple(subject, predicate, object)` | Insert one triple. |
| `createTriples(triples)` | Batch insert. |
| `listTriples(subject, predicate, object, limit, offset)` | List with optional filters. |
| `getTriple(id)` | Fetch one triple by id. |
| `deleteTriple(id)` | Delete a triple by id. |

### Query

| Method | Description |
|--------|-------------|
| `query(subject, predicate, object, limit)` | Pattern match over the graph. |
| `subjects(predicate, limit)` | Distinct subjects. |
| `predicates(subject, limit)` | Distinct predicates. |

## Error handling

Any non-2xx response is surfaced as a typed `AIngleException(status, message)`
where `status` is the HTTP status code and `message` is the server error text.

```kotlin
import com.apilium.aingle.AIngleClient
import com.apilium.aingle.AIngleException
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val client = AIngleClient()
    try {
        client.getTriple("does-not-exist")
    } catch (e: AIngleException) {
        println("API error ${e.status}: ${e.message}")
    }
}
```

## Requirements

Requires Java 11 or newer, since the transport is the built-in
`java.net.http.HttpClient`.

## Development

```bash
# Build
./gradlew build

# Run tests
./gradlew test

# Generate documentation
./gradlew dokkaHtml
```

## License

Apache-2.0, see [LICENSE](LICENSE)

## Links

- [AIngle Core](https://github.com/ApiliumCode/aingle)
- [Documentation](https://docs.apilium.com)

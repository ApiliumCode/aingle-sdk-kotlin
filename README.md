# AIngle SDK for Kotlin

Official Kotlin SDK for [AIngle](https://apilium.com) - the ultra-light distributed ledger for IoT devices.

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("com.apilium:aingle-sdk:0.1.0")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'com.apilium:aingle-sdk:0.1.0'
}
```

### Maven

```xml
<dependency>
    <groupId>com.apilium</groupId>
    <artifactId>aingle-sdk</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Quick Start

```kotlin
import com.apilium.aingle.AIngleClient

suspend fun main() {
    val client = AIngleClient()

    // Create an entry
    val hash = client.createEntry(mapOf(
        "type" to "sensor_reading",
        "value" to 23.5,
        "unit" to "celsius"
    ))
    println("Created entry: $hash")

    // Retrieve an entry
    val entry = client.getEntry(hash)
    println("Entry: $entry")

    // Get node info
    val info = client.getNodeInfo()
    println("Node version: ${info.version}")

    client.close()
}
```

## Subscribe to Real-time Updates

```kotlin
import com.apilium.aingle.AIngleClient
import kotlinx.coroutines.flow.collect

suspend fun main() {
    val client = AIngleClient()

    client.subscribe().collect { entry ->
        println("New entry: ${entry.hash}")
    }

    client.close()
}
```

## API Reference

### AIngleClient

| Method | Description |
|--------|-------------|
| `createEntry(data)` | Create a new entry |
| `getEntry(hash)` | Retrieve an entry by hash |
| `getNodeInfo()` | Get node information |
| `subscribe()` | Subscribe to real-time updates |
| `close()` | Close the client |

### Configuration

```kotlin
val config = AIngleClientConfig(
    nodeUrl = "http://localhost:8080",
    wsUrl = "ws://localhost:8081",
    timeout = 30.seconds,
    debug = false
)
val client = AIngleClient(config)
```

## Android Support

The SDK is compatible with Android API 21+. Add to your `build.gradle`:

```kotlin
android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
```

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

Apache-2.0 - see [LICENSE](LICENSE)

## Links

- [AIngle Core](https://github.com/ApiliumCode/aingle)
- [Documentation](https://docs.apilium.com)
- [Discord](https://discord.gg/apilium)

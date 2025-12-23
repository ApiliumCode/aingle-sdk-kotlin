package com.apilium.aingle

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Entry hash (Blake3 hash as hex string) */
typealias EntryHash = String

/** Agent public key (Ed25519 as hex string) */
typealias AgentPubKey = String

/**
 * Entry in the AIngle DAG
 */
@Serializable
data class Entry(
    /** Unique hash of this entry */
    val hash: EntryHash,
    /** Author's public key */
    val author: AgentPubKey,
    /** Parent entry hashes */
    val parents: List<EntryHash>,
    /** Entry payload */
    val data: JsonElement,
    /** Creation timestamp */
    val timestamp: Long,
    /** Sequence number for this author */
    val sequence: UInt,
    /** Entry signature */
    val signature: String
)

/**
 * Node information
 */
@Serializable
data class NodeInfo(
    /** Node ID */
    @SerialName("node_id")
    val nodeId: String,
    /** Node version */
    val version: String,
    /** Uptime in seconds */
    val uptime: Long,
    /** Number of entries */
    @SerialName("entries_count")
    val entriesCount: Long,
    /** Number of connected peers */
    @SerialName("peers_count")
    val peersCount: Int,
    /** Storage backend */
    @SerialName("storage_backend")
    val storageBackend: String,
    /** Enabled features */
    val features: List<String>
)

/**
 * Peer information
 */
@Serializable
data class PeerInfo(
    /** Peer ID */
    @SerialName("peer_id")
    val peerId: String,
    /** Peer address */
    val address: String,
    /** Connection quality (0-100) */
    val quality: Int,
    /** Last seen timestamp */
    @SerialName("last_seen")
    val lastSeen: Long,
    /** Peer's latest sequence number */
    @SerialName("latest_seq")
    val latestSeq: UInt
)

/**
 * Sync status
 */
@Serializable
data class SyncStatus(
    /** Whether sync is in progress */
    val syncing: Boolean,
    /** Number of entries to sync */
    val pending: Int,
    /** Last sync timestamp */
    @SerialName("last_sync")
    val lastSync: Long
)

/**
 * Error codes
 */
enum class ErrorCode {
    CONNECTION_FAILED,
    TIMEOUT,
    NOT_FOUND,
    INVALID_ENTRY,
    STORAGE_ERROR,
    NETWORK_ERROR,
    AUTH_ERROR
}

/**
 * SDK Exception
 */
class AIngleException(
    val code: ErrorCode,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

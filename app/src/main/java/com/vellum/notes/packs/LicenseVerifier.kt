package com.vellum.notes.packs

import android.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/** Payload inside a `vellum.license` file. */
@Serializable
data class LicensePayload(
    /** Pack ids, e.g. ["classroom","pdf","gesture"] or ["*"] for all. */
    val packs: List<String> = emptyList(),
    /** Unix epoch seconds; 0 = never expires. */
    val exp: Long = 0L,
)

private val licenseJson = Json { ignoreUnknownKeys = true }

/**
 * Offline Ed25519 license verification (no backend, no new deps — `java.security`
 * only). File format (`vellum.license` in the app files dir, or imported):
 *
 * ```
 * base64url(payloadJson) + "." + base64(signatureOverPayloadBytes)
 * ```
 *
 * Payload JSON: `{"packs":["classroom","pdf"],"exp":0}`.
 */
object LicenseVerifier {
    /**
     * Production public key (32 raw bytes, base64). The matching private key is
     * kept offline by the publisher and never committed.
     */
    const val PROD_PUBLIC_KEY_B64 = "baAd7qKbv0+JTsCBfFhlxufp3aJpYIhHwU1gYpD1NdA="

    fun prodPublicKey(): ByteArray = Base64.decode(PROD_PUBLIC_KEY_B64, Base64.DEFAULT)

    fun verify(payload: ByteArray, signature: ByteArray, publicKeyRaw32: ByteArray): Boolean {
        return try {
            // Raw 32-byte Ed25519 pubkey -> X.509 SubjectPublicKeyInfo for KeyFactory.
            val x509 = ed25519RawToX509(publicKeyRaw32)
            val key = KeyFactory.getInstance("Ed25519").generatePublic(X509EncodedKeySpec(x509))
            val sig = Signature.getInstance("Ed25519")
            sig.initVerify(key)
            sig.update(payload)
            sig.verify(signature)
        } catch (t: Throwable) {
            false
        }
    }

    /** Parses + verifies a license file's text; returns unlocked packs or empty set. */
    fun verifyLicenseText(text: String, publicKeyRaw32: ByteArray): Set<PackId> {
        val trimmed = text.trim()
        val dot = trimmed.lastIndexOf('.')
        if (dot <= 0) return emptySet()
        return try {
            val payloadB64 = trimmed.substring(0, dot)
            val sigB64 = trimmed.substring(dot + 1)
            val payload = Base64.decode(payloadB64, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
            val signature = Base64.decode(sigB64, Base64.DEFAULT)
            if (!verify(payload, signature, publicKeyRaw32)) return emptySet()
            val parsed = licenseJson.decodeFromString<LicensePayload>(
                payload.toString(Charsets.UTF_8),
            )
            if (parsed.exp != 0L && parsed.exp * 1000L < System.currentTimeMillis()) return emptySet()
            packsFromStrings(parsed.packs)
        } catch (t: Throwable) {
            emptySet()
        }
    }

    fun packsFromStrings(ids: List<String>): Set<PackId> {
        if (ids.contains("*") || ids.contains("all")) return PackId.entries.toSet()
        return ids.mapNotNull { PackId.fromId(it) }.toSet()
    }

    /** Encodes a payload the same way license files do (for tests/tools). */
    fun encodePayload(payload: LicensePayload): String {
        val bytes = licenseJson.encodeToString(payload).toByteArray(Charsets.UTF_8)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    internal fun ed25519RawToX509(raw32: ByteArray): ByteArray {
        require(raw32.size == 32) { "Ed25519 pubkey must be 32 bytes" }
        // SEQUENCE { SEQUENCE { OID 1.3.101.112 } BIT STRING <raw> }
        val prefix = byteArrayOf(
            0x30, 0x2A, 0x30, 0x05, 0x06, 0x03, 0x2B, 0x65, 0x70, 0x03, 0x21, 0x00,
        )
        return prefix + raw32
    }
}

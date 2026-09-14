package com.vellum.notes.packs

import android.content.Context
import java.io.File

/**
 * Unlock orchestration: Play Billing one-time IAP OR Ed25519 license file.
 * Offline-first: both paths write straight to [PackRepository] DataStore.
 */
class PackUnlocker(
    private val packs: PackRepository,
    private val billing: PackBilling,
    private val licensePublicKey: ByteArray = LicenseVerifier.prodPublicKey(),
) {
    /** License file name inside the app files dir (`vellum.license`). */
    fun licenseFile(context: Context): File = File(context.filesDir, "vellum.license")

    /** "Restore Purchases": marks every Play-owned pack unlocked. */
    suspend fun restorePurchases(): Set<PackId> {
        val owned = runCatching { billing.queryOwned() }.getOrDefault(emptySet())
        owned.forEach { packs.setUnlocked(it, true) }
        return owned
    }

    /** Imports a license file's text (picker bytes or files-dir read). */
    suspend fun importLicenseText(text: String): Set<PackId> {
        val unlocked = LicenseVerifier.verifyLicenseText(text, licensePublicKey)
        unlocked.forEach { packs.setUnlocked(it, true) }
        return unlocked
    }

    /** Reads `vellum.license` from the files dir if present and applies it. */
    suspend fun applyBundledLicense(context: Context): Set<PackId> {
        val file = licenseFile(context)
        if (!file.exists()) return emptySet()
        val text = runCatching { file.readText() }.getOrNull() ?: return emptySet()
        return importLicenseText(text)
    }

    /** Dev/test backdoor for offline verification without Play or license. */
    suspend fun unlockForTest(pack: PackId) {
        packs.setUnlocked(pack, true)
    }
}

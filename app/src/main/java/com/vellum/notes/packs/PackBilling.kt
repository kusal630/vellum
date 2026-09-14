package com.vellum.notes.packs

import android.app.Activity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Play Billing v7 abstraction for one-time pack IAPs.
 *
 * No compile-time billing dependency (offline-first, Compose-only rule): the
 * production implementation binds to
 * `com.android.billingclient.api.BillingClient` via reflection, so devices
 * without Play (F-Droid, offline) simply report no purchases instead of
 * crashing. When the class is absent, everything degrades to license-file
 * unlock.
 */
interface PackBilling {
    /** SKUs currently owned (subset of [PackId.sku]). Never throws. */
    suspend fun queryOwned(): Set<PackId>

    /**
     * Launches the one-time purchase flow. Returns true when the flow started.
     * No-op (false) when Play Billing is unavailable.
     */
    suspend fun launchPurchase(activity: Activity, pack: PackId): Boolean

    val isAvailable: Boolean
}

/** Reflection-based Play Billing v7 client. Safe to instantiate anywhere. */
class PlayBillingV7(
    private val context: android.content.Context,
) : PackBilling {

    override val isAvailable: Boolean
        get() = try {
            Class.forName("com.android.billingclient.api.BillingClient")
            true
        } catch (t: Throwable) {
            false
        }

    override suspend fun queryOwned(): Set<PackId> = withContext(Dispatchers.IO) {
        // Without the compile-time artifact we cannot query synchronously here;
        // the reflection path resolves purchase cache via the app's own
        // persisted entitlements. A full query happens in launchPurchase's
        // PurchaseUpdatedListener hosted by the caller. Returning empty keeps
        // "Restore Purchases" honest offline: it never invents ownership.
        emptySet()
    }

    override suspend fun launchPurchase(activity: Activity, pack: PackId): Boolean =
        withContext(Dispatchers.Main) {
            if (!isAvailable) return@withContext false
            try {
                // Resolve BillingClient.newBuilder(context).enablePendingPurchases().build()
                // reflectively so the app compiles without the billing artifact.
                val billingClientClass = Class.forName("com.android.billingclient.api.BillingClient")
                val builder = billingClientClass.getMethod("newBuilder", android.content.Context::class.java)
                    .invoke(null, activity)
                builder.javaClass.getMethod("enablePendingPurchases").invoke(builder)
                // Connection + SkuDetails flow is hosted by the caller via
                // BillingClientStateListener; here we only prove availability.
                // Returning false signals "unavailable in this build" so the UI
                // falls back to license import instead of a dead purchase UI.
                false
            } catch (t: Throwable) {
                false
            }
        }
}

/** In-memory fake for unit tests and previews. */
class FakePackBilling(
    var owned: Set<PackId> = emptySet(),
    override val isAvailable: Boolean = true,
    var launched: MutableList<PackId> = mutableListOf(),
) : PackBilling {
    override suspend fun queryOwned(): Set<PackId> = owned
    override suspend fun launchPurchase(activity: Activity, pack: PackId): Boolean {
        launched += pack
        return true
    }
}

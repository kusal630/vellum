package com.vellum.notes

import android.app.Application
import com.vellum.notes.data.NotesRepository
import com.vellum.notes.data.SettingsRepository
import com.vellum.notes.data.SyncRepository
import com.vellum.notes.data.createRepository
import com.vellum.notes.data.settingsDataStore
import com.vellum.notes.data.syncDataStore
import com.vellum.notes.packs.FakePackBilling
import com.vellum.notes.packs.PackBilling
import com.vellum.notes.packs.PackRepository
import com.vellum.notes.packs.PackUnlocker
import com.vellum.notes.packs.PlayBillingV7
import com.vellum.notes.packs.packsDataStore
import com.vellum.notes.input.PalmRejectionEngine
import com.vellum.notes.input.InputCapabilities
import com.vellum.notes.input.PalmRejectionSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Minimal application-scoped container for manual dependency injection.
 * Avoids heavy DI frameworks for the core graph while keeping systems testable.
 */
class AppContainer(private val application: Application) {

    private val dataStore = application.settingsDataStore()
    val settingsRepository = SettingsRepository(dataStore)
    val syncRepository = SyncRepository(application.syncDataStore())

    /** Revenue packs: offline-first entitlements + pack data (DataStore JSON). */
    val packRepository = PackRepository(application.packsDataStore())
    val packBilling: PackBilling by lazy {
        // Reflection-based Play Billing v7; degrades to unavailable (license
        // path) on F-Droid/offline builds with no billing artifact.
        runCatching { PlayBillingV7(application) }.getOrNull()
            ?: FakePackBilling(owned = emptySet(), isAvailable = false)
    }
    val packUnlocker: PackUnlocker by lazy {
        PackUnlocker(packRepository, packBilling)
    }

    /** Latest persisted settings, cached for synchronous reads by the input engine. */
    @Volatile
    var currentSettings: PalmRejectionSettings = PalmRejectionSettings()
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        appScope.launch {
            settingsRepository.settingsFlow.collect { currentSettings = it }
        }
    }

    val inputCapabilities: InputCapabilities by lazy {
        InputCapabilities.detect(application)
    }

    val palmRejectionSettingsFlow: Flow<PalmRejectionSettings> = settingsRepository.settingsFlow

    val palmRejectionEngine: PalmRejectionEngine by lazy {
        PalmRejectionEngine(inputCapabilities) { currentSettings }
    }

    val notesRepository: NotesRepository by lazy {
        createRepository(application)
    }
}
package com.vellum.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vellum.notes.data.NotesRepository
import com.vellum.notes.data.SettingsRepository
import com.vellum.notes.data.SyncRepository
import com.vellum.notes.input.InputCapabilities
import com.vellum.notes.input.PalmRejectionEngine
import com.vellum.notes.input.PalmRejectionMode
import com.vellum.notes.input.PalmRejectionSettings
import com.vellum.notes.input.PalmZoneMode
import com.vellum.notes.input.PalmZoneSide
import com.vellum.notes.input.SensitivityLevel
import com.vellum.notes.input.SmoothingMode
import com.vellum.notes.input.WritingPosture
import com.vellum.notes.input.withWritingPosture
import com.vellum.notes.ui.diagnostics.DiagnosticsScreen
import com.vellum.notes.ui.editor.EditorScreen
import com.vellum.notes.ui.home.HomeScreen
import com.vellum.notes.ui.reader.HighlightsScreen
import com.vellum.notes.ui.reader.ReadModeScreen
import com.vellum.notes.ui.sync.SyncSection
import com.vellum.notes.ui.theme.VellumTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object Routes {
    const val HOME = "home"
    const val EDITOR = "editor/{notebookId}"
    const val DIAGNOSTICS = "diagnostics"
    const val SETTINGS = "settings"
    const val READ = "read/{notebookId}?pageId={pageId}"
    const val HIGHLIGHTS = "highlights"

    fun editor(notebookId: Long) = "editor/$notebookId"
    fun read(notebookId: Long, pageId: Long? = null) =
        if (pageId == null) "read/$notebookId" else "read/$notebookId?pageId=$pageId"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VellumTheme {
                NotesAppRoot()
            }
        }
    }
}

@Composable
fun NotesAppRoot() {
    val navController = rememberNavController()
    val container = (androidx.compose.ui.platform.LocalContext.current.applicationContext as VellumApp).container

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                repository = container.notesRepository,
                onOpenNotebook = { navController.navigate(Routes.editor(it)) },
                onOpenDiagnostics = { navController.navigate(Routes.DIAGNOSTICS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenReader = { navController.navigate(Routes.read(it)) },
            )
        }
        composable(Routes.EDITOR) { backStackEntry ->
            val notebookId = backStackEntry.arguments?.getString("notebookId")?.toLongOrNull() ?: 0L
            EditorScreen(
                notebookId = notebookId,
                repository = container.notesRepository,
                capabilities = container.inputCapabilities,
                engine = container.palmRejectionEngine,
                settingsFlow = container.palmRejectionSettingsFlow,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.DIAGNOSTICS) {
            DiagnosticsScreen(
                engine = container.palmRejectionEngine,
                capabilities = container.inputCapabilities,
                settingsRepository = container.settingsRepository,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            val app = LocalContext.current.applicationContext as VellumApp
            SettingsScreen(
                settingsRepository = app.container.settingsRepository,
                syncRepository = app.container.syncRepository,
                notesRepository = app.container.notesRepository,
                onBack = { navController.popBackStack() },
                onOpenDiagnostics = { navController.navigate(Routes.DIAGNOSTICS) },
            )
        }
        composable(
            Routes.READ,
            arguments = listOf(
                navArgument("notebookId") { type = NavType.StringType },
                navArgument("pageId") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
        ) { backStackEntry ->
            val notebookId = backStackEntry.arguments?.getString("notebookId")?.toLongOrNull() ?: 0L
            val pageId = backStackEntry.arguments?.getString("pageId")?.toLongOrNull()
            ReadModeScreen(
                notebookId = notebookId,
                startPageId = pageId,
                repository = container.notesRepository,
                onBack = { navController.popBackStack() },
                onOpenHighlights = { navController.navigate(Routes.HIGHLIGHTS) },
            )
        }
        composable(Routes.HIGHLIGHTS) {
            HighlightsScreen(
                repository = container.notesRepository,
                onOpenHighlight = { notebookId, pageId ->
                    navController.navigate(Routes.read(notebookId, pageId)) {
                        // Reading replaces the review on the stack so Back returns home.
                        popUpTo(Routes.HIGHLIGHTS) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    syncRepository: SyncRepository,
    notesRepository: NotesRepository,
    onBack: () -> Unit,
    onOpenDiagnostics: () -> Unit = {},
) {
    val settings by settingsRepository.settingsFlow.collectAsState(initial = PalmRejectionSettings())
    val scope = rememberCoroutineScope()
    val app = LocalContext.current.applicationContext as VellumApp
    val packRepository = app.container.packRepository
    val packUnlocker = app.container.packUnlocker
    val packBilling = app.container.packBilling
    val entitlements by packRepository.entitlements.collectAsState(
        initial = com.vellum.notes.packs.PackEntitlements(),
    )
    var unlockPack by remember { mutableStateOf<com.vellum.notes.packs.PackId?>(null) }
    var unlockMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val licensePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use {
                        it.readBytes().toString(Charsets.UTF_8)
                    }
                }.getOrNull()
            }
            if (text == null) {
                unlockMessage = "Could not read that license file."
            } else {
                val unlocked = packUnlocker.importLicenseText(text)
                unlockMessage = if (unlocked.isEmpty()) "Invalid license file."
                else "Unlocked: ${unlocked.joinToString { it.title }}"
            }
        }
    }
    unlockPack?.let { pack ->
        com.vellum.notes.packs.ui.PackUnlockDialog(
            pack = pack,
            purchaseAvailable = packBilling.isAvailable,
            restoreMessage = null,
            licenseMessage = unlockMessage,
            onRestorePurchases = {
                scope.launch {
                    val owned = packUnlocker.restorePurchases()
                    unlockMessage = if (owned.isEmpty()) "No purchases found."
                    else "Restored: ${owned.joinToString { it.title }}"
                }
            },
            onImportLicense = { licensePicker.launch(arrayOf("*/*")) },
            onBuyPack = {
                scope.launch {
                    val activity = context as? android.app.Activity
                    if (activity != null) {
                        packBilling.launchPurchase(activity, pack)
                        val owned = packUnlocker.restorePurchases()
                        unlockMessage = if (owned.isEmpty()) "Purchase flow unavailable — use Import License."
                        else "Restored: ${owned.joinToString { it.title }}"
                    } else {
                        unlockMessage = "Purchase unavailable — use Import License."
                    }
                }
            },
            onDismiss = { unlockPack = null; unlockMessage = null },
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            SettingsContent(settings = settings, packEntitlements = entitlements,
                onPackClick = { unlockPack = it },
                onSettingChange = { newSettings ->
                scope.launch {
                    settingsRepository.updateSettings { 
                        this.mode = newSettings.mode 
                        this.sensitivity = newSettings.sensitivity
                        this.writingMaxMm = newSettings.writingMaxMm
                        this.fingerMaxMm = newSettings.fingerMaxMm
                        this.relaxedPalmMm = newSettings.relaxedPalmMm
                        this.writingHoldoffMs = newSettings.writingHoldoffMs
                        this.palmProximityMm = newSettings.palmProximityMm
                        this.smoothing = newSettings.smoothing
                        this.enableFingerWriting = newSettings.enableFingerWriting
                        this.autoConvertHandwritingToText = newSettings.autoConvertHandwritingToText
                        this.palmZone = newSettings.palmZone
                        // BUG 2 fix: the once-per-install coachmark flag must round-trip
                        // through Settings edits — previously omitted here, so any future
                        // divergence between the read snapshot and the latest value could
                        // lose a dismissal (or resurrect the bubble).
                        this.showPalmZoneCoachmark = newSettings.showPalmZoneCoachmark
                        this.palmRejectionEnabled = newSettings.palmRejectionEnabled
                        this.restingHandModeEnabled = newSettings.restingHandModeEnabled
                        this.palmSizeThresholdMm = newSettings.palmSizeThresholdMm
                        this.suspiciousSizeThresholdMm = newSettings.suspiciousSizeThresholdMm
                        this.movementPromoteThresholdMm = newSettings.movementPromoteThresholdMm
                        this.stationaryRestTimeMs = newSettings.stationaryRestTimeMs
                        this.candidateEvaluationWindowMs = newSettings.candidateEvaluationWindowMs
                        this.edgeMarginMm = newSettings.edgeMarginMm
                        this.clusterDistanceThresholdMm = newSettings.clusterDistanceThresholdMm
                        this.clusterStationaryThresholdMs = newSettings.clusterStationaryThresholdMs
                        this.palmGrowthCancelEnabled = newSettings.palmGrowthCancelEnabled
                        this.palmGrowthFactor = newSettings.palmGrowthFactor
                        this.minPromoteVelocityMmPerSec = newSettings.minPromoteVelocityMmPerSec
                        this.velocityWindowMs = newSettings.velocityWindowMs
                        this.sizeGrowthCancelThresholdMm = newSettings.sizeGrowthCancelThresholdMm
                        this.allowImmediateDrawWhenIsolated = newSettings.allowImmediateDrawWhenIsolated
                        this.debugOverlayEnabled = newSettings.debugOverlayEnabled
                        this.scribbleSensitivity = newSettings.scribbleSensitivity
                        this.writingPosture = newSettings.writingPosture
                        this.pressureAssistEnabled = newSettings.pressureAssistEnabled
                        this.autoEraseEnabled = newSettings.autoEraseEnabled
                        this.calibration = newSettings.calibration
                    }
                }
            },
            syncRepository = syncRepository,
            notesRepository = notesRepository,
            onOpenDiagnostics = onOpenDiagnostics,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    settings: PalmRejectionSettings,
    onSettingChange: (PalmRejectionSettings) -> Unit,
    syncRepository: SyncRepository,
    notesRepository: NotesRepository,
    onOpenDiagnostics: () -> Unit = {},
    packEntitlements: com.vellum.notes.packs.PackEntitlements = com.vellum.notes.packs.PackEntitlements(),
    onPackClick: (com.vellum.notes.packs.PackId) -> Unit = {},
) {
    var advancedExpanded by remember { mutableStateOf(false) }
    Column(Modifier.padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState())) {
        SettingsSectionTitle("1 · Writing feel")

        SettingSwitchRow(
            title = "Finger writing",
            explainer = "Write with a bare finger while palm rejection stays on; a second finger pans/zooms.",
            checked = settings.enableFingerWriting,
            onCheckedChange = { onSettingChange(settings.copy(enableFingerWriting = it)) },
        )

        Spacer(Modifier.height(16.dp))

        Text("Sensitivity", style = MaterialTheme.typography.bodyLarge)
        Text(
            "How small a contact must be to write. Low accepts bigger tips; High only accepts fine tips.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SensitivityLevel.entries.forEachIndexed { index, level ->
                SegmentedButton(
                    selected = SensitivityLevel.fromValue(settings.sensitivity) == level,
                    onClick = { onSettingChange(settings.copy(sensitivity = level.value)) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = SensitivityLevel.entries.size)
                ) { Text(level.label) }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("Smoothing", style = MaterialTheme.typography.bodyLarge)
        Text(
            "How much stroke wobble is ironed out. Higher is steadier but slightly laggier.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SmoothingMode.values().forEachIndexed { index, sm ->
                SegmentedButton(
                    selected = settings.smoothing == sm,
                    onClick = { onSettingChange(settings.copy(smoothing = sm)) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = SmoothingMode.values().size)
                ) { Text(sm.name) }
            }
        }

        Spacer(Modifier.height(16.dp))

        SettingSwitchRow(
            title = "Pressure assist",
            explainer = "A large contact at saturated pressure is the palm heel, not a finger.",
            checked = settings.pressureAssistEnabled,
            onCheckedChange = { onSettingChange(settings.copy(pressureAssistEnabled = it)) },
        )

        SettingsSectionDivider()
        SettingsSectionTitle("2 · Palm rejection")

        SettingSwitchRow(
            title = "Palm rejection",
            explainer = "Master switch for the whole palm/resting-hand pipeline; off means plain touch input.",
            checked = settings.palmRejectionEnabled,
            onCheckedChange = { onSettingChange(settings.copy(palmRejectionEnabled = it)) },
        )

        SettingSwitchRow(
            title = "Resting-hand mode",
            explainer = "Ignores resting fingers and palm clusters while a moving writer still draws.",
            checked = settings.restingHandModeEnabled,
            onCheckedChange = { onSettingChange(settings.copy(restingHandModeEnabled = it)) },
        )

        Spacer(Modifier.height(16.dp))

        Text("Rejection strictness", style = MaterialTheme.typography.bodyLarge)
        Text(
            "Preset that tunes the raw size, travel and velocity cutoffs below in one tap.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            PalmStrictnessPreset.entries.forEachIndexed { index, preset ->
                SegmentedButton(
                    selected = nearestPalmStrictness(settings) == preset,
                    onClick = { onSettingChange(preset.applyTo(settings)) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = PalmStrictnessPreset.entries.size)
                ) { Text(preset.label) }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("Mode", style = MaterialTheme.typography.bodyLarge)
        Text(
            "Overall input policy: Writing is strictest, Relaxed accepts larger contacts.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            PalmRejectionMode.values().forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = settings.mode == mode,
                    onClick = { onSettingChange(settings.copy(mode = mode)) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = PalmRejectionMode.values().size)
                ) { Text(mode.label) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            modeHelp(settings.mode),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))

        Text("Tune on-device", style = MaterialTheme.typography.bodyLarge)
        Text(
            "Open Diagnostics to see live contact sizes and calibrate your hardware.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = onOpenDiagnostics, modifier = Modifier.fillMaxWidth()) {
            Text("Open Diagnostics")
        }

        Spacer(Modifier.height(16.dp))

        Text("Writing hand", style = MaterialTheme.typography.bodyLarge)
        Text(
            "Makes edge margins handedness-aware and sets the default palm side.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            WritingPosture.entries.forEachIndexed { index, posture ->
                SegmentedButton(
                    selected = settings.writingPosture == posture,
                    onClick = { onSettingChange(settings.withWritingPosture(posture)) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = WritingPosture.entries.size)
                ) { Text(posture.label) }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text("Palm rest zone", style = MaterialTheme.typography.bodyLarge)
        Text(
            "Reserve a canvas area for your palm; touches inside it never draw or pan.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            PalmZoneMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = settings.palmZone.mode == mode,
                    onClick = { onSettingChange(settings.copy(palmZone = settings.palmZone.copy(mode = mode))) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = PalmZoneMode.entries.size)
                ) { Text(mode.label) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Off = automatic detection only · Auto = the zone follows where you write · " +
                "Manual = it stays where you place it",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (settings.palmZone.mode == PalmZoneMode.AUTO) {
            Spacer(Modifier.height(12.dp))
            Text("Palm side", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Which side of the pen your palm rests on.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                PalmZoneSide.entries.forEachIndexed { index, side ->
                    SegmentedButton(
                        selected = settings.palmZone.side == side,
                        onClick = { onSettingChange(settings.copy(palmZone = settings.palmZone.copy(side = side))) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = PalmZoneSide.entries.size)
                    ) { Text(side.label) }
                }
            }
        }

        if (settings.palmZone.enabled) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Current zone: ${"%.0f".format(settings.palmZone.widthMm)} × " +
                    "${"%.0f".format(settings.palmZone.heightMm)} mm",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SettingsSectionDivider()
        SettingsSectionTitle("3 · Handwriting")

        SettingSwitchRow(
            title = "Auto-convert handwriting to text (Coming soon)",
            explainer = "Reserved for a future update; currently has no effect.",
            checked = settings.autoConvertHandwritingToText,
            onCheckedChange = { onSettingChange(settings.copy(autoConvertHandwritingToText = it)) },
        )

        SettingSwitchRow(
            title = "Auto-erase on scribble",
            explainer = "A tight zigzag over ink erases instead of writing; the pen tool always wins.",
            checked = settings.autoEraseEnabled,
            onCheckedChange = { onSettingChange(settings.copy(autoEraseEnabled = it)) },
        )

        Spacer(Modifier.height(16.dp))

        Text("Scribble sensitivity", style = MaterialTheme.typography.bodyLarge)
        Text(
            "How deliberate the strike-out zigzag must be to erase the whole word.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            com.vellum.notes.input.ScribbleSensitivity.entries.forEachIndexed { index, s ->
                SegmentedButton(
                    selected = settings.scribbleSensitivity == s,
                    onClick = { onSettingChange(settings.copy(scribbleSensitivity = s)) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = com.vellum.notes.input.ScribbleSensitivity.entries.size),
                ) { Text(s.name.lowercase().replaceFirstChar { it.uppercase() }) }
            }
        }

        SettingsSectionDivider()
        SettingsSectionTitle("4 · Advanced")

        Text(
            "Fine-tune every raw mm/ms cutoff. The presets in §1–§3 write these same values.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        Card(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { advancedExpanded = !advancedExpanded }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Raw thresholds", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (advancedExpanded) "Tap to collapse the fine-tuning sliders."
                        else "Tap to expand all size, travel and timing cutoffs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    if (advancedExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (advancedExpanded) "Collapse" else "Expand",
                )
            }
            if (advancedExpanded) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    ThresholdSliderRow(
                        title = "Writing max size: ${"%.1f".format(settings.writingMaxMm)} mm",
                        explainer = "Largest contact still accepted as writing before sensitivity scaling.",
                        value = settings.writingMaxMm,
                        onValueChange = { onSettingChange(settings.copy(writingMaxMm = it)) },
                        valueRange = 4f..16f,
                        steps = 11,
                    )
                    ThresholdSliderRow(
                        title = "Finger max size: ${"%.1f".format(settings.fingerMaxMm)} mm",
                        explainer = "Largest contact treated as a normal finger gesture.",
                        value = settings.fingerMaxMm,
                        onValueChange = { onSettingChange(settings.copy(fingerMaxMm = it)) },
                        valueRange = 8f..22f,
                        steps = 13,
                    )
                    ThresholdSliderRow(
                        title = "Relaxed palm size: ${"%.1f".format(settings.relaxedPalmMm)} mm",
                        explainer = "Above this a contact counts as a palm in the most permissive mode.",
                        value = settings.relaxedPalmMm,
                        onValueChange = { onSettingChange(settings.copy(relaxedPalmMm = it)) },
                        valueRange = 18f..45f,
                        steps = 26,
                    )
                    ThresholdSliderRow(
                        title = "Palm proximity: ${"%.1f".format(settings.palmProximityMm)} mm",
                        explainer = "Large contacts this close to the writer are tolerated, not rejected.",
                        value = settings.palmProximityMm,
                        onValueChange = { onSettingChange(settings.copy(palmProximityMm = it)) },
                        valueRange = 0f..20f,
                        steps = 19,
                    )
                    ThresholdSliderRow(
                        title = "Palm cancel size: ${"%.1f".format(settings.palmSizeThresholdMm)} mm",
                        explainer = "A drawing pointer growing past this on sustained growth is cancelled.",
                        value = settings.palmSizeThresholdMm,
                        onValueChange = { onSettingChange(settings.copy(palmSizeThresholdMm = it)) },
                        valueRange = 12f..40f,
                        steps = 13,
                    )
                    ThresholdSliderRow(
                        title = "Suspicious size: ${"%.1f".format(settings.suspiciousSizeThresholdMm)} mm",
                        explainer = "Large enough to hint at a palm when other resting signals agree.",
                        value = settings.suspiciousSizeThresholdMm,
                        onValueChange = { onSettingChange(settings.copy(suspiciousSizeThresholdMm = it)) },
                        valueRange = 8f..28f,
                        steps = 19,
                    )
                    ThresholdSliderRow(
                        title = "Min stroke travel: ${"%.1f".format(settings.movementPromoteThresholdMm)} mm",
                        explainer = "How far a buffered contact must move to become the writing pointer.",
                        value = settings.movementPromoteThresholdMm,
                        onValueChange = { onSettingChange(settings.copy(movementPromoteThresholdMm = it)) },
                        valueRange = 1f..10f,
                        steps = 17,
                    )
                    ThresholdSliderRow(
                        title = "Edge margin: ${"%.1f".format(settings.edgeMarginMm)} mm",
                        explainer = "Contacts this close to a screen edge count as edge-resting evidence.",
                        value = settings.edgeMarginMm,
                        onValueChange = { onSettingChange(settings.copy(edgeMarginMm = it)) },
                        valueRange = 0f..60f,
                        steps = 29,
                    )
                    ThresholdSliderRow(
                        title = "Cluster distance: ${"%.1f".format(settings.clusterDistanceThresholdMm)} mm",
                        explainer = "Contacts within this gap belong to the same resting cluster.",
                        value = settings.clusterDistanceThresholdMm,
                        onValueChange = { onSettingChange(settings.copy(clusterDistanceThresholdMm = it)) },
                        valueRange = 20f..80f,
                        steps = 29,
                    )
                    ThresholdSliderRow(
                        title = "Palm growth factor: ${"%.1f".format(settings.palmGrowthFactor)}×",
                        explainer = "A writer is cancelled only after growing this multiple of its start size.",
                        value = settings.palmGrowthFactor,
                        onValueChange = { onSettingChange(settings.copy(palmGrowthFactor = it)) },
                        valueRange = 1.2f..3.5f,
                        steps = 22,
                    )
                    ThresholdSliderRow(
                        title = "Stroke velocity gate: ${settings.minPromoteVelocityMmPerSec.toInt()} mm/s",
                        explainer = "Minimum windowed speed for a contact to count as a deliberate stroke.",
                        value = settings.minPromoteVelocityMmPerSec,
                        onValueChange = { onSettingChange(settings.copy(minPromoteVelocityMmPerSec = it)) },
                        valueRange = 0f..400f,
                        steps = 39,
                    )
                    ThresholdSliderRow(
                        title = "Growth cancel size: ${"%.1f".format(settings.sizeGrowthCancelThresholdMm)} mm",
                        explainer = "A locked writer growing past this size is cancelled as a palm.",
                        value = settings.sizeGrowthCancelThresholdMm,
                        onValueChange = { onSettingChange(settings.copy(sizeGrowthCancelThresholdMm = it)) },
                        valueRange = 15f..50f,
                        steps = 34,
                    )
                    MsThresholdSliderRow(
                        title = "Writing hold-off: ${settings.writingHoldoffMs} ms",
                        explainer = "Lock-out after a pen lift before a new pointer can claim writing.",
                        value = settings.writingHoldoffMs,
                        onValueChange = { onSettingChange(settings.copy(writingHoldoffMs = it)) },
                        valueRange = 0L..300L,
                        steps = 14,
                    )
                    MsThresholdSliderRow(
                        title = "Rest confirm time: ${settings.stationaryRestTimeMs} ms",
                        explainer = "How long a contact may sit still before it is classified as resting.",
                        value = settings.stationaryRestTimeMs,
                        onValueChange = { onSettingChange(settings.copy(stationaryRestTimeMs = it)) },
                        valueRange = 100L..800L,
                        steps = 13,
                    )
                    MsThresholdSliderRow(
                        title = "Candidate window: ${settings.candidateEvaluationWindowMs} ms",
                        explainer = "Observation window before a still candidate is demoted to resting.",
                        value = settings.candidateEvaluationWindowMs,
                        onValueChange = { onSettingChange(settings.copy(candidateEvaluationWindowMs = it)) },
                        valueRange = 100L..600L,
                        steps = 9,
                    )
                    MsThresholdSliderRow(
                        title = "Cluster still time: ${settings.clusterStationaryThresholdMs} ms",
                        explainer = "How long cluster members must sit still to count as a resting hand.",
                        value = settings.clusterStationaryThresholdMs,
                        onValueChange = { onSettingChange(settings.copy(clusterStationaryThresholdMs = it)) },
                        valueRange = 100L..600L,
                        steps = 9,
                    )
                    MsThresholdSliderRow(
                        title = "Velocity window: ${settings.velocityWindowMs} ms",
                        explainer = "Sliding window used for velocity and continuity analysis (80–180 ms).",
                        value = settings.velocityWindowMs,
                        onValueChange = { onSettingChange(settings.copy(velocityWindowMs = it)) },
                        valueRange = 80L..180L,
                        steps = 9,
                    )
                    SettingSwitchRow(
                        title = "Palm growth cancel",
                        explainer = "Cancels a writer whose smoothed size grows into palm territory.",
                        checked = settings.palmGrowthCancelEnabled,
                        onCheckedChange = { onSettingChange(settings.copy(palmGrowthCancelEnabled = it)) },
                    )
                    SettingSwitchRow(
                        title = "Draw immediately when isolated",
                        explainer = "A lone small contact draws at once; off means observe-then-promote.",
                        checked = settings.allowImmediateDrawWhenIsolated,
                        onCheckedChange = { onSettingChange(settings.copy(allowImmediateDrawWhenIsolated = it)) },
                    )
                    SettingSwitchRow(
                        title = "Classification debug overlay",
                        explainer = "Draws every live contact with its classification color on the canvas.",
                        checked = settings.debugOverlayEnabled,
                        onCheckedChange = { onSettingChange(settings.copy(debugOverlayEnabled = it)) },
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Calibration", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Measured on the Diagnostics screen; pen/finger/palm maxima tune the cutoffs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    settings.calibration?.let { cal ->
                        Column(Modifier.fillMaxWidth()) {
                            cal.fingerMaxDimMm?.let { Text("Finger max: ${"%.1f".format(it)} mm") }
                            cal.penMaxDimMm?.let { Text("Pen max: ${"%.1f".format(it)} mm") }
                            cal.palmMaxDimMm?.let { Text("Palm max: ${"%.1f".format(it)} mm") }
                            if (cal.fingerMaxDimMm == null && cal.penMaxDimMm == null && cal.palmMaxDimMm == null) {
                                Text(
                                    "No measurements yet — open Diagnostics to calibrate.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        SettingsSectionDivider()
        com.vellum.notes.packs.ui.PacksCardGrid(
            entitlements = packEntitlements,
            onPackClick = onPackClick,
        )
        SettingsSectionDivider()
        SettingsSectionTitle("Device Sync")
        SyncSection(
            syncRepository = syncRepository,
            notesRepository = notesRepository,
        )
        SettingsSectionDivider()
        SupportSection()
    }
}

/**
 * Low/Med/High presets for the rejection strictness row in §2. Each preset writes the
 * same raw mm/ms thresholds that the collapsed Advanced group exposes, so presets and
 * fine-tuning stay in sync.
 */
private enum class PalmStrictnessPreset(
    val label: String,
    val palmSizeMm: Float,
    val suspiciousMm: Float,
    val travelMm: Float,
    val velocityMmPerSec: Float,
) {
    LOW("Low", palmSizeMm = 28f, suspiciousMm = 18f, travelMm = 4f, velocityMmPerSec = 150f),
    MEDIUM("Medium", palmSizeMm = 24f, suspiciousMm = 16f, travelMm = 3f, velocityMmPerSec = 120f),
    HIGH("High", palmSizeMm = 20f, suspiciousMm = 14f, travelMm = 2f, velocityMmPerSec = 90f);

    fun applyTo(settings: PalmRejectionSettings): PalmRejectionSettings =
        settings.copy(
            palmSizeThresholdMm = palmSizeMm,
            suspiciousSizeThresholdMm = suspiciousMm,
            movementPromoteThresholdMm = travelMm,
            minPromoteVelocityMmPerSec = velocityMmPerSec,
            sizeGrowthCancelThresholdMm = palmSizeMm * 1.15f,
        )
}

private fun nearestPalmStrictness(settings: PalmRejectionSettings): PalmStrictnessPreset =
    PalmStrictnessPreset.entries.minByOrNull {
        kotlin.math.abs(it.palmSizeMm - settings.palmSizeThresholdMm) +
            kotlin.math.abs(it.suspiciousMm - settings.suspiciousSizeThresholdMm) +
            kotlin.math.abs(it.travelMm - settings.movementPromoteThresholdMm) +
            kotlin.math.abs(it.velocityMmPerSec - settings.minPromoteVelocityMmPerSec) / 100f
    } ?: PalmStrictnessPreset.MEDIUM

@Composable
private fun SettingSwitchRow(
    title: String,
    explainer: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                explainer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ThresholdSliderRow(
    title: String,
    explainer: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
) {
    Spacer(Modifier.height(12.dp))
    Text(title, style = MaterialTheme.typography.bodyLarge)
    androidx.compose.material3.Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        explainer,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun MsThresholdSliderRow(
    title: String,
    explainer: String,
    value: Long,
    onValueChange: (Long) -> Unit,
    valueRange: LongRange,
    steps: Int,
) {
    Spacer(Modifier.height(12.dp))
    Text(title, style = MaterialTheme.typography.bodyLarge)
    androidx.compose.material3.Slider(
        value = value.toFloat(),
        onValueChange = { onValueChange(it.toLong()) },
        valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
        steps = steps,
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        explainer,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private val PalmRejectionMode.label: String
    get() = when (this) {
        PalmRejectionMode.WRITING -> "Writing"
        PalmRejectionMode.BALANCED -> "Balanced"
        PalmRejectionMode.RELAXED -> "Relaxed"
        PalmRejectionMode.STRICT -> "Strict"
    }

private val PalmZoneMode.label: String
    get() = when (this) {
        PalmZoneMode.OFF -> "Off"
        PalmZoneMode.AUTO -> "Auto"
        PalmZoneMode.MANUAL -> "Manual"
    }

private val PalmZoneSide.label: String
    get() = when (this) {
        PalmZoneSide.LEFT -> "Left"
        PalmZoneSide.RIGHT -> "Right"
    }

private val SensitivityLevel.label: String
    get() = when (this) {
        SensitivityLevel.LOW -> "Low"
        SensitivityLevel.MEDIUM -> "Medium"
        SensitivityLevel.HIGH -> "High"
    }

private val WritingPosture.label: String
    get() = when (this) {
        WritingPosture.RIGHT_HANDED -> "Right"
        WritingPosture.LEFT_HANDED -> "Left"
        WritingPosture.TWO_HANDED -> "Two"
    }

private fun modeHelp(mode: PalmRejectionMode): String = when (mode) {
    PalmRejectionMode.WRITING ->
        "Best for a stylus or one finger: a resting palm is always rejected, and a second " +
            "finger starts a two-finger pan/zoom."
    PalmRejectionMode.BALANCED ->
        "Default for mixed use: fingertips write, two-finger gestures work, and palm-sized " +
            "contacts are ignored."
    PalmRejectionMode.RELAXED ->
        "Accepts larger contacts for relaxed writing; palm rejection is weakest."
    PalmRejectionMode.STRICT ->
        "Most aggressive palm rejection: only small contacts can write."
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun SettingsSectionDivider() {
    Spacer(Modifier.height(24.dp))
    HorizontalDivider()
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun SupportSection() {
    val context = LocalContext.current
    val openUrl = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    Column(Modifier.padding(top = 8.dp)) {
        Text("Support Vellum", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "Vellum is free and offline — no ads, no tracking, no paywall. If it earns its place in your pocket, supporting it funds palm-rejection calibration devices, F-Droid and Play fees, and late-night ink-smoothing sessions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        ) {
            androidx.compose.material3.Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/kusal630"))
                    context.startActivity(Intent.createChooser(intent, "Support Vellum"))
                },
                modifier = Modifier.weight(1f),
            ) { Text("Buy Me a Coffee") }
            androidx.compose.material3.OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sponsors/kusal630"))
                    context.startActivity(Intent.createChooser(intent, "Support Vellum"))
                },
                modifier = Modifier.weight(1f),
            ) { Text("GitHub Sponsors") }
        }
        Spacer(Modifier.height(8.dp))
        androidx.compose.material3.OutlinedButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://liberapay.com/kusal630"))
                context.startActivity(Intent.createChooser(intent, "Support Vellum"))
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Liberapay") }
    }
}
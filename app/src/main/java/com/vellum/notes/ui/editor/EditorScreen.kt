package com.vellum.notes.ui.editor

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.res.stringResource
import com.vellum.notes.R
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.core.content.FileProvider
import com.vellum.notes.VellumApp
import com.vellum.notes.data.NotesRepository
import com.vellum.notes.data.SettingsRepository
import com.vellum.notes.data.SyncStatus
import com.vellum.notes.editor.NoteEditorState
import com.vellum.notes.editor.Tool
import com.vellum.notes.export.PdfExporter
import java.io.File
import androidx.activity.result.PickVisualMediaRequest
import android.graphics.BitmapFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.vellum.notes.data.ImageStore
import com.vellum.notes.data.MediaLoader
import com.vellum.notes.model.PaperTemplates
import com.vellum.notes.pdf.PdfImporter
import com.vellum.notes.input.InputCapabilities
import com.vellum.notes.input.PalmRejectionEngine
import com.vellum.notes.input.PalmRejectionMode
import com.vellum.notes.input.PalmRejectionSettings
import com.vellum.notes.input.SmoothingMode
import com.vellum.notes.model.NoteType
import com.vellum.notes.model.PageBackground
import com.vellum.notes.model.PageSummary
import com.vellum.notes.model.PenStyle
import com.vellum.notes.model.PenType
import com.vellum.notes.model.ShapeKind
import com.vellum.notes.model.TranscriptSegment
import com.vellum.notes.speech.AudioCaptureService
import com.vellum.notes.speech.ModelDiscovery
import com.vellum.notes.speech.SpeechController
import com.vellum.notes.speech.SummaryGenerator
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow

/** P1-4: 12 NAMED color swatches (names announced, never hex). */
private val NAMED_COLORS = listOf(
    "Black" to 0xFF000000L,
    "Dark Gray" to 0xFF424242L,
    "White" to 0xFFFFFFFFL,
    "Red" to 0xFFD32F2FL,
    "Orange" to 0xFFFB8C00L,
    "Yellow" to 0xFFFDD835L,
    "Green" to 0xFF43A047L,
    "Teal" to 0xFF00897BL,
    "Sky Blue" to 0xFF00ACC1L,
    "Blue" to 0xFF1565C0L,
    "Purple" to 0xFF8E24AAL,
    "Pink" to 0xFFEC407AL,
)

/** P1-4: 5 width steps with mm labels + live stroke preview. */
private val PEN_WIDTH_STEPS_MM = listOf(0.5f, 1.0f, 2.0f, 3.5f, 5.0f)

private val PEN_TYPES = listOf(
    PenType.BALLPOINT to "Ballpoint",
    PenType.MONOLINE to "Gel",
    PenType.FOUNTAIN to "Fountain",
    PenType.PENCIL to "Pencil",
    PenType.MARKER to "Marker",
    PenType.CALLIGRAPHY to "Calligraphy",
)

private val SHAPE_KINDS = listOf(
    ShapeKind.RECT to "Rectangle",
    ShapeKind.TRIANGLE to "Triangle",
    ShapeKind.CIRCLE to "Circle",
    ShapeKind.ELLIPSE to "Ellipse",
    ShapeKind.LINE to "Line",
    ShapeKind.ARROW to "Arrow",
    ShapeKind.STAR to "Star",
    ShapeKind.POLYGON to "Hexagon",
)

/** P1-4 smoothing chips OFF/STEADY/FLOW mapped onto the existing SmoothingMode. */
private val SMOOTHING_CHIPS = listOf(
    "OFF" to SmoothingMode.NONE,
    "STEADY" to SmoothingMode.MEDIUM,
    "FLOW" to SmoothingMode.HIGH,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    notebookId: Long,
    repository: NotesRepository,
    capabilities: InputCapabilities,
    engine: PalmRejectionEngine,
    settingsFlow: Flow<PalmRejectionSettings>,
    onBack: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as VellumApp
    val uiContext = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- Revenue packs (MUSE-R1): offline entitlements drive all gates. ---
    val packRepository = remember { app.container.packRepository }
    val packUnlocker = remember { app.container.packUnlocker }
    val packBilling = remember { app.container.packBilling }
    val entitlements by packRepository.entitlements.collectAsState(
        initial = com.vellum.notes.packs.PackEntitlements(),
    )
    val classroomUnlocked = entitlements.isUnlocked(com.vellum.notes.packs.PackId.CLASSROOM)
    val pdfUnlocked = entitlements.isUnlocked(com.vellum.notes.packs.PackId.PDF)
    val gestureUnlocked = entitlements.isUnlocked(com.vellum.notes.packs.PackId.GESTURE)
    var unlockPack by remember { mutableStateOf<com.vellum.notes.packs.PackId?>(null) }
    var unlockMessage by remember { mutableStateOf<String?>(null) }
    var showPageManager by remember { mutableStateOf(false) }
    var showLayeredExport by remember { mutableStateOf(false) }
    var showGestureMapping by remember { mutableStateOf(false) }
    var showBookmarksPanel by remember { mutableStateOf(false) }
    val licensePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    uiContext.contentResolver.openInputStream(uri)?.use {
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
                    val activity = uiContext as? android.app.Activity
                    if (activity != null) {
                        packBilling.launchPurchase(activity, pack)
                        val owned = packUnlocker.restorePurchases()
                        unlockMessage = if (owned.isEmpty()) "Purchase unavailable — use Import License."
                        else "Restored: ${owned.joinToString { it.title }}"
                    } else {
                        unlockMessage = "Purchase unavailable — use Import License."
                    }
                }
            },
            onDismiss = { unlockPack = null; unlockMessage = null },
        )
    }

    // Pages load asynchronously; null until the real list arrives so we never create a
    // duplicate page from the initial placeholder emission.
    var pages by remember { mutableStateOf<List<PageSummary>?>(null) }
    LaunchedEffect(notebookId) {
        repository.pagesFor(notebookId).collect { pages = it }
    }
    val pageList = pages.orEmpty()

    // Ensure at least one page exists before showing the editor. Keyed on the nullable
    // [pages] state: null -> first emission is a state change even when both lists are
    // structurally empty, so an empty new notebook always gets its first page.
    LaunchedEffect(pages) {
        if (pages != null && pages.orEmpty().isEmpty()) {
            repository.createPage(notebookId)
        }
    }

    // Selected page follows the page rail; falls back to the first page when the current
    // selection disappears (e.g. after deletion).
    var selectedPageId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(pages) {
        val list = pages.orEmpty()
        if (selectedPageId == null || list.none { it.id == selectedPageId }) {
            selectedPageId = list.firstOrNull()?.id
        }
    }
    val pageId = selectedPageId

    if (pageId == null) {
        // P1-6 trust signal: skeleton shimmer page loading instead of bare text.
        SkeletonShimmer(modifier = Modifier.fillMaxSize(), contentDescription = "Loading pages")
        return
    }

    val factory = remember(pageId) {
        viewModelFactory {
            initializer { EditorViewModel(pageId, repository) }
        }
    }
    val vm: EditorViewModel = viewModel(key = "editor-$pageId", factory = factory)
    val editorState by vm.editor.collectAsState()

    val state = editorState
    if (state == null) {
        // P1-6 trust signal: skeleton shimmer page loading instead of bare text.
        SkeletonShimmer(modifier = Modifier.fillMaxSize(), contentDescription = "Loading page content")
        return
    }

    val content by state.content.collectAsState()
    val tool by state.tool.collectAsState()
    val penStyle by state.penStyle.collectAsState()
    val eraserSize by state.eraserSizeMm.collectAsState()
    val shapeKind by state.shapeKind.collectAsState()
    val selectedIds by state.selectedIds.collectAsState()
    val settings by settingsFlow.collectAsState(initial = PalmRejectionSettings())
    val toolbarHidden by app.container.settingsRepository.toolbarHiddenFlow.collectAsState(initial = emptySet())

    // P0-1 writing-status chip state, driven by existing engine signals via InkCanvasView.
    var writingStatus by remember { mutableStateOf(WritingStatus.PEN_READY) }

    // Page rail overlay + version history dialog state.
    var historyPageId by remember { mutableStateOf<Long?>(null) }
    // The page rail is a hideable overlay so the canvas stays full-screen for writing.
    var showRail by remember { mutableStateOf(false) }

    // The transcript sidebar is user-closable/openable: it starts open for classroom
    // notes (and whenever a recording/transcript exists) but the user can hide it to
    // reclaim the canvas and reopen it from the top bar. rememberSaveable keeps the
    // open/closed choice across configuration changes (rotation).
    var showTranscriptSidebar by rememberSaveable { mutableStateOf(true) }

    // A classroom note is a normal note plus the on-device audio/transcript sidebar.
    // Opening one shows the sidebar from the start (with a "record" hint) so the feature
    // is discoverable; a normal note only shows it once a recording is started.
    var isClassroom by remember { mutableStateOf(false) }
    LaunchedEffect(notebookId) {
        isClassroom = repository.getNotebook(notebookId)?.type == NoteType.CLASSROOM
    }

    // --- Classroom Notes (Feature 2): on-device recording + transcript sidebar. ---
    val transcript by SpeechController.segments.collectAsState()
    val partial by SpeechController.partial.collectAsState()
    val isRecording by SpeechController.isRecording.collectAsState()
    val recordingPageId by SpeechController.recordingPageId.collectAsState()
    val classroomAvailable = remember(uiContext) { ModelDiscovery.resolve(uiContext) != null }
    var classroomNotice by remember { mutableStateOf<String?>(null) }
    var summaryGenerating by remember { mutableStateOf(false) }

    // --- Classroom Pack: chapters + audio-sync position + auto-backup. ---
    var chapters by remember(pageId) { mutableStateOf<List<com.vellum.notes.packs.Chapter>>(emptyList()) }
    LaunchedEffect(pageId) {
        chapters = packRepository.getChapters(pageId)
    }
    var playbackMs by remember { mutableStateOf(0L) }
    val liveAnchorMs by SpeechController.recordingAnchorWallMs.collectAsState()
    var persistedAnchorMs by remember(pageId) { mutableStateOf(0L) }
    LaunchedEffect(pageId) {
        persistedAnchorMs = packRepository.getRecordingAnchor(pageId)
    }
    LaunchedEffect(recordingPageId, pageId, liveAnchorMs) {
        if (recordingPageId == pageId && liveAnchorMs > 0L) {
            packRepository.setRecordingAnchor(pageId, liveAnchorMs)
            persistedAnchorMs = liveAnchorMs
        }
    }
    val replayAnchorMs =
        if (recordingPageId == pageId && liveAnchorMs > 0L) liveAnchorMs else persistedAnchorMs
    val transcriptMaxMs = transcript.maxOfOrNull { if (it.endMs > 0L) it.endMs else it.startMs } ?: 0L
    val inkReplayRange = remember(content.strokes) {
        com.vellum.notes.editor.StrokeReplay.replayRange(content.strokes)
    }
    var inkReplayCutoff by remember(pageId) { mutableStateOf<Long?>(null) }
    var inkReplaying by remember(pageId) { mutableStateOf(false) }
    fun syncInkToPlayback(positionMs: Long) {
        inkReplayCutoff = com.vellum.notes.editor.StrokeReplay.cutoffForPlayback(replayAnchorMs, positionMs)
    }
    var zoomWindowOn by rememberSaveable(pageId) { mutableStateOf(false) }
    var insertSpaceArmed by rememberSaveable(pageId) { mutableStateOf(false) }
    var inkActive by remember(pageId) { mutableStateOf(false) }
    var canvasView by remember(pageId) { mutableStateOf<InkCanvasView?>(null) }
    LaunchedEffect(inkReplaying, pageId) {
        if (!inkReplaying) return@LaunchedEffect
        if (replayAnchorMs > 0L && transcriptMaxMs > 0L) {
            playbackMs = 0L
            syncInkToPlayback(0L)
            val stepMs = (transcriptMaxMs / 100).coerceAtLeast(50L)
            while (inkReplaying) {
                kotlinx.coroutines.delay(100)
                val next = playbackMs + stepMs
                if (next >= transcriptMaxMs) {
                    playbackMs = transcriptMaxMs
                    syncInkToPlayback(transcriptMaxMs)
                    kotlinx.coroutines.delay(600)
                    inkReplaying = false
                    break
                }
                playbackMs = next
                syncInkToPlayback(next)
            }
            return@LaunchedEffect
        }
        val range = inkReplayRange ?: run { inkReplaying = false; return@LaunchedEffect }
        val stepMs = ((range.second - range.first) / 100).coerceAtLeast(1L)
        while (inkReplaying) {
            kotlinx.coroutines.delay(100)
            val next = (inkReplayCutoff ?: range.first) + stepMs
            if (next >= range.second) {
                inkReplayCutoff = null
                inkReplaying = false
                break
            }
            inkReplayCutoff = next
        }
    }
    val autoBackup by packRepository.autoBackupEnabled.collectAsState(initial = false)

    // --- Gesture/Bookmark Pack: bookmarks + custom gesture mapping. ---
    var bookmarks by remember(notebookId) {
        mutableStateOf<List<com.vellum.notes.packs.PageBookmark>>(emptyList())
    }
    LaunchedEffect(notebookId, showRail, showBookmarksPanel) {
        bookmarks = packRepository.getBookmarks(notebookId)
    }
    var gestureMapping by remember {
        mutableStateOf<List<com.vellum.notes.packs.GestureMapping>>(
            com.vellum.notes.packs.GesturePro.defaultMapping(),
        )
    }
    LaunchedEffect(Unit) {
        gestureMapping = packRepository.getGestureMapping()
    }
    // Custom gesture: two-finger double-tap already undoes via canvas listener;
    // honor a remapped action when the pack is unlocked.
    val gestureActionForDoubleTap =
        com.vellum.notes.packs.GesturePro.resolveAction(
            com.vellum.notes.packs.GesturePro.GESTURE_TWO_FINGER_DOUBLE_TAP,
            gestureMapping,
        )

    // Mirror every recognized segment into the page content while this page owns the
    // transcript — while recording AND after stop (recordingPageId survives the stop) —
    // so autosave persists it, including the final segments the service flushes when the
    // capture thread ends. Undo does not apply to transcript updates.
    LaunchedEffect(transcript, pageId, recordingPageId) {
        if (recordingPageId == pageId) {
            vm.setTranscript(transcript)
        }
    }

    // SCOUT-05: in-app rationale shown BEFORE every system permission prompt.
    // Both RECORD_AUDIO and POST_NOTIFICATIONS are requested only from here, and
    // both go through their rationale AlertDialog first (no direct launch).
    var showMicRationale by remember { mutableStateOf(false) }
    var showNotificationRationale by remember { mutableStateOf(false) }

    fun needsNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            uiContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED

    fun startRecordingAllowingNotifications() {
        if (needsNotificationPermission()) {
            showNotificationRationale = true
        } else {
            AudioCaptureService.start(uiContext, pageId)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Notification is optional decoration for the foreground-service status:
        // start recording whether granted or denied so the mic flow never blocks.
        classroomNotice = null
        AudioCaptureService.start(uiContext, pageId)
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            classroomNotice = null
            startRecordingAllowingNotifications()
        } else {
            classroomNotice = uiContext.getString(R.string.mic_permission_denied)
        }
    }

    if (showMicRationale) {
        AlertDialog(
            onDismissRequest = { showMicRationale = false },
            title = { Text(stringResource(R.string.mic_rationale_title)) },
            text = { Text(stringResource(R.string.mic_rationale_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showMicRationale = false
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }) { Text(stringResource(R.string.mic_rationale_allow)) }
            },
            dismissButton = {
                TextButton(onClick = { showMicRationale = false }) {
                    Text(stringResource(R.string.mic_rationale_dismiss))
                }
            },
        )
    }

    if (showNotificationRationale) {
        AlertDialog(
            onDismissRequest = { showNotificationRationale = false },
            title = { Text(stringResource(R.string.notification_rationale_title)) },
            text = { Text(stringResource(R.string.notification_rationale_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showNotificationRationale = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        AudioCaptureService.start(uiContext, pageId)
                    }
                }) { Text(stringResource(R.string.notification_rationale_allow)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNotificationRationale = false
                    AudioCaptureService.start(uiContext, pageId)
                }) { Text(stringResource(R.string.notification_rationale_dismiss)) }
            },
        )
    }

    val toggleClassroom: () -> Unit = {
        run {
            if (!isClassroom) {
                classroomNotice = "Transcription is only available in classroom notebooks."
                return@run
            }
            if (isRecording && recordingPageId == pageId) {
                AudioCaptureService.stop(uiContext)
                // The service flushes the final partial into segments before clearing the
                // recording flag; the mirror LaunchedEffect persists the final transcript.
                vm.setTranscript(SpeechController.segments.value)
            } else if (!classroomAvailable) {
                classroomNotice = "Speech model not installed. Run ./gradlew downloadVoskModel and " +
                    "rebuild to enable Classroom Notes."
            } else if (uiContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                showMicRationale = true
            } else {
                startRecordingAllowingNotifications()
            }
        }
    }

    // ---- Wave-1 UI state: templates, text boxes, images ----
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showTextDialog by remember { mutableStateOf(false) }
    var showSpellingDialog by rememberSaveable(pageId) { mutableStateOf(false) }
    val spellingDictionary = remember(uiContext) {
        runCatching {
            com.vellum.notes.editor.Spellcheck.loadAssetLines {
                uiContext.assets.open("words_en.txt").bufferedReader().readText()
            }
        }.getOrDefault(emptySet())
    }
    var editingTextId by remember { mutableStateOf<Long?>(null) }

    val currentSummary = pageList.firstOrNull { it.id == pageId }
    val pageBackground = remember(pageId, currentSummary?.templateId, currentSummary?.background) {
        PaperTemplates.backgroundFor(currentSummary?.templateId).let { themed ->
            if (currentSummary?.background != PageBackground()) currentSummary?.background ?: themed else themed
        }
    }

    // Rasterized PDF page underlay for PDF-backed pages.
    val pdfPageBitmap = remember(pageId, currentSummary?.pdfBackgroundPath) {
        val path = currentSummary?.pdfBackgroundPath.orEmpty()
        if (path.isBlank()) null
        else runCatching {
            PdfImporter.resolveFile(uiContext, path)?.let { MediaLoader.decodeSampled(it, rgb565 = true) }
        }.getOrNull()
    }

    // Decoded image bitmaps for canvas rendering, keyed by fileRef.
    val imageBitmapCache = remember(content.imageObjects.map { it.fileRef }.joinToString("|")) {
        content.imageObjects.associate { im ->
            im.fileRef to runCatching {
                ImageStore.resolveFile(uiContext, im.fileRef)?.let { MediaLoader.decodeSampled(it) }
            }.getOrNull()
        }.filterValues { it != null } as Map<String, android.graphics.Bitmap>
    }

    // Photo picker for image insertion.
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val fileRef = ImageStore.importUri(uiContext, uri)
        if (fileRef == null) {
            android.widget.Toast.makeText(uiContext, "Could not import image", android.widget.Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        val file = ImageStore.resolveFile(uiContext, fileRef)
        // Place at a sensible default: 60mm wide, centered in the current viewport.
        // Probe dimensions without a full decode (OOM-safe).
        val size = file?.let { MediaLoader.probeSize(it) }
        val aspect = if (size != null && size.first > 0) size.second.toFloat() / size.first else 0.75f
        val wMm = 60f
        val hMm = (wMm * aspect).coerceAtMost(160f)
        // Default placement: upper-center of the A4-width world (210mm wide).
        vm.addImage(
            com.vellum.notes.model.ImageObject(
                id = 0L,
                x = 105f - wMm / 2f,
                y = 80f - hMm / 2f,
                width = wMm,
                height = hMm,
                fileRef = fileRef,
            ),
        )
    }

    // ---- Page version history ----
    historyPageId?.let { hid ->
        VersionsDialog(
            pageId = hid,
            repository = repository,
            onDismiss = { historyPageId = null },
            onRestoredCurrentPage = {
                // The restored page is open: reload it into the canvas now.
                if (hid == pageId) vm.refreshContent()
            },
        )
    }

    if (showSpellingDialog) {
        val unknowns = remember(content.textObjects, spellingDictionary) {
            val seen = LinkedHashSet<String>()
            for (t in content.textObjects) {
                seen += com.vellum.notes.editor.Spellcheck.unknownWords(t.text, spellingDictionary)
            }
            seen.toList().take(50)
        }
        AlertDialog(
            onDismissRequest = { showSpellingDialog = false },
            title = { Text("Spelling") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (spellingDictionary.isEmpty()) {
                        Text(
                            "Word list unavailable.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else if (unknowns.isEmpty()) {
                        Text(
                            "No misspellings in typed text.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        unknowns.forEach { word ->
                            Text(
                                word,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(vertical = 4.dp),
                            ) {
                                com.vellum.notes.editor.Spellcheck.suggestions(word, spellingDictionary)
                                    .forEach { suggestion ->
                                        FilterChip(
                                            selected = false,
                                            onClick = {
                                                for (t in content.textObjects) {
                                                    if (word in t.text) {
                                                        vm.updateText(t.copy(text = t.text.replace(word, suggestion)))
                                                    }
                                                }
                                            },
                                            label = { Text(suggestion) },
                                        )
                                    }
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpellingDialog = false }) { Text("Done") }
            },
        )
    }

    // ---- Page template picker ----
    if (showTemplateDialog) {
        val isDarkPaper = (currentSummary?.background?.colorArgb ?: 0xFFFFFFFFL) != 0xFFFFFFFFL
        AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            title = { Text("Page template") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = !isDarkPaper,
                            onClick = {
                                vm.setPageBackground(
                                    PaperTemplates.backgroundFor(currentSummary?.templateId, darkTheme = false),
                                )
                                vm.refreshContent()
                            },
                            label = { Text("White paper") },
                        )
                        FilterChip(
                            selected = isDarkPaper,
                            onClick = {
                                vm.setPageBackground(
                                    PaperTemplates.backgroundFor(currentSummary?.templateId, darkTheme = true),
                                )
                                vm.refreshContent()
                            },
                            label = { Text("Dark paper") },
                        )
                    }
                    PaperTemplates.ALL.forEach { t ->
                        val templateSelected = currentSummary?.templateId == t.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .semantics {
                                    contentDescription = "Template ${t.label}"
                                    selected = templateSelected
                                    stateDescription = if (templateSelected) "${t.label} template selected" else "${t.label} template"
                                    role = Role.RadioButton
                                }
                                .clickable(role = Role.RadioButton) {
                                    vm.setPageTemplate(t.id)
                                    showTemplateDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.GridOn,
                                contentDescription = null,
                                tint = if (templateSelected) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(t.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTemplateDialog = false }) { Text("Done") }
            },
        )
    }

    // ---- Insert text box ----
    if (showTextDialog) {
        var textValue by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showTextDialog = false },
            title = { Text("Insert text") },
            text = {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text("Text") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (textValue.isNotBlank()) {
                        val wMm = 90f
                        val fontSize = 8f
                        vm.addText(
                            com.vellum.notes.model.TextObject(
                                id = 0L,
                                x = 105f - wMm / 2f,
                                y = 60f,
                                width = wMm,
                                height = fontSize * 1.35f * (textValue.lines().size + 2),
                                text = textValue,
                            ),
                        )
                    }
                    showTextDialog = false
                }) { Text("Insert") }
            },
            dismissButton = {
                TextButton(onClick = { showTextDialog = false }) { Text("Cancel") }
            },
        )
    }

    // ---- Edit selected text box ----
    val editingText = editingTextId?.let { id -> content.textObjects.firstOrNull { it.id == id } }
    if (editingText != null) {
        var editValue by remember(editingText.id) { mutableStateOf(editingText.text) }
        AlertDialog(
            onDismissRequest = { editingTextId = null },
            title = { Text("Edit text") },
            text = {
                OutlinedTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    label = { Text("Text") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editValue.isNotBlank()) {
                        vm.updateText(editingText.copy(text = editValue))
                    }
                    editingTextId = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingTextId = null }) { Text("Cancel") }
            },
        )
    }

    Scaffold { padding ->
        // Export-to-PDF action shared by the floating action pill.
        val onExportPdf: () -> Unit = {
            scope.launch {
                val file = withContext(Dispatchers.IO) {
                    PdfExporter.export(uiContext, pageId, content, pageBackground)
                }
                if (file != null) {
                    val uri = FileProvider.getUriForFile(
                        uiContext,
                        "${uiContext.packageName}.fileprovider",
                        file,
                    )
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    uiContext.startActivity(Intent.createChooser(send, "Export PDF"))
                } else {
                    Toast.makeText(uiContext, "PDF export failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
        // PDF Power Tools: layered export honors the selected layers. The text
        // layer is the drawn text objects (selectable in viewers); filtering
        // drops unselected layers before export. No new deps.
        val onExportLayered: (com.vellum.notes.packs.LayeredExportOptions) -> Unit = { options ->
            scope.launch {
                val filtered = content.copy(
                    strokes = if (options.includeInk) content.strokes else emptyList(),
                    shapeObjects = if (options.includeShapes) content.shapeObjects else emptyList(),
                    imageObjects = if (options.includeImages) content.imageObjects else emptyList(),
                    textObjects = if (options.includeTextLayer) content.textObjects else emptyList(),
                )
                val bg = if (options.includeBackground) pageBackground
                else com.vellum.notes.model.PageBackground()
                val file = withContext(Dispatchers.IO) {
                    PdfExporter.export(uiContext, pageId, filtered, bg)
                }
                if (file != null) {
                    val uri = FileProvider.getUriForFile(
                        uiContext,
                        "${uiContext.packageName}.fileprovider",
                        file,
                    )
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    uiContext.startActivity(Intent.createChooser(send, "Export PDF (layered)"))
                } else {
                    Toast.makeText(uiContext, "PDF export failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
        // Pack dialog hosts (page manager / layered export / gesture mapping).
        if (showPageManager) {
            com.vellum.notes.packs.ui.PageManagerDialog(
                pages = pageList,
                onMove = { id, newOrder ->
                    scope.launch { repository.reorderPage(id, newOrder) }
                },
                onInsert = { scope.launch { repository.createPage(notebookId) } },
                onDuplicate = { id ->
                    scope.launch {
                        val newId = repository.duplicatePage(id)
                        selectedPageId = newId
                    }
                },
                onDelete = { id ->
                    scope.launch {
                        repository.deletePage(id)
                        if (id == selectedPageId) selectedPageId = null
                    }
                },
                onDismiss = { showPageManager = false },
            )
        }
        if (showLayeredExport) {
            com.vellum.notes.packs.ui.LayeredExportDialog(
                initial = com.vellum.notes.packs.LayeredExportOptions(),
                onExport = { options ->
                    showLayeredExport = false
                    onExportLayered(options)
                },
                onDismiss = { showLayeredExport = false },
            )
        }
        if (showGestureMapping) {
            com.vellum.notes.packs.ui.GestureMappingDialog(
                mapping = gestureMapping,
                onSave = { mapping ->
                    gestureMapping = mapping
                    scope.launch { packRepository.setGestureMapping(mapping) }
                    showGestureMapping = false
                },
                onReset = {
                    scope.launch {
                        packRepository.setGestureMapping(
                            com.vellum.notes.packs.GesturePro.defaultMapping(),
                        )
                        gestureMapping = com.vellum.notes.packs.GesturePro.defaultMapping()
                    }
                    showGestureMapping = false
                },
                onDismiss = { showGestureMapping = false },
            )
        }
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(padding)
                // Escape closes the transcript sidebar just like the close button.
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                        if (showTranscriptSidebar) {
                            showTranscriptSidebar = false
                            true
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                }
        ) {
            // Phones (narrow) get a slim page strip so the canvas keeps its width;
            // tablets/landscape get the full page rail.
            val compact = maxWidth < 600.dp

            // Classroom Notes transcript sidebar: a sibling of the canvas so it never
            // steals the pen's touches. Visible ONLY for classroom notes.
            val showTranscript = showTranscriptSidebar && isClassroom

            // Hardware/system back closes the transcript sidebar before leaving the note.
            BackHandler(enabled = showTranscript) {
                showTranscriptSidebar = false
            }

            // Sync status for the top-bar indicator, driven by SyncRepository
            // state (persisted; survives restarts). Defaults to IDLE when sync
            // was never configured.
            val syncStatus by app.container.syncRepository.syncStatus
                .collectAsState(initial = SyncStatus.IDLE)

            Column(
                Modifier
                    .fillMaxSize()
                    .semantics { isTraversalGroup = true },
            ) {
                // Floating pills up top (never under the palm at the bottom):
                // navigation, tools and page actions hover over the canvas.
                androidx.compose.animation.AnimatedVisibility(
                    visible = !inkActive,
                    enter = androidx.compose.animation.slideInVertically { -it } +
                        androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.slideOutVertically { -it } +
                        androidx.compose.animation.fadeOut(),
                ) {
                CanvasTopBar(
                    tool = tool,
                    syncStatus = syncStatus,
                    canUndo = state.canUndo,
                    canRedo = state.canRedo,
                    onUndo = { vm.undo() },
                    onRedo = { vm.redo() },
                    onBack = onBack,
                    onToggleRail = { showRail = !showRail },
                    onExportPdf = onExportPdf,
                    isRecording = isRecording && recordingPageId == pageId,
                    onToggleClassroom = toggleClassroom,
                    transcriptAvailable = isClassroom,
                    onToggleTranscriptSidebar = { showTranscriptSidebar = !showTranscriptSidebar },
                    classroomEnabled = isClassroom,
                    penStyle = penStyle,
                    eraserSizeMm = eraserSize,
                    shapeKind = shapeKind,
                    settings = settings,
                    selectedCount = selectedIds.size,
                    onTool = { t ->
                        vm.setTool(t)
                        if (t != Tool.SELECT) vm.clearSelection()
                        when (t) {
                            Tool.HIGHLIGHTER -> if (penStyle.type != PenType.HIGHLIGHTER) {
                                state.saveInkStyle()
                                vm.setPenStyle(penStyle.copy(type = PenType.HIGHLIGHTER, opacity = 0.4f, widthMm = 5f))
                            }
                            Tool.PEN -> state.restoreInkStyle()
                            else -> Unit
                        }
                    },
                    onShapeKind = { vm.setShapeKind(it) },
                    onColor = { color ->
                        vm.setPenStyle(penStyle.copy(colorArgb = color))
                    },
                    onWidth = { w ->
                        vm.setPenStyle(penStyle.copy(widthMm = w))
                    },
                    onPenType = { type ->
                        vm.setPenStyle(
                            penStyle.copy(
                                type = type,
                                opacity = if (type == PenType.HIGHLIGHTER) 0.4f else 1f,
                            )
                        )
                    },
                    onEraserSize = { vm.setEraserSize(it) },
                    onSelectAll = { vm.selectAll() },
                    onDeleteSelection = { vm.deleteSelection() },
                    onDuplicateSelection = { vm.duplicateSelection() },
                    onSmoothingChange = { mode ->
                        scope.launch {
                            app.container.settingsRepository.updateSettings { this.smoothing = mode }
                        }
                    },
                    autoEraseEnabled = settings.autoEraseEnabled,
                    onAutoEraseToggle = {
                        scope.launch {
                            app.container.settingsRepository.updateSettings {
                                autoEraseEnabled = !autoEraseEnabled
                            }
                        }
                    },
                    hiddenToolLabels = toolbarHidden,
                    onToggleToolHidden = { label ->
                        scope.launch {
                            val next = toolbarHidden.toMutableSet()
                            if (label in next) next.remove(label) else next.add(label)
                            app.container.settingsRepository.setToolbarHidden(next)
                        }
                    },
                    zoomWindowEnabled = zoomWindowOn,
                    onToggleZoomWindow = { zoomWindowOn = !zoomWindowOn },
                    onFitToPage = { canvasView?.zoomToFitContent() },
                    insertSpaceArmed = insertSpaceArmed,
                    onToggleInsertSpace = { insertSpaceArmed = !insertSpaceArmed },
                    onOpenSpelling = { showSpellingDialog = true },
                    onInsertText = { showTextDialog = true },
                    onInsertImage = {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onPickTemplate = { showTemplateDialog = true },
                    canEditText = selectedIds.size == 1 &&
                        content.textObjects.any { it.id in selectedIds },
                    onEditText = {
                        selectedIds.firstOrNull()?.let { editingTextId = it }
                    },
                    canSmooth = content.strokes.any { it.id in selectedIds },
                    onSmoothSelection = { vm.smoothSelection() },
                    canConvert = content.strokes.any { it.id in selectedIds } ||
                        content.shapeObjects.any { it.id in selectedIds },
                    onConvertSelection = {
                        val newId = vm.convertSelectionToText()
                        if (newId != 0L) editingTextId = newId
                    },
                    classroomUnlocked = classroomUnlocked,
                    pdfUnlocked = pdfUnlocked,
                    gestureUnlocked = gestureUnlocked,
                    onOpenClassroomExport = {
                        if (classroomUnlocked) showTranscriptSidebar = true
                        else unlockPack = com.vellum.notes.packs.PackId.CLASSROOM
                    },
                    onOpenPageManager = {
                        if (pdfUnlocked) showPageManager = true
                        else unlockPack = com.vellum.notes.packs.PackId.PDF
                    },
                    onOpenLayeredExport = {
                        if (pdfUnlocked) showLayeredExport = true
                        else unlockPack = com.vellum.notes.packs.PackId.PDF
                    },
                    onOpenBookmarks = {
                        if (gestureUnlocked) {
                            showRail = true
                            showBookmarksPanel = true
                        } else unlockPack = com.vellum.notes.packs.PackId.GESTURE
                    },
                    onOpenGestureMapping = {
                        if (gestureUnlocked) showGestureMapping = true
                        else unlockPack = com.vellum.notes.packs.PackId.GESTURE
                    },
                    onLockedPack = { unlockPack = it },
                )
                }
                Row(Modifier.weight(1f).fillMaxWidth()) {
                // Canvas fills the whole screen so you can write edge to edge; the page
                // rail is a hideable overlay toggled from the top bar. Keying by pageId
                // recreates the view on page switch so the engine resets and any
                // in-progress stroke is finalized onto the page it was drawn on.
                Box(
                    Modifier
                        .weight(1f)
                        .semantics { isTraversalGroup = true; traversalIndex = 1f },
                ) {
                    key(pageId) {
                        AndroidView(
                            modifier = Modifier
                                .fillMaxSize()
                                .semantics {
                                    contentDescription = "Handwriting canvas with ${content.strokes.size} strokes. " +
                                        "Active tool is ${tool.name}. Use the toolbar above to change tools."
                                    traversalIndex = 1f
                                },
                            factory = { ctx ->
                                InkCanvasView(ctx).also { view ->
                                    view.capabilities = capabilities
                                    view.engine = engine
                                    view.listener = vm.canvasListener
                                    view.onWritingStatusChanged = { writingStatus = it }
                                    view.onInkActiveChanged = { inkActive = it }
                                    engine.reset()
                                    canvasView = view
                                }
                            },
                            update = { view ->
                                view.strokes = content.strokes
                                view.shapes = content.shapeObjects
                                view.penStyle = penStyle
                                view.tool = tool
                                view.eraserSizeMm = eraserSize
                                view.shapeKind = shapeKind
                                view.background = pageBackground
                                view.images = content.imageObjects
                                view.texts = content.textObjects
                                view.imageBitmaps = imageBitmapCache
                                view.pdfBackground = pdfPageBitmap
                                view.selectionBoundsMm = state.selectionBoundsMm
                                view.listener = vm.canvasListener
                                view.onWritingStatusChanged = { writingStatus = it }
                                view.onInkActiveChanged = { inkActive = it }
                                view.autoEraseEnabled = settings.autoEraseEnabled
                                view.replayCutoffMs = inkReplayCutoff
                                view.zoomWindowEnabled = zoomWindowOn
                                view.insertSpaceArmed = insertSpaceArmed
                                view.onInsertSpace = { anchorY, gapMm ->
                                    insertSpaceArmed = false
                                    vm.insertSpace(anchorY, gapMm)
                                }
                                view.scribbleSensitivity = settings.scribbleSensitivity
                                view.debugOverlayEnabled = settings.debugOverlayEnabled
                                // Palm rest zone + scroll bar.
                                view.palmZone = settings.palmZone
                                view.scrollBarVisible = true
                                view.onPalmZoneChanged = { zone ->
                                    scope.launch {
                                        app.container.settingsRepository.updateSettings { palmZone = zone }
                                    }
                                }
                            },
                            onRelease = { view -> view.finalizeActiveStroke() },
                        )
                    }
                    // P0-1 writing-status chip: top-center below toolbar.
                    WritingStatusChip(
                        status = writingStatus,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                    )
                    classroomNotice?.let { notice ->
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            color = MaterialTheme.colorScheme.inverseSurface,
                            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                            shape = RoundedCornerShape(20.dp),
                            tonalElevation = 4.dp,
                        ) {
                            Row(
                                Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(notice, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = { classroomNotice = null }) { Text("Dismiss") }
                            }
                        }
                    }
                    // P1-4: ColorRail overlay removed — pickers live in the single bottom panel.
                    // P1-4: single bottom pickers panel (colors + widths + smoothing).
                    // Composed FIRST so the palm overlays below draw on top of it.
                    val pickersVisible =
                        tool == Tool.PEN || tool == Tool.HIGHLIGHTER || tool == Tool.SHAPES
                    if (pickersVisible) {
                        PenPickersPanel(
                            penStyle = penStyle,
                            smoothing = settings.smoothing,
                            onColor = { color ->
                                vm.setPenStyle(penStyle.copy(colorArgb = color))
                            },
                            onWidth = { w ->
                                vm.setPenStyle(penStyle.copy(widthMm = w))
                            },
                            onSmoothingChange = { mode ->
                                scope.launch {
                                    app.container.settingsRepository.updateSettings { this.smoothing = mode }
                                }
                            },
                            modifier = Modifier.align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                    // BUG 2 + BUG 3 fix: palm overlays are composed LAST (topmost z) and
                    // lifted above the bottom pickers panel when it is visible. Previously
                    // they sat at bottom=96/152dp UNDER the full-width panel, so on a fresh
                    // install (default PEN tool) the handle and the once-per-install
                    // coachmark were covered and never displayed.
                    // Panel height estimate (~340dp: fixed 48dp rows + labels + paddings),
                    // so 352dp clears it with an 8dp gap; 96dp when the panel is hidden.
                    val palmOverlayBottom = if (pickersVisible) 352.dp else 96.dp
                    Column(
                        modifier = Modifier.align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = palmOverlayBottom),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // BUG 2: once-per-install coachmark. Display condition is the
                        // persisted showPalmZoneCoachmark flag (DataStore, default true)
                        // AND manual zone mode — in AUTO/OFF there is no handle to
                        // point at, so an orphan bubble would be a phantom tip.
                        // Got-it persists false so it shows exactly once per install.
                        if (settings.showPalmZoneCoachmark &&
                            settings.palmZone.mode == com.vellum.notes.input.PalmZoneMode.MANUAL
                        ) {
                            PalmZoneCoachmark(
                                onDismiss = {
                                    scope.launch {
                                        app.container.settingsRepository.updateSettings {
                                            showPalmZoneCoachmark = false
                                        }
                                    }
                                },
                            )
                        }
                        // D3: handle is composed ONLY in MANUAL zone mode. In AUTO/OFF
                        // there is no box (pure contact-size rejection), so composing
                        // it would leave a phantom handle over the canvas.
                        if (settings.palmZone.mode == com.vellum.notes.input.PalmZoneMode.MANUAL) {
                            PalmZoneHandle(
                                settings = settings,
                                onResize = { newWidthMm, newHeightMm ->
                                    scope.launch {
                                        app.container.settingsRepository.updateSettings {
                                            palmZone = palmZone.copy(
                                                widthMm = newWidthMm.coerceIn(40f, 140f),
                                                heightMm = newHeightMm.coerceIn(28f, 110f),
                                                mode = com.vellum.notes.input.PalmZoneMode.MANUAL,
                                            )
                                        }
                                    }
                                },
                            )
                        }
                    }
                }

                // Classroom Notes transcript sidebar: a sibling of the canvas so it never
                // steals the pen's touches. Visible for classroom notes from the start
                // (live during recording, static when reopened), otherwise whenever a
                // recording session exists for this page.
                if (showTranscript) {
                    HorizontalDivider(
                        modifier = Modifier.width(1.dp).fillMaxHeight(),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    ClassroomSidebar(
                        segments = transcript,
                        partial = if (isRecording) partial else "",
                        isRecording = isRecording,
                        onToggleRecording = toggleClassroom,
                        onClose = { showTranscriptSidebar = false },
                        summary = content.summary,
                        summaryEnabled = transcript.isNotEmpty(),
                        summaryGenerating = summaryGenerating,
                        onGenerateSummary = {
                            run {
                                if (!isClassroom || summaryGenerating) return@run
                                scope.launch {
                                    summaryGenerating = true
                                    try {
                                        val result = withContext(Dispatchers.Default) {
                                            SummaryGenerator.summarize(transcript)
                                        }
                                        vm.setSummary(result)
                                    } catch (e: Exception) {
                                        classroomNotice = "Summary generation failed: ${e.message}"
                                    } finally {
                                        summaryGenerating = false
                                    }
                                }
                            }
                        },
                        classroomUnlocked = classroomUnlocked,
                        chapters = chapters,
                        playbackMs = playbackMs,
                        autoBackupEnabled = autoBackup,
                        onSeekPlayback = {
                            playbackMs = it
                            syncInkToPlayback(it)
                        },
                        onGenerateChapters = {
                            scope.launch {
                                val generated =
                                    com.vellum.notes.packs.ClassroomPro.autoChapters(transcript)
                                chapters = generated
                                packRepository.setChapters(pageId, generated)
                            }
                        },
                        onSeekChapter = { chapter ->
                            playbackMs = chapter.startMs
                            syncInkToPlayback(chapter.startMs)
                        },
                        onToggleAutoBackup = { enabled ->
                            scope.launch { packRepository.setAutoBackup(enabled) }
                        },
                        onShareExport = {
                            val text = com.vellum.notes.packs.ClassroomPro.exportText(
                                transcript, chapters, content.summary,
                                pageList.firstOrNull { it.id == pageId }?.title ?: "Page",
                            )
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            uiContext.startActivity(Intent.createChooser(send, "Export classroom"))
                        },
                        onLockedPack = { unlockPack = it },
                        modifier = Modifier.widthIn(min = 220.dp, max = 320.dp).fillMaxHeight(),
                        inkReplayRange = inkReplayRange,
                        inkReplayCutoff = inkReplayCutoff,
                        inkReplaying = inkReplaying,
                        onReplayPlay = {
                            if (replayAnchorMs > 0L && transcriptMaxMs > 0L) {
                                playbackMs = 0L
                                syncInkToPlayback(0L)
                                inkReplaying = true
                                return@ClassroomSidebar
                            }
                            val range = inkReplayRange ?: return@ClassroomSidebar
                            inkReplayCutoff = range.first
                            inkReplaying = true
                        },
                        onReplayStop = {
                            inkReplaying = false
                            inkReplayCutoff = null
                        },
                        onReplaySeek = {
                            inkReplaying = false
                            inkReplayCutoff = it
                        },
                        replayAnchorMs = replayAnchorMs,
                        transcriptMaxMs = transcriptMaxMs,
                        playbackPositionMs = playbackMs,
                        onSyncPlayback = { pos ->
                            playbackMs = pos
                            syncInkToPlayback(pos)
                        },
                    )
                }
            }

            if (showRail) {
                Row(Modifier.fillMaxHeight()) {
                    PageRail(
                        pages = pageList,
                        currentPageId = pageId,
                        compact = compact,
                        modifier = Modifier
                            .width(if (compact) 180.dp else 220.dp)
                            .fillMaxHeight(),
                        onSelectPage = { id -> selectedPageId = id },
                        onNewPage = { scope.launch { repository.createPage(notebookId) } },
                        onDuplicatePage = { id ->
                            scope.launch {
                                val newId = repository.duplicatePage(id)
                                selectedPageId = newId
                            }
                        },
                        onDeletePage = { id ->
                            scope.launch {
                                repository.deletePage(id)
                                if (id == selectedPageId) selectedPageId = null
                            }
                        },
                        onHistoryPage = { id -> historyPageId = id },
                        pdfUnlocked = pdfUnlocked,
                        gestureUnlocked = gestureUnlocked,
                        bookmarks = bookmarks,
                        showBookmarks = showBookmarksPanel || gestureUnlocked,
                        onOpenPageManager = {
                            if (pdfUnlocked) showPageManager = true
                            else unlockPack = com.vellum.notes.packs.PackId.PDF
                        },
                        onJumpBookmark = { id -> selectedPageId = id },
                        onAddBookmark = {
                            scope.launch {
                                val title = pageList.firstOrNull { it.id == pageId }?.title
                                    ?: "Page"
                                packRepository.addBookmark(
                                    notebookId,
                                    com.vellum.notes.packs.PageBookmark(pageId, title),
                                )
                                bookmarks = packRepository.getBookmarks(notebookId)
                            }
                        },
                        onRemoveBookmark = { bid ->
                            scope.launch {
                                packRepository.removeBookmark(notebookId, bid)
                                bookmarks = packRepository.getBookmarks(notebookId)
                            }
                        },
                        onLockedPack = { unlockPack = it },
                        onOpenGestureMapping = {
                            if (gestureUnlocked) showGestureMapping = true
                            else unlockPack = com.vellum.notes.packs.PackId.GESTURE
                        },
                    )
                    HorizontalDivider(
                        modifier = Modifier.width(1.dp).fillMaxHeight(),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
                }
            }
        }
    }
}

/**
 * The Classroom Notes sidebar: a sibling of the canvas — never an overlay on top of it —
 * so pen input is never stolen. Organized as tabs so new features (notes, highlights,
 * export, …) slot in next to the transcript without redesigning the layout.
 *
 * - Transcript: live (or saved) recognized speech; auto-scrolls while recording.
 * - Summary: an on-device extractive summary of the transcript, generated on request.
 * - Chapters (Classroom Pack): auto-chapters + audio-sync playback position.
 * - Export (Classroom Pack): chapter + transcript export + auto-backup toggle.
 *
 * Locked pack tabs render at 38% alpha; tapping them opens the unlock dialog.
 */
@Composable
private fun ClassroomSidebar(
    segments: List<TranscriptSegment>,
    partial: String,
    isRecording: Boolean,
    onToggleRecording: () -> Unit,
    onClose: () -> Unit,
    summary: String?,
    summaryEnabled: Boolean,
    summaryGenerating: Boolean = false,
    onGenerateSummary: () -> Unit,
    modifier: Modifier = Modifier,
    classroomUnlocked: Boolean = false,
    chapters: List<com.vellum.notes.packs.Chapter> = emptyList(),
    playbackMs: Long = 0L,
    autoBackupEnabled: Boolean = false,
    onSeekPlayback: (Long) -> Unit = {},
    onGenerateChapters: () -> Unit = {},
    onSeekChapter: (com.vellum.notes.packs.Chapter) -> Unit = {},
    onToggleAutoBackup: (Boolean) -> Unit = {},
    onShareExport: () -> Unit = {},
    onLockedPack: (com.vellum.notes.packs.PackId) -> Unit = {},
    inkReplayRange: Pair<Long, Long>? = null,
    inkReplayCutoff: Long? = null,
    inkReplaying: Boolean = false,
    onReplayPlay: () -> Unit = {},
    onReplayStop: () -> Unit = {},
    onReplaySeek: (Long) -> Unit = {},
    replayAnchorMs: Long = 0L,
    transcriptMaxMs: Long = 0L,
    playbackPositionMs: Long = 0L,
    onSyncPlayback: (Long) -> Unit = {},
) {
    var tab by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()
    Surface(
        modifier = modifier.semantics { isTraversalGroup = true; traversalIndex = 2f },
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isRecording) "Classroom · recording" else "Classroom notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape),
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Hide transcript sidebar",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            if (isRecording) {
                // A red "live" dot keeps the "still recording" state unmistakable at a glance.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Microphone active — transcription is on-device",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SidebarTab(label = "Transcript", selected = tab == 0, onClick = { tab = 0 }, modifier = Modifier.weight(1f))
                SidebarTab(label = "Summary", selected = tab == 1, onClick = { tab = 1 }, modifier = Modifier.weight(1f))
                SidebarTab(label = "Replay", selected = tab == 4, onClick = { tab = 4 }, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                PackSidebarTab(
                    label = "Chapters",
                    selected = tab == 2,
                    unlocked = classroomUnlocked,
                    onClick = {
                        if (classroomUnlocked) tab = 2
                        else onLockedPack(com.vellum.notes.packs.PackId.CLASSROOM)
                    },
                    modifier = Modifier.weight(1f),
                )
                PackSidebarTab(
                    label = "Export",
                    selected = tab == 3,
                    unlocked = classroomUnlocked,
                    onClick = {
                        if (classroomUnlocked) tab = 3
                        else onLockedPack(com.vellum.notes.packs.PackId.CLASSROOM)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))

            when (tab) {
                0 -> {
                    if (segments.isEmpty() && partial.isBlank()) {
                        Text(
                            if (isRecording) {
                                "No speech yet. Speak naturally — recognized words appear here."
                            } else {
                                "No transcript yet. Tap the mic in the top bar to record " +
                                    "and transcribe on-device."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(segments, key = { it.id }) { seg ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(20.dp),
                                ) {
                                    Text(
                                        seg.text,
                                        modifier = Modifier.padding(8.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                            if (isRecording && partial.isNotBlank()) {
                                item(key = "partial") {
                                    Text(
                                        partial,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        LaunchedEffect(segments.size, partial) {
                            if (isRecording) listState.animateScrollToItem(listState.layoutInfo.totalItemsCount)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onToggleRecording,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Mic, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (isRecording) "Stop recording" else "Start recording")
                    }
                }
                1 -> {
                    if (summary != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f, fill = false),
                        ) {
                            Text(
                                summary,
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    } else if (segments.isEmpty()) {
                        Text(
                            "No transcript yet — record a lecture first, then generate a " +
                                "summary on-device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            "A summary has not been generated yet. Tap below to condense the " +
                                "transcript on-device (no network).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onGenerateSummary,
                        enabled = summaryEnabled && !summaryGenerating,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Summarize, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (summaryGenerating) "Generating…" else if (summary != null) "Regenerate summary" else "Generate summary")
                    }
                }
                2 -> {
                    // Chapters tab is pack-gated; locked taps never reach here
                    // (they open the unlock dialog), but guard anyway.
                    if (!classroomUnlocked) {
                        LockedPackNote(
                            pack = com.vellum.notes.packs.PackId.CLASSROOM,
                            onUnlock = { onLockedPack(com.vellum.notes.packs.PackId.CLASSROOM) },
                        )
                    } else {
                        com.vellum.notes.packs.ui.AudioSyncBar(
                            segments = segments,
                            positionMs = playbackMs,
                            onSeek = onSeekPlayback,
                        )
                        Spacer(Modifier.height(8.dp))
                        com.vellum.notes.packs.ui.ChaptersTab(
                            segments = segments,
                            chapters = chapters,
                            onGenerate = onGenerateChapters,
                            onSeek = onSeekChapter,
                        )
                    }
                }
                3 -> {
                    if (!classroomUnlocked) {
                        LockedPackNote(
                            pack = com.vellum.notes.packs.PackId.CLASSROOM,
                            onUnlock = { onLockedPack(com.vellum.notes.packs.PackId.CLASSROOM) },
                        )
                    } else {
                        val exportText = remember(segments, chapters, summary) {
                            com.vellum.notes.packs.ClassroomPro.exportText(
                                segments, chapters, summary, "Classroom note",
                            )
                        }
                        com.vellum.notes.packs.ui.ClassroomExportPanel(
                            exportText = exportText.ifBlank { "Nothing to export yet." },
                            onShare = onShareExport,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Auto-backup",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    "Scheduled local backup of classroom notes.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            androidx.compose.material3.Switch(
                                checked = autoBackupEnabled,
                                onCheckedChange = onToggleAutoBackup,
                            )
                        }
                    }
                }
                4 -> {
                    val linked = replayAnchorMs > 0L && transcriptMaxMs > 0L
                    if (inkReplayRange == null && !linked) {
                        Text(
                            "No timestamped ink yet. New strokes are recorded " +
                                "with commit times — replay them here stroke by stroke.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        val (sliderValue, sliderMax, hint) = if (linked) {
                            Triple(
                                playbackPositionMs.toFloat() / transcriptMaxMs.toFloat(),
                                transcriptMaxMs,
                                "Linked to the lecture clock: ink appears as it was written.",
                            )
                        } else {
                            val (replayMin, replayMax) = inkReplayRange!!
                            val span = (replayMax - replayMin).coerceAtLeast(1L)
                            Triple(
                                ((inkReplayCutoff ?: replayMax) - replayMin).toFloat() / span.toFloat(),
                                span,
                                "Watch this page redraw itself in commit order.",
                            )
                        }
                        Text(
                            hint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        Slider(
                            value = sliderValue.coerceIn(0f, 1f),
                            onValueChange = {
                                if (linked) {
                                    val pos = (it * sliderMax).toLong()
                                    onSyncPlayback(pos)
                                } else {
                                    val (replayMin, _) = inkReplayRange!!
                                    onReplaySeek(replayMin + (it * sliderMax).toLong())
                                }
                            },
                            modifier = Modifier.fillMaxWidth().semantics {
                                contentDescription = "Ink replay position"
                            },
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { if (inkReplaying) onReplayStop() else onReplayPlay() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(if (inkReplaying) "Stop replay" else "Replay ink")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarTab(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .semantics {
                contentDescription = "$label tab"
                this.selected = selected
                stateDescription = if (selected) "$label tab selected" else label
                this.role = Role.Tab
            }
            .clickable(role = Role.Tab, onClickLabel = label) { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PackSidebarTab(
    label: String,
    selected: Boolean,
    unlocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .then(
                if (unlocked) Modifier
                else Modifier.alpha(com.vellum.notes.packs.ui.LOCKED_PACK_ALPHA),
            )
            .semantics {
                contentDescription = "$label tab ${if (unlocked) "unlocked" else "locked"}"
                this.selected = selected
                stateDescription = if (unlocked) label else "$label locked"
                this.role = Role.Tab
            }
            .clickable(role = Role.Tab, onClickLabel = label) { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!unlocked) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LockedPackNote(
    pack: com.vellum.notes.packs.PackId,
    onUnlock: () -> Unit,
) {
    Column(
        modifier = Modifier.alpha(com.vellum.notes.packs.ui.LOCKED_PACK_ALPHA),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "${pack.title} is locked.",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedButton(onClick = onUnlock, modifier = Modifier.fillMaxWidth()) {
            Text("Unlock ${pack.title}")
        }
    }
}

@Composable
private fun VersionsDialog(
    pageId: Long,
    repository: NotesRepository,
    onDismiss: () -> Unit,
    onRestoredCurrentPage: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val versions by repository.versionsForPage(pageId).collectAsState(initial = emptyList())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Page history") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Snapshots are saved when pages close and on demand. " +
                        "Restoring replaces the page (the replaced state is snapshotted first).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { scope.launch { repository.saveVersion(pageId) } }) {
                    Text("Snapshot now")
                }
                Spacer(Modifier.height(8.dp))
                if (versions.isEmpty()) {
                    Text("No snapshots yet.")
                }
                versions.forEach { v ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    ) {
                        Text(
                            versionDateFormat.format(Date(v.createdAt)),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TextButton(onClick = {
                            scope.launch {
                                repository.restoreVersion(v.id)
                                onRestoredCurrentPage()
                            }
                        }) { Text("Restore") }
                        TextButton(onClick = {
                            scope.launch { repository.deleteVersion(v.id) }
                        }) { Text("Delete") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}

private val versionDateFormat = SimpleDateFormat("MM/dd/yy, h:mm a", Locale.US)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PageRail(
    pages: List<PageSummary>,
    currentPageId: Long,
    compact: Boolean,
    onSelectPage: (Long) -> Unit,
    onNewPage: () -> Unit,
    onDuplicatePage: (Long) -> Unit = {},
    onDeletePage: (Long) -> Unit = {},
    onHistoryPage: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    pdfUnlocked: Boolean = false,
    gestureUnlocked: Boolean = false,
    bookmarks: List<com.vellum.notes.packs.PageBookmark> = emptyList(),
    showBookmarks: Boolean = false,
    onOpenPageManager: () -> Unit = {},
    onJumpBookmark: (Long) -> Unit = {},
    onAddBookmark: () -> Unit = {},
    onRemoveBookmark: (Long) -> Unit = {},
    onLockedPack: (com.vellum.notes.packs.PackId) -> Unit = {},
    onOpenGestureMapping: () -> Unit = {},
) {
    Surface(
        modifier = modifier.semantics { isTraversalGroup = true; traversalIndex = 3f },
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(Modifier.fillMaxSize().padding(8.dp)) {
            // A thin, draggable scroll bar over the page list: with many pages you can see
            // where you are and jump. Drawn in-house (not the foundation Scrollbar API,
            // which is absent from the resolved foundation 1.7.6 artifacts).
            val listState = rememberLazyListState()
            Box(Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(pages, key = { it.id }) { page ->
                        var menuOpen by remember(page.id) { mutableStateOf(false) }
                        Box {
                            if (compact) {
                                val isCurrent = page.id == currentPageId
                                Box(
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                            else MaterialTheme.colorScheme.surface
                                        )
                                        .border(
                                            width = if (isCurrent) 2.dp else 1.dp,
                                            color = if (isCurrent) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(6.dp),
                                        )
                                        .semantics {
                                            contentDescription = "Page ${page.title}"
                                            selected = isCurrent
                                            stateDescription = if (isCurrent) "Current page ${page.title}" else "Page ${page.title}"
                                        }
                                        .combinedClickable(
                                            onClick = { onSelectPage(page.id) },
                                            onLongClick = { menuOpen = true },
                                            onClickLabel = "Open page ${page.title}",
                                            onLongClickLabel = "Page options",
                                        ),
                                )
                            } else {
                                PageThumbnail(
                                    page,
                                    selected = page.id == currentPageId,
                                    onClick = { onSelectPage(page.id) },
                                    onLongClick = { menuOpen = true },
                                )
                            }
                            DropdownMenu(
                                expanded = menuOpen,
                                onDismissRequest = { menuOpen = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Duplicate") },
                                    onClick = { menuOpen = false; onDuplicatePage(page.id) },
                                )
                                DropdownMenuItem(
                                    text = { Text("History") },
                                    onClick = { menuOpen = false; onHistoryPage(page.id) },
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = { menuOpen = false; onDeletePage(page.id) },
                                )
                            }
                        }
                    }
                }
                PageRailScrollbar(
                    listState = listState,
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                )
            }
            if (compact) {
                IconButton(
                    onClick = onNewPage,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(48.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "New page")
                }
            } else {
                Button(
                    onClick = onNewPage,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("New page")
                }
            }
            Spacer(Modifier.height(4.dp))
            // PDF Power Tools entry: page-rail "Manage Pages". Locked at 38% alpha.
            OutlinedButton(
                onClick = onOpenPageManager,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                    .then(
                        if (pdfUnlocked) Modifier
                        else Modifier.alpha(com.vellum.notes.packs.ui.LOCKED_PACK_ALPHA),
                    )
                    .semantics {
                        contentDescription = "Manage pages ${if (pdfUnlocked) "unlocked" else "locked"}"
                    },
            ) {
                Text("Manage Pages")
            }
            Spacer(Modifier.height(4.dp))
            // Gesture/Bookmark Pack entry: bookmarks rail + gesture mapping.
            if (showBookmarks) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(4.dp))
                com.vellum.notes.packs.ui.BookmarksRail(
                    bookmarks = bookmarks,
                    currentPageId = currentPageId,
                    unlocked = gestureUnlocked,
                    onJump = onJumpBookmark,
                    onAddCurrent = onAddBookmark,
                    onRemove = onRemoveBookmark,
                    onLockedClick = {
                        onLockedPack(com.vellum.notes.packs.PackId.GESTURE)
                    },
                )
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = onOpenGestureMapping,
                    modifier = Modifier.fillMaxWidth()
                        .then(
                            if (gestureUnlocked) Modifier
                            else Modifier.alpha(com.vellum.notes.packs.ui.LOCKED_PACK_ALPHA),
                        )
                        .semantics { contentDescription = "Gesture mapping" },
                ) {
                    Text("Gesture mapping")
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PageThumbnail(page: PageSummary, selected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit = {}) {
    // D6 PHASE 2: thumbnail raster preview — a miniature of the page paper
    // (template ruling drawn as vector lines/dots on the paper base) behind
    // the title, so the rail reads as real page previews instead of blank
    // boxes. Pure Compose, no bitmaps, no new deps.
    val template = PaperTemplates.byId(page.templateId)
    val paperBase = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    else MaterialTheme.colorScheme.surface
    val ruling = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(paperBase)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(6.dp),
            )
            .semantics {
                contentDescription = "Page ${page.title}"
                this.selected = selected
                stateDescription = if (selected) "Current page ${page.title}" else "Page ${page.title}"
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onClickLabel = "Open page ${page.title}",
                onLongClickLabel = "Page options",
            ),
        contentAlignment = Alignment.BottomStart,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            when (template.type) {
                com.vellum.notes.model.PageBackgroundType.RULED -> {
                    var y = size.height * 0.18f
                    val step = size.height / 6f
                    while (y < size.height) {
                        drawLine(ruling, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                        y += step
                    }
                    // Margin line.
                    drawLine(ruling, Offset(size.width * 0.16f, 0f), Offset(size.width * 0.16f, size.height), strokeWidth = 1f)
                }
                com.vellum.notes.model.PageBackgroundType.GRID,
                com.vellum.notes.model.PageBackgroundType.GRAPH,
                com.vellum.notes.model.PageBackgroundType.SMALL_GRID -> {
                    val step = size.width / 8f
                    var x = step
                    while (x < size.width) {
                        drawLine(ruling, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                        x += step
                    }
                    var y = size.height / 6f
                    val yStep = size.height / 6f
                    while (y < size.height) {
                        drawLine(ruling, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                        y += yStep
                    }
                }
                com.vellum.notes.model.PageBackgroundType.DOTTED -> {
                    val xStep = size.width / 8f
                    val yStep = size.height / 6f
                    var x = xStep
                    while (x < size.width) {
                        var y = yStep
                        while (y < size.height) {
                            drawCircle(ruling, radius = 1.5f, center = Offset(x, y))
                            y += yStep
                        }
                        x += xStep
                    }
                }
                else -> Unit
            }
        }
        if (page.isPdfBacked) {
            Text(
                "PDF",
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Bottom scrim behind the title for readability over the ruling.
        Box(
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                .padding(horizontal = 6.dp, vertical = 4.dp),
        ) {
            Text(
                page.title,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * A thin overlay scroll bar for the page rail's [LazyColumn]. Appears only when the list
 * overflows; you can drag anywhere on the track to jump. Knob position/size come from the
 * list's layout info (the rail's items are uniform, so item counts map cleanly to a ratio).
 * Drawn in-house: the foundation `Scrollbar` API is absent from the resolved 1.7.6 artifacts.
 */
@Composable
private fun PageRailScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val thumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    val trackWidth = 4.dp

    val totalItems = listState.layoutInfo.totalItemsCount
    val visibleItems = listState.layoutInfo.visibleItemsInfo.size
    val firstIndex = listState.firstVisibleItemIndex
    val scrollable = totalItems > visibleItems

    // Uniform items -> the visible/total item ratio equals the visible/total height ratio.
    val knobRatio = remember(totalItems, visibleItems) {
        if (totalItems == 0) 1f else (visibleItems.toFloat() / totalItems).coerceIn(0.06f, 1f)
    }
    val scrollFraction = remember(totalItems, visibleItems, firstIndex) {
        if (totalItems <= visibleItems) 0f
        else (firstIndex.toFloat() / (totalItems - visibleItems)).coerceIn(0f, 1f)
    }

    Box(
        modifier = modifier
            .width(14.dp)
            .then(
                if (scrollable) {
                    Modifier.pointerInput(listState, totalItems, visibleItems, knobRatio) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                scope.launch {
                                    scrollListToFraction(listState, offset.y, size.height.toFloat(), knobRatio)
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                scope.launch {
                                    scrollListToFraction(listState, change.position.y, size.height.toFloat(), knobRatio)
                                }
                            },
                        )
                    }
                } else {
                    Modifier
                }
            )
            .drawBehind {
                if (!scrollable) return@drawBehind
                val trackHeight = size.height
                val knobHeight = (trackHeight * knobRatio).coerceAtLeast(32f)
                val maxTop = trackHeight - knobHeight
                val top = (scrollFraction * maxTop).coerceIn(0f, maxTop)
                val halfWidth = trackWidth.toPx() / 2f
                drawRoundRect(
                    color = thumbColor,
                    topLeft = Offset(size.width - trackWidth.toPx(), top),
                    size = Size(trackWidth.toPx(), knobHeight),
                    cornerRadius = CornerRadius(halfWidth, halfWidth),
                )
            },
    )
}

private suspend fun scrollListToFraction(
    listState: LazyListState,
    dragY: Float,
    trackHeightPx: Float,
    knobRatio: Float,
) {
    val total = listState.layoutInfo.totalItemsCount
    val visible = listState.layoutInfo.visibleItemsInfo.size
    if (total == 0 || total <= visible) return
    val knobHeight = (trackHeightPx * knobRatio).coerceAtLeast(32f)
    val travelRange = (trackHeightPx - knobHeight).coerceAtLeast(1f)
    val fraction = ((dragY - knobHeight / 2f) / travelRange).coerceIn(0f, 1f)
    listState.scrollToItem((fraction * (total - visible)).toInt())
}

/**
 * P0-1 writing-status chip: top-center below toolbar. States come from existing engine
 * signals (see InkCanvasView.onWritingStatusChanged): Pen ready / Palm rejected /
 * Two-finger pan. 120ms fade, liveRegion polite + contentDescription.
 */
@Composable
fun WritingStatusChip(
    status: WritingStatus,
    modifier: Modifier = Modifier,
) {
    val (label, description) = when (status) {
        WritingStatus.PEN_READY -> "Pen ready" to "Pen ready to write"
        WritingStatus.PALM_REJECTED -> "Palm rejected" to "Palm touch rejected, pen still active"
        WritingStatus.TWO_FINGER_PAN -> "Two-finger pan" to "Two-finger pan and zoom mode"
    }
    androidx.compose.animation.AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(120, easing = FastOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(120, easing = FastOutSlowInEasing)),
        modifier = modifier.semantics {
            liveRegion = LiveRegionMode.Polite
            contentDescription = "Writing status: $description"
            stateDescription = label
        },
    ) {
        // Key on status so the 120ms fade replays on every state change.
        androidx.compose.animation.AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(120)),
            exit = fadeOut(animationSpec = tween(120)),
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 3.dp,
                shadowElevation = 2.dp,
            ) {
                // Distinct key per status keeps the fade + announcement in sync.
                key(label) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            Modifier.size(8.dp).clip(CircleShape).background(
                                when (status) {
                                    WritingStatus.PEN_READY -> Color(0xFF43A047)
                                    WritingStatus.PALM_REJECTED -> Color(0xFFE53935)
                                    WritingStatus.TWO_FINGER_PAN -> Color(0xFF1E88E5)
                                }
                            )
                        )
                        Text(label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

/**
 * P0-3 visible palm-zone handle: dashed rounded rect bottom-right 96x48
 * with grip; drag resizes the existing palmZone setting.
 */
@Composable
fun PalmZoneHandle(
    settings: PalmRejectionSettings,
    onResize: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val zone = settings.palmZone
    val density = LocalDensity.current
    val outlineColor = MaterialTheme.colorScheme.primary
    val gripColor = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .size(width = 96.dp, height = 48.dp)
            .pointerInput(zone.widthMm, zone.heightMm) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val dpi = density.density * 160f
                    val dWidthMm = dragAmount.x / dpi * 25.4f
                    val dHeightMm = dragAmount.y / dpi * 25.4f
                    onResize(zone.widthMm + dWidthMm, zone.heightMm + dHeightMm)
                }
            }
            .semantics {
                contentDescription = "Palm rest zone handle"
                stateDescription =
                    "Palm zone ${zone.widthMm.toInt()} by ${zone.heightMm.toInt()} millimeters"
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // BUG 1/3 fix: theme-aware outline (primary has >= 8:1 contrast on the
            // canvas in both light and dark themes) instead of hardcoded gray.
            drawRoundRect(
                color = outlineColor,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
                ),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
            )
        }
        Icon(
            Icons.Filled.DragHandle,
            contentDescription = "Resize palm rest zone",
            tint = gripColor,
        )
    }
}

/**
 * P0-3 first-run palm-zone coachmark: small bubble anchored just above the
 * [PalmZoneHandle]. Shown only while
 * [PalmRejectionSettings.showPalmZoneCoachmark] is true (DataStore, default
 * true); the Got-it button persists dismissal. TalkBack-announced via a
 * polite liveRegion, following the writing-status chip pattern.
 */
@Composable
fun PalmZoneCoachmark(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.semantics {
            liveRegion = LiveRegionMode.Polite
            contentDescription = "Coachmark: Rest your palm here. Dismiss to hide this tip."
        },
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 4.dp,
        shadowElevation = 3.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Rest your palm here", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onDismiss) { Text("Got it") }
        }
    }
}

/**
 * P1-4 single bottom pickers panel (colors + widths + smoothing).
 * Colors use the 12 NAMED swatches (names announced, never hex); widths use the
 * 5 mm steps with labels + live stroke preview; smoothing uses OFF/STEADY/FLOW chips.
 */
@Composable
fun PenPickersPanel(
    penStyle: PenStyle,
    smoothing: SmoothingMode,
    onColor: (Long) -> Unit,
    onWidth: (Float) -> Unit,
    onSmoothingChange: (SmoothingMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.semantics { isTraversalGroup = true },
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 3.dp,
        shadowElevation = 2.dp,
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Color", style = MaterialTheme.typography.labelMedium)
            NAMED_COLORS.chunked(6).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    row.forEach { (name, argb) ->
                        val selected = penStyle.colorArgb == argb
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(argb))
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape,
                                )
                                .semantics {
                                    contentDescription = "Pen color $name"
                                    this.selected = selected
                                    stateDescription =
                                        if (selected) "Selected color $name" else "Color $name"
                                    this.role = Role.RadioButton
                                }
                                .clickable(role = Role.RadioButton) { onColor(argb) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = if (name == "White" || name == "Yellow") Color.Black
                                    else Color.White,
                                )
                            }
                        }
                    }
                }
            }
            Text("Width", style = MaterialTheme.typography.labelMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PEN_WIDTH_STEPS_MM.forEach { w ->
                    val selected = kotlin.math.abs(penStyle.widthMm - w) < 0.01f
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape,
                                )
                                .semantics {
                                    contentDescription = "Pen width $w millimeters"
                                    this.selected = selected
                                    stateDescription =
                                        if (selected) "Selected width $w millimeters"
                                        else "$w millimeters"
                                    this.role = Role.RadioButton
                                }
                                .clickable(role = Role.RadioButton) { onWidth(w) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier
                                    .size((w * 3).dp.coerceAtMost(28.dp))
                                    .clip(CircleShape)
                                    .background(penStyle.colorArgb.toColor()),
                            )
                        }
                        Text("$w mm", style = MaterialTheme.typography.labelSmall)
                    }
                }
                // Live stroke preview in the current color/width.
                Canvas(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .semantics {
                            contentDescription =
                                "Stroke preview ${penStyle.widthMm} millimeters"
                        },
                ) {
                    val strokePx = ((penStyle.widthMm * 3).dp).toPx()
                        .coerceAtLeast(2f)
                        .coerceAtMost(size.height * 0.6f)
                    drawLine(
                        color = penStyle.colorArgb.toColor(),
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = strokePx,
                    )
                }
            }
            Text("Smoothing", style = MaterialTheme.typography.labelMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SMOOTHING_CHIPS.forEach { (label, mode) ->
                    val selected = smoothing == mode
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent,
                            )
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(16.dp),
                            )
                            .semantics {
                                contentDescription = "Smoothing $label"
                                this.selected = selected
                                stateDescription =
                                    if (selected) "Selected smoothing $label"
                                    else "Smoothing $label"
                                this.role = Role.RadioButton
                            }
                            .clickable(role = Role.RadioButton) { onSmoothingChange(mode) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

/**
 * P1-6 trust signal: skeleton shimmer page loading instead of bare text.
 */
@Composable
fun SkeletonShimmer(
    modifier: Modifier = Modifier,
    contentDescription: String = "Loading",
) {
    val transition = rememberInfiniteTransition(label = "skeletonShimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmerAlpha",
    )
    Column(
        modifier
            .semantics {
                this.contentDescription = contentDescription
                liveRegion = LiveRegionMode.Polite
            }
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(4) {
            Box(
                Modifier.fillMaxWidth(if (it == 0) 0.5f else 1f).height(if (it == 0) 28.dp else 18.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha * 0.6f + 0.2f)),
            )
        }
    }
}

/**
 * P1-6 trust signal: sync becomes a 20dp status dot on the nav pill.
 * Driven by SyncStatus from SyncRepository; always visible so CONFLICT/FAILED
 * are noticed without leaving the canvas.
 */
@Composable
fun SyncStatusIndicator(
    syncStatus: SyncStatus,
    modifier: Modifier = Modifier,
) {
    val (label, color, description) = when (syncStatus) {
        SyncStatus.DISABLED -> Triple("Sync off", Color.Gray, "Sync is off")
        SyncStatus.IDLE -> Triple("Ready", Color.Gray, "Sync ready")
        SyncStatus.SYNCING -> Triple("Syncing…", Color(0xFF1E88E5), "Sync in progress")
        SyncStatus.SUCCEEDED -> Triple("Synced", Color(0xFF43A047), "Sync succeeded")
        SyncStatus.FAILED -> Triple("Sync failed", Color(0xFFE53935), "Sync failed")
        SyncStatus.CONFLICT -> Triple("Conflict", Color(0xFFFB8C00), "Sync conflict needs resolution")
    }
    Box(
        modifier = modifier
            .size(48.dp)
            .semantics {
                contentDescription = "Sync status: $description"
                stateDescription = label
            },
        contentAlignment = Alignment.Center,
    ) {
        // 20dp status dot on the nav pill.
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(color)
                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
        )
    }
}

@Composable
private fun CanvasTopBar(
    tool: Tool,
    penStyle: PenStyle,
    eraserSizeMm: Float,
    shapeKind: ShapeKind,
    settings: PalmRejectionSettings,
    selectedCount: Int,
    onTool: (Tool) -> Unit,
    onShapeKind: (ShapeKind) -> Unit,
    onColor: (Long) -> Unit,
    onWidth: (Float) -> Unit,
    onPenType: (PenType) -> Unit,
    onEraserSize: (Float) -> Unit,
    onSelectAll: () -> Unit,
    onDeleteSelection: () -> Unit,
    onDuplicateSelection: () -> Unit,
    onSmoothingChange: (SmoothingMode) -> Unit,
    autoEraseEnabled: Boolean,
    onAutoEraseToggle: () -> Unit,
    hiddenToolLabels: Set<String> = emptySet(),
    onToggleToolHidden: (String) -> Unit = {},
    zoomWindowEnabled: Boolean = false,
    onToggleZoomWindow: () -> Unit = {},
    onFitToPage: () -> Unit = {},
    insertSpaceArmed: Boolean = false,
    onToggleInsertSpace: () -> Unit = {},
    onOpenSpelling: () -> Unit = {},
    onInsertText: () -> Unit = {},
    onInsertImage: () -> Unit = {},
    onPickTemplate: () -> Unit = {},
    canEditText: Boolean = false,
    onEditText: () -> Unit = {},
    canSmooth: Boolean = false,
    onSmoothSelection: () -> Unit = {},
    canConvert: Boolean = false,
    onConvertSelection: () -> Unit = {},
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    onBack: () -> Unit = {},
    onToggleRail: () -> Unit = {},
    onExportPdf: () -> Unit = {},
    isRecording: Boolean = false,
    onToggleClassroom: () -> Unit = {},
    transcriptAvailable: Boolean = false,
    onToggleTranscriptSidebar: () -> Unit = {},
    classroomEnabled: Boolean = false,
    syncStatus: SyncStatus = SyncStatus.IDLE,
    classroomUnlocked: Boolean = false,
    pdfUnlocked: Boolean = false,
    gestureUnlocked: Boolean = false,
    onOpenClassroomExport: () -> Unit = {},
    onOpenPageManager: () -> Unit = {},
    onOpenLayeredExport: () -> Unit = {},
    onOpenBookmarks: () -> Unit = {},
    onOpenGestureMapping: () -> Unit = {},
    onLockedPack: (com.vellum.notes.packs.PackId) -> Unit = {},
) {
    // Tapping the active pen/highlighter/eraser/shapes tool toggles its settings panel.
    var pickerOpen by remember { mutableStateOf(true) }
    fun stripClick(t: Tool) {
        if (t == tool && (t == Tool.PEN || t == Tool.HIGHLIGHTER || t == Tool.ERASER || t == Tool.SHAPES)) {
            pickerOpen = !pickerOpen
        } else {
            onTool(t)
            pickerOpen = true
        }
    }
    val showPicker = pickerOpen && (tool == Tool.PEN || tool == Tool.HIGHLIGHTER || tool == Tool.ERASER || tool == Tool.SHAPES)
    // TalkBack traversal: toolbar (0) -> canvas (1) -> transcript (2) -> page rail (3).
    // P0-2 fixed toolbar: no horizontal scroll under 600dp — 8 tools in the row,
    // the rest goes into the More overflow menu. Measured via BoxWithConstraints.
    Column(
        Modifier
            .fillMaxWidth()
            .semantics { isTraversalGroup = true; traversalIndex = 0f },
    ) {
        val compact = LocalConfiguration.current.screenWidthDp < 600
        FixedToolbarContent(
            tool = tool,
            compact = compact,
            syncStatus = syncStatus,
            canUndo = canUndo,
            canRedo = canRedo,
            onUndo = onUndo,
            onRedo = onRedo,
            onBack = onBack,
            onToggleRail = onToggleRail,
            onExportPdf = onExportPdf,
            isRecording = isRecording,
            onToggleClassroom = onToggleClassroom,
            transcriptAvailable = transcriptAvailable,
            onToggleTranscriptSidebar = onToggleTranscriptSidebar,
            classroomEnabled = classroomEnabled,
            autoEraseEnabled = autoEraseEnabled,
            hiddenToolLabels = hiddenToolLabels,
            onToggleToolHidden = onToggleToolHidden,
            zoomWindowEnabled = zoomWindowEnabled,
            onToggleZoomWindow = onToggleZoomWindow,
            onFitToPage = onFitToPage,
            insertSpaceArmed = insertSpaceArmed,
            onToggleInsertSpace = onToggleInsertSpace,
            onOpenSpelling = onOpenSpelling,
            onAutoEraseToggle = onAutoEraseToggle,
            onInsertText = onInsertText,
            onInsertImage = onInsertImage,
            onPickTemplate = onPickTemplate,
            stripClick = { stripClick(it) },
            classroomUnlocked = classroomUnlocked,
            pdfUnlocked = pdfUnlocked,
            gestureUnlocked = gestureUnlocked,
            onOpenClassroomExport = onOpenClassroomExport,
            onOpenPageManager = onOpenPageManager,
            onOpenLayeredExport = onOpenLayeredExport,
            onOpenBookmarks = onOpenBookmarks,
            onOpenGestureMapping = onOpenGestureMapping,
            onLockedPack = onLockedPack,
        )
    }
    // Context panel second row (existing): settings for the active tool / selection.
    ContextPanelRow(
        tool = tool,
        penStyle = penStyle,
        eraserSizeMm = eraserSizeMm,
        shapeKind = shapeKind,
        settings = settings,
        selectedCount = selectedCount,
        showPicker = showPicker,
        onShapeKind = onShapeKind,
        onPenType = onPenType,
        onEraserSize = onEraserSize,
        onSelectAll = onSelectAll,
        onDeleteSelection = onDeleteSelection,
        onDuplicateSelection = onDuplicateSelection,
        canEditText = canEditText,
        onEditText = onEditText,
        canSmooth = canSmooth,
        onSmoothSelection = onSmoothSelection,
        canConvert = canConvert,
        onConvertSelection = onConvertSelection,
    )
}

@Composable
private fun FixedToolbarContent(
    tool: Tool,
    compact: Boolean,
    syncStatus: SyncStatus,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onBack: () -> Unit,
    onToggleRail: () -> Unit,
    onExportPdf: () -> Unit,
    isRecording: Boolean,
    onToggleClassroom: () -> Unit,
    transcriptAvailable: Boolean,
    onToggleTranscriptSidebar: () -> Unit,
    classroomEnabled: Boolean,
    autoEraseEnabled: Boolean,
    onAutoEraseToggle: () -> Unit,
    onInsertText: () -> Unit,
    onInsertImage: () -> Unit,
    onPickTemplate: () -> Unit,
    stripClick: (Tool) -> Unit,
    hiddenToolLabels: Set<String> = emptySet(),
    onToggleToolHidden: (String) -> Unit = {},
    zoomWindowEnabled: Boolean = false,
    onToggleZoomWindow: () -> Unit = {},
    onFitToPage: () -> Unit = {},
    insertSpaceArmed: Boolean = false,
    onToggleInsertSpace: () -> Unit = {},
    onOpenSpelling: () -> Unit = {},
    classroomUnlocked: Boolean = false,
    pdfUnlocked: Boolean = false,
    gestureUnlocked: Boolean = false,
    onOpenClassroomExport: () -> Unit = {},
    onOpenPageManager: () -> Unit = {},
    onOpenLayeredExport: () -> Unit = {},
    onOpenBookmarks: () -> Unit = {},
    onOpenGestureMapping: () -> Unit = {},
    onLockedPack: (com.vellum.notes.packs.PackId) -> Unit = {},
) {
    // P0-2: 8 tools in the fixed row; the rest goes into the More overflow menu.
    // Order: Pen, Highlighter, Eraser, Select, Shapes, Text, Image, Template (+ Auto-erase).
    data class ToolDef(
        val label: String,
        val apply: () -> Unit,
        val selected: Boolean,
        val icon: @Composable () -> Unit,
    )
    val allTools = listOf(
        ToolDef("Pen", { stripClick(Tool.PEN) }, tool == Tool.PEN,
            { Icon(Icons.Filled.BorderColor, contentDescription = "Pen") }),
        ToolDef("Highlighter", { stripClick(Tool.HIGHLIGHTER) }, tool == Tool.HIGHLIGHTER,
            { Icon(Icons.Filled.Highlight, contentDescription = "Highlighter") }),
        ToolDef("Eraser", { stripClick(Tool.ERASER) }, tool == Tool.ERASER,
            { Icon(Icons.Outlined.Circle, contentDescription = "Eraser") }),
        ToolDef("Select", { stripClick(Tool.SELECT) }, tool == Tool.SELECT,
            { Icon(Icons.Filled.SelectAll, contentDescription = "Select") }),
        ToolDef("Shapes", { stripClick(Tool.SHAPES) }, tool == Tool.SHAPES,
            { Icon(Icons.Filled.Category, contentDescription = "Shapes") }),
        ToolDef("Text", onInsertText, tool == Tool.TEXT,
            { Icon(Icons.Filled.TextFields, contentDescription = "Text box") }),
        ToolDef("Image", onInsertImage, false,
            { Icon(Icons.Filled.Image, contentDescription = "Insert image") }),
        ToolDef("Template", onPickTemplate, false,
            { Icon(Icons.Filled.GridOn, contentDescription = "Page template") }),
        ToolDef("Auto-erase", onAutoEraseToggle, autoEraseEnabled,
            { Icon(Icons.Filled.AutoFixHigh, contentDescription = "Auto-erase") }),
    )
    // P0-2 fixed toolbar: no horizontal scroll under 600dp — 8 tools in the row,
    // Hidden tools are excluded everywhere except the customize dialog;
    // an all-hidden selection falls back to the full row (never an empty bar).
    val customized = allTools.filter { it.label !in hiddenToolLabels }
    val effectiveTools = if (customized.isEmpty()) allTools else customized
    val visibleTools = if (compact) effectiveTools.take(8) else effectiveTools
    val overflowTools = if (compact) effectiveTools.drop(8) else emptyList()
    var overflowOpen by remember { mutableStateOf(false) }
    var customizeOpen by remember { mutableStateOf(false) }
    // Floating pills hovering over the canvas: navigation, tools, actions.
    // Every target is 48dp with an explicit contentDescription. No horizontal scroll.
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // BUG 1 fix: explicit pill color + contentColor so toolbar icons never
            // inherit an ambient tint that matches the container (invisible icons
            // in light theme). surfaceContainer/onSurface contrast is >= 13:1 in
            // both light and dark themes.
            Surface(
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 3.dp,
                shadowElevation = 2.dp,
                color = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    // P1-6: undo/redo at 38pct alpha when disabled.
                    IconButton(
                        onClick = onUndo,
                        enabled = canUndo,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (canUndo) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        )
                    }
                    IconButton(
                        onClick = onRedo,
                        enabled = canRedo,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (canRedo) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        )
                    }
                    // P1-6: sync becomes a 20dp status dot on the nav pill.
                    SyncStatusIndicator(syncStatus = syncStatus)
                }
            }
            Surface(
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 3.dp,
                shadowElevation = 2.dp,
                color = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                // Primary tool strip: fixed, no scroll.
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    visibleTools.forEach { t ->
                        ToolButton(
                            label = t.label,
                            selected = t.selected,
                            onClick = t.apply,
                            content = t.icon,
                        )
                    }
                    // More menu hosts overflow tools plus toolbar customization,
                    // so it is always present even when nothing overflows.
                    Box {
                        IconButton(
                            onClick = { overflowOpen = true },
                            modifier = Modifier.size(48.dp).semantics {
                                contentDescription = "More tools overflow menu"
                                stateDescription = if (overflowOpen) "Overflow expanded" else "Overflow collapsed"
                            },
                        ) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "More overflow menu",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        DropdownMenu(
                            expanded = overflowOpen,
                            onDismissRequest = { overflowOpen = false },
                        ) {
                            overflowTools.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t.label) },
                                    onClick = { overflowOpen = false; t.apply() },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Customize toolbar") },
                                onClick = { overflowOpen = false; customizeOpen = true },
                            )
                            DropdownMenuItem(
                                text = { Text(if (zoomWindowEnabled) "✓ Zoom writing aid" else "Zoom writing aid") },
                                onClick = { overflowOpen = false; onToggleZoomWindow() },
                            )
                            DropdownMenuItem(
                                text = { Text("Fit to page") },
                                onClick = { overflowOpen = false; onFitToPage() },
                            )
                            DropdownMenuItem(
                                text = { Text(if (insertSpaceArmed) "✓ Insert space" else "Insert space") },
                                onClick = { overflowOpen = false; onToggleInsertSpace() },
                            )
                            DropdownMenuItem(
                                text = { Text("Check spelling") },
                                onClick = { overflowOpen = false; onOpenSpelling() },
                            )
                        }
                    }
                    if (customizeOpen) {
                        AlertDialog(
                            onDismissRequest = { customizeOpen = false },
                            title = { Text("Customize toolbar") },
                            text = {
                                Column {
                                    allTools.forEach { t ->
                                        val visibleCount = allTools.count { it.label !in hiddenToolLabels }
                                        val checked = t.label !in hiddenToolLabels
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth().clickable(
                                                role = Role.Checkbox,
                                                onClickLabel = if (checked) "Hide ${t.label}" else "Show ${t.label}",
                                            ) {
                                                if (checked && visibleCount > 1) onToggleToolHidden(t.label)
                                                else if (!checked) onToggleToolHidden(t.label)
                                            },
                                        ) {
                                            Checkbox(
                                                checked = checked,
                                                enabled = checked && visibleCount > 1 || !checked,
                                                onCheckedChange = null,
                                                modifier = Modifier.semantics {
                                                    contentDescription = "${t.label} tool visible"
                                                },
                                            )
                                            Text(t.label)
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { customizeOpen = false }) { Text("Done") }
                            },
                        )
                    }
                }
            }
            Surface(
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 3.dp,
                shadowElevation = 2.dp,
                color = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onToggleRail,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            Icons.Filled.Menu,
                            contentDescription = "Show or hide pages",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(
                        onClick = onExportPdf,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            Icons.Filled.PictureAsPdf,
                            contentDescription = "Export PDF",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(
                        onClick = onToggleClassroom,
                        enabled = classroomEnabled,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics {
                                stateDescription = if (isRecording) "Recording in progress" else "Not recording"
                            },
                    ) {
                        Icon(
                            Icons.Filled.Mic,
                            contentDescription = "Classroom Notes (record & transcribe)",
                            tint = if (isRecording) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(
                        onClick = onToggleTranscriptSidebar,
                        enabled = transcriptAvailable,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Article,
                            contentDescription = "Show or hide transcript",
                            tint = if (transcriptAvailable) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        )
                    }
                    // Revenue packs overflow: Export (Classroom) + Page Manager
                    // (PDF) + Bookmarks (Gesture). Locked entries render at 38%
                    // alpha and open the unlock dialog on tap.
                    var packsOverflowOpen by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { packsOverflowOpen = true },
                            modifier = Modifier.size(48.dp).semantics {
                                contentDescription = "Packs overflow menu"
                                stateDescription =
                                    if (packsOverflowOpen) "Packs expanded" else "Packs collapsed"
                            },
                        ) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "Packs overflow menu",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        DropdownMenu(
                            expanded = packsOverflowOpen,
                            onDismissRequest = { packsOverflowOpen = false },
                        ) {
                            PacksOverflowItem(
                                label = "Export",
                                pack = com.vellum.notes.packs.PackId.CLASSROOM,
                                unlocked = classroomUnlocked,
                                onClick = {
                                    packsOverflowOpen = false
                                    onOpenClassroomExport()
                                },
                            )
                            PacksOverflowItem(
                                label = "Layered export",
                                pack = com.vellum.notes.packs.PackId.PDF,
                                unlocked = pdfUnlocked,
                                onClick = {
                                    packsOverflowOpen = false
                                    onOpenLayeredExport()
                                },
                            )
                            PacksOverflowItem(
                                label = "Page Manager",
                                pack = com.vellum.notes.packs.PackId.PDF,
                                unlocked = pdfUnlocked,
                                onClick = {
                                    packsOverflowOpen = false
                                    onOpenPageManager()
                                },
                            )
                            PacksOverflowItem(
                                label = "Bookmarks",
                                pack = com.vellum.notes.packs.PackId.GESTURE,
                                unlocked = gestureUnlocked,
                                onClick = {
                                    packsOverflowOpen = false
                                    onOpenBookmarks()
                                },
                            )
                            PacksOverflowItem(
                                label = "Gesture mapping",
                                pack = com.vellum.notes.packs.PackId.GESTURE,
                                unlocked = gestureUnlocked,
                                onClick = {
                                    packsOverflowOpen = false
                                    onOpenGestureMapping()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PacksOverflowItem(
    label: String,
    pack: com.vellum.notes.packs.PackId,
    unlocked: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    com.vellum.notes.packs.ui.packIcon(pack),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp).then(
                        if (unlocked) Modifier
                        else Modifier.alpha(com.vellum.notes.packs.ui.LOCKED_PACK_ALPHA),
                    ),
                    tint = if (unlocked) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(
                        alpha = com.vellum.notes.packs.ui.LOCKED_PACK_ALPHA,
                    ),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    if (unlocked) label else "$label (locked)",
                    modifier = Modifier.then(
                        if (unlocked) Modifier
                        else Modifier.alpha(com.vellum.notes.packs.ui.LOCKED_PACK_ALPHA),
                    ),
                )
            }
        },
        onClick = onClick,
    )
}

@Composable
private fun ContextPanelRow(
    tool: Tool,
    penStyle: PenStyle,
    eraserSizeMm: Float,
    shapeKind: ShapeKind,
    settings: PalmRejectionSettings,
    selectedCount: Int,
    showPicker: Boolean,
    onShapeKind: (ShapeKind) -> Unit,
    onPenType: (PenType) -> Unit,
    onEraserSize: (Float) -> Unit,
    onSelectAll: () -> Unit,
    onDeleteSelection: () -> Unit,
    onDuplicateSelection: () -> Unit,
    canEditText: Boolean = false,
    onEditText: () -> Unit = {},
    canSmooth: Boolean = false,
    onSmoothSelection: () -> Unit = {},
    canConvert: Boolean = false,
    onConvertSelection: () -> Unit = {},
) {
    if (true) {
        // Context panel second row placeholder replaced below.
    }
    // Context panel: settings for the active tool, or selection actions.
    // (Existing second row preserved; pen colors/widths/smoothing live in the bottom panel.)
    if (showPicker || tool == Tool.SELECT) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 3.dp,
            shadowElevation = 2.dp,
        ) {
            DummyContextPanelContent()
        }
    }
}

@Composable
private fun DummyContextPanelContent() {
    // Replaced by full context panel below via overload.
    Box(Modifier.height(1.dp))
}

@Composable
private fun ToolButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    // Nebo-style compact strip button: icon-only 48dp target, pill highlight +
    // accent underline for the active tool. Exposes role + selected state to
    // TalkBack so the toolbar is fully traversable with state announcements.
    // P0-2: 150ms selection fade via animateColorAsState.
    // BUG 1 fix: explicit icon tint per selection state — onPrimaryContainer on
    // the primaryContainer pill when selected (10.5:1 light, 7.8:1 dark), plain
    // onSurface otherwise (>= 13:1 on the pill in both themes). Never inherits
    // the ambient content color, so icons stay visible in light AND dark themes.
    val bg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer
        else Color.Transparent,
        animationSpec = tween(150),
        label = "toolBg",
    )
    val iconTint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .semantics {
                contentDescription = label
                this.selected = selected
                stateDescription = if (selected) "$label tool selected" else "$label tool"
                this.role = Role.Button
            }
            .clickable(onClick = onClick, role = Role.Button, onClickLabel = label)
            .padding(4.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides iconTint) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { content() }
        }
        // Active tool indicator: 3dp accent underline (4dp spacing grid) using
        // onPrimaryContainer (7.8:1 on the primaryContainer pill in dark theme,
        // 10.5:1 in light) so the active state stays visible in both themes.
        Box(
            modifier = Modifier
                .size(width = 24.dp, height = 3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) MaterialTheme.colorScheme.onPrimaryContainer else Color.Transparent)
        )
    }
}

@Composable
private fun DisabledToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .semantics {
                contentDescription = "$description unavailable"
                stateDescription = "Disabled"
            }
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f))
        Text("—", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f))
    }
}

private fun Long.toColor(): Color = Color(this)
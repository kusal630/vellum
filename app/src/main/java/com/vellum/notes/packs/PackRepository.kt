package com.vellum.notes.packs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vellum.notes.model.TranscriptSegment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val PACKS_STORE_NAME = "vellum_packs.preferences_pb"

private val Context.packsDataStoreInstance: DataStore<Preferences> by preferencesDataStore(
    name = PACKS_STORE_NAME,
)

/** Returns the application-scoped packs DataStore (entitlements + pack data). */
fun Context.packsDataStore(): DataStore<Preferences> = packsDataStoreInstance

/** One auto-generated chapter over a transcript. */
@Serializable
data class Chapter(
    val id: Long = 0L,
    val title: String,
    /** Start offset in ms from recording start; tap seeks audio-sync playback. */
    val startMs: Long,
    val segmentIds: List<Long> = emptyList(),
)

/** A bookmarked page inside a notebook. */
@Serializable
data class PageBookmark(
    val pageId: Long,
    val title: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

/** Custom gesture -> action mapping (Gesture/Bookmark pack). */
@Serializable
data class GestureMapping(
    /** e.g. "two_finger_double_tap", "three_finger_tap", "two_finger_swipe_left" */
    val gesture: String,
    /** e.g. "undo", "redo", "toggle_rail", "new_page", "export_pdf" */
    val action: String,
)

private val CLASSROOM_KEY = booleanPreferencesKey("classroomPack")
private val PDF_KEY = booleanPreferencesKey("pdfPack")
private val GESTURE_KEY = booleanPreferencesKey("gesturePack")
private val CLASSROOM_AUTO_BACKUP_KEY = booleanPreferencesKey("classroom_auto_backup")
private val CHAPTERS_KEY = stringPreferencesKey("classroom_chapters_json")
private val BOOKMARKS_KEY = stringPreferencesKey("gesture_bookmarks_json")
private val GESTURES_KEY = stringPreferencesKey("gesture_mapping_json")

private val packJson = Json { ignoreUnknownKeys = true }

/**
 * Offline-first pack storage.
 *
 * Entitlements are three DataStore JSON booleans {classroomPack, pdfPack, gesturePack}.
 * Pack data (chapters, bookmarks, gesture mapping) is also DataStore JSON so the
 * packs stay fully offline with no new dependencies.
 */
class PackRepository(private val dataStore: DataStore<Preferences>) {

    val entitlements: Flow<PackEntitlements> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            PackEntitlements(
                classroomPack = prefs[CLASSROOM_KEY] == true,
                pdfPack = prefs[PDF_KEY] == true,
                gesturePack = prefs[GESTURE_KEY] == true,
            )
        }

    suspend fun isUnlocked(pack: PackId): Boolean = entitlements.first().isUnlocked(pack)

    suspend fun setUnlocked(pack: PackId, unlocked: Boolean = true) {
        dataStore.edit { prefs ->
            when (pack) {
                PackId.CLASSROOM -> prefs[CLASSROOM_KEY] = unlocked
                PackId.PDF -> prefs[PDF_KEY] = unlocked
                PackId.GESTURE -> prefs[GESTURE_KEY] = unlocked
            }
        }
    }

    suspend fun setAll(entitlements: PackEntitlements) {
        dataStore.edit { prefs ->
            prefs[CLASSROOM_KEY] = entitlements.classroomPack
            prefs[PDF_KEY] = entitlements.pdfPack
            prefs[GESTURE_KEY] = entitlements.gesturePack
        }
    }

    // --- Classroom Pack data ---

    val autoBackupEnabled: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[CLASSROOM_AUTO_BACKUP_KEY] == true }

    suspend fun setAutoBackup(enabled: Boolean) {
        dataStore.edit { it[CLASSROOM_AUTO_BACKUP_KEY] = enabled }
    }

    /** pageId (String key) -> chapters. */
    suspend fun getChapters(pageId: Long): List<Chapter> =
        readChapters().get(pageId.toString()).orEmpty()

    suspend fun setChapters(pageId: Long, chapters: List<Chapter>) {
        val map = readChapters().toMutableMap()
        map[pageId.toString()] = chapters
        dataStore.edit { it[CHAPTERS_KEY] = packJson.encodeToString(map) }
    }

    // --- Gesture/Bookmark Pack data ---

    /** notebookId (String key) -> bookmarks. */
    suspend fun getBookmarks(notebookId: Long): List<PageBookmark> =
        readBookmarks().get(notebookId.toString()).orEmpty()

    suspend fun addBookmark(notebookId: Long, bookmark: PageBookmark) {
        val map = readBookmarks().toMutableMap()
        val list = map[notebookId.toString()].orEmpty().toMutableList()
        if (list.none { it.pageId == bookmark.pageId }) list += bookmark
        map[notebookId.toString()] = list
        dataStore.edit { it[BOOKMARKS_KEY] = packJson.encodeToString(map) }
    }

    suspend fun removeBookmark(notebookId: Long, pageId: Long) {
        val map = readBookmarks().toMutableMap()
        map[notebookId.toString()] = map[notebookId.toString()].orEmpty()
            .filterNot { it.pageId == pageId }
        dataStore.edit { it[BOOKMARKS_KEY] = packJson.encodeToString(map) }
    }

    suspend fun getGestureMapping(): List<GestureMapping> =
        runCatching {
            val raw = dataStore.data.first()[GESTURES_KEY] ?: return GesturePro.defaultMapping()
            packJson.decodeFromString<List<GestureMapping>>(raw)
        }.getOrDefault(GesturePro.defaultMapping())

    suspend fun setGestureMapping(mapping: List<GestureMapping>) {
        dataStore.edit { it[GESTURES_KEY] = packJson.encodeToString(mapping) }
    }

    private suspend fun readChapters(): Map<String, List<Chapter>> =
        runCatching {
            val raw = dataStore.data.first()[CHAPTERS_KEY] ?: return emptyMap()
            packJson.decodeFromString<Map<String, List<Chapter>>>(raw)
        }.getOrDefault(emptyMap())

    private suspend fun readBookmarks(): Map<String, List<PageBookmark>> =
        runCatching {
            val raw = dataStore.data.first()[BOOKMARKS_KEY] ?: return emptyMap()
            packJson.decodeFromString<Map<String, List<PageBookmark>>>(raw)
        }.getOrDefault(emptyMap())
}

/** Pure helpers shared with tests; segments type reused from the model. */
internal fun segmentsForTest(): List<TranscriptSegment> = emptyList()

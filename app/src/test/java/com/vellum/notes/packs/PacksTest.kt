package com.vellum.notes.packs

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.preferencesDataStoreFile
import com.vellum.notes.model.PageContent
import com.vellum.notes.model.PageSummary
import com.vellum.notes.model.TextObject
import com.vellum.notes.model.TranscriptSegment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/**
 * MUSE-R1 Revenue Pack acceptance: entitlements persist as DataStore booleans,
 * license verification round-trips Ed25519, billing restore unlocks, and the
 * pack helpers (chapters/audio-sync, layered export/page ops, gestures,
 * bookmarks) behave.
 */
@RunWith(RobolectricTestRunner::class)
class PacksTest {

    private fun repo(): PackRepository {
        val ctx: Context = RuntimeEnvironment.getApplication()
        // Isolate from the app singleton: dedicated test file is unnecessary —
        // PackRepository writes only its own keys; reset between tests.
        return PackRepository(ctx.packsDataStore())
    }

    private fun reset() = runBlocking {
        repo().setAll(PackEntitlements())
    }

    // --- Entitlements ---

    @Test
    fun entitlements_defaultLocked() = runBlocking {
        reset()
        val e = repo().entitlements.first()
        assertFalse(e.classroomPack)
        assertFalse(e.pdfPack)
        assertFalse(e.gesturePack)
        // Paywalls disabled: flags stay false but everything reads unlocked.
        assertTrue(PackEntitlements.PAYWALLS_DISABLED)
        assertTrue(repo().isUnlocked(PackId.CLASSROOM))
    }

    @Test
    fun entitlements_unlockPersists() = runBlocking {
        reset()
        val r = repo()
        r.setUnlocked(PackId.CLASSROOM, true)
        r.setUnlocked(PackId.PDF, true)
        assertTrue(r.entitlements.first().classroomPack)
        assertTrue(r.entitlements.first().pdfPack)
        assertTrue(r.isUnlocked(PackId.CLASSROOM))
        assertTrue(r.isUnlocked(PackId.PDF))
        assertTrue(r.isUnlocked(PackId.GESTURE))
        r.setUnlocked(PackId.CLASSROOM, false)
        assertTrue(r.isUnlocked(PackId.CLASSROOM))
        reset()
    }

    // --- License (Ed25519 round-trip with an ephemeral keypair) ---

    private fun ephemeralKeys(): Pair<ByteArray, java.security.PrivateKey> {
        val kpg = KeyPairGenerator.getInstance("Ed25519")
        val kp = kpg.generateKeyPair()
        val x509 = kp.public.encoded
        // Strip the 12-byte SubjectPublicKeyInfo prefix to raw 32 bytes.
        val raw = x509.copyOfRange(x509.size - 32, x509.size)
        // Sanity: re-wrap must equal the original encoding.
        val rewrapped = LicenseVerifier.ed25519RawToX509(raw)
        val reparsed = KeyFactoryHolder.factory.generatePublic(X509EncodedKeySpec(rewrapped))
        assertEquals(kp.public, reparsed)
        return raw to kp.private
    }

    @Test
    fun license_validSignatureUnlocks() {
        val (pub, priv) = ephemeralKeys()
        val payload = LicensePayload(packs = listOf("classroom", "pdf"), exp = 0L)
        val payloadBytes = payloadJson(payload)
        val sig = Signature.getInstance("Ed25519").apply {
            initSign(priv)
            update(payloadBytes)
        }.sign()
        val text = b64url(payloadBytes) + "." +
            Base64.encodeToString(sig, Base64.NO_WRAP)
        val unlocked = LicenseVerifier.verifyLicenseText(text, pub)
        assertEquals(setOf(PackId.CLASSROOM, PackId.PDF), unlocked)
    }

    @Test
    fun license_tamperedPayloadRejected() {
        val (pub, priv) = ephemeralKeys()
        val payloadBytes = payloadJson(LicensePayload(listOf("gesture"), 0L))
        val sig = Signature.getInstance("Ed25519").apply {
            initSign(priv)
            update(payloadBytes)
        }.sign()
        val tampered = payloadJson(LicensePayload(listOf("classroom", "pdf", "gesture"), 0L))
        val text = b64url(tampered) + "." + Base64.encodeToString(sig, Base64.NO_WRAP)
        assertTrue(LicenseVerifier.verifyLicenseText(text, pub).isEmpty())
    }

    @Test
    fun license_wrongKeyRejected() {
        val (_, priv) = ephemeralKeys()
        val (otherPub, _) = ephemeralKeys()
        val payloadBytes = payloadJson(LicensePayload(listOf("*"), 0L))
        val sig = Signature.getInstance("Ed25519").apply {
            initSign(priv)
            update(payloadBytes)
        }.sign()
        val text = b64url(payloadBytes) + "." + Base64.encodeToString(sig, Base64.NO_WRAP)
        assertTrue(LicenseVerifier.verifyLicenseText(text, otherPub).isEmpty())
    }

    @Test
    fun license_wildcardUnlocksAll() {
        val (pub, priv) = ephemeralKeys()
        val payloadBytes = payloadJson(LicensePayload(listOf("*"), 0L))
        val sig = Signature.getInstance("Ed25519").apply {
            initSign(priv)
            update(payloadBytes)
        }.sign()
        val text = b64url(payloadBytes) + "." + Base64.encodeToString(sig, Base64.NO_WRAP)
        assertEquals(PackId.entries.toSet(), LicenseVerifier.verifyLicenseText(text, pub))
    }

    // --- Billing restore ---

    @Test
    fun restorePurchases_unlocksOwned() = runBlocking {
        reset()
        val r = repo()
        val billing = FakePackBilling(owned = setOf(PackId.GESTURE, PackId.PDF))
        val unlocker = PackUnlocker(r, billing)
        val restored = unlocker.restorePurchases()
        assertEquals(setOf(PackId.GESTURE, PackId.PDF), restored)
        assertTrue(r.isUnlocked(PackId.PDF))
        assertTrue(r.isUnlocked(PackId.GESTURE))
        assertTrue(r.isUnlocked(PackId.CLASSROOM))
        reset()
    }

    @Test
    fun importLicense_unlocksViaUnlocker() = runBlocking {
        reset()
        val (pub, priv) = ephemeralKeys()
        val r = repo()
        val unlocker = PackUnlocker(r, FakePackBilling(), licensePublicKey = pub)
        val payloadBytes = payloadJson(LicensePayload(listOf("classroom"), 0L))
        val sig = Signature.getInstance("Ed25519").apply {
            initSign(priv)
            update(payloadBytes)
        }.sign()
        val unlocked = unlocker.importLicenseText(
            b64url(payloadBytes) + "." + Base64.encodeToString(sig, Base64.NO_WRAP),
        )
        assertEquals(setOf(PackId.CLASSROOM), unlocked)
        assertTrue(r.isUnlocked(PackId.CLASSROOM))
        reset()
    }

    // --- Classroom Pro ---

    private fun segments(n: Int): List<TranscriptSegment> =
        (0 until n).map { i ->
            TranscriptSegment(id = i.toLong(), startMs = i * 1000L, endMs = i * 1000L + 800L, text = "word $i")
        }

    @Test
    fun classroom_segmentAt_mapsPosition() {
        val segs = segments(5)
        assertEquals(0, ClassroomPro.segmentAt(segs, 0L))
        assertEquals(2, ClassroomPro.segmentAt(segs, 2500L))
        assertEquals(4, ClassroomPro.segmentAt(segs, 99999L))
        assertEquals(-1, ClassroomPro.segmentAt(emptyList(), 100L))
    }

    @Test
    fun classroom_autoChapters_groups() {
        val chapters = ClassroomPro.autoChapters(segments(10), segmentsPerChapter = 4)
        assertEquals(3, chapters.size)
        assertEquals(0L, chapters[0].startMs)
        assertEquals(4000L, chapters[1].startMs)
        assertTrue(chapters[0].title.startsWith("Chapter 1"))
        assertTrue(ClassroomPro.autoChapters(emptyList()).isEmpty())
    }

    @Test
    fun classroom_exportText_hasSections() {
        val text = ClassroomPro.exportText(
            segments(2), ClassroomPro.autoChapters(segments(2)), "sum", "Lecture 1",
        )
        assertTrue(text.contains("## Chapters"))
        assertTrue(text.contains("## Summary"))
        assertTrue(text.contains("## Transcript"))
        assertTrue(text.contains("word 0"))
    }

    @Test
    fun classroom_chaptersAndAutoBackup_persist() = runBlocking {
        reset()
        val r = repo()
        r.setChapters(42L, listOf(Chapter(0L, "Chapter 1 · intro", 0L, listOf(1L))))
        assertEquals(1, r.getChapters(42L).size)
        assertTrue(r.getChapters(43L).isEmpty())
        r.setAutoBackup(true)
        assertTrue(r.autoBackupEnabled.first())
        r.setAutoBackup(false)
        assertFalse(r.autoBackupEnabled.first())
        reset()
    }

    // --- PDF Pro ---

    @Test
    fun pdf_textLayer_concatenates() {
        val content = PageContent(
            textObjects = listOf(
                TextObject(1L, 0f, 0f, 10f, 10f, text = "hello"),
                TextObject(2L, 0f, 0f, 10f, 10f, text = "world"),
            ),
        )
        val layer = PdfPro.buildTextLayer(content)
        assertTrue(layer.contains("hello"))
        assertTrue(layer.contains("world"))
        assertEquals("", PdfPro.buildTextLayer(content, LayeredExportOptions(includeTextLayer = false)))
    }

    @Test
    fun pdf_movePage_reorders() {
        val pages = (1L..4L).map {
            PageSummary(id = it, notebookId = 1L, title = "P$it", order = (it - 1).toInt())
        }
        val moved = PdfPro.movePage(pages, 1L, 3)
        assertEquals(listOf(2L, 3L, 4L, 1L), moved.map { it.id })
        assertEquals(pages.map { it.id }, PdfPro.movePage(pages, 99L, 0).map { it.id })
        assertEquals(3, PdfPro.deletePage(pages, 1L).size)
    }

    // --- Gesture Pro ---

    @Test
    fun gesture_defaultMapping_resolves() {
        val mapping = GesturePro.defaultMapping()
        assertEquals(
            GesturePro.ACTION_UNDO,
            GesturePro.resolveAction(GesturePro.GESTURE_TWO_FINGER_DOUBLE_TAP, mapping),
        )
        val custom = listOf(
            GestureMapping(GesturePro.GESTURE_TWO_FINGER_DOUBLE_TAP, GesturePro.ACTION_EXPORT_PDF),
        )
        assertEquals(
            GesturePro.ACTION_EXPORT_PDF,
            GesturePro.resolveAction(GesturePro.GESTURE_TWO_FINGER_DOUBLE_TAP, custom),
        )
    }

    @Test
    fun bookmarks_addRemovePersist() = runBlocking {
        reset()
        val r = repo()
        r.addBookmark(7L, PageBookmark(11L, "Intro"))
        r.addBookmark(7L, PageBookmark(11L, "Intro")) // dedup
        r.addBookmark(7L, PageBookmark(12L, "Ch.1"))
        assertEquals(2, r.getBookmarks(7L).size)
        r.removeBookmark(7L, 11L)
        assertEquals(listOf(12L), r.getBookmarks(7L).map { it.pageId })
        assertTrue(r.getBookmarks(8L).isEmpty())
        reset()
    }

    @Test
    fun gestureMapping_persists() = runBlocking {
        reset()
        val r = repo()
        assertEquals(GesturePro.defaultMapping(), r.getGestureMapping())
        val custom = listOf(
            GestureMapping(GesturePro.GESTURE_THREE_FINGER_TAP, GesturePro.ACTION_NEW_PAGE),
        )
        r.setGestureMapping(custom)
        assertEquals(custom, r.getGestureMapping())
        r.setGestureMapping(GesturePro.defaultMapping())
        reset()
    }

    // --- helpers ---

    private fun payloadJson(payload: LicensePayload): ByteArray =
        kotlinx.serialization.json.Json.encodeToString(
            LicensePayload.serializer(),
            payload,
        ).toByteArray(Charsets.UTF_8)

    private fun b64url(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

    private object KeyFactoryHolder {
        val factory: KeyFactory = KeyFactory.getInstance("Ed25519")
    }
}

// Keep the unused-import guard quiet for the datastore file helper used in docs.
@Suppress("unused")
private fun Context.testPrefsName(): String = preferencesDataStoreFile("vellum_packs.preferences_pb").absolutePath

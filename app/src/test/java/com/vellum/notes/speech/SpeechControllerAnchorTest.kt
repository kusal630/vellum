package com.vellum.notes.speech

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechControllerAnchorTest {

    @Test
    fun beginRecording_stampsAnchor() = runBlocking {
        val before = System.currentTimeMillis()
        SpeechController.beginRecording(42L)
        val anchor = SpeechController.recordingAnchorWallMs.first()
        val after = System.currentTimeMillis()
        assertTrue("anchor $anchor not in [$before, $after]", anchor in before..after)
        assertEquals(42L, SpeechController.recordingPageId.first())
    }
}

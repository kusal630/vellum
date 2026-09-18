package com.vellum.notes.editor

import com.vellum.notes.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CanvasFontsTest {

    @Test
    fun sansMapsToInter() {
        assertEquals(R.font.inter_regular, CanvasFonts.fontResForFamily("sans-serif", false))
        assertEquals(R.font.inter_semibold, CanvasFonts.fontResForFamily("sans-serif", true))
        assertEquals(R.font.inter_regular, CanvasFonts.fontResForFamily("", false))
    }

    @Test
    fun serifMapsToFraunces() {
        assertEquals(R.font.fraunces_medium, CanvasFonts.fontResForFamily("serif", false))
        assertEquals(R.font.fraunces_semibold, CanvasFonts.fontResForFamily("serif", true))
    }

    @Test
    fun unknownFallsBackToSystem() {
        assertNull(CanvasFonts.fontResForFamily("monospace", false))
        assertNull(CanvasFonts.fontResForFamily("cursive", true))
    }
}

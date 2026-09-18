package com.vellum.notes.render

import android.graphics.RectF
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CullBudgetTest {

    @Test
    fun tenThousandItemCullPass_withinBudget() {
        val items = Array(10_000) { i ->
            val x = (i % 100) * 50f
            val y = (i / 100) * 50f
            RectF(x, y, x + 20f, y + 20f)
        }
        val clip = RectF(1000f, 1000f, 1500f, 1500f)
        val start = System.nanoTime()
        var visible = 0
        for (item in items) {
            if (StrokeCull.isVisible(item, clip)) visible++
        }
        val ms = (System.nanoTime() - start) / 1_000_000L
        assertTrue("expected some visible items, got $visible", visible > 0)
        assertTrue("10k cull took ${ms}ms", ms < 1000L)
    }
}

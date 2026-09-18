package com.vellum.notes.editor

import android.content.Context
import android.graphics.Typeface
import androidx.annotation.FontRes
import androidx.core.content.res.ResourcesCompat
import com.vellum.notes.R

object CanvasFonts {

    @FontRes
    fun fontResForFamily(family: String, bold: Boolean): Int? = when (family.lowercase()) {
        "sans-serif", "sans", "" -> if (bold) R.font.inter_semibold else R.font.inter_regular
        "serif" -> if (bold) R.font.fraunces_semibold else R.font.fraunces_medium
        else -> null
    }

    private val cache = HashMap<Triple<String, Boolean, Int>, Typeface>()

    fun typefaceForFamily(context: Context, family: String, bold: Boolean): Typeface {
        val res = fontResForFamily(family, bold)
            ?: return Typeface.create(
                family.ifBlank { "sans-serif" },
                if (bold) Typeface.BOLD else Typeface.NORMAL,
            )
        val key = Triple(family.lowercase(), bold, res)
        return cache.getOrPut(key) {
            runCatching { ResourcesCompat.getFont(context, res) }.getOrNull()
                ?: Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        }
    }
}

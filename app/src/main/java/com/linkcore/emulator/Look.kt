package com.linkcore.emulator

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.widget.*

object Look {
    val bg = Color.rgb(11, 13, 21)
    val panel = Color.rgb(24, 28, 43)
    val line = Color.rgb(48, 54, 75)
    val muted = Color.rgb(158, 166, 189)
    val purple = Color.rgb(156, 134, 255)
    val mint = Color.rgb(113, 234, 202)
    fun dp(c: Context, n: Int) = (c.resources.displayMetrics.density * n).toInt()
    fun box(c: Context, color: Int = panel, radius: Int = 22, border: Boolean = false) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(c, radius).toFloat()
        if (border) setStroke(dp(c, 1), line)
    }
    fun text(c: Context, value: String, size: Float = 15f, color: Int = Color.WHITE, bold: Boolean = false) = TextView(c).apply {
        text = value; textSize = size; setTextColor(color)
        typeface = Typeface.create(if (bold) "sans-serif-medium" else "sans-serif", Typeface.NORMAL)
        includeFontPadding = false
    }
    fun button(c: Context, value: String, primary: Boolean = false, action: () -> Unit) = Button(c).apply {
        text = value; textSize = 14f; isAllCaps = false
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setTextColor(if (primary) bg else Color.WHITE)
        background = RippleDrawable(ColorStateList.valueOf(0x338888bb), box(c, if (primary) purple else panel, 14, !primary), null)
        setPadding(dp(c, 20), 0, dp(c, 20), 0)
        minWidth = 0; minimumWidth = 0; minHeight = dp(c, 48)
        setOnClickListener { action() }
    }
}

/** Original vector illustration; scales without bundled artwork. */
class ConsoleArt(context: Context) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(c: Canvas) {
        val s = minOf(width / 360f, height / 230f)
        c.save(); c.translate((width - 360*s)/2, (height-230*s)/2); c.scale(s,s)
        p.shader = RadialGradient(180f,115f,145f, intArrayOf(0x55675cbc, 0x00675cbc), null, Shader.TileMode.CLAMP)
        c.drawCircle(180f,115f,145f,p); p.shader = null
        c.save(); c.rotate(-9f,180f,115f)
        p.color = 0xff30374f.toInt(); c.drawRoundRect(36f,37f,324f,200f,42f,42f,p)
        p.color = 0xff444c67.toInt(); c.drawRoundRect(36f,30f,324f,190f,42f,42f,p)
        p.color = Look.bg; c.drawRoundRect(105f,49f,255f,166f,16f,16f,p)
        p.shader = LinearGradient(120f,60f,240f,155f,0xff5f548e.toInt(),0xff273b54.toInt(),Shader.TileMode.CLAMP)
        c.drawRoundRect(117f,61f,243f,150f,7f,7f,p); p.shader = null
        p.color = Look.mint; c.drawRoundRect(163f,89f,183f,123f,6f,6f,p)
        p.color = Look.purple; c.drawRoundRect(180f,89f,200f,123f,6f,6f,p)
        p.color = Look.bg; c.drawRoundRect(58f,92f,94f,104f,3f,3f,p); c.drawRoundRect(70f,80f,82f,116f,3f,3f,p)
        p.color = Look.purple; c.drawCircle(285f,89f,13f,p)
        p.color = Look.muted; c.drawCircle(270f,121f,13f,p)
        p.color = Look.mint; c.drawCircle(92f,56f,3f,p)
        c.restore(); c.restore()
    }
}

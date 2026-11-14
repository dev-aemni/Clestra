package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ClestraBubbleSent
import com.example.ui.theme.ClestraCopper
import com.example.ui.theme.ClestraCream
import com.example.ui.theme.ClestraNeuDark
import com.example.ui.theme.ClestraNeuLight
import com.example.ui.theme.ClestraSand
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * WhatsApp-inspired subtle curved organic doodle wallpaper over the warm Cream Soft UI canvas.
 * Uses hardware-friendly Compose Canvas paths and gentle opacity so message bubbles always pop cleanly.
 */
@Composable
fun ClestraWallpaperBox(
    modifier: Modifier = Modifier,
    intensePattern: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ClestraCream,
                        Color(0xFFF6EFE6),
                        Color(0xFFEEE4D8)
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val baseAlpha = if (intensePattern) 0.085f else 0.055f

            // Flowing organic ribbon curves across the background
            drawCurvedRibbons(w, h, baseAlpha)

            // Repeated WhatsApp-style tiled soft line-art doodles
            val tileSize = 112.dp.toPx()
            val cols = (w / tileSize).toInt() + 2
            val rows = (h / tileSize).toInt() + 2
            val strokeColor = ClestraCopper.copy(alpha = if (intensePattern) 0.095f else 0.055f)
            val altColor = ClestraNeuDark.copy(alpha = if (intensePattern) 0.24f else 0.14f)

            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val ox = c * tileSize + if (r % 2 == 1) tileSize * 0.48f else 0f
                    val oy = r * tileSize
                    val patternIdx = (r * 7 + c * 3) % 8
                    drawDoodleGlyph(
                        patternIdx = patternIdx,
                        center = Offset(ox + tileSize * 0.45f, oy + tileSize * 0.45f),
                        unit = tileSize * 0.20f,
                        primary = strokeColor,
                        secondary = altColor
                    )
                }
            }
        }

        content()
    }
}

private fun DrawScope.drawCurvedRibbons(w: Float, h: Float, baseAlpha: Float) {
    val ribbonTop = Path().apply {
        moveTo(0f, h * 0.14f)
        cubicTo(
            w * 0.28f, h * 0.05f,
            w * 0.68f, h * 0.26f,
            w, h * 0.12f
        )
    }
    drawPath(
        path = ribbonTop,
        color = ClestraCopper.copy(alpha = baseAlpha * 0.9f),
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )

    val ribbonMid = Path().apply {
        moveTo(0f, h * 0.52f)
        cubicTo(
            w * 0.35f, h * 0.64f,
            w * 0.65f, h * 0.40f,
            w, h * 0.56f
        )
    }
    drawPath(
        path = ribbonMid,
        color = ClestraSand.copy(alpha = baseAlpha * 2.1f),
        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
    )

    val ribbonBot = Path().apply {
        moveTo(0f, h * 0.86f)
        cubicTo(
            w * 0.32f, h * 0.77f,
            w * 0.72f, h * 0.95f,
            w, h * 0.82f
        )
    }
    drawPath(
        path = ribbonBot,
        color = ClestraCopper.copy(alpha = baseAlpha * 0.75f),
        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    )

    // Soft ambient radial curves in corners
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(ClestraBubbleSent.copy(alpha = 0.30f), Color.Transparent),
            center = Offset(w * 0.92f, h * 0.08f),
            radius = w * 0.45f
        ),
        center = Offset(w * 0.92f, h * 0.08f),
        radius = w * 0.45f
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(ClestraNeuLight.copy(alpha = 0.45f), Color.Transparent),
            center = Offset(w * 0.08f, h * 0.82f),
            radius = w * 0.5f
        ),
        center = Offset(w * 0.08f, h * 0.82f),
        radius = w * 0.5f
    )
}

private fun DrawScope.drawDoodleGlyph(
    patternIdx: Int,
    center: Offset,
    unit: Float,
    primary: Color,
    secondary: Color
) {
    val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (patternIdx) {
        0 -> {
            // Curved mini chat bubble
            rotate(degrees = -14f, pivot = center) {
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(center.x - unit, center.y - unit * 0.7f),
                    size = Size(unit * 2f, unit * 1.4f),
                    cornerRadius = CornerRadius(unit * 0.5f, unit * 0.5f),
                    style = stroke
                )
                drawCircle(color = secondary, radius = unit * 0.14f, center = Offset(center.x - unit * 0.35f, center.y))
                drawCircle(color = secondary, radius = unit * 0.14f, center = center)
                drawCircle(color = secondary, radius = unit * 0.14f, center = Offset(center.x + unit * 0.35f, center.y))
            }
        }
        1 -> {
            // Soft curved squiggle wave
            val path = Path().apply {
                moveTo(center.x - unit, center.y + unit * 0.25f)
                quadraticTo(center.x - unit * 0.35f, center.y - unit * 0.65f, center.x, center.y)
                quadraticTo(center.x + unit * 0.35f, center.y + unit * 0.65f, center.x + unit, center.y - unit * 0.25f)
            }
            drawPath(path, color = secondary, style = stroke)
        }
        2 -> {
            // Sparkle star
            val path = Path().apply {
                moveTo(center.x, center.y - unit)
                quadraticTo(center.x, center.y, center.x + unit, center.y)
                quadraticTo(center.x, center.y, center.x, center.y + unit)
                quadraticTo(center.x, center.y, center.x - unit, center.y)
                quadraticTo(center.x, center.y, center.x, center.y - unit)
                close()
            }
            drawPath(path, color = primary, style = stroke)
        }
        3 -> {
            // Curved paper plane
            rotate(degrees = 18f, pivot = center) {
                val path = Path().apply {
                    moveTo(center.x - unit, center.y)
                    lineTo(center.x + unit, center.y - unit * 0.6f)
                    lineTo(center.x + unit * 0.1f, center.y + unit * 0.85f)
                    lineTo(center.x - unit * 0.15f, center.y + unit * 0.15f)
                    close()
                }
                drawPath(path, color = secondary, style = stroke)
            }
        }
        4 -> {
            // Delicate heart curve
            val path = Path().apply {
                moveTo(center.x, center.y + unit * 0.75f)
                cubicTo(
                    center.x - unit * 1.15f, center.y + unit * 0.05f,
                    center.x - unit * 0.75f, center.y - unit * 0.85f,
                    center.x, center.y - unit * 0.25f
                )
                cubicTo(
                    center.x + unit * 0.75f, center.y - unit * 0.85f,
                    center.x + unit * 1.15f, center.y + unit * 0.05f,
                    center.x, center.y + unit * 0.75f
                )
            }
            drawPath(path, color = primary, style = stroke)
        }
        5 -> {
            // Crescent moon / arc swirl
            val path = Path()
            val pts = 16
            for (i in 0..pts) {
                val angle = (-0.7 + 1.55 * (i.toFloat() / pts)) * PI
                val x = center.x + (cos(angle) * unit * 0.8f).toFloat()
                val y = center.y + (sin(angle) * unit * 0.8f).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = secondary, style = stroke)
        }
        6 -> {
            // Minimal brackets </> symbol
            val path = Path().apply {
                moveTo(center.x - unit * 0.35f, center.y - unit * 0.45f)
                lineTo(center.x - unit * 0.85f, center.y)
                lineTo(center.x - unit * 0.35f, center.y + unit * 0.45f)
                moveTo(center.x + unit * 0.35f, center.y - unit * 0.45f)
                lineTo(center.x + unit * 0.85f, center.y)
                lineTo(center.x + unit * 0.35f, center.y + unit * 0.45f)
            }
            drawPath(path, color = primary, style = stroke)
        }
        else -> {
            // Concentric curved orb + dot
            drawCircle(color = secondary, radius = unit * 0.7f, center = center, style = stroke)
            drawCircle(color = primary, radius = unit * 0.22f, center = center)
        }
    }
}

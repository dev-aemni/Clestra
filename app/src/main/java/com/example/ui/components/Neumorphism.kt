package com.example.ui.components

import android.graphics.BlurMaskFilter
import android.graphics.Paint as NativePaint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ClestraNeuDark
import com.example.ui.theme.ClestraNeuLight
import com.example.ui.theme.ClestraNeuSurface
import com.example.ui.theme.ClestraNeuWell

private val CssDropShadowColor = Color(0x29000000) // exact rgba(0, 0, 0, .16) from style.css

/**
 * Exact CSS Soft UI raised/extruded shadow modifier matching style.css:
 *   background-color: #ecf0f3;
 *   box-shadow: -5px -5px 12px #ffffff, 5px 5px 12px rgba(0, 0, 0, .16);
 */
fun Modifier.neumorphicRaised(
    cornerRadius: Dp = 20.dp,
    elevation: Dp = 5.dp,
    containerColor: Color = ClestraNeuSurface,
    lightShadowColor: Color = ClestraNeuLight,
    darkShadowColor: Color = ClestraNeuDark,
    accentBorderColor: Color? = null
): Modifier = this.drawBehind {
    if (size.width <= 0f || size.height <= 0f) return@drawBehind
    val radiusPx = cornerRadius.toPx().coerceAtMost(minOf(size.width, size.height) / 2f)
    val corner = CornerRadius(radiusPx, radiusPx)
    val offsetPx = elevation.toPx()
    val spreadScale = (elevation.value / 5f).coerceIn(0.45f, 1.6f)
    val blurPx = (12.dp.toPx() * spreadScale).coerceAtLeast(1f)

    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas

        // Top-left CSS white highlight (-5px -5px 12px #ffffff)
        val lightPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
            color = lightShadowColor.copy(alpha = 0.94f).toArgb()
            maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
        }
        native.drawRoundRect(
            -offsetPx,
            -offsetPx,
            size.width - offsetPx,
            size.height - offsetPx,
            radiusPx,
            radiusPx,
            lightPaint
        )

        // Bottom-right CSS ambient dark shadow (5px 5px 12px rgba(0, 0, 0, .16))
        val darkPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
            color = CssDropShadowColor.toArgb()
            maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
        }
        native.drawRoundRect(
            offsetPx,
            offsetPx,
            size.width + offsetPx,
            size.height + offsetPx,
            radiusPx,
            radiusPx,
            darkPaint
        )
    }

    // Crisp #ECF0F3 surface covering interior
    drawRoundRect(
        color = containerColor,
        topLeft = Offset.Zero,
        size = size,
        cornerRadius = corner
    )

    if (accentBorderColor != null) {
        drawRoundRect(
            color = accentBorderColor.copy(alpha = 0.32f),
            topLeft = Offset(0.5.dp.toPx(), 0.5.dp.toPx()),
            size = Size(
                (size.width - 1.dp.toPx()).coerceAtLeast(0f),
                (size.height - 1.dp.toPx()).coerceAtLeast(0f)
            ),
            cornerRadius = corner,
            style = Stroke(width = 1.dp.toPx())
        )
    } else {
        // Whisper-thin contour for sharp curved geometry on multi-card lists
        drawRoundRect(
            color = darkShadowColor.copy(alpha = 0.14f),
            topLeft = Offset(0.35.dp.toPx(), 0.35.dp.toPx()),
            size = Size(
                (size.width - 0.7.dp.toPx()).coerceAtLeast(0f),
                (size.height - 0.7.dp.toPx()).coerceAtLeast(0f)
            ),
            cornerRadius = corner,
            style = Stroke(width = 0.6.dp.toPx())
        )
    }
}

/**
 * Asymmetric Soft UI message bubble matching style.css Gaussian shadows
 * without harsh blown-out white top overlays so curved depth reads clearly.
 */
fun Modifier.neumorphicBubble(
    topStart: Dp = 20.dp,
    topEnd: Dp = 20.dp,
    bottomStart: Dp = 20.dp,
    bottomEnd: Dp = 20.dp,
    elevation: Dp = 4.dp,
    topColor: Color,
    bottomColor: Color,
    lightShadowColor: Color = ClestraNeuLight,
    darkShadowColor: Color = ClestraNeuDark,
    accentBorderColor: Color? = null
): Modifier = this.drawBehind {
    if (size.width <= 0f || size.height <= 0f) return@drawBehind
    val tl = CornerRadius(topStart.toPx())
    val tr = CornerRadius(topEnd.toPx())
    val bl = CornerRadius(bottomStart.toPx())
    val br = CornerRadius(bottomEnd.toPx())
    val offsetPx = elevation.toPx()
    val blurPx = (10.dp.toPx() * (elevation.value / 4f).coerceIn(0.6f, 1.5f)).coerceAtLeast(1f)

    fun buildBubblePath(dx: Float, dy: Float, inset: Float = 0f): Path {
        val rr = RoundRect(
            rect = Rect(
                left = dx + inset,
                top = dy + inset,
                right = dx + size.width - inset,
                bottom = dy + size.height - inset
            ),
            topLeft = tl,
            topRight = tr,
            bottomRight = br,
            bottomLeft = bl
        )
        return Path().apply { addRoundRect(rr) }
    }

    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas

        val lightPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
            color = lightShadowColor.copy(alpha = 0.90f).toArgb()
            maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
        }
        native.drawPath(buildBubblePath(-offsetPx, -offsetPx).asAndroidPath(), lightPaint)

        val darkPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
            color = CssDropShadowColor.toArgb()
            maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
        }
        native.drawPath(buildBubblePath(offsetPx, offsetPx).asAndroidPath(), darkPaint)
    }

    val mainPath = buildBubblePath(0f, 0f)
    drawPath(
        path = mainPath,
        brush = Brush.linearGradient(
            colors = listOf(topColor, bottomColor),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        )
    )

    val rimColor = accentBorderColor?.copy(alpha = 0.28f) ?: darkShadowColor.copy(alpha = 0.16f)
    drawPath(
        path = buildBubblePath(0f, 0f, inset = 0.4.dp.toPx()),
        color = rimColor,
        style = Stroke(width = 0.7.dp.toPx())
    )
}

/**
 * Exact CSS Soft UI inset/recessed modifier matching style.css input:
 *   background-color: #ecf0f3;
 *   box-shadow: inset -5px -5px 12px #ffffff, inset 5px 5px 12px rgba(0, 0, 0, .16);
 */
fun Modifier.neumorphicPressed(
    cornerRadius: Dp = 20.dp,
    containerColor: Color = ClestraNeuWell,
    lightShadowColor: Color = ClestraNeuLight,
    darkShadowColor: Color = ClestraNeuDark
): Modifier = this.drawBehind {
    if (size.width <= 0f || size.height <= 0f) return@drawBehind
    val radiusPx = cornerRadius.toPx().coerceAtMost(minOf(size.width, size.height) / 2f)
    val corner = CornerRadius(radiusPx, radiusPx)

    // Base recessed well (#ecf0f3)
    drawRoundRect(
        color = containerColor,
        topLeft = Offset.Zero,
        size = size,
        cornerRadius = corner
    )

    // Clipped Gaussian inner shadows matching CSS `inset 5px 5px 12px rgba(0,0,0,.16)` & `inset -5px -5px 12px #ffffff`
    drawIntoCanvas { canvas ->
        val clipPath = Path().apply {
            addRoundRect(RoundRect(0f, 0f, size.width, size.height, radiusPx, radiusPx))
        }
        canvas.save()
        canvas.clipPath(clipPath)

        val offsetPx = 5.dp.toPx()
        val blurPx = 11.dp.toPx()
        val strokeW = blurPx * 1.35f
        val halfStroke = strokeW / 2f

        fun drawInsetRing(dx: Float, dy: Float, argb: Int) {
            val paint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
                style = NativePaint.Style.STROKE
                strokeWidth = strokeW
                color = argb
                maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.nativeCanvas.drawRoundRect(
                dx - halfStroke,
                dy - halfStroke,
                size.width + dx + halfStroke,
                size.height + dy + halfStroke,
                radiusPx + halfStroke * 0.45f,
                radiusPx + halfStroke * 0.45f,
                paint
            )
        }

        // Top-left recessed dark shadow
        drawInsetRing(offsetPx, offsetPx, CssDropShadowColor.toArgb())
        // Bottom-right recessed white glow
        drawInsetRing(-offsetPx, -offsetPx, lightShadowColor.copy(alpha = 0.92f).toArgb())

        canvas.restore()
    }

    // Subtle inner bevel contour
    drawRoundRect(
        color = darkShadowColor.copy(alpha = 0.16f),
        topLeft = Offset(0.4.dp.toPx(), 0.4.dp.toPx()),
        size = Size(
            (size.width - 0.8.dp.toPx()).coerceAtLeast(0f),
            (size.height - 0.8.dp.toPx()).coerceAtLeast(0f)
        ),
        cornerRadius = corner,
        style = Stroke(width = 0.7.dp.toPx())
    )
}

/**
 * Interactive tactile scale-on-press modifier for Soft UI buttons and cards.
 */
fun Modifier.neumorphicPressEffect(
    interactionSource: MutableInteractionSource
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "neu_press_scale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun NeumorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    elevation: Dp = 5.dp,
    containerColor: Color = ClestraNeuSurface,
    accentBorderColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val clickMod = if (onClick != null) {
        Modifier
            .neumorphicPressEffect(interactionSource)
            .clip(RoundedCornerShape(cornerRadius))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
    } else Modifier

    Box(
        modifier = modifier
            .neumorphicRaised(
                cornerRadius = cornerRadius,
                elevation = elevation,
                containerColor = containerColor,
                accentBorderColor = accentBorderColor
            )
            .then(clickMod)
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun NeumorphicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cornerRadius: Dp = 18.dp,
    containerColor: Color = ClestraNeuSurface,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(cornerRadius)

    val depthMod = if (pressed || !enabled) {
        Modifier.neumorphicPressed(cornerRadius = cornerRadius, containerColor = containerColor)
    } else {
        Modifier.neumorphicRaised(cornerRadius = cornerRadius, elevation = 5.dp, containerColor = containerColor)
    }

    Box(
        modifier = modifier
            .neumorphicPressEffect(interactionSource)
            .then(depthMod)
            .clip(shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
        content = content
    )
}

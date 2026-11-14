package com.example.ui.components

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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ClestraNeuDark
import com.example.ui.theme.ClestraNeuLight
import com.example.ui.theme.ClestraNeuSurface
import com.example.ui.theme.ClestraNeuWell

/**
 * Tactile clean Soft UI (Neumorphism) modifier:
 * Draws warm diffused bottom-right elevation shadows, smooth rounded surface,
 * and a refined hairline contour without harsh white edge artifacts.
 */
fun Modifier.neumorphicRaised(
    cornerRadius: Dp = 18.dp,
    elevation: Dp = 5.dp,
    containerColor: Color = ClestraNeuSurface,
    lightShadowColor: Color = ClestraNeuLight,
    darkShadowColor: Color = ClestraNeuDark,
    accentBorderColor: Color? = null
): Modifier = this.drawBehind {
    val radiusPx = cornerRadius.toPx()
    val corner = CornerRadius(radiusPx, radiusPx)
    val e = elevation.toPx()

    // Soft ambient warm drop shadow underneath & subtly toward bottom-right
    val darkLayers = listOf(1.15f to 0.08f, 0.75f to 0.14f, 0.38f to 0.18f)
    darkLayers.forEach { (mult, alpha) ->
        val d = e * mult
        drawRoundRect(
            color = darkShadowColor.copy(alpha = alpha),
            topLeft = Offset(d * 0.45f, d),
            size = size,
            cornerRadius = CornerRadius(radiusPx + d * 0.12f, radiusPx + d * 0.12f)
        )
    }

    // Main extruded surface
    drawRoundRect(
        color = containerColor,
        topLeft = Offset.Zero,
        size = size,
        cornerRadius = corner
    )

    // Delicate contour hairline (no harsh thick white top-left bar)
    val hairlineAlpha = if (accentBorderColor != null) 0.42f else 0.22f
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            accentBorderColor?.copy(alpha = hairlineAlpha) ?: lightShadowColor.copy(alpha = 0.14f),
            darkShadowColor.copy(alpha = 0.12f),
            accentBorderColor?.copy(alpha = 0.28f) ?: darkShadowColor.copy(alpha = 0.24f)
        ),
        start = Offset.Zero,
        end = Offset(size.width, size.height)
    )
    drawRoundRect(
        brush = borderBrush,
        topLeft = Offset(0.4.dp.toPx(), 0.4.dp.toPx()),
        size = Size(
            (size.width - 0.8.dp.toPx()).coerceAtLeast(0f),
            (size.height - 0.8.dp.toPx()).coerceAtLeast(0f)
        ),
        cornerRadius = corner,
        style = Stroke(width = if (accentBorderColor != null) 1.1.dp.toPx() else 0.75.dp.toPx())
    )
}

/**
 * Tactile Soft UI bubble modifier with signature asymmetric corner radii,
 * soft tonal gradient fill, and gentle diffused depth shadow.
 */
fun Modifier.neumorphicBubble(
    topStart: Dp = 20.dp,
    topEnd: Dp = 20.dp,
    bottomStart: Dp = 20.dp,
    bottomEnd: Dp = 20.dp,
    elevation: Dp = 3.dp,
    topColor: Color,
    bottomColor: Color,
    lightShadowColor: Color = ClestraNeuLight,
    darkShadowColor: Color = ClestraNeuDark,
    accentBorderColor: Color? = null
): Modifier = this.drawBehind {
    val tl = CornerRadius(topStart.toPx())
    val tr = CornerRadius(topEnd.toPx())
    val bl = CornerRadius(bottomStart.toPx())
    val br = CornerRadius(bottomEnd.toPx())
    val e = elevation.toPx()

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

    drawPath(
        path = buildBubblePath(e * 0.45f, e * 0.95f),
        color = darkShadowColor.copy(alpha = 0.14f)
    )
    drawPath(
        path = buildBubblePath(e * 0.25f, e * 0.50f),
        color = darkShadowColor.copy(alpha = 0.18f)
    )

    val mainPath = buildBubblePath(0f, 0f)
    drawPath(
        path = mainPath,
        brush = Brush.linearGradient(
            colors = listOf(topColor, bottomColor),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        )
    )

    val bevelBrush = Brush.linearGradient(
        colors = listOf(
            accentBorderColor?.copy(alpha = 0.38f) ?: lightShadowColor.copy(alpha = 0.12f),
            darkShadowColor.copy(alpha = 0.10f),
            accentBorderColor?.copy(alpha = 0.20f) ?: darkShadowColor.copy(alpha = 0.22f)
        ),
        start = Offset.Zero,
        end = Offset(size.width, size.height)
    )
    drawPath(
        path = buildBubblePath(0f, 0f, inset = 0.5.dp.toPx()),
        brush = bevelBrush,
        style = Stroke(width = if (accentBorderColor != null) 1.05.dp.toPx() else 0.75.dp.toPx())
    )
}

/**
 * Tactile recessed/concave Neumorphism (Soft UI) modifier:
 * Ideal for text inputs, search wells, recessed badges, and depressed states.
 */
fun Modifier.neumorphicPressed(
    cornerRadius: Dp = 16.dp,
    containerColor: Color = ClestraNeuWell,
    lightShadowColor: Color = ClestraNeuLight,
    darkShadowColor: Color = ClestraNeuDark
): Modifier = this.drawBehind {
    val radiusPx = cornerRadius.toPx()
    val corner = CornerRadius(radiusPx, radiusPx)

    // Base recessed well
    drawRoundRect(
        color = containerColor,
        topLeft = Offset.Zero,
        size = size,
        cornerRadius = corner
    )

    // Gentle inward top-left shadow wash
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                darkShadowColor.copy(alpha = 0.24f),
                Color.Transparent,
                lightShadowColor.copy(alpha = 0.10f)
            ),
            start = Offset.Zero,
            end = Offset(size.width * 0.35f, size.height * 0.5f)
        ),
        topLeft = Offset.Zero,
        size = size,
        cornerRadius = corner
    )

    // Clean recessed rim without bright white border glare
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                darkShadowColor.copy(alpha = 0.34f),
                darkShadowColor.copy(alpha = 0.14f),
                lightShadowColor.copy(alpha = 0.20f)
            ),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        ),
        topLeft = Offset(0.45.dp.toPx(), 0.45.dp.toPx()),
        size = Size(
            (size.width - 0.9.dp.toPx()).coerceAtLeast(0f),
            (size.height - 0.9.dp.toPx()).coerceAtLeast(0f)
        ),
        cornerRadius = corner,
        style = Stroke(width = 0.9.dp.toPx())
    )
}

/**
 * Interactive tactile scale-on-press modifier for Neumorphic buttons and cards.
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
    cornerRadius: Dp = 18.dp,
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
    cornerRadius: Dp = 14.dp,
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

package app.rope.android.ui

import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.rope.android.RopeBlack
import app.rope.android.RopeGrayDark
import app.rope.android.RopeGrayLight
import app.rope.android.RopeShapes
import kotlinx.coroutines.delay

@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val resolver = context.contentResolver
        val animator = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        val transition = Settings.Global.getFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
        SplashTiming.isReduceMotion(animator, transition)
    }
}

fun ropeKnotPath(): Path = Path().apply {
    moveTo(22f, 64f)
    cubicTo(22f, 34f, 50f, 30f, 64f, 64f)
    cubicTo(78f, 98f, 106f, 94f, 106f, 64f)
    cubicTo(106f, 34f, 78f, 30f, 64f, 64f)
    cubicTo(50f, 98f, 22f, 94f, 22f, 64f)
}

@Composable
fun RopeLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 128.dp,
    animate: Boolean = true,
    loop: Boolean = false,
    breathe: Boolean = true,
    replayKey: Int = 0,
    underColor: Color = RopeGrayDark,
    strokeColor: Color = MaterialTheme.colorScheme.secondary,
) {
    val reduce = rememberReduceMotion()
    var drawn by remember(replayKey) { mutableFloatStateOf(if (animate && !reduce) 0f else 1f) }
    LaunchedEffect(animate, reduce, loop, replayKey) {
        if (!animate || reduce) {
            drawn = 1f
            return@LaunchedEffect
        }
        do {
            drawn = 0f
            val startNs = withFrameNanos { it }
            while (drawn < 1f) {
                val now = withFrameNanos { it }
                val elapsedMs = ((now - startNs) / 1_000_000L).coerceAtLeast(0L)
                drawn = SplashTiming.paintProgress(elapsedMs)
            }
            drawn = 1f
            if (loop) delay(420)
        } while (loop)
    }
    val live = animate && !reduce && breathe
    val inf = rememberInfiniteTransition(label = "knot")
    val pulseRaw by inf.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(tween(2800), RepeatMode.Reverse),
        label = "pulse",
    )
    val tiltRaw by inf.animateFloat(
        initialValue = -2.2f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(tween(5200), RepeatMode.Reverse),
        label = "tilt",
    )
    val pulse = if (live) pulseRaw else 1f
    val tilt = if (live) tiltRaw else 0f
    Canvas(
        modifier
            .size(size)
            .graphicsLayer {
                scaleX = pulse
                scaleY = pulse
                rotationZ = tilt
            },
    ) {
        val dim = this.size.minDimension
        val scale = dim / 128f
        rotate(-16f, Offset(this.size.width / 2f, this.size.height / 2f)) {
            val src = ropeKnotPath()
            src.transform(
                Matrix().apply { scale(scale, scale) },
            )
            val measure = PathMeasure()
            measure.setPath(src, false)
            val dest = Path()
            measure.getSegment(0f, measure.length * drawn, dest, true)
            val underW = 12f * scale
            val strokeW = 8.2f * scale
            drawPath(
                src,
                underColor,
                style = Stroke(width = underW, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawPath(
                dest,
                strokeColor,
                style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

@Composable
fun RopeKnot(
    modifier: Modifier = Modifier,
    size: Dp = 128.dp,
    animate: Boolean = true,
    underColor: Color = RopeGrayDark,
    strokeColor: Color = MaterialTheme.colorScheme.secondary,
) {
    RopeLogoMark(
        modifier = modifier,
        size = size,
        animate = animate,
        underColor = underColor,
        strokeColor = strokeColor,
    )
}

@Composable
fun RopeSplash(
    modifier: Modifier = Modifier,
    caption: String? = null,
    loop: Boolean = true,
    compact: Boolean = false,
) {
    val reduce = rememberReduceMotion()
    Box(
        modifier
            .fillMaxSize()
            .background(RopeBlack),
        contentAlignment = Alignment.Center,
    ) {
        BrandBackdrop(ink = true)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 28.dp),
        ) {
            RopeLogoMark(
                size = if (compact) 92.dp else 148.dp,
                animate = !reduce,
                loop = loop && !reduce,
                breathe = true,
                underColor = RopeGrayDark,
                strokeColor = RopeGrayLight,
            )
            if (!caption.isNullOrBlank()) {
                Spacer(Modifier.height(18.dp))
                Text(
                    caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = RopeGrayLight.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun BrandBackdrop(modifier: Modifier = Modifier, ink: Boolean = false) {
    val reduce = rememberReduceMotion()
    val inf = rememberInfiniteTransition(label = "orbs")
    val a by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(16_000), RepeatMode.Reverse), "oa")
    val b by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(20_000), RepeatMode.Reverse), "ob")
    val c by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(24_000), RepeatMode.Reverse), "oc")
    val secondary = if (ink) RopeGrayLight.copy(alpha = 0.16f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
    val variant = if (ink) RopeGrayDark.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val outline = if (ink) RopeGrayDark.copy(alpha = 0.28f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val ax = if (reduce) 0.18f else 0.16f + 0.05f * a
        val ay = if (reduce) 0.22f else 0.20f + 0.04f * a
        val bx = if (reduce) 0.82f else 0.80f - 0.04f * b
        val by = if (reduce) 0.18f else 0.16f + 0.05f * b
        val cx = if (reduce) 0.52f else 0.48f + 0.04f * c
        val cy = if (reduce) 0.72f else 0.70f - 0.05f * c
        fun orb(x: Float, y: Float, r: Float, color: Color) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color, Color.Transparent),
                    center = Offset(w * x, h * y),
                    radius = r,
                ),
                radius = r,
                center = Offset(w * x, h * y),
            )
        }
        orb(ax, ay, w * 0.46f, variant)
        orb(bx, by, w * 0.34f, secondary)
        orb(cx, cy, w * 0.50f, outline)
    }
}

@Composable
fun FadeIn(
    delayMs: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val reduce = rememberReduceMotion()
    val instant = reduce || delayMs <= 0
    var shown by remember { mutableStateOf(instant) }
    LaunchedEffect(delayMs, reduce) {
        if (!instant) {
            shown = false
            delay(delayMs.toLong())
        }
        shown = true
    }
    val alpha by animateFloatAsState(if (shown) 1f else 0f, tween(if (instant) 0 else 180), label = "fadeA")
    val ty by animateFloatAsState(if (shown) 0f else 8f, tween(if (instant) 0 else 180), label = "fadeY")
    Box(
        modifier.graphicsLayer {
            this.alpha = alpha
            translationY = ty
        },
    ) { content() }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && onClick != null) 0.985f else 1f, tween(120), label = "cardPress")
    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = LocalIndication.current,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            ),
        shape = RoundedCornerShape(RopeShapes.card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content,
        )
    }
}

@Composable
fun GlowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val reduce = rememberReduceMotion()
    val inf = rememberInfiniteTransition(label = "glow")
    val glowRaw by inf.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.20f,
        animationSpec = infiniteRepeatable(tween(3600), RepeatMode.Reverse),
        label = "glowA",
    )
    val glow = if (reduce) 0.10f else glowRaw
    val tint = MaterialTheme.colorScheme.primary.copy(alpha = glow)
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        modifier = modifier
            .height(52.dp)
            .shadow(10.dp, CircleShape, ambientColor = tint, spotColor = tint),
    ) {
        Text(text)
    }
}

@Composable
fun QuietButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        modifier = modifier.height(52.dp),
    ) { Text(text) }
}

@Composable
fun RopeEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val reduce = rememberReduceMotion()
    val inf = rememberInfiniteTransition(label = "empty")
    val yRaw by inf.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(2800), RepeatMode.Reverse),
        label = "emptyY",
    )
    FadeIn(60, modifier.fillMaxSize().padding(24.dp)) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.graphicsLayer { translationY = if (reduce) 0f else yRaw }) {
                RopeLogoMark(
                    size = 76.dp,
                    animate = !reduce,
                    loop = !reduce,
                    strokeColor = RopeGrayLight,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, start = 12.dp, end = 12.dp),
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(16.dp))
                GlowButton(actionLabel, onAction, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun Modifier.pressScale(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, tween(120), label = "rowPress")
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clip(RoundedCornerShape(RopeShapes.card))
        .clickable(
            interactionSource = interaction,
            indication = LocalIndication.current,
            onClick = onClick,
        )
}

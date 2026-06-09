package io.github.xororz.localdream.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.TileMode

// ==================== 现代化渐变色 ====================

// 紫色浪漫渐变
val PurpleRomanceGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF667EEA),
        Color(0xFF764BA2)
    )
)

// 清新薄荷渐变
val FreshMintGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF11998E),
        Color(0xFF38EF7D)
    )
)

// 热情夕阳渐变
val SunsetGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFC466B),
        Color(0xFF3F5EFB)
    )
)

// 深海蓝渐变
val OceanGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF2E3192),
        Color(0xFF1BFFFF)
    )
)

// 珊瑚渐变
val CoralGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFF6B6B),
        Color(0xFFFFE66D)
    )
)

// 极光渐变
val AuroraGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF4FFFB0),
        Color(0xFF00D9FF),
        Color(0xFF7B2FF7)
    )
)

// 科技蓝渐变
val TechBlueGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF0F2027),
        Color(0xFF203A43),
        Color(0xFF2C5364)
    )
)

// 霓虹渐变
val NeonGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFF80759),
        Color(0xFFBC4E9C)
    )
)

// ==================== 动态渐变 ====================

@Composable
fun rememberAnimatedGradientBrush(
    colors: List<Color>,
    duration: Int = 3000
): Brush {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val animatedValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientAnim"
    )
    
    return remember(colors, animatedValue) {
        Brush.linearGradient(
            colors = colors,
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(
                1000f,
                1000f
            ),
            tileMode = TileMode.Mirror
        )
    }
}

// ==================== 动画状态 ====================

@Composable
fun rememberShimmerState(): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    return infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    ).value
}

@Composable
fun rememberPulseState(): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    return infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    ).value
}


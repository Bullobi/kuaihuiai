package comkuaihuiai.ui.components

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.xororz.localdream.ui.theme.ProcessingColor
import io.github.xororz.localdream.ui.theme.BrandGradient
import kotlin.math.cos
import kotlin.math.sin

/**
 * 炫酷的生成中动画组件
 */

// Particle data class for animation effects
data class Particle(
    val x: Float,
    val y: Float,
    val radius: Float,
    val speed: Float,
    val alpha: Float
)

@Composable
fun CoolGeneratingAnimation(
    progress: Float,
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cool")
    
    // 旋转动画
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // 脉冲动画
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    // 颜色渐变偏移
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient"
    )
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // 外圈旋转动画
        Canvas(
            modifier = Modifier
                .size(160.dp)
        ) {
            val strokeWidth = 8.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            
            rotate(rotation) {
                // 渐变色弧
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            BrandGradient[0],
                            BrandGradient[1],
                            BrandGradient[0]
                        )
                    ),
                    startAngle = 0f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(radius * 2, radius * 2)
                )
            }
            
            // 进度背景
            drawArc(
                color = Color.White.copy(alpha = 0.1f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth),
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(radius * 2, radius * 2)
            )
            
            // 进度弧
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        BrandGradient[0],
                        BrandGradient[1]
                    )
                ),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(radius * 2, radius * 2)
            )
        }
        
        // 中心内容
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Step $currentStep/$totalSteps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * 脉冲圆点动画
 */
@Composable
fun PulsingDot(
    modifier: Modifier = Modifier,
    color: Color = ProcessingColor,
    size: Dp = 12.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dot")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )
    
    Canvas(modifier = modifier.size(size * 2)) {
        // 外圈脉冲
        drawCircle(
            color = color.copy(alpha = alpha * 0.3f),
            radius = size.toPx() * scale
        )
        
        // 核心圆点
        drawCircle(
            color = color,
            radius = size.toPx()
        )
    }
}

/**
 * 粒子效果背景
 */
@Composable
fun ParticleBackground(
    modifier: Modifier = Modifier,
    particleCount: Int = 20,
    color: Color = BrandGradient[0]
) {
    val particles = remember {
        List(particleCount) {
            Particle(
                x = Math.random().toFloat(),
                y = Math.random().toFloat(),
                radius = (4 + Math.random() * 8).toFloat(),
                speed = (0.001 + Math.random() * 0.003).toFloat(),
                alpha = (0.2 + Math.random() * 0.5).toFloat()
            )
        }
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "particle")
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleAnim"
    )
    
    Canvas(modifier = modifier) {
        particles.forEach { particle ->
            val y = (particle.y + animationProgress * particle.speed) % 1f
            drawCircle(
                color = color.copy(alpha = particle.alpha),
                radius = particle.radius,
                center = Offset(
                    particle.x * size.width,
                    y * size.height
                )
            )
        }
    }
}

/**
 * 加载骨架屏
 */
@Composable
fun ShimmerSkeleton(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.3f),
        Color.LightGray.copy(alpha = 0.5f),
        Color.LightGray.copy(alpha = 0.3f)
    )
    
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = shimmerColors,
                    start = Offset(translateAnim - 200f, 0f),
                    end = Offset(translateAnim, 0f)
                )
            )
    )
}

/**
 * 卡片加载骨架
 */
@Composable
fun CardSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        ShimmerSkeleton(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Spacer(modifier = Modifier.height(12.dp))
        ShimmerSkeleton(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(20.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        ShimmerSkeleton(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(16.dp)
        )
    }
}

/**
 * 渐变按钮
 */
@Composable
fun GradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    gradientColors: List<Color> = BrandGradient,
    contentBlock: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "btn")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient"
    )
    
    androidx.compose.material3.Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color.Gray.copy(alpha = 0.3f),
            disabledContentColor = Color.Gray
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        contentBlock()
    }
}

/**
 * 动画过渡组件
 */
@Composable
fun <T> AnimatedContentEx(
    targetState: T,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedContentScope.(T) -> Unit
) {
    androidx.compose.animation.AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
        },
        label = "animated",
        content = content
    )
}

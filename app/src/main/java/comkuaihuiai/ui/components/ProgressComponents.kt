package comkuaihuiai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.xororz.localdream.ui.theme.BrandGradient

/**
 * 现代化进度指示器 - 带渐变和动画效果
 */
@Composable
fun ModernProgressIndicator(
    progress: Float,
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    strokeWidth: Dp = 8.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "progress")
    
    // 旋转动画
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // 脉冲动画
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // 背景圆环
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val radius = (size.toPx() - stroke) / 2
            
            // 背景
            drawCircle(
                color = Color.White.copy(alpha = 0.1f),
                radius = radius,
                style = Stroke(width = stroke)
            )
            
            // 旋转渐变效果
            rotate(rotation) {
                // 外圈光晕
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            BrandGradient[0].copy(alpha = 0.3f * pulseAlpha),
                            BrandGradient[1].copy(alpha = 0.5f * pulseAlpha),
                            BrandGradient[0].copy(alpha = 0.3f * pulseAlpha)
                        )
                    ),
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = stroke * 1.5f, cap = StrokeCap.Round)
                )
            }
            
            // 进度弧
            drawArc(
                brush = Brush.sweepGradient(
                    colors = BrandGradient
                ),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        
        // 中心内容
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "Step $currentStep",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * 线性进度条 - 带渐变效果
 */
@Composable
fun GradientLinearProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    backgroundColor: Color = Color.White.copy(alpha = 0.2f)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "linear")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    Canvas(
        modifier = modifier
            .height(height)
            .background(backgroundColor, RoundedCornerShape(height / 2))
    ) {
        val progressWidth = size.width * progress
        
        // 渐变进度
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = BrandGradient,
                startX = 0f,
                endX = progressWidth
            ),
            size = Size(progressWidth, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(height.toPx() / 2)
        )
        
        // 高光效果
        if (progress > 0.1f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.4f),
                        Color.White.copy(alpha = 0f)
                    ),
                    startX = progressWidth * shimmerOffset,
                    endX = progressWidth * shimmerOffset + 50f
                ),
                topLeft = Offset(progressWidth * shimmerOffset, 0f),
                size = Size(50f, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(height.toPx() / 2)
            )
        }
    }
}

/**
 * 步骤指示器
 */
@Composable
fun StepIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(totalSteps.coerceAtMost(20)) { index ->
            val isCompleted = index < currentStep
            val isCurrent = index == currentStep
            
            Box(
                modifier = Modifier
                    .width(if (isCurrent) 16.dp else 6.dp)
                    .height(6.dp)
                    .background(
                        color = when {
                            isCompleted -> BrandGradient[0]
                            isCurrent -> BrandGradient[1]
                            else -> Color.White.copy(alpha = 0.2f)
                        },
                        shape = RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}

/**
 * 动画进度文字
 */
@Composable
fun AnimatedProgressText(
    text: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "text")
    val dotCount by infiniteTransition.animateValue(
        initialValue = 0,
        targetValue = 3,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Restart
        ),
        label = "dots"
    )
    
    val dots = ".".repeat((dotCount % 3) + 1)
    
    Text(
        text = text + dots,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    )
}

/**
 * 等待动画 - 多个圆点
 */
@Composable
fun WaitingDots(
    modifier: Modifier = Modifier,
    dotCount: Int = 3,
    color: Color = BrandGradient[0]
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(dotCount) { index ->
            val infiniteTransition = rememberInfiniteTransition(label = "dot_$index")
            
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 600,
                        delayMillis = index * 200
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale_$index"
            )
            
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 600,
                        delayMillis = index * 200
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha_$index"
            )
            
            Canvas(modifier = Modifier.size(10.dp)) {
                drawCircle(
                    color = color.copy(alpha = alpha),
                    radius = 5.dp.toPx() * scale
                )
            }
        }
    }
}

/**
 * 状态指示器 - 带图标和文字
 */
@Composable
fun StatusIndicator(
    status: GenerationStatus,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (status) {
            GenerationStatus.IDLE -> {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            Color.Gray.copy(alpha = 0.5f),
                            CircleShape
                        )
                )
                Text("就绪", style = MaterialTheme.typography.bodySmall)
            }
            GenerationStatus.GENERATING -> {
                WaitingDots(dotCount = 3)
                AnimatedProgressText("生成中")
            }
            GenerationStatus.COMPLETED -> {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            Color(0xFF4CAF50),
                            CircleShape
                        )
                )
                Text("完成", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50))
            }
            GenerationStatus.ERROR -> {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            Color(0xFFF44336),
                            CircleShape
                        )
                )
                Text("错误", style = MaterialTheme.typography.bodySmall, color = Color(0xFFF44336))
            }
        }
    }
}

enum class GenerationStatus {
    IDLE,
    GENERATING,
    COMPLETED,
    ERROR
}

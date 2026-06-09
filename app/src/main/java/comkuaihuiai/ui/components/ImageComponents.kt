package comkuaihuiai.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import io.github.xororz.localdream.ui.theme.BrandGradient
import io.github.xororz.localdream.ui.theme.ProcessingColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 高性能图片组件 - 支持缓存、懒加载和过渡动画
 */
@Composable
fun AsyncImageView(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: @Composable () -> Unit = {
        ImagePlaceholder(modifier = modifier)
    },
    error: @Composable () -> Unit = {
        ImageError(modifier = modifier)
    },
    onClick: (() -> Unit)? = null
) {
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    // 加载动画
    val alpha by animateFloatAsState(
        targetValue = if (isLoading) 0.5f else 1f,
        animationSpec = tween(300),
        label = "alpha"
    )
    
    LaunchedEffect(model) {
        if (model is String && (model.startsWith("http") || model.startsWith("file://") || model.startsWith("/"))) {
            isLoading = true
            isError = false
            try {
                bitmap = withContext(Dispatchers.IO) {
                    loadOptimizedBitmap(model)
                }
            } catch (e: Exception) {
                isError = true
            } finally {
                isLoading = false
            }
        }
    }
    
    Box(
        modifier = modifier
            .graphicsLayer { this.alpha = alpha }
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading || isError -> {
                if (isError) {
                    error()
                } else {
                    placeholder()
                }
            }
            bitmap != null -> {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }
            else -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(model)
                        .crossfade(true)
                        .build(),
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                    onLoading = { isLoading = true },
                    onSuccess = { isLoading = false },
                    onError = { isError = true }
                )
            }
        }
    }
}

/**
 * 优化图片加载 - 支持采样和缩放
 */
private fun loadOptimizedBitmap(path: String, maxSize: Int = 1024): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)
        
        // 计算采样率
        val sampleSize = calculateSampleSize(options.outWidth, options.outHeight, maxSize)
        options.inJustDecodeBounds = false
        options.inSampleSize = sampleSize
        
        val bitmap = BitmapFactory.decodeFile(path, options)
        
        // 缩放到目标大小
        if (bitmap != null && (bitmap.width > maxSize || bitmap.height > maxSize)) {
            val scale = maxSize.toFloat() / maxOf(bitmap.width, bitmap.height)
            val matrix = Matrix().apply { setScale(scale, scale) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    } catch (e: Exception) {
        null
    }
}

private fun calculateSampleSize(width: Int, height: Int, targetSize: Int): Int {
    var sampleSize = 1
    while (width / sampleSize > targetSize || height / sampleSize > targetSize) {
        sampleSize *= 2
    }
    return sampleSize
}

/**
 * 图片加载占位符 - 带动画
 */
@Composable
fun ImagePlaceholder(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "placeholder")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )
    
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF2A2A2A).copy(alpha = alpha),
                        Color(0xFF1A1A1A).copy(alpha = alpha),
                        Color(0xFF2A2A2A).copy(alpha = alpha)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Image,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = Color.White.copy(alpha = 0.5f)
        )
    }
}

/**
 * 图片加载错误
 */
@Composable
fun ImageError(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.errorContainer),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.BrokenImage,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                "加载失败",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * 渐变边框图片
 */
@Composable
fun GradientImageBorder(
    bitmap: Bitmap?,
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = BrandGradient,
    borderWidth: Float = 3f,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    onClick: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "border")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient"
    )
    
    Box(modifier = modifier) {
        // 渐变边框
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = gradientColors.map { it.copy(alpha = 0.5f + 0.5f * kotlin.math.sin(animatedOffset * 2 * Math.PI).toFloat()) },
                        start = androidx.compose.ui.geometry.Offset.Zero,
                        end = androidx.compose.ui.geometry.Offset(100f, 100f)
                    ),
                    shape = shape
                )
        )
        
        // 内部图片
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(borderWidth.dp)
                    .clip(shape)
                    .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
                contentScale = ContentScale.Crop
            )
        }
    }
}

/**
 * 生成中的加载动画
 */
@Composable
fun GenerationLoadingIndicator(
    progress: Float,
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 环形进度条
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(100.dp)
        ) {
            // 背景圈
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 6.dp,
                color = ProcessingColor.copy(alpha = 0.2f),
                trackColor = androidx.compose.ui.graphics.Color.Transparent
            )
            
            // 进度圈
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 6.dp,
                color = ProcessingColor,
                trackColor = androidx.compose.ui.graphics.Color.Transparent
            )
            
            // 百分比文字
            Text(
                "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 步骤信息
        Text(
            "Step $currentStep / $totalSteps",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 进度条
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = ProcessingColor,
            trackColor = ProcessingColor.copy(alpha = 0.2f)
        )
    }
}

/**
 * 脉冲动画按钮
 */
@Composable
fun PulsingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    Box(
        modifier = modifier
            .scale(scale)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        content()
    }
}

/**
 * 缩略图网格项
 */
@Composable
fun ThumbnailGridItem(
    bitmap: Bitmap?,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) ProcessingColor else androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = tween(200),
        label = "border"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            ImagePlaceholder(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

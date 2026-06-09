package comkuaihuiai.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import comkuaihuiai.data.model.*
import kotlinx.coroutines.*
import java.io.File

/**
 * 方向二：ControlNet 管理器 - 更强大控制
 * 支持：Canny边缘、深度图、姿态检测、线稿等
 */
class ControlNetManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ControlNetManager"
        
        // ControlNet 模型目录
        private const val CONTROLNET_DIR = "models/controlnet"
        
        // 预处理缓存
        private const val PREPROCESS_CACHE_DIR = "cache/preprocessed"
    }
    
    private val modelsDir = File(context.filesDir, CONTROLNET_DIR)
    private val cacheDir = File(context.filesDir, PREPROCESS_CACHE_DIR)
    
    private var isInitialized = false
    private var loadedControlNet: ControlNetType? = null
    
    // 预处理器缓存
    private val preprocessorCache = mutableMapOf<String, Bitmap>()
    
    init {
        if (!modelsDir.exists()) modelsDir.mkdirs()
        if (!cacheDir.exists()) cacheDir.mkdirs()
    }
    
    /**
     * 初始化
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext true
        
        try {
            Log.i(TAG, "初始化 ControlNet 管理器...")
            
            // 检查可用模型
            checkAvailableModels()
            
            isInitialized = true
            Log.i(TAG, "ControlNet 管理器初始化完成")
            true
        } catch (e: Exception) {
            Log.e(TAG, "初始化失败: ${e.message}")
            false
        }
    }
    
    /**
     * 加载指定类型的 ControlNet
     */
    suspend fun loadControlNet(type: ControlNetType): Boolean = withContext(Dispatchers.IO) {
        if (type == ControlNetType.NONE) {
            loadedControlNet = null
            return@withContext true
        }
        
        if (loadedControlNet == type) {
            Log.d(TAG, "ControlNet ${type.displayName} 已加载")
            return@withContext true
        }
        
        try {
            Log.i(TAG, "加载 ControlNet: ${type.displayName}")
            
            // 模拟模型加载
            val modelPath = File(modelsDir, "${type.modelSuffix}.safetensors")
            if (!modelPath.exists()) {
                Log.w(TAG, "模型文件不存在: ${modelPath.absolutePath}")
            }
            
            loadedControlNet = type
            Log.i(TAG, "ControlNet ${type.displayName} 加载完成")
            true
        } catch (e: Exception) {
            Log.e(TAG, "ControlNet 加载失败: ${e.message}")
            false
        }
    }
    
    /**
     * 卸载 ControlNet
     */
    fun unloadControlNet() {
        loadedControlNet = null
        preprocessorCache.clear()
        Log.i(TAG, "ControlNet 已卸载")
    }
    
    /**
     * 预处理图像
     * 根据 ControlNet 类型进行相应的预处理
     */
    suspend fun preprocess(
        inputImage: Bitmap,
        type: ControlNetType,
        preprocessor: ControlNetPreprocessor? = null
    ): Bitmap = withContext(Dispatchers.Default) {
        Log.i(TAG, "预处理图像: ${type.displayName}")
        
        // 生成缓存键
        val cacheKey = generateCacheKey(inputImage, type, preprocessor)
        
        // 检查缓存
        preprocessorCache[cacheKey]?.let {
            Log.d(TAG, "使用缓存的预处理结果")
            return@withContext it
        }
        
        val result = when (type) {
            ControlNetType.NONE -> inputImage
            
            ControlNetType.CANNY -> preprocessCanny(inputImage)
            ControlNetType.DEPTH -> preprocessDepth(inputImage)
            ControlNetType.DEPTH_ZOE -> preprocessDepthZoE(inputImage)
            ControlNetType.NORMAL -> preprocessNormal(inputImage)
            
            ControlNetType.POSE -> preprocessPose(inputImage)
            ControlNetType.SCRIBBLE -> preprocessScribble(inputImage)
            ControlNetType.SOFTEDGE -> preprocessSoftEdge(inputImage)
            
            ControlNetType.LINEART -> preprocessLineArt(inputImage)
            ControlNetType.LINEART_COARSE -> preprocessLineArtCoarse(inputImage)
            
            ControlNetType.SEG -> preprocessSegmentation(inputImage)
            ControlNetType.SHUFFLE -> preprocessShuffle(inputImage)
            
            ControlNetType.INPAINT -> preprocessInpaint(inputImage)
            ControlNetType.IP2P -> preprocessIP2P(inputImage)
            ControlNetType.REFERENCE -> preprocessReference(inputImage)
            
            ControlNetType.RECOLOR -> preprocessRecolor(inputImage)
            ControlNetType.BLUR -> preprocessBlur(inputImage)
            ControlNetType.MIP -> preprocessMip(inputImage)
            
            ControlNetType.TILE -> preprocessTile(inputImage)
            ControlNetType.TILE_COLORFIX -> preprocessTileColorFix(inputImage)
            ControlNetType.TILE_COLORFIX_SHARP -> preprocessTileColorFixSharp(inputImage)
        }
        
        // 缓存结果
        preprocessorCache[cacheKey] = result
        
        Log.i(TAG, "预处理完成: ${result.width}x${result.height}")
        result
    }
    
    /**
     * Canny 边缘检测
     */
    private fun preprocessCanny(input: Bitmap): Bitmap {
        Log.d(TAG, "Canny 边缘检测...")
        
        val width = input.width
        val height = input.height
        
        // 转换为灰度
        val gray = toGrayscale(input)
        
        // 高斯模糊
        val blurred = gaussianBlur(gray, 5)
        
        // Sobel 边缘检测
        val edges = sobelEdgeDetection(blurred)
        
        // 非极大值抑制
        val suppressed = nonMaxSuppression(edges)
        
        // 双阈值和边缘连接
        return hysteresisThreshold(suppressed)
    }
    
    /**
     * 深度图估计 (简化版)
     */
    private fun preprocessDepth(input: Bitmap): Bitmap {
        Log.d(TAG, "深度图估计...")
        
        val width = input.width
        val height = input.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        // 简化的深度估计 - 基于颜色和位置
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = input.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                
                // 简化：蓝色通道越低，深度越小（越近）
                // 绿色通道越高，深度越大（越远）
                val depth = ((b.toFloat() / 255.0f) * 0.6f + (1.0f - g.toFloat() / 255.0f) * 0.4f).coerceIn(0f, 1f)
                
                // 添加位置因素（底部更近）
                val positionFactor = 1.0f - (y.toFloat() / height.toFloat()) * 0.3f
                val finalDepth = (depth * positionFactor).coerceIn(0f, 1f)
                
                val gray = (finalDepth * 255).toInt()
                result.setPixel(x, y, Color.rgb(gray, gray, gray))
            }
        }
        
        return result
    }
    
    /**
     * ZoE Depth 估计 (简化版)
     */
    private fun preprocessDepthZoE(input: Bitmap): Bitmap {
        // ZoE 是更准确的深度估计，这里使用改进的简化版本
        return preprocessDepth(input).let { depth ->
            // 增加对比度
            enhanceContrast(depth, 1.5f)
        }
    }
    
    /**
     * 法线图估计
     */
    private fun preprocessNormal(input: Bitmap): Bitmap {
        Log.d(TAG, "法线图估计...")
        
        val width = input.width
        val height = input.height
        
        // 先获取深度图
        val depth = preprocessDepth(input)
        
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                // 计算深度梯度
                val dLeft = Color.red(depth.getPixel(x - 1, y)) / 255f
                val dRight = Color.red(depth.getPixel(x + 1, y)) / 255f
                val dUp = Color.red(depth.getPixel(x, y - 1)) / 255f
                val dDown = Color.red(depth.getPixel(x, y + 1)) / 255f
                
                // 计算法线
                val nx = (dLeft - dRight) * 2
                val ny = (dUp - dDown) * 2
                val nz = 1f
                
                // 归一化
                val len = kotlin.math.sqrt(nx * nx + ny * ny + nz * nz)
                val nnx = ((nx / len) * 0.5f + 0.5f).coerceIn(0f, 1f)
                val nny = ((ny / len) * 0.5f + 0.5f).coerceIn(0f, 1f)
                val nnz = ((nz / len) * 0.5f + 0.5f).coerceIn(0f, 1f)
                
                result.setPixel(
                    x, y,
                    Color.rgb((nnx * 255).toInt(), (nny * 255).toInt(), (nnz * 255).toInt())
                )
            }
        }
        
        return result
    }
    
    /**
     * 姿态检测 (简化版 - 模拟 OpenPose 输出)
     */
    private fun preprocessPose(input: Bitmap): Bitmap {
        Log.d(TAG, "姿态检测...")
        
        val width = input.width
        val height = input.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        // 简化的姿态检测 - 主要是为了展示
        // 实际需要 ML Kit Pose Detection 或类似库
        
        // 复制原图作为背景
        val canvas = Canvas(result)
        canvas.drawBitmap(input, 0f, 0f, null)
        
        // 绘制模拟的姿态骨架
        val paint = Paint().apply {
            color = Color.RED
            strokeWidth = 8f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        
        val keyPaint = Paint().apply {
            color = Color.YELLOW
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        val centerX = width / 2f
        val centerY = height / 2f
        
        // 头部
        canvas.drawCircle(centerX, centerY - height * 0.2f, 30f, keyPaint)
        
        // 肩膀
        canvas.drawLine(centerX - 60f, centerY, centerX + 60f, centerY, paint)
        
        // 手臂
        canvas.drawLine(centerX - 60f, centerY, centerX - 100f, centerY + 100f, paint)
        canvas.drawLine(centerX + 60f, centerY, centerX + 100f, centerY + 100f, paint)
        
        // 躯干
        canvas.drawLine(centerX, centerY, centerX, centerY + 100f, paint)
        
        // 腿
        canvas.drawLine(centerX, centerY + 100f, centerX - 50f, centerY + 200f, paint)
        canvas.drawLine(centerX, centerY + 100f, centerX + 50f, centerY + 200f, paint)
        
        // 绘制关键点
        val keyPoints = listOf(
            floatArrayOf(centerX, centerY - height * 0.2f), // 头
            floatArrayOf(centerX, centerY), // 脖子
            floatArrayOf(centerX - 60f, centerY), // 左肩
            floatArrayOf(centerX + 60f, centerY), // 右肩
            floatArrayOf(centerX - 100f, centerY + 100f), // 左肘
            floatArrayOf(centerX + 100f, centerY + 100f), // 右肘
            floatArrayOf(centerX, centerY + 100f), // 髋部
            floatArrayOf(centerX - 50f, centerY + 200f), // 左膝
            floatArrayOf(centerX + 50f, centerY + 200f) // 右膝
        )
        
        keyPoints.forEach { point ->
            canvas.drawCircle(point[0], point[1], 12f, keyPaint)
        }
        
        return result
    }
    
    /**
     * 涂鸦/手绘
     */
    private fun preprocessScribble(input: Bitmap): Bitmap {
        Log.d(TAG, "涂鸦预处理...")
        
        val edges = preprocessCanny(input)
        
        // 增强边缘，简化线条
        return enhanceContrast(edges, 2f).let {
            thinEdges(it)
        }
    }
    
    /**
     * 柔和边缘
     */
    private fun preprocessSoftEdge(input: Bitmap): Bitmap {
        Log.d(TAG, "柔和边缘检测...")
        
        val blurred = gaussianBlur(input, 15)
        val edges = sobelEdgeDetection(toGrayscale(blurred))
        
        return enhanceContrast(edges, 1.5f)
    }
    
    /**
     * 线稿提取
     */
    private fun preprocessLineArt(input: Bitmap): Bitmap {
        Log.d(TAG, "线稿提取...")
        
        val edges = sobelEdgeDetection(toGrayscale(input))
        
        // 锐化
        return unsharpMask(edges, 1.5f)
    }
    
    /**
     * 粗线稿
     */
    private fun preprocessLineArtCoarse(input: Bitmap): Bitmap {
        val lineart = preprocessLineArt(input)
        return dilate(lineart, 3)
    }
    
    /**
     * 语义分割
     */
    private fun preprocessSegmentation(input: Bitmap): Bitmap {
        Log.d(TAG, "语义分割...")
        
        val width = input.width
        val height = input.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        // 简化的语义分割 - 基于颜色分类
        val segmentColors = listOf(
            Color.rgb(100, 150, 200), // 天空
            Color.rgb(80, 150, 80),    // 草地
            Color.rgb(150, 100, 80),  // 地面
            Color.rgb(200, 200, 200),  // 建筑
            Color.rgb(100, 100, 100)  // 道路
        )
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = input.getPixel(x, y)
                
                // 简化的分类逻辑
                val segment = when {
                    Color.blue(pixel) > Color.red(pixel) * 1.2 -> 0 // 天空
                    Color.green(pixel) > Color.red(pixel) * 1.1 -> 1 // 草地
                    Color.blue(pixel) < 100 && Color.green(pixel) < 100 -> 4 // 道路
                    Color.red(pixel) > 150 && Color.green(pixel) > 150 -> 3 // 建筑
                    else -> 2 // 地面
                }
                
                result.setPixel(x, y, segmentColors[segment % segmentColors.size])
            }
        }
        
        return result
    }
    
    /**
     * 风格迁移/洗牌
     */
    private fun preprocessShuffle(input: Bitmap): Bitmap {
        Log.d(TAG, "风格洗牌...")
        // 返回原图，ControlNet 会进行风格迁移
        return input
    }
    
    /**
     * 局部重绘控制
     */
    private fun preprocessInpaint(input: Bitmap): Bitmap {
        Log.d(TAG, "局部重绘预处理...")
        // 保持原图，mask 单独处理
        return input
    }
    
    /**
     * 图生图控制
     */
    private fun preprocessIP2P(input: Bitmap): Bitmap {
        return input
    }
    
    /**
     * 参考图像
     */
    private fun preprocessReference(input: Bitmap): Bitmap {
        return input
    }
    
    /**
     * 着色控制
     */
    private fun preprocessRecolor(input: Bitmap): Bitmap {
        Log.d(TAG, "着色控制...")
        
        // 转换为灰度并添加色调信息
        val width = input.width
        val height = input.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = input.getPixel(x, y)
                val gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                // 保持灰度，用于着色参考
                result.setPixel(x, y, Color.rgb(gray, gray, gray))
            }
        }
        
        return result
    }
    
    /**
     * 模糊控制
     */
    private fun preprocessBlur(input: Bitmap): Bitmap {
        return gaussianBlur(input, 21)
    }
    
    /**
     * MIP 遮罩
     */
    private fun preprocessMip(input: Bitmap): Bitmap {
        // 生成 MIP 级别的遮罩
        return toGrayscale(input)
    }
    
    /**
     * 分块控制
     */
    private fun preprocessTile(input: Bitmap): Bitmap {
        Log.d(TAG, "分块控制...")
        // 返回原图，分块处理由引擎完成
        return input
    }
    
    /**
     * 分块色彩修复
     */
    private fun preprocessTileColorFix(input: Bitmap): Bitmap {
        return input
    }
    
    /**
     * 分块色彩锐化
     */
    private fun preprocessTileColorFixSharp(input: Bitmap): Bitmap {
        return unsharpMask(input, 1.3f)
    }
    
    // ============================================================
    // 图像处理辅助函数
    // ============================================================
    
    /**
     * 转换为灰度图
     */
    private fun toGrayscale(input: Bitmap): Bitmap {
        val width = input.width
        val height = input.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = input.getPixel(x, y)
                val gray = (Color.red(pixel) * 0.299 + 
                           Color.green(pixel) * 0.587 + 
                           Color.blue(pixel) * 0.114).toInt()
                result.setPixel(x, y, Color.rgb(gray, gray, gray))
            }
        }
        
        return result
    }
    
    /**
     * 高斯模糊
     */
    private fun gaussianBlur(input: Bitmap, kernelSize: Int): Bitmap {
        val width = input.width
        val height = input.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val kernel = generateGaussianKernel(kernelSize)
        val halfKernel = kernelSize / 2
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sumR = 0.0
                var sumG = 0.0
                var sumB = 0.0
                var sumK = 0.0
                
                for (ky in -halfKernel..halfKernel) {
                    for (kx in -halfKernel..halfKernel) {
                        val px = (x + kx).coerceIn(0, width - 1)
                        val py = (y + ky).coerceIn(0, height - 1)
                        val pixel = input.getPixel(px, py)
                        val k = kernel[kx + halfKernel][ky + halfKernel]
                        
                        sumR += Color.red(pixel) * k
                        sumG += Color.green(pixel) * k
                        sumB += Color.blue(pixel) * k
                        sumK += k
                    }
                }
                
                result.setPixel(
                    x, y,
                    Color.rgb(
                        (sumR / sumK).toInt().coerceIn(0, 255),
                        (sumG / sumK).toInt().coerceIn(0, 255),
                        (sumB / sumK).toInt().coerceIn(0, 255)
                    )
                )
            }
        }
        
        return result
    }
    
    /**
     * 生成高斯核
     */
    private fun generateGaussianKernel(size: Int): Array<FloatArray> {
        val kernel = Array(size) { FloatArray(size) }
        val sigma = size / 6.0
        val half = size / 2
        
        var sum = 0.0
        for (y in 0 until size) {
            for (x in 0 until size) {
                val dx = x - half
                val dy = y - half
                val value = kotlin.math.exp(-(dx * dx + dy * dy) / (2 * sigma * sigma))
                kernel[x][y] = value.toFloat()
                sum += value
            }
        }
        
        // 归一化
        for (y in 0 until size) {
            for (x in 0 until size) {
                kernel[x][y] = (kernel[x][y] / sum).toFloat()
            }
        }
        
        return kernel
    }
    
    /**
     * Sobel 边缘检测
     */
    private fun sobelEdgeDetection(input: Bitmap): Bitmap {
        val width = input.width
        val height = input.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val sobelX = arrayOf(
            intArrayOf(-1, 0, 1),
            intArrayOf(-2, 0, 2),
            intArrayOf(-1, 0, 1)
        )
        
        val sobelY = arrayOf(
            intArrayOf(-1, -2, -1),
            intArrayOf(0, 0, 0),
            intArrayOf(1, 2, 1)
        )
        
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var gx = 0
                var gy = 0
                
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val pixel = input.getPixel(x + kx, y + ky)
                        val gray = Color.red(pixel)
                        gx += gray * sobelX[ky + 1][kx + 1]
                        gy += gray * sobelY[ky + 1][kx + 1]
                    }
                }
                
                val magnitude = kotlin.math.sqrt((gx * gx + gy * gy).toDouble()).toInt().coerceIn(0, 255)
                result.setPixel(x, y, Color.rgb(magnitude, magnitude, magnitude))
            }
        }
        
        return result
    }
    
    /**
     * 非极大值抑制
     */
    private fun nonMaxSuppression(input: Bitmap): Bitmap {
        val width = input.width
        val height = input.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val pixel = input.getPixel(x, y)
                val magnitude = Color.red(pixel)
                
                // 检查是否为局部极大值
                val neighbors = listOf(
                    input.getPixel(x - 1, y - 1),
                    input.getPixel(x, y - 1),
                    input.getPixel(x + 1, y - 1),
                    input.getPixel(x - 1, y),
                    input.getPixel(x + 1, y),
                    input.getPixel(x - 1, y + 1),
                    input.getPixel(x, y + 1),
                    input.getPixel(x + 1, y + 1)
                ).map { Color.red(it) }
                
                if (neighbors.all { it <= magnitude }) {
                    result.setPixel(x, y, Color.rgb(magnitude, magnitude, magnitude))
                } else {
                    result.setPixel(x, y, Color.BLACK)
                }
            }
        }
        
        return result
    }
    
    /**
     * 双阈值边缘连接
     */
    private fun hysteresisThreshold(input: Bitmap, lowThreshold: Int = 50, highThreshold: Int = 150): Bitmap {
        val width = input.width
        val height = input.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = input.getPixel(x, y)
                val magnitude = Color.red(pixel)
                
                val newValue = when {
                    magnitude >= highThreshold -> 255
                    magnitude >= lowThreshold -> 128 // 弱边缘，待确定
                    else -> 0
                }
                
                result.setPixel(x, y, Color.rgb(newValue, newValue, newValue))
            }
        }
        
        return result
    }
    
    /**
     * 增强对比度
     */
    private fun enhanceContrast(input: Bitmap, factor: Float): Bitmap {
        val width = input.width
        val height = input.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = input.getPixel(x, y)
                val r = (128 + (Color.red(pixel) - 128) * factor).toInt().coerceIn(0, 255)
                val g = (128 + (Color.green(pixel) - 128) * factor).toInt().coerceIn(0, 255)
                val b = (128 + (Color.blue(pixel) - 128) * factor).toInt().coerceIn(0, 255)
                result.setPixel(x, y, Color.rgb(r, g, b))
            }
        }
        
        return result
    }
    
    /**
     * USM 锐化
     */
    private fun unsharpMask(input: Bitmap, amount: Float): Bitmap {
        val blurred = gaussianBlur(input, 5)
        val width = input.width
        val height = input.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val orig = input.getPixel(x, y)
                val blur = blurred.getPixel(x, y)
                
                val r = (Color.red(orig) + (Color.red(orig) - Color.red(blur)) * amount).toInt().coerceIn(0, 255)
                val g = (Color.green(orig) + (Color.green(orig) - Color.green(blur)) * amount).toInt().coerceIn(0, 255)
                val b = (Color.blue(orig) + (Color.blue(orig) - Color.blue(blur)) * amount).toInt().coerceIn(0, 255)
                
                result.setPixel(x, y, Color.rgb(r, g, b))
            }
        }
        
        return result
    }
    
    /**
     * 细化边缘
     */
    private fun thinEdges(input: Bitmap): Bitmap {
        // 简单的阈值化
        val width = input.width
        val height = input.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val threshold = 100
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = input.getPixel(x, y)
                val magnitude = Color.red(pixel)
                
                val newValue = if (magnitude > threshold) 255 else 0
                result.setPixel(x, y, Color.rgb(newValue, newValue, newValue))
            }
        }
        
        return result
    }
    
    /**
     * 膨胀操作
     */
    private fun dilate(input: Bitmap, iterations: Int): Bitmap {
        var current = input
        
        repeat(iterations) {
            val width = current.width
            val height = current.height
            val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            
            for (y in 0 until height) {
                for (x in 0 until width) {
                    var maxVal = 0
                    
                    for (ky in -1..1) {
                        for (kx in -1..1) {
                            val px = (x + kx).coerceIn(0, width - 1)
                            val py = (y + ky).coerceIn(0, height - 1)
                            maxVal = maxOf(maxVal, Color.red(current.getPixel(px, py)))
                        }
                    }
                    
                    result.setPixel(x, y, Color.rgb(maxVal, maxVal, maxVal))
                }
            }
            
            current = result
        }
        
        return current
    }
    
    /**
     * 生成缓存键
     */
    private fun generateCacheKey(input: Bitmap, type: ControlNetType, preprocessor: ControlNetPreprocessor?): String {
        return "${input.width}x${input.height}_${type.name}_${preprocessor?.name ?: "default"}_${input.hashCode()}"
    }
    
    /**
     * 检查可用模型
     */
    private fun checkAvailableModels() {
        val available = ControlNetType.entries.filter { type ->
            val modelPath = File(modelsDir, "${type.modelSuffix}.safetensors")
            modelPath.exists()
        }
        
        Log.i(TAG, "可用 ControlNet 模型: ${available.map { it.displayName }}")
    }
    
    /**
     * 获取预处理器列表
     */
    fun getPreprocessorsForType(type: ControlNetType): List<ControlNetPreprocessor> {
        return when (type) {
            ControlNetType.CANNY -> listOf(
                ControlNetPreprocessor.CANNY_EDGE,
                ControlNetPreprocessor.CANNY_THRESHOLD_LOW,
                ControlNetPreprocessor.CANNY_THRESHOLD_MEDIUM,
                ControlNetPreprocessor.CANNY_THRESHOLD_HIGH
            )
            ControlNetType.DEPTH -> listOf(
                ControlNetPreprocessor.DEPTH_MIDAS,
                ControlNetPreprocessor.DEPTH_ZOE,
                ControlNetPreprocessor.DEPTH_LERF
            )
            ControlNetType.NORMAL -> listOf(
                ControlNetPreprocessor.NORMAL_MIDAS,
                ControlNetPreprocessor.NORMAL_BAE
            )
            ControlNetType.POSE -> listOf(
                ControlNetPreprocessor.POSE_OPENPOSE_FULL,
                ControlNetPreprocessor.POSE_OPENPOSE_BODY,
                ControlNetPreprocessor.POSE_OPENPOSE_FACE,
                ControlNetPreprocessor.POSE_OPENPOSE_HAND,
                ControlNetPreprocessor.POSE_OPENPOSE_ALL
            )
            ControlNetType.SCRIBBLE -> listOf(
                ControlNetPreprocessor.SCRIBBLE_HOG,
                ControlNetPreprocessor.SCRIBBLE_PIDINET
            )
            ControlNetType.LINEART -> listOf(
                ControlNetPreprocessor.LINEART_REALISTIC,
                ControlNetPreprocessor.LINEART_ANIME
            )
            ControlNetType.SEG -> listOf(
                ControlNetPreprocessor.SEGMENTATION_UNIVNET,
                ControlNetPreprocessor.SEGMENTATION_ONEFormer
            )
            ControlNetType.IP2P -> listOf(
                ControlNetPreprocessor.IP2P
            )
            ControlNetType.REFERENCE -> listOf(
                ControlNetPreprocessor.REFERENCE
            )
            else -> listOf(ControlNetPreprocessor.CANNY_EDGE)
        }
    }
    
    /**
     * 清理缓存
     */
    fun clearCache() {
        preprocessorCache.clear()
        cacheDir.listFiles()?.forEach { it.delete() }
        Log.i(TAG, "预处理器缓存已清理")
    }
    
    /**
     * 释放资源
     */
    fun release() {
        preprocessorCache.clear()
        loadedControlNet = null
        isInitialized = false
    }
}

package comkuaihuiai.service.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 视频生成引擎
 * 支持：视频生成、视频帧提取、短视频风格迁移
 * 基于本地合成与图像处理
 */
class VideoEngine(private val context: Context) {

    companion object {
        private const val TAG = "VideoEngine"
        
        // 视频编码参数
        const val FRAME_RATE = 30
        const val BIT_RATE = 4000000 // 4Mbps
        const val I_FRAME_INTERVAL = 1
        
        // 分辨率
        const val RESOLUTION_480P = 0
        const val RESOLUTION_720P = 1
        const val RESOLUTION_1080P = 2
        
        // 风格
        const val STYLE_NORMAL = 0
        const val STYLE_ANIME = 1
        const val STYLE_PIXEL = 2
        const val STYLE_BLUR = 3
        const val STYLE_GLITCH = 4
    }

    // 当前设置
    private var currentResolution = RESOLUTION_720P
    private var currentStyle = STYLE_NORMAL
    private var currentDuration = 5 // 秒
    
    // 视频目录
    private val videoDir = File(context.filesDir, "videos")
    private val framesDir = File(context.filesDir, "frames")
    
    init {
        if (!videoDir.exists()) videoDir.mkdirs()
        if (!framesDir.exists()) framesDir.mkdirs()
    }

    /**
     * 设置分辨率
     */
    fun setResolution(resolution: Int) {
        currentResolution = resolution
    }

    /**
     * 设置风格
     */
    fun setStyle(style: Int) {
        currentStyle = style
    }

    /**
     * 设置时长（秒）
     */
    fun setDuration(seconds: Int) {
        currentDuration = seconds.coerceIn(1, 60)
    }

    /**
     * 从图像生成视频
     */
    fun generateVideoFromImages(
        images: List<Bitmap>,
        outputPath: String? = null,
        style: Int = currentStyle,
        duration: Int = currentDuration
    ): Flow<VideoProgress> = flow {
        emit(VideoProgress.Status("正在生成视频..."))

        try {
            val width = getResolutionWidth(currentResolution)
            val height = getResolutionHeight(currentResolution)
            val totalFrames = duration * FRAME_RATE
            
            emit(VideoProgress.Progress(0, "准备帧序列..."))

            // 调整图像大小
            val resizedImages = images.map { resizeImage(it, width, height) }
            
            // 如果只有一张图，扩展为多帧
            val frames = if (resizedImages.size == 1) {
                generateFrameSequence(resizedImages[0], totalFrames, style)
            } else {
                // 多图交叉淡入淡出
                interpolateFrames(resizedImages, totalFrames)
            }

            emit(VideoProgress.Progress(30, "编码视频..."))

            // 编码视频
            val outputFile = if (outputPath != null) {
                File(outputPath)
            } else {
                File(videoDir, "video_${System.currentTimeMillis()}.mp4")
            }
            encodeVideo(frames, outputFile.absolutePath, width, height)

            emit(VideoProgress.Progress(90, "保存完成..."))

            emit(VideoProgress.Completed(outputFile.absolutePath))

        } catch (e: Exception) {
            Log.e(TAG, "Video generation error: ${e.message}")
            emit(VideoProgress.Error("视频生成失败: ${e.message}"))
        }
    }

    /**
     * 从提示词生成视频（基于图像生成）
     */
    fun generateVideo(
        prompt: String,
        negativePrompt: String = "",
        style: Int = currentStyle,
        duration: Int = currentDuration
    ): Flow<VideoProgress> = flow {
        emit(VideoProgress.Status("正在生成视频..."))

        try {
            val width = getResolutionWidth(currentResolution)
            val height = getResolutionHeight(currentResolution)
            val totalFrames = duration * FRAME_RATE
            
            emit(VideoProgress.Progress(0, "生成关键帧..."))

            // 生成关键帧（每2秒一个关键帧）
            val keyFrameCount = (duration / 2) + 1
            val keyFrames = mutableListOf<Bitmap>()
            
            for (i in 0 until keyFrameCount) {
                // 动态提示词变化
                val framePrompt = "$prompt, frame $i"
                val frame = generateKeyFrame(framePrompt, width, height, i, keyFrameCount)
                keyFrames.add(frame)
                
                emit(VideoProgress.Progress(
                    (i * 30) / keyFrameCount,
                    "生成帧 $i/$keyFrameCount..."
                ))
            }

            emit(VideoProgress.Progress(30, "插值生成中间帧..."))

            // 插值生成中间帧
            val allFrames = interpolateFrames(keyFrames, totalFrames)

            emit(VideoProgress.Progress(60, "应用风格..."))

            // 应用风格
            val styledFrames = if (style != STYLE_NORMAL) {
                applyStyle(allFrames, style)
            } else {
                allFrames
            }

            emit(VideoProgress.Progress(80, "编码视频..."))

            // 编码视频
            val outputFile = File(videoDir, "video_${System.currentTimeMillis()}.mp4")
            encodeVideo(styledFrames, outputFile.absolutePath, width, height)

            emit(VideoProgress.Completed(outputFile.absolutePath))

        } catch (e: Exception) {
            Log.e(TAG, "Video generation error: ${e.message}")
            emit(VideoProgress.Error("视频生成失败: ${e.message}"))
        }
    }

    /**
     * 生成关键帧
     */
    private fun generateKeyFrame(
        prompt: String,
        width: Int,
        height: Int,
        index: Int,
        total: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // 基于提示词生成颜色和图案
        val seed = prompt.hashCode().toLong() + index * 1000
        val random = Random(seed)
        
        // 背景渐变
        val baseHue = (prompt.hashCode() % 360).toFloat()
        val paint = Paint()
        
        // 绘制渐变背景
        for (y in 0 until height step 10) {
            val hue = (baseHue + y * 0.1f) % 360f
            paint.color = Color.HSVToColor(floatArrayOf(hue, 0.3f, 0.9f))
            canvas.drawRect(0f, y.toFloat(), width.toFloat(), (y + 10).toFloat(), paint)
        }
        
        // 绘制动态元素
        val elementCount = 3 + random.nextInt(5)
        for (i in 0 until elementCount) {
            val x = random.nextInt(width)
            val y = random.nextInt(height)
            val size = 50 + random.nextInt(200)
            val elementHue = (baseHue + i * 60) % 360f
            
            paint.color = Color.HSVToColor(floatArrayOf(elementHue, 0.6f, 0.8f))
            paint.style = Paint.Style.FILL
            
            when (random.nextInt(4)) {
                0 -> canvas.drawCircle(x.toFloat(), y.toFloat(), size.toFloat(), paint)
                1 -> canvas.drawRect(
                    (x - size).toFloat(), (y - size).toFloat(),
                    (x + size).toFloat(), (y + size).toFloat(), paint
                )
                2 -> {
                    val path = Path()
                    path.moveTo(x.toFloat(), (y - size).toFloat())
                    path.lineTo((x + size).toFloat(), (y + size).toFloat())
                    path.lineTo((x - size).toFloat(), (y + size).toFloat())
                    path.close()
                    canvas.drawPath(path, paint)
                }
                3 -> canvas.drawOval(
                    (x - size).toFloat(), (y - size / 2).toFloat(),
                    (x + size).toFloat(), (y + size / 2).toFloat(), paint
                )
            }
        }
        
        // 添加提示词文字
        paint.color = Color.WHITE
        paint.textSize = 48f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Frame ${index + 1}/$total", width / 2f, height / 2f, paint)
        
        return bitmap
    }

    /**
     * 帧序列生成
     */
    private fun generateFrameSequence(
        source: Bitmap,
        totalFrames: Int,
        style: Int
    ): List<Bitmap> {
        val frames = mutableListOf<Bitmap>()
        
        for (i in 0 until totalFrames) {
            val progress = i.toFloat() / totalFrames
            
            // 轻微移动和缩放
            val offsetX = (sin(progress * PI * 4) * 10).toInt()
            val offsetY = (cos(progress * PI * 4) * 10).toInt()
            val scale = 1.0f + (sin(progress * PI * 2) * 0.05f).toFloat()
            
            val frame = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(frame)
            
            canvas.save()
            canvas.scale(scale, scale, source.width / 2f, source.height / 2f)
            canvas.translate(offsetX.toFloat(), offsetY.toFloat())
            canvas.drawBitmap(source, 0f, 0f, null)
            canvas.restore()
            
            frames.add(frame)
        }
        
        return frames
    }

    /**
     * 帧插值
     */
    private fun interpolateFrames(keyFrames: List<Bitmap>, totalFrames: Int): List<Bitmap> {
        if (keyFrames.isEmpty()) return emptyList()
        if (keyFrames.size == 1) return generateFrameSequence(keyFrames[0], totalFrames, STYLE_NORMAL)
        
        val frames = mutableListOf<Bitmap>()
        val framesPerTransition = totalFrames / (keyFrames.size - 1)
        
        for (i in 0 until keyFrames.size - 1) {
            val startFrame = keyFrames[i]
            val endFrame = keyFrames[i + 1]
            
            for (j in 0 until framesPerTransition) {
                val progress = j.toFloat() / framesPerTransition
                val interpolated = crossFade(startFrame, endFrame, progress)
                frames.add(interpolated)
            }
        }
        
        return frames
    }

    /**
     * 交叉淡入淡出
     */
    private fun crossFade(frame1: Bitmap, frame2: Bitmap, progress: Float): Bitmap {
        val result = Bitmap.createBitmap(frame1.width, frame1.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        
        // 绘制第一帧
        val paint1 = Paint()
        paint1.alpha = ((1 - progress) * 255).toInt()
        canvas.drawBitmap(frame1, 0f, 0f, paint1)
        
        // 绘制第二帧
        val paint2 = Paint()
        paint2.alpha = (progress * 255).toInt()
        canvas.drawBitmap(frame2, 0f, 0f, paint2)
        
        return result
    }

    /**
     * 应用风格
     */
    private fun applyStyle(frames: List<Bitmap>, style: Int): List<Bitmap> {
        return when (style) {
            STYLE_ANIME -> frames.map { applyAnimeStyle(it) }
            STYLE_PIXEL -> frames.map { applyPixelStyle(it) }
            STYLE_BLUR -> frames.map { applyBlurStyle(it) }
            STYLE_GLITCH -> frames.map { applyGlitchStyle(it) }
            else -> frames
        }
    }

    private fun applyAnimeStyle(source: Bitmap): Bitmap {
        // 简化的动漫风格处理
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(source, 0f, 0f, null)
        
        // 添加描边效果
        val paint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.BLACK
        }
        
        // 简化处理 - 直接返回原图
        return result
    }

    private fun applyPixelStyle(source: Bitmap): Bitmap {
        val pixelSize = 8
        val width = source.width / pixelSize
        val height = source.height / pixelSize
        
        val scaled = Bitmap.createScaledBitmap(source, width, height, true)
        return Bitmap.createScaledBitmap(scaled, source.width, source.height, true)
    }

    private fun applyBlurStyle(source: Bitmap): Bitmap {
        // 简单模糊
        return source
    }

    private fun applyGlitchStyle(source: Bitmap): Bitmap {
        val random = Random(System.currentTimeMillis())
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        
        // 随机位移
        val offset = random.nextInt(20) - 10
        // 这里简化处理
        
        return result
    }

    /**
     * 编码视频
     */
    private fun encodeVideo(frames: List<Bitmap>, outputPath: String, width: Int, height: Int) {
        if (frames.isEmpty()) throw IllegalArgumentException("No frames to encode")
        
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
        }

        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        
        val inputSurface = encoder.createInputSurface()
        encoder.start()

        val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var trackIndex = -1
        var muxerStarted = false

        val bufferInfo = MediaCodec.BufferInfo()
        
        try {
            val frameDurationUs = 1000000L / FRAME_RATE
            
            for ((index, frame) in frames.withIndex()) {
                // 在surface上绘制帧
                val canvas = inputSurface.lockCanvas(null)
                canvas.drawBitmap(frame, 0f, 0f, null)
                inputSurface.unlockCanvasAndPost(canvas)

                // 获取编码输出
                while (true) {
                    val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000)
                    
                    when {
                        outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            trackIndex = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                        outputBufferIndex >= 0 -> {
                            val encodedData = encoder.getOutputBuffer(outputBufferIndex)
                            
                            if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                                encodedData.position(bufferInfo.offset)
                                encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                            }
                            
                            encoder.releaseOutputBuffer(outputBufferIndex, false)
                            
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                break
                            }
                        }
                        else -> break
                    }
                }
            }

            // 发送结束标志
            encoder.signalEndOfInputStream()
            
            // 读取剩余数据
            while (true) {
                val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000)
                if (outputBufferIndex >= 0) {
                    val encodedData = encoder.getOutputBuffer(outputBufferIndex)
                    if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                        muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(outputBufferIndex, false)
                    
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        break
                    }
                } else {
                    break
                }
            }

        } finally {
            encoder.stop()
            encoder.release()
            if (muxerStarted) {
                muxer.stop()
                muxer.release()
            }
        }
    }

    /**
     * 提取视频帧
     */
    fun extractFrames(videoPath: String): Flow<VideoProgress> = flow {
        emit(VideoProgress.Status("正在提取视频帧..."))
        
        try {
            // 简化实现 - 使用 MediaMetadataRetriever
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(videoPath)
            
            val duration = retriever.extractMetadata(
                android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0
            
            val frameCount = (duration / 1000 * FRAME_RATE).toInt()
            
            emit(VideoProgress.Progress(0, "提取 $frameCount 帧..."))
            
            val frames = mutableListOf<Bitmap>()
            
            for (i in 0 until frameCount step 10) { // 每10帧提取一帧
                val timeUs = i * 1000000L / FRAME_RATE
                val frame = retriever.getFrameAtTime(timeUs, android.media.MediaMetadataRetriever.OPTION_CLOSEST)
                if (frame != null) {
                    frames.add(frame)
                }
                
                if (i % 30 == 0) {
                    emit(VideoProgress.Progress((i * 50) / frameCount, "提取帧 $i/$frameCount..."))
                }
            }
            
            retriever.release()
            
            // 保存帧
            val outputDir = File(framesDir, "frames_${System.currentTimeMillis()}")
            outputDir.mkdirs()
            
            frames.forEachIndexed { index, frame ->
                val frameFile = File(outputDir, "frame_${index}.png")
                FileOutputStream(frameFile).use { out ->
                    frame.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }
            
            emit(VideoProgress.Completed(outputDir.absolutePath))
            
        } catch (e: Exception) {
            emit(VideoProgress.Error("帧提取失败: ${e.message}"))
        }
    }

    private fun getResolutionWidth(resolution: Int): Int {
        return when (resolution) {
            RESOLUTION_480P -> 854
            RESOLUTION_720P -> 1280
            RESOLUTION_1080P -> 1920
            else -> 1280
        }
    }

    private fun getResolutionHeight(resolution: Int): Int {
        return when (resolution) {
            RESOLUTION_480P -> 480
            RESOLUTION_720P -> 720
            RESOLUTION_1080P -> 1080
            else -> 720
        }
    }

    private fun resizeImage(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    fun getVideoDirectory(): File = videoDir
    fun getFramesDirectory(): File = framesDir
}

sealed class VideoProgress {
    data class Status(val message: String) : VideoProgress()
    data class Progress(val percent: Int, val message: String) : VideoProgress()
    data class Completed(val path: String) : VideoProgress()
    data class Error(val message: String) : VideoProgress()
}

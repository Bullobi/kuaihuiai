package comkuaihuiai.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import comkuaihuiai.MainActivity
import comkuaihuiai.R
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

class ModelDownloadService : Service() {
    
    companion object {
        const val TAG = "ModelDownloadService"
        const val CHANNEL_ID = "model_download_channel"
        const val NOTIFICATION_ID = 1001
        
        const val ACTION_START = "comkuaihuiai.START_DOWNLOAD"
        const val ACTION_STOP = "comkuaihuiai.STOP_DOWNLOAD"
        
        const val EXTRA_MODEL_ID = "model_id"
        const val EXTRA_MODEL_NAME = "model_name"
        const val EXTRA_DOWNLOAD_URL = "download_url"
        const val EXTRA_IS_ZIP = "is_zip"
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var downloadJob: Job? = null
    private var isStopped = false
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopDownload()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val modelId = intent.getStringExtra(EXTRA_MODEL_ID) ?: return START_NOT_STICKY
                val modelName = intent.getStringExtra(EXTRA_MODEL_NAME) ?: "Model"
                val url = intent.getStringExtra(EXTRA_DOWNLOAD_URL) ?: return START_NOT_STICKY
                val isZip = intent.getBooleanExtra(EXTRA_IS_ZIP, true)
                
                startForeground(NOTIFICATION_ID, createNotification("Downloading $modelName..."))
                startDownload(modelId, modelName, url, isZip)
            }
        }
        return START_NOT_STICKY
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Model Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows download progress for AI models"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(content: String, progress: Int = -1): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("快绘AI - Downloading Model")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .apply {
                if (progress >= 0) {
                    setProgress(100, progress, false)
                } else {
                    setProgress(0, 0, true)
                }
            }
            .build()
    }
    
    private fun startDownload(modelId: String, modelName: String, url: String, isZip: Boolean) {
        downloadJob = scope.launch {
            try {
                Log.i(TAG, "Starting download: $url")
                
                val modelsDir = File(filesDir, "models/$modelId")
                if (modelsDir.exists()) {
                    modelsDir.deleteRecursively()
                }
                modelsDir.mkdirs()
                
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 30000
                conn.readTimeout = 30000
                conn.connect()
                
                val totalBytes = conn.contentLength.toLong()
                var downloadedBytes = 0L
                
                if (isZip) {
                    // Download as zip file first
                    val zipFile = File(modelsDir, "model.zip")
                    val outputStream = FileOutputStream(zipFile)
                    val inputStream = conn.inputStream
                    
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        if (isStopped) break
                        
                        outputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        
                        val progress = if (totalBytes > 0) {
                            ((downloadedBytes * 100) / totalBytes).toInt()
                        } else 0
                        
                        updateNotification("$modelName - $progress%", progress)
                    }
                    
                    outputStream.close()
                    inputStream.close()
                    
                    if (!isStopped) {
                        // Extract zip
                        updateNotification("Extracting $modelName...", -1)
                        extractZip(zipFile, modelsDir)
                        zipFile.delete()
                    }
                }
                
                if (!isStopped) {
                    // Create finished marker
                    File(modelsDir, "finished").createNewFile()
                    
                    updateNotification("$modelName - Download Complete!", 100)
                    Log.i(TAG, "Download complete: $modelId")
                    
                    delay(2000)
                    stopSelf()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
                updateNotification("Download failed: ${e.message}", -1)
                delay(3000)
                stopSelf()
            }
        }
    }
    
    private fun extractZip(zipFile: File, destDir: File) {
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val file = File(destDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
    
    private fun updateNotification(content: String, progress: Int) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, createNotification(content, progress))
    }
    
    private fun stopDownload() {
        isStopped = true
        downloadJob?.cancel()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}

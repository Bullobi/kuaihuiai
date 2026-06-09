package comkuaihuiai.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import comkuaihuiai.KuaiHuiAIApplication
import comkuaihuiai.MainActivity
import comkuaihuiai.R
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Background service for downloading models
 */
class DownloadService : Service() {
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }
    
    companion object {
        const val ACTION_START_DOWNLOAD = "comkuaihuiai.START_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "comkuaihuiai.CANCEL_DOWNLOAD"
        const val EXTRA_URL = "extra_url"
        const val EXTRA_SAVE_PATH = "extra_save_path"
        const val EXTRA_MODEL_NAME = "extra_model_name"
        const val NOTIFICATION_ID = 1001
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                val url = intent.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
                val savePath = intent.getStringExtra(EXTRA_SAVE_PATH) ?: return START_NOT_STICKY
                val modelName = intent.getStringExtra(EXTRA_MODEL_NAME) ?: "模型"
                
                startForeground(NOTIFICATION_ID, createNotification(modelName, 0))
                startDownload(url, savePath, modelName)
            }
            ACTION_CANCEL_DOWNLOAD -> {
                serviceScope.coroutineContext.cancelChildren()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }
    
    private fun startDownload(url: String, savePath: String, modelName: String) {
        serviceScope.launch {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connect()
                
                val totalSize = connection.contentLength.toLong()
                var downloadedSize = 0L
                
                connection.inputStream.use { input ->
                    FileOutputStream(File(savePath)).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedSize += bytesRead
                            
                            val progress = if (totalSize > 0) {
                                (downloadedSize * 100 / totalSize).toInt()
                            } else {
                                0
                            }
                            
                            updateNotification(modelName, progress, downloadedSize, totalSize)
                        }
                    }
                }
                
                // Download completed
                showCompletionNotification(modelName)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                
            } catch (e: Exception) {
                e.printStackTrace()
                showErrorNotification(modelName, e.message ?: "下载失败")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }
    
    private fun createNotification(modelName: String, progress: Int): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, KuaiHuiAIApplication.DOWNLOAD_CHANNEL_ID)
            .setContentTitle("正在下载: $modelName")
            .setContentText("$progress%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification(modelName: String, progress: Int, downloaded: Long, total: Long) {
        val downloadedMB = downloaded / (1024 * 1024)
        val totalMB = total / (1024 * 1024)
        
        val notification = NotificationCompat.Builder(this, KuaiHuiAIApplication.DOWNLOAD_CHANNEL_ID)
            .setContentTitle("正在下载: $modelName")
            .setContentText("$progress% (${downloadedMB}MB / ${totalMB}MB)")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun showCompletionNotification(modelName: String) {
        val notification = NotificationCompat.Builder(this, KuaiHuiAIApplication.DOWNLOAD_CHANNEL_ID)
            .setContentTitle("下载完成")
            .setContentText("$modelName 已下载完成")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }
    
    private fun showErrorNotification(modelName: String, error: String) {
        val notification = NotificationCompat.Builder(this, KuaiHuiAIApplication.DOWNLOAD_CHANNEL_ID)
            .setContentTitle("下载失败")
            .setContentText("$modelName: $error")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID + 2, notification)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}

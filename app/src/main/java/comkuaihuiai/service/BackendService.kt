package comkuaihuiai.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import comkuaihuiai.MainActivity
import comkuaihuiai.R
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * 本地推理后端服务
 * 类似于Local Dream的BackendService
 * 启动本地后端可执行文件，通过HTTP与后端通信
 */
class BackendService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var backendProcess: Process? = null
    
    companion object {
        private const val TAG = "BackendService"
        
        // 通知渠道
        private const val CHANNEL_ID = "backend_service_channel"
        private const val NOTIFICATION_ID = 2001
        
        // 后端配置
        private const val DEFAULT_PORT = 8081
        private const val BACKEND_DIR = "backend"
        
        // 后端状态
        sealed class BackendState {
            object Idle : BackendState()
            object Starting : BackendState()
            object Ready : BackendState()
            object Running : BackendState()
            data class Error(val message: String) : BackendState()
        }
        
        private val _backendState = kotlinx.coroutines.flow.MutableStateFlow<BackendState>(BackendState.Idle)
        val backendState: kotlinx.coroutines.flow.StateFlow<BackendState> = _backendState
        
        fun updateState(state: BackendState) {
            _backendState.value = state
        }
        
        // Action
        const val ACTION_START_BACKEND = "comkuaihuiai.START_BACKEND"
        const val ACTION_STOP_BACKEND = "comkuaihuiai.STOP_BACKEND"
        
        // 参数
        const val EXTRA_MODEL_NAME = "model_name"
        const val EXTRA_WIDTH = "width"
        const val EXTRA_HEIGHT = "height"
    }
    
    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_BACKEND -> {
                val modelName = intent.getStringExtra(EXTRA_MODEL_NAME) ?: "stable-diffusion"
                val width = intent.getIntExtra(EXTRA_WIDTH, 512)
                val height = intent.getIntExtra(EXTRA_HEIGHT, 512)
                startBackend(modelName, width, height)
            }
            ACTION_STOP_BACKEND -> {
                stopBackend()
            }
        }
        return START_STICKY
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.backend_notify),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "推理后端服务"
        }
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun createNotification(message: String): android.app.Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.backend_notify_title))
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_inference)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    /**
     * 启动后端服务
     */
    private fun startBackend(modelName: String, width: Int, height: Int) {
        serviceScope.launch {
            try {
                updateState(BackendState.Starting)
                startForeground(NOTIFICATION_ID, createNotification("正在启动推理引擎..."))
                
                // 准备后端目录
                val backendDir = File(filesDir, BACKEND_DIR)
                if (!backendDir.exists()) {
                    backendDir.mkdirs()
                }
                
                // 检查后端可执行文件
                val backendExecutable = findBackendExecutable(backendDir)
                
                if (backendExecutable == null) {
                    // 后端文件不存在，使用模拟模式
                    Log.w(TAG, "Backend executable not found, using remote API mode")
                    updateState(BackendState.Ready)
                    updateNotification("后端就绪 (API模式)")
                } else {
                    // 启动后端进程
                    startBackendProcess(backendExecutable, modelName, width, height)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start backend", e)
                updateState(BackendState.Error(e.message ?: "启动失败"))
                updateNotification("后端启动失败: ${e.message}")
            }
        }
    }
    
    /**
     * 查找后端可执行文件
     */
    private fun findBackendExecutable(backendDir: File): File? {
        // 检查assets中的后端
        val backendNames = listOf(
            "localdream-backend",
            "sd-backend",
            "python-backend"
        )
        
        for (name in backendNames) {
            val file = File(backendDir, name)
            if (file.exists() && file.canExecute()) {
                return file
            }
        }
        
        return null
    }
    
    /**
     * 启动后端进程
     */
    private fun startBackendProcess(executable: File, modelName: String, width: Int, height: Int) {
        val command = listOf(
            executable.absolutePath,
            "--model", modelName,
            "--width", width.toString(),
            "--height", height.toString(),
            "--port", DEFAULT_PORT.toString()
        )
        
        try {
            Log.i(TAG, "Starting backend: $command")
            backendProcess = ProcessBuilder(command)
                .directory(filesDir)
                .redirectErrorStream(true)
                .start()
            
            // 等待后端就绪
            serviceScope.launch {
                waitForBackendReady()
                updateState(BackendState.Running)
                updateNotification("推理引擎运行中")
            }
            
        } catch (e: IOException) {
            Log.e(TAG, "Backend process error", e)
            updateState(BackendState.Error(e.message ?: "进程错误"))
        }
    }
    
    /**
     * 等待后端就绪
     */
    private suspend fun waitForBackendReady() {
        val maxAttempts = 30
        for (i in 1..maxAttempts) {
            if (checkBackendConnection()) {
                Log.i(TAG, "Backend is ready after $i attempts")
                return
            }
            delay(500)
        }
        Log.w(TAG, "Backend ready check timeout, assuming ready")
    }
    
    /**
     * 检查后端连接
     */
    private fun checkBackendConnection(): Boolean {
        return try {
            val url = URL("http://127.0.0.1:$DEFAULT_PORT/health")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 1000
            connection.readTimeout = 1000
            val response = connection.responseCode
            connection.disconnect()
            response == 200
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 更新通知
     */
    private fun updateNotification(message: String) {
        notificationManager.notify(NOTIFICATION_ID, createNotification(message))
    }
    
    /**
     * 停止后端
     */
    private fun stopBackend() {
        serviceScope.launch {
            try {
                backendProcess?.destroy()
                backendProcess = null
                updateState(BackendState.Idle)
                updateNotification("推理引擎已停止")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping backend", e)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        backendProcess?.destroy()
        serviceScope.cancel()
    }
}

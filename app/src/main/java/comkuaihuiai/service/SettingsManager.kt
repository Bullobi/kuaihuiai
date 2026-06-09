package comkuaihuiai.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 设置管理器
 * 管理应用设置、用户首选项
 */
class SettingsManager(context: Context) {

    companion object {
        private const val TAG = "SettingsManager"
        
        // 设置文件名
        private const val PREFS_NAME = "kuaihui_settings"
        
        // 默认设置
        private const val DEFAULT_WIDTH = 512
        private const val DEFAULT_HEIGHT = 512
        private const val DEFAULT_STEPS = 20
        private const val DEFAULT_CFG = 7f
        private const val DEFAULT_SAMPLER = "Euler a"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    // ==================== 生成设置 ====================

    var defaultWidth: Int
        get() = prefs.getInt("default_width", DEFAULT_WIDTH)
        set(value) = prefs.edit().putInt("default_width", value).apply()

    var defaultHeight: Int
        get() = prefs.getInt("default_height", DEFAULT_HEIGHT)
        set(value) = prefs.edit().putInt("default_height", value).apply()

    var defaultSteps: Int
        get() = prefs.getInt("default_steps", DEFAULT_STEPS)
        set(value) = prefs.edit().putInt("default_steps", value).apply()

    var defaultCFGScale: Float
        get() = prefs.getFloat("default_cfg_scale", DEFAULT_CFG)
        set(value) = prefs.edit().putFloat("default_cfg_scale", value).apply()

    var defaultSampler: String
        get() = prefs.getString("default_sampler", DEFAULT_SAMPLER) ?: DEFAULT_SAMPLER
        set(value) = prefs.edit().putString("default_sampler", value).apply()

    var defaultSeed: Long
        get() = prefs.getLong("default_seed", -1L)
        set(value) = prefs.edit().putLong("default_seed", value).apply()

    var restoreFaces: Boolean
        get() = prefs.getBoolean("restore_faces", false)
        set(value) = prefs.edit().putBoolean("restore_faces", value).apply()

    var tiling: Boolean
        get() = prefs.getBoolean("tiling", false)
        set(value) = prefs.edit().putBoolean("tiling", value).apply()

    // ==================== 性能设置 ====================

    var useGPU: Boolean
        get() = prefs.getBoolean("use_gpu", true)
        set(value) = prefs.edit().putBoolean("use_gpu", value).apply()

    var lowMemoryMode: Boolean
        get() = prefs.getBoolean("low_memory_mode", false)
        set(value) = prefs.edit().putBoolean("low_memory_mode", value).apply()

    var cpuThreads: Int
        get() = prefs.getInt("cpu_threads", 4)
        set(value) = prefs.edit().putInt("cpu_threads", value).apply()

    var useFP16: Boolean
        get() = prefs.getBoolean("use_fp16", true)
        set(value) = prefs.edit().putBoolean("use_fp16", value).apply()

    var attentionPrecision: String
        get() = prefs.getString("attention_precision", "auto") ?: "auto"
        set(value) = prefs.edit().putString("attention_precision", value).apply()

    var vaePrecision: String
        get() = prefs.getString("vae_precision", "auto") ?: "auto"
        set(value) = prefs.edit().putString("vae_precision", value).apply()

    // ==================== 模型设置 ====================

    var selectedModel: String
        get() = prefs.getString("selected_model", "") ?: ""
        set(value) = prefs.edit().putString("selected_model", value).apply()

    var selectedVAE: String
        get() = prefs.getString("selected_vae", "auto") ?: "auto"
        set(value) = prefs.edit().putString("selected_vae", value).apply()

    var selectedScheduler: String
        get() = prefs.getString("selected_scheduler", "euler") ?: "euler"
        set(value) = prefs.edit().putString("selected_scheduler", value).apply()

    var modelCacheSize: Int
        get() = prefs.getInt("model_cache_size", 2)  // GB
        set(value) = prefs.edit().putInt("model_cache_size", value).apply()

    // ==================== 输出设置 ====================

    var outputFormat: String
        get() = prefs.getString("output_format", "png") ?: "png"
        set(value) = prefs.edit().putString("output_format", value).apply()

    var outputQuality: Int
        get() = prefs.getInt("output_quality", 100)
        set(value) = prefs.edit().putInt("output_quality", value).apply()

    var outputDirectory: String
        get() = prefs.getString("output_directory", "") ?: ""
        set(value) = prefs.edit().putString("output_directory", value).apply()

    var saveMetadata: Boolean
        get() = prefs.getBoolean("save_metadata", true)
        set(value) = prefs.edit().putBoolean("save_metadata", value).apply()

    var filenamePattern: String
        get() = prefs.getString("filename_pattern", "{prompt}-{seed}") ?: "{prompt}-{seed}"
        set(value) = prefs.edit().putString("filename_pattern", value).apply()

    // ==================== 界面设置 ====================

    var darkMode: Boolean
        get() = prefs.getBoolean("dark_mode", true)
        set(value) = prefs.edit().putBoolean("dark_mode", value).apply()

    var language: String
        get() = prefs.getString("language", "zh-CN") ?: "zh-CN"
        set(value) = prefs.edit().putString("language", value).apply()

    var showPreview: Boolean
        get() = prefs.getBoolean("show_preview", true)
        set(value) = prefs.edit().putBoolean("show_preview", value).apply()

    var gridColumns: Int
        get() = prefs.getInt("grid_columns", 3)
        set(value) = prefs.edit().putInt("grid_columns", value).apply()

    var autoSaveHistory: Boolean
        get() = prefs.getBoolean("auto_save_history", true)
        set(value) = prefs.edit().putBoolean("auto_save_history", value).apply()

    // ==================== 网络设置 ====================

    var apiBaseUrl: String
        get() = prefs.getString("api_base_url", "http://127.0.0.1:7860") ?: "http://127.0.0.1:7860"
        set(value) = prefs.edit().putString("api_base_url", value).apply()

    var apiTimeout: Int
        get() = prefs.getInt("api_timeout", 60)  // 秒
        set(value) = prefs.edit().putInt("api_timeout", value).apply()

    var useAuth: Boolean
        get() = prefs.getBoolean("use_auth", false)
        set(value) = prefs.edit().putBoolean("use_auth", value).apply()

    var apiUsername: String
        get() = prefs.getString("api_username", "") ?: ""
        set(value) = prefs.edit().putString("api_username", value).apply()

    var apiPassword: String
        get() = prefs.getString("api_password", "") ?: ""
        set(value) = prefs.edit().putString("api_password", value).apply()

    // ==================== 缓存设置 ====================

    var cacheSize: Long
        get() = prefs.getLong("cache_size", 500L)  // MB
        set(value) = prefs.edit().putLong("cache_size", value).apply()

    var autoCleanup: Boolean
        get() = prefs.getBoolean("auto_cleanup", true)
        set(value) = prefs.edit().putBoolean("auto_cleanup", value).apply()

    var cleanupThreshold: Float
        get() = prefs.getFloat("cleanup_threshold", 0.9f)
        set(value) = prefs.edit().putFloat("cleanup_threshold", value).apply()

    // ==================== 快捷键/手势 ====================

    var quickGenerate: Boolean
        get() = prefs.getBoolean("quick_generate", false)
        set(value) = prefs.edit().putBoolean("quick_generate", value).apply()

    var swipeToRegenerate: Boolean
        get() = prefs.getBoolean("swipe_to_regenerate", true)
        set(value) = prefs.edit().putBoolean("swipe_to_regenerate", value).apply()

    var doubleTapUpscale: Boolean
        get() = prefs.getBoolean("double_tap_upscale", true)
        set(value) = prefs.edit().putBoolean("double_tap_upscale", value).apply()

    // ==================== 高级设置 ====================

    var enableControlNet: Boolean
        get() = prefs.getBoolean("enable_controlnet", true)
        set(value) = prefs.edit().putBoolean("enable_controlnet", value).apply()

    var enableInpainting: Boolean
        get() = prefs.getBoolean("enable_inpainting", true)
        set(value) = prefs.edit().putBoolean("enable_inpainting", value).apply()

    var enableUpscaling: Boolean
        get() = prefs.getBoolean("enable_upscaling", true)
        set(value) = prefs.edit().putBoolean("enable_upscaling", value).apply()

    var enableModelscope: Boolean
        get() = prefs.getBoolean("enable_modelscope", true)
        set(value) = prefs.edit().putBoolean("enable_modelscope", value).apply()

    var customModelsPath: String
        get() = prefs.getString("custom_models_path", "") ?: ""
        set(value) = prefs.edit().putString("custom_models_path", value).apply()

    // ==================== 批量操作 ====================

    /**
     * 获取所有设置
     */
    fun getAllSettings(): Map<String, Any> {
        return mapOf(
            "generation" to mapOf(
                "default_width" to defaultWidth,
                "default_height" to defaultHeight,
                "default_steps" to defaultSteps,
                "default_cfg_scale" to defaultCFGScale,
                "default_sampler" to defaultSampler,
                "restore_faces" to restoreFaces,
                "tiling" to tiling
            ),
            "performance" to mapOf(
                "use_gpu" to useGPU,
                "low_memory_mode" to lowMemoryMode,
                "cpu_threads" to cpuThreads,
                "use_fp16" to useFP16
            ),
            "output" to mapOf(
                "output_format" to outputFormat,
                "output_quality" to outputQuality,
                "save_metadata" to saveMetadata
            ),
            "ui" to mapOf(
                "dark_mode" to darkMode,
                "language" to language,
                "grid_columns" to gridColumns
            )
        )
    }

    /**
     * 导出设置到 JSON
     */
    fun exportSettings(): String {
        return gson.toJson(getAllSettings())
    }

    /**
     * 从 JSON 导入设置
     */
    fun importSettings(json: String): Boolean {
        return try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val settings: Map<String, Any> = gson.fromJson(json, type)
            
            settings["generation"]?.let { gen ->
                (gen as? Map<String, Any>)?.forEach { (key, value) ->
                    when (key) {
                        "default_width" -> defaultWidth = (value as? Number)?.toInt() ?: DEFAULT_WIDTH
                        "default_height" -> defaultHeight = (value as? Number)?.toInt() ?: DEFAULT_HEIGHT
                        "default_steps" -> defaultSteps = (value as? Number)?.toInt() ?: DEFAULT_STEPS
                        "default_cfg_scale" -> defaultCFGScale = (value as? Number)?.toFloat() ?: DEFAULT_CFG
                        "default_sampler" -> defaultSampler = value as? String ?: DEFAULT_SAMPLER
                        "restore_faces" -> restoreFaces = value as? Boolean ?: false
                        "tiling" -> tiling = value as? Boolean ?: false
                    }
                }
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Import error: ${e.message}")
            false
        }
    }

    /**
     * 重置为默认设置
     */
    fun resetToDefaults() {
        prefs.edit().clear().apply()
        Log.i(TAG, "Settings reset to defaults")
    }

    /**
     * 获取默认生成参数
     */
    fun getDefaultGenerationParams(): GenerationParams {
        return GenerationParams(
            width = defaultWidth,
            height = defaultHeight,
            steps = defaultSteps,
            cfgScale = defaultCFGScale,
            sampler = defaultSampler,
            seed = defaultSeed,
            restoreFaces = restoreFaces,
            tiling = tiling
        )
    }
}

/**
 * 生成参数
 */
data class GenerationParams(
    val width: Int = 512,
    val height: Int = 512,
    val steps: Int = 20,
    val cfgScale: Float = 7f,
    val sampler: String = "Euler a",
    val seed: Long = -1,
    val restoreFaces: Boolean = false,
    val tiling: Boolean = false,
    val prompt: String = "",
    val negativePrompt: String = ""
)

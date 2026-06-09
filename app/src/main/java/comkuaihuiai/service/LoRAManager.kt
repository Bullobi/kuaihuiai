package comkuaihuiai.service

import android.content.Context
import android.util.Log
import comkuaihuiai.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

/**
 * 方向三：LoRA 管理器 - 更多模型生态
 * LoRA (Low-Rank Adaptation) 用于模型微调
 */
class LoRAManager(private val context: Context) {
    
    companion object {
        private const val TAG = "LoRAManager"
        private const val LORA_DIR = "models/lora"
    }
    
    private val loraDir = File(context.filesDir, LORA_DIR)
    
    private val _availableLoras = MutableStateFlow<List<LoraParam>>(emptyList())
    val availableLoras: StateFlow<List<LoraParam>> = _availableLoras.asStateFlow()
    
    private val _loadedLoras = MutableStateFlow<List<LoraParam>>(emptyList())
    val loadedLoras: StateFlow<List<LoraParam>> = _loadedLoras.asStateFlow()
    
    private val loraCache = mutableMapOf<String, LoraParam>()
    
    init {
        if (!loraDir.exists()) loraDir.mkdirs()
    }
    
    /**
     * 刷新 LoRA 列表
     */
    fun refreshLoras() {
        val loras = mutableListOf<LoraParam>()
        
        loraDir.listFiles()?.forEach { file ->
            if (file.extension in listOf("safetensors", "ckpt", "pt", "pth")) {
                loras.add(
                    LoraParam(
                        id = file.nameWithoutExtension,
                        name = file.nameWithoutExtension,
                        path = file.absolutePath,
                        weight = 1.0f,
                        category = detectCategory(file.nameWithoutExtension)
                    )
                )
            }
        }
        
        _availableLoras.value = loras
        Log.i(TAG, "发现 ${loras.size} 个 LoRA 模型")
    }
    
    /**
     * 加载 LoRA
     */
    suspend fun loadLora(lora: LoraParam, weight: Float = 1.0f): LoraParam = withContext(Dispatchers.IO) {
        Log.i(TAG, "加载 LoRA: ${lora.name} (权重: $weight)")
        
        val weightedLora = lora.copy(weight = weight)
        loraCache[lora.id] = weightedLora
        
        val current = _loadedLoras.value.toMutableList()
        current.removeAll { it.id == lora.id }
        current.add(weightedLora)
        _loadedLoras.value = current
        
        Log.i(TAG, "LoRA ${lora.name} 加载完成")
        weightedLora
    }
    
    /**
     * 卸载 LoRA
     */
    fun unloadLora(loraId: String) {
        loraCache.remove(loraId)
        val current = _loadedLoras.value.toMutableList()
        current.removeAll { it.id == loraId }
        _loadedLoras.value = current
        Log.i(TAG, "LoRA $loraId 已卸载")
    }
    
    /**
     * 获取已加载的 LoRA 权重
     */
    fun getLoadedLoras(): Map<String, Float> {
        return _loadedLoras.value.associate { it.id to it.weight }
    }
    
    /**
     * 搜索 LoRA
     */
    fun searchLoras(query: String): List<LoraParam> {
        return _availableLoras.value.filter { 
            it.name.contains(query, ignoreCase = true) ||
            it.category.displayName.contains(query, ignoreCase = true)
        }
    }
    
    /**
     * 按类别获取 LoRA
     */
    fun getLorasByCategory(category: LoraCategory): List<LoraParam> {
        return _availableLoras.value.filter { it.category == category }
    }
    
    /**
     * 检测 LoRA 类别
     */
    private fun detectCategory(name: String): LoraCategory {
        val lower = name.lowercase()
        return when {
            lower.contains("style") || lower.contains(" Aesthetic") -> LoraCategory.STYLE
            lower.contains("character") || lower.contains("char") || lower.contains("girl") || lower.contains("boy") -> LoraCategory.CHARACTER
            lower.contains("pose") || lower.contains("action") -> LoraCategory.POSE
            lower.contains("hair") -> LoraCategory.HAIR
            lower.contains("cloth") || lower.contains("outfit") || lower.contains("dress") -> LoraCategory.CLOTHING
            lower.contains("background") || lower.contains("bg") || lower.contains("scene") -> LoraCategory.BACKGROUND
            lower.contains("light") || lower.contains("lighting") -> LoraCategory.LIGHTING
            lower.contains("camera") || lower.contains("lens") -> LoraCategory.CAMERA
            lower.contains("concept") || lower.contains("celestia") -> LoraCategory.CONCEPT
            else -> LoraCategory.CONCEPT
        }
    }
    
    /**
     * 检测触发词
     */
    private fun detectTriggerWords(name: String): List<String> {
        val words = mutableListOf<String>()
        val lower = name.lowercase()
        
        if (lower.contains("anime")) words.add("anime")
        if (lower.contains("realistic")) words.add("realistic")
        if (lower.contains("photo")) words.add("photorealistic")
        if (lower.contains("style")) words.add("style")
        
        return words
    }
    
    /**
     * 获取内置 LoRA 列表
     */
    fun getBuiltinLoras(): List<LoraParam> = listOf(
        LoraParam(id = "anime-style", name = "动漫风格", path = "models/lora/anime-style.safetensors", weight = 0f, category = LoraCategory.STYLE),
        LoraParam(id = "realistic-style", name = "写实风格", path = "models/lora/realistic-style.safetensors", weight = 0f, category = LoraCategory.STYLE),
        LoraParam(id = "portrait-enhance", name = "人像增强", path = "models/lora/portrait-enhance.safetensors", weight = 0f, category = LoraCategory.CHARACTER),
        LoraParam(id = "dynamic-pose", name = "动态姿势", path = "models/lora/dynamic-pose.safetensors", weight = 0f, category = LoraCategory.POSE)
    )
}

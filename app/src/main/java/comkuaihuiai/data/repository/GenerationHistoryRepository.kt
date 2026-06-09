package comkuaihuiai.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 生成历史记录管理器
 * 管理所有图像生成历史、收藏、标签
 */
class GenerationHistoryRepository(private val context: Context) {

    private val gson = Gson()
    private val historyDir = File(context.filesDir, "history")
    private val historyFile = File(historyDir, "generation_history.json")
    private val favoritesFile = File(historyDir, "favorites.json")
    private val tagsFile = File(historyDir, "tags.json")

    init {
        if (!historyDir.exists()) historyDir.mkdirs()
    }

    // ==================== 历史记录 ====================

    /**
     * 获取生成历史
     */
    fun getHistory(
        limit: Int = 100,
        offset: Int = 0,
        filter: HistoryFilter? = null
    ): List<GenerationRecord> {
        return try {
            if (!historyFile.exists()) return emptyList()
            
            val json = historyFile.readText()
            val type = object : TypeToken<List<GenerationRecord>>() {}.type
            var history: MutableList<GenerationRecord> = gson.fromJson(json, type)
            
            // 应用筛选
            if (filter != null) {
                history = history.filter { record ->
                    var match = true
                    
                    if (filter.startTime != null && record.timestamp < filter.startTime) match = false
                    if (filter.endTime != null && record.timestamp > filter.endTime) match = false
                    if (filter.model != null && record.model != filter.model) match = false
                    if (filter.sampler != null && record.sampler != filter.sampler) match = false
                    if (filter.width != null && record.width != filter.width) match = false
                    if (filter.height != null && record.height != filter.height) match = false
                    if (filter.isFavorite == true && !record.isFavorite) match = false
                    
                    match
                }.toMutableList()
            }
            
            // 排序和分页
            history.sortByDescending { it.timestamp }
            history.drop(offset).take(limit)
            
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 添加历史记录
     */
    fun addRecord(record: GenerationRecord) {
        try {
            val history = getHistory(limit = Int.MAX_VALUE).toMutableList()
            
            // 添加新记录到开头
            history.add(0, record.copy(
                id = generateId(),
                timestamp = System.currentTimeMillis()
            ))
            
            // 限制历史记录数量（保留最近10000条）
            val trimmed = history.take(10000)
            
            historyFile.writeText(gson.toJson(trimmed))
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 删除历史记录
     */
    fun deleteRecord(recordId: String): Boolean {
        return try {
            val history = getHistory(limit = Int.MAX_VALUE).toMutableList()
            val removed = history.removeAll { it.id == recordId }
            
            if (removed) {
                historyFile.writeText(gson.toJson(history))
            }
            
            removed
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 批量删除
     */
    fun deleteRecords(recordIds: List<String>): Int {
        return try {
            val history = getHistory(limit = Int.MAX_VALUE).toMutableList()
            val originalSize = history.size
            
            history.removeAll { it.id in recordIds }
            
            historyFile.writeText(gson.toJson(history))
            
            originalSize - history.size
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 清空历史
     */
    fun clearHistory() {
        historyFile.writeText("[]")
    }

    /**
     * 搜索历史
     */
    fun searchHistory(query: String): List<GenerationRecord> {
        return getHistory(limit = Int.MAX_VALUE).filter { record ->
            record.prompt.contains(query, ignoreCase = true) ||
            record.negativePrompt.contains(query, ignoreCase = true) ||
            record.tags.any { it.contains(query, ignoreCase = true) }
        }
    }

    /**
     * 获取统计信息
     */
    fun getStatistics(): HistoryStatistics {
        val history = getHistory(limit = Int.MAX_VALUE)
        
        if (history.isEmpty()) {
            return HistoryStatistics()
        }
        
        // 计算统计
        val totalGenerations = history.size
        val totalImages = history.sumOf { it.images.size }
        
        // 常用模型
        val modelUsage = history.groupBy { it.model }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
        
        // 常用采样器
        val samplerUsage = history.groupBy { it.sampler }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
        
        // 平均参数
        val avgSteps = history.map { it.steps }.average().takeIf { !it.isNaN() } ?: 0.0
        val avgCFG = history.map { it.cfgScale }.average().takeIf { !it.isNaN() } ?: 0.0
        
        // 分辨率分布
        val resolutions = history.groupBy { "${it.width}x${it.height}" }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
        
        return HistoryStatistics(
            totalGenerations = totalGenerations,
            totalImages = totalImages,
            favoriteCount = history.count { it.isFavorite },
            modelUsage = modelUsage.toMap(),
            samplerUsage = samplerUsage.toMap(),
            avgSteps = avgSteps,
            avgCFG = avgCFG,
            topResolutions = resolutions.toMap()
        )
    }

    // ==================== 收藏 ====================

    /**
     * 获取收藏
     */
    fun getFavorites(): List<GenerationRecord> {
        return getHistory(limit = Int.MAX_VALUE).filter { it.isFavorite }
    }

    /**
     * 切换收藏状态
     */
    fun toggleFavorite(recordId: String): Boolean {
        return try {
            val history = getHistory(limit = Int.MAX_VALUE).toMutableList()
            val index = history.indexOfFirst { it.id == recordId }
            
            if (index >= 0) {
                val record = history[index]
                history[index] = record.copy(isFavorite = !record.isFavorite)
                historyFile.writeText(gson.toJson(history))
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 添加到收藏
     */
    fun addToFavorites(recordId: String): Boolean {
        return try {
            val history = getHistory(limit = Int.MAX_VALUE).toMutableList()
            val index = history.indexOfFirst { it.id == recordId }
            
            if (index >= 0) {
                history[index] = history[index].copy(isFavorite = true)
                historyFile.writeText(gson.toJson(history))
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    // ==================== 标签 ====================

    /**
     * 获取所有标签
     */
    fun getAllTags(): Map<String, Int> {
        return try {
            if (!tagsFile.exists()) return emptyMap()
            
            val json = tagsFile.readText()
            val type = object : TypeToken<Map<String, Int>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * 为记录添加标签
     */
    fun addTag(recordId: String, tag: String): Boolean {
        return try {
            val history = getHistory(limit = Int.MAX_VALUE).toMutableList()
            val index = history.indexOfFirst { it.id == recordId }
            
            if (index >= 0) {
                val record = history[index]
                val newTags = record.tags + tag
                history[index] = record.copy(tags = newTags)
                historyFile.writeText(gson.toJson(history))
                
                // 更新标签统计
                updateTagCount(tag, 1)
                
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 移除标签
     */
    fun removeTag(recordId: String, tag: String): Boolean {
        return try {
            val history = getHistory(limit = Int.MAX_VALUE).toMutableList()
            val index = history.indexOfFirst { it.id == recordId }
            
            if (index >= 0) {
                val record = history[index]
                val newTags = record.tags - tag
                history[index] = record.copy(tags = newTags)
                historyFile.writeText(gson.toJson(history))
                
                // 更新标签统计
                updateTagCount(tag, -1)
                
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 按标签搜索
     */
    fun getByTag(tag: String): List<GenerationRecord> {
        return getHistory(limit = Int.MAX_VALUE).filter { tag in it.tags }
    }

    private fun updateTagCount(tag: String, delta: Int) {
        val tags = getAllTags().toMutableMap()
        tags[tag] = (tags[tag] ?: 0) + delta
        tagsFile.writeText(gson.toJson(tags))
    }

    // ==================== 工具方法 ====================

    private fun generateId(): String {
        return "gen_${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}"
    }
}

/**
 * 生成记录
 */
data class GenerationRecord(
    val id: String,
    val timestamp: Long,
    val prompt: String,
    val negativePrompt: String,
    val width: Int,
    val height: Int,
    val steps: Int,
    val sampler: String,
    val cfgScale: Float,
    val seed: Long,
    val model: String,
    val modelHash: String?,
    val images: List<String>,  // 图片路径列表
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList(),
    val notes: String = "",
    val generationTime: Long = 0  // 毫秒
) {
    fun getDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    fun getResolution(): String = "${width}x${height}"
}

/**
 * 历史筛选条件
 */
data class HistoryFilter(
    val startTime: Long? = null,
    val endTime: Long? = null,
    val model: String? = null,
    val sampler: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val isFavorite: Boolean? = null,
    val tags: List<String>? = null
)

/**
 * 历史统计
 */
data class HistoryStatistics(
    val totalGenerations: Int = 0,
    val totalImages: Int = 0,
    val favoriteCount: Int = 0,
    val modelUsage: Map<String, Int> = emptyMap(),
    val samplerUsage: Map<String, Int> = emptyMap(),
    val avgSteps: Double = 0.0,
    val avgCFG: Double = 0.0,
    val topResolutions: Map<String, Int> = emptyMap()
)

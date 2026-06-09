package comkuaihuiai.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * 提示词模板库
 * 包含常用提示词模板、热门提示词、历史记录
 */
class PromptTemplateRepository(private val context: Context) {

    private val gson = Gson()
    private val templatesFile = File(context.filesDir, "prompt_templates.json")
    private val historyFile = File(context.filesDir, "prompt_history.json")
    private val favoritesFile = File(context.filesDir, "prompt_favorites.json")

    /**
     * 获取所有模板分类
     */
    fun getCategories(): List<PromptCategory> {
        return listOf(
            PromptCategory(
                id = "portrait",
                name = "人物肖像",
                icon = "👤",
                templates = getPortraitTemplates()
            ),
            PromptCategory(
                id = "landscape",
                name = "风景自然",
                icon = "🏞️",
                templates = getLandscapeTemplates()
            ),
            PromptCategory(
                id = "anime",
                name = "动漫风格",
                icon = "🎨",
                templates = getAnimeTemplates()
            ),
            PromptCategory(
                id = "architecture",
                name = "建筑室内",
                icon = "🏠",
                templates = getArchitectureTemplates()
            ),
            PromptCategory(
                id = "abstract",
                name = "抽象艺术",
                icon = "✨",
                templates = getAbstractTemplates()
            ),
            PromptCategory(
                id = "photo",
                name = "摄影风格",
                icon = "📷",
                templates = getPhotoTemplates()
            ),
            PromptCategory(
                id = "concept",
                name = "概念艺术",
                icon = "🎭",
                templates = getConceptArtTemplates()
            ),
            PromptCategory(
                id = "fantasy",
                name = "奇幻魔幻",
                icon = "🐉",
                templates = getFantasyTemplates()
            )
        )
    }

    /**
     * 获取模板
     */
    fun getTemplates(categoryId: String): List<PromptTemplate> {
        return when (categoryId) {
            "portrait" -> getPortraitTemplates()
            "landscape" -> getLandscapeTemplates()
            "anime" -> getAnimeTemplates()
            "architecture" -> getArchitectureTemplates()
            "abstract" -> getAbstractTemplates()
            "photo" -> getPhotoTemplates()
            "concept" -> getConceptArtTemplates()
            "fantasy" -> getFantasyTemplates()
            else -> emptyList()
        }
    }

    /**
     * 搜索模板
     */
    fun searchTemplates(query: String): List<PromptTemplate> {
        val allTemplates = getCategories().flatMap { it.templates }
        return allTemplates.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.prompt.contains(query, ignoreCase = true) ||
            it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }
    }

    /**
     * 获取历史记录
     */
    fun getHistory(): List<PromptHistoryItem> {
        return try {
            if (!historyFile.exists()) return emptyList()
            val json = historyFile.readText()
            val type = object : TypeToken<List<PromptHistoryItem>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 添加到历史记录
     */
    fun addToHistory(prompt: String, negativePrompt: String = "") {
        val history = getHistory().toMutableList()
        
        // 避免重复
        history.removeAll { it.prompt == prompt }
        
        // 添加到开头
        history.add(0, PromptHistoryItem(
            prompt = prompt,
            negativePrompt = negativePrompt,
            timestamp = System.currentTimeMillis()
        ))
        
        // 只保留最近100条
        val trimmed = history.take(100)
        
        historyFile.writeText(gson.toJson(trimmed))
    }

    /**
     * 获取收藏
     */
    fun getFavorites(): List<PromptTemplate> {
        return try {
            if (!favoritesFile.exists()) return emptyList()
            val json = favoritesFile.readText()
            val type = object : TypeToken<List<PromptTemplate>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 添加收藏
     */
    fun addFavorite(template: PromptTemplate) {
        val favorites = getFavorites().toMutableList()
        if (favorites.none { it.id == template.id }) {
            favorites.add(template.copy(isFavorite = true))
            favoritesFile.writeText(gson.toJson(favorites))
        }
    }

    /**
     * 移除收藏
     */
    fun removeFavorite(templateId: String) {
        val favorites = getFavorites().toMutableList()
        favorites.removeAll { it.id == templateId }
        favoritesFile.writeText(gson.toJson(favorites))
    }

    /**
     * 热门提示词
     */
    fun getTrendingPrompts(): List<TrendingPrompt> {
        return listOf(
            TrendingPrompt("masterpiece, best quality, 1girl, beautiful face", 15000),
            TrendingPrompt("photorealistic, 4k, detailed skin", 12000),
            TrendingPrompt("landscape, mountain, sunset, village", 10000),
            TrendingPrompt("cyberpunk, neon lights, rain, city", 9500),
            TrendingPrompt("anime style, colorful, detailed background", 9000),
            TrendingPrompt("portrait, studio lighting, professional", 8500),
            TrendingPrompt("fantasy castle, dragons, epic", 8000),
            TrendingPrompt("steampunk, mechanical, brass", 7500),
            TrendingPrompt("watercolor painting, soft colors", 7000),
            TrendingPrompt("minimalist, geometric, modern", 6500)
        )
    }

    // ==================== 模板集合 ====================

    private fun getPortraitTemplates(): List<PromptTemplate> = listOf(
        PromptTemplate("portrait_1", "精致人像", "masterpiece, best quality, 1girl, beautiful face, detailed eyes, porcelain skin, soft lighting, professional photography", "blurry, low quality, deformed, ugly", listOf("portrait", "girl", "beautiful")),
        PromptTemplate("portrait_2", "商务肖像", "professional portrait, business suit, studio lighting, confident smile, sharp focus, 4k", "blurry, low quality, casual", listOf("portrait", "business", "professional")),
        PromptTemplate("portrait_3", "复古风格", "vintage portrait, 1950s style, film grain, soft colors, nostalgic, classic beauty", "modern, blurry", listOf("portrait", "vintage", "retro")),
        PromptTemplate("portrait_4", "水下人像", "underwater portrait, flowing hair, ethereal, bubbles, soft lighting, dreamlike", "blurry, dark, murky", listOf("portrait", "underwater", "fantasy")),
        PromptTemplate("portrait_5", "赛博朋克", "cyberpunk portrait, neon makeup, cybernetic implants, futuristic, rgb lights", "natural, normal lighting", listOf("portrait", "cyberpunk", "scifi"))
    )

    private fun getLandscapeTemplates(): List<PromptTemplate> = listOf(
        PromptTemplate("landscape_1", "山脉日落", "mountain landscape, golden hour, sunset, dramatic clouds, warm colors, epic scale, nature photography", "indoor, urban, blurry", listOf("landscape", "mountain", "sunset")),
        PromptTemplate("landscape_2", "森林清晨", "mystical forest, morning light, rays of light, fog, enchanted, fantasy landscape, tall trees", "urban, city, dark", listOf("landscape", "forest", "fantasy")),
        PromptTemplate("landscape_3", "海岸线", "coastal landscape, ocean waves, rocky cliffs, dramatic sky, seascape, golden hour", "indoor, blurry", listOf("landscape", "ocean", "beach")),
        PromptTemplate("landscape_4", "城市夜景", "cityscape, night, lights, reflections, skyscrapers, urban, long exposure", "daytime, rural", listOf("landscape", "city", "night")),
        PromptTemplate("landscape_5", "沙漠风光", "desert landscape, sand dunes, palm trees, oasis, golden hour, epic, vast", "forest, water, cold", listOf("landscape", "desert", "沙漠"))
    )

    private fun getAnimeTemplates(): List<PromptTemplate> = listOf(
        PromptTemplate("anime_1", "动漫少女", "anime style, 1girl, cute, colorful hair, large eyes, detailed background, vibrant colors, illustration", "realistic, photorealistic", listOf("anime", "girl", "illustration")),
        PromptTemplate("anime_2", "日式风景", "anime style landscape, ghibli inspired, hills, sky clouds,治愈, peaceful, detailed background", "realistic, dark", listOf("anime", "landscape", "ghibli")),
        PromptTemplate("anime_3", "战斗场景", "anime action scene, dynamic pose, energy effects, detailed, intense, comic book style", "still, calm", listOf("anime", "action", "comic")),
        PromptTemplate("anime_4", "Q版人物", "chibi style, cute, small, round, adorable, pastel colors, kawaii", "realistic, detailed", listOf("anime", "chibi", "cute")),
        PromptTemplate("anime_5", "机甲风格", "mecha anime, robot, mechanical, detailed, sci-fi, dynamic pose, intense", "natural, organic", listOf("anime", "mecha", "robot"))
    )

    private fun getArchitectureTemplates(): List<PromptTemplate> = listOf(
        PromptTemplate("arch_1", "现代建筑", "modern architecture, minimalist, glass and steel, clean lines, contemporary, 4k, architectural photography", "old, messy, cluttered", listOf("architecture", "modern", "building")),
        PromptTemplate("arch_2", "欧式城堡", "european castle, gothic, medieval, detailed, stone walls, towers, dramatic sky", "modern, ruin", listOf("architecture", "castle", "gothic")),
        PromptTemplate("arch_3", "室内设计", "interior design, modern living room, minimalist, natural light, cozy, stylish furniture, 3d render", "cluttered, old, dirty", listOf("interior", "design", "room")),
        PromptTemplate("arch_4", "日本神社", "shinto shrine, japanese traditional, torii gate, cherry blossoms, peaceful, serene", "modern, western", listOf("architecture", "japanese", "temple")),
        PromptTemplate("arch_5", "未来城市", "futuristic city, sci-fi, flying cars, neon, vertical city, cyberpunk architecture", "old, traditional", listOf("architecture", "scifi", "future"))
    )

    private fun getAbstractTemplates(): List<PromptTemplate> = listOf(
        PromptTemplate("abstract_1", "色彩渐变", "abstract art, fluid colors, gradient, vibrant, smooth, colorful, psychedelic", "realistic, detailed", listOf("abstract", "colorful", "gradient")),
        PromptTemplate("abstract_2", "几何艺术", "geometric abstraction, shapes, patterns, minimalist, clean, modern", "realistic, organic", listOf("abstract", "geometric", "minimal")),
        PromptTemplate("abstract_3", "数字抽象", "digital art, glitch art, distorted, abstract, futuristic, cyber", "natural, traditional", listOf("abstract", "digital", "glitch")),
        PromptTemplate("abstract_4", "水彩抽象", "abstract watercolor, fluid, soft, pastel colors, flowing, dreamy", "sharp, digital", listOf("abstract", "watercolor", "soft")),
        PromptTemplate("abstract_5", "金属质感", "abstract metal, chrome, shiny, reflections, modern, sleek, industrial", "matte, organic", listOf("abstract", "metal", "modern"))
    )

    private fun getPhotoTemplates(): List<PromptTemplate> = listOf(
        PromptTemplate("photo_1", "人像摄影", "professional portrait photography, studio lighting, sharp focus, 4k, high detail, professional", "blurry, low quality", listOf("photo", "portrait", "studio")),
        PromptTemplate("photo_2", "风景摄影", "landscape photography, national geographic style, epic, dramatic, professional quality", "art, illustration", listOf("photo", "landscape", "nature")),
        PromptTemplate("photo_3", "微距摄影", "macro photography, extreme close-up, detailed, nature, insects, flowers, 4k", "blurry, far", listOf("photo", "macro", "closeup")),
        PromptTemplate("photo_4", "街拍风格", "street photography, urban, candid, documentary style, natural light, 35mm film look", "posed, studio", listOf("photo", "street", "urban")),
        PromptTemplate("photo_5", "黑白摄影", "black and white photography, monochrome, high contrast, dramatic, timeless, classic", "color, modern", listOf("photo", "bw", "monochrome"))
    )

    private fun getConceptArtTemplates(): List<PromptTemplate> = listOf(
        PromptTemplate("concept_1", "角色设计", "character concept art, detailed, detailed clothing, accessories, fantasy, rpg, trending on artstation", "blurry, low detail", listOf("concept", "character", "design")),
        PromptTemplate("concept_2", "环境概念", "environment concept art, detailed, epic, fantasy world, concept design, artstation trending", "simple, sketch", listOf("concept", "environment", "fantasy")),
        PromptTemplate("concept_3", "产品设计", "product design concept, industrial design, sleek, modern, 3d render, clean", "messy, sketch", listOf("concept", "product", "design")),
        PromptTemplate("concept_4", "UI设计", "UI design concept, app interface, modern, clean, minimalist, glassmorphism, 4k", "old style, cluttered", listOf("concept", "ui", "interface")),
        PromptTemplate("concept_5", "科幻概念", "sci-fi concept art, spaceship, alien world, futuristic, concept design, epic scale", "realistic, present", listOf("concept", "scifi", "future"))
    )

    private fun getFantasyTemplates(): List<PromptTemplate> = listOf(
        PromptTemplate("fantasy_1", "奇幻生物", "fantasy creature, dragon, epic, detailed scales, fire, wings, legendary, artstation", "realistic, modern", listOf("fantasy", "dragon", "creature")),
        PromptTemplate("fantasy_2", "魔法场景", "fantasy magic scene, spell, magical, particles, ethereal, epic fantasy, glowing", "mundane, normal", listOf("fantasy", "magic", "spell")),
        PromptTemplate("fantasy_3", "精灵王子", "elf character, fantasy, elegant, detailed armor, pointed ears, forest, ethereal beauty", "modern, casual", listOf("fantasy", "elf", "character")),
        PromptTemplate("fantasy_4", "暗黑骑士", "dark fantasy knight, armor, gothic, ominous, epic, detailed, dramatic lighting", "bright, happy", listOf("fantasy", "knight", "dark")),
        PromptTemplate("fantasy_5", "海底世界", "underwater fantasy, mermaids, coral reef, magical, ethereal, detailed, ocean", "land, surface", listOf("fantasy", "underwater", "ocean"))
    )
}

/**
 * 提示词模板
 */
data class PromptTemplate(
    val id: String,
    val name: String,
    val prompt: String,
    val negativePrompt: String = "",
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val usageCount: Int = 0
)

/**
 * 提示词分类
 */
data class PromptCategory(
    val id: String,
    val name: String,
    val icon: String,
    val templates: List<PromptTemplate>
)

/**
 * 历史记录项
 */
data class PromptHistoryItem(
    val prompt: String,
    val negativePrompt: String,
    val timestamp: Long
)

/**
 * 热门提示词
 */
data class TrendingPrompt(
    val prompt: String,
    val usageCount: Int
)

package comkuaihuiai.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import comkuaihuiai.data.model.*
import comkuaihuiai.data.repository.GenerationRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 快绘AI v2.3.0 生成界面 - 五大方向全面增强
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerationScreenV23(
    repository: GenerationRepository,
    onNavigateToGallery: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("生成", "图生图", "局部重绘", "超分辨率")
    
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { 
                    Text("快绘AI v2.3.0", fontWeight = FontWeight.Bold) 
                },
                actions = {
                    IconButton(onClick = onNavigateToGallery) {
                        Icon(Icons.Outlined.PhotoLibrary, "图库")
                    }
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Outlined.History, "历史")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Outlined.Settings, "设置")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                when (index) {
                                    0 -> Icons.Outlined.AutoAwesome
                                    1 -> Icons.Outlined.Image
                                    2 -> Icons.Outlined.Edit
                                    else -> Icons.Outlined.ZoomIn
                                },
                                contentDescription = title
                            )
                        },
                        label = { Text(title) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { paddingValues ->
        when (selectedTab) {
            0 -> TextToImageTab(modifier = Modifier.padding(paddingValues), repository)
            1 -> ImageToImageTab(modifier = Modifier.padding(paddingValues), repository)
            2 -> InpaintTab(modifier = Modifier.padding(paddingValues), repository)
            3 -> UpscaleTab(modifier = Modifier.padding(paddingValues), repository)
        }
    }
}

/**
 * 方向一：更高质量生成 - SDXL 4K + Hires.fix
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextToImageTab(
    modifier: Modifier = Modifier,
    repository: GenerationRepository
) {
    var positivePrompt by remember { mutableStateOf("") }
    var negativePrompt by remember { mutableStateOf("") }
    
    // 基础参数
    var selectedWidth by remember { mutableStateOf(512) }
    var selectedHeight by remember { mutableStateOf(512) }
    var steps by remember { mutableIntStateOf(20) }
    var guidanceScale by remember { mutableFloatStateOf(7.5f) }
    var seed by remember { mutableStateOf("") }
    var selectedScheduler by remember { mutableStateOf(SchedulerType.DPMSOLVER_PLUS_PLUS_2M_KARRAS) }
    var batchSize by remember { mutableIntStateOf(1) }
    var selectedBaseModel by remember { mutableStateOf(BaseModelType.SD_1_5) }
    var clipSkip by remember { mutableIntStateOf(0) }
    
    // 方向一：高质量生成
    var enableHiresFix by remember { mutableStateOf(false) }
    var hiresScale by remember { mutableFloatStateOf(1.5f) }
    var hiresSteps by remember { mutableIntStateOf(15) }
    var hiresDenoise by remember { mutableFloatStateOf(0.4f) }
    var selectedHiresUpscaler by remember { mutableStateOf(HiresUpscaler.R_ESRGAN_4X) }
    var enableRefiner by remember { mutableStateOf(false) }
    
    // 方向二：更强大控制
    var enableControlNet by remember { mutableStateOf(false) }
    var selectedControlNetType by remember { mutableStateOf(ControlNetType.CANNY) }
    var controlNetWeight by remember { mutableFloatStateOf(1.0f) }
    var controlNetImage by remember { mutableStateOf<String?>(null) }
    
    // 方向三：更多模型生态
    var selectedLoras by remember { mutableStateOf<List<LoraParam>>(emptyList()) }
    var selectedVAE by remember { mutableStateOf<String?>(null) }
    var selectedEmbeddings by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // 方向四：性能优化
    var enableONNX by remember { mutableStateOf(false) }
    var selectedONNXProvider by remember { mutableStateOf(ONNXProvider.CPU) }
    var enableFP16 by remember { mutableStateOf(true) }
    
    // 方向五：UI升级
    var showAdvancedSettings by remember { mutableStateOf(false) }
    var showHighQualitySettings by remember { mutableStateOf(false) }
    var showControlNetSettings by remember { mutableStateOf(false) }
    var showModelSettings by remember { mutableStateOf(false) }
    var showPerformanceSettings by remember { mutableStateOf(false) }
    var showTemplateSheet by remember { mutableStateOf(false) }
    
    var isGenerating by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var progressMessage by remember { mutableStateOf("") }
    var generatedImages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var currentStep by remember { mutableIntStateOf(0) }
    
    var showSchedulerSheet by remember { mutableStateOf(false) }
    var showResolutionSheet by remember { mutableStateOf(false) }
    var showBaseModelSheet by remember { mutableStateOf(false) }
    var showHiresUpscalerSheet by remember { mutableStateOf(false) }
    var showControlNetTypeSheet by remember { mutableStateOf(false) }
    var showONNXProviderSheet by remember { mutableStateOf(false) }
    var showLoraSheet by remember { mutableStateOf(false) }
    var showVAESheet by remember { mutableStateOf(false) }
    var showEmbeddingSheet by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ===== 预览区域 =====
        ImagePreviewCard(
            images = generatedImages,
            isGenerating = isGenerating,
            progress = progress,
            progressMessage = progressMessage,
            currentStep = currentStep,
            totalSteps = steps,
            onImageClick = { /* 打开大图 */ }
        )
        
        // ===== 提示词区域 =====
        PromptInputSection(
            positivePrompt = positivePrompt,
            onPositivePromptChange = { positivePrompt = it },
            negativePrompt = negativePrompt,
            onNegativePromptChange = { negativePrompt = it },
            onTemplateClick = { showTemplateSheet = true },
            templates = PresetTemplates.builtInTemplates.take(6),
            onSelectTemplate = { template ->
                positivePrompt = template.positivePrompt
                negativePrompt = template.negativePrompt
                steps = template.defaultSteps
                guidanceScale = template.defaultGuidance
                selectedBaseModel = template.defaultBaseModel
            }
        )
        
        // ===== 方向五：预设模板快捷选择 =====
        TemplateQuickSelect(
            templates = PresetTemplates.builtInTemplates,
            onSelectTemplate = { template ->
                positivePrompt = template.positivePrompt
                negativePrompt = template.negativePrompt
            }
        )
        
        // ===== 基础设置快捷卡片 =====
        QuickSettingsRow(
            selectedWidth = selectedWidth,
            selectedHeight = selectedHeight,
            selectedScheduler = selectedScheduler,
            selectedBaseModel = selectedBaseModel,
            batchSize = batchSize,
            onResolutionClick = { showResolutionSheet = true },
            onSchedulerClick = { showSchedulerSheet = true },
            onBaseModelClick = { showBaseModelSheet = true },
            onBatchSizeChange = { batchSize = it }
        )
        
        // ===== 采样参数 =====
        ParameterSliders(
            steps = steps,
            onStepsChange = { steps = it },
            guidanceScale = guidanceScale,
            onGuidanceChange = { guidanceScale = it },
            seed = seed,
            onSeedChange = { seed = it },
            clipSkip = clipSkip,
            onClipSkipChange = { clipSkip = it }
        )
        
        // ===== 方向一：更高质量生成设置 =====
        HighQualitySettingsCard(
            expanded = showHighQualitySettings,
            onExpandChange = { showHighQualitySettings = it },
            enableHiresFix = enableHiresFix,
            onHiresFixChange = { enableHiresFix = it },
            hiresScale = hiresScale,
            onHiresScaleChange = { hiresScale = it },
            hiresSteps = hiresSteps,
            onHiresStepsChange = { hiresSteps = it },
            hiresDenoise = hiresDenoise,
            onHiresDenoiseChange = { hiresDenoise = it },
            selectedHiresUpscaler = selectedHiresUpscaler,
            onHiresUpscalerClick = { showHiresUpscalerSheet = true },
            enableRefiner = enableRefiner,
            onRefinerChange = { enableRefiner = it },
            isSDXL = selectedBaseModel.supportsSDXL,
            onShowResolutionSheet = { showResolutionSheet = true }
        )
        
        // ===== 方向二：更强大控制设置 =====
        ControlNetSettingsCard(
            expanded = showControlNetSettings,
            onExpandChange = { showControlNetSettings = it },
            enableControlNet = enableControlNet,
            onControlNetChange = { enableControlNet = it },
            selectedType = selectedControlNetType,
            onTypeClick = { showControlNetTypeSheet = true },
            weight = controlNetWeight,
            onWeightChange = { controlNetWeight = it },
            hasControlImage = controlNetImage != null,
            onSelectImage = { /* 选择控制图像 */ }
        )
        
        // ===== 方向三：更多模型生态设置 =====
        ModelEcosystemCard(
            expanded = showModelSettings,
            onExpandChange = { showModelSettings = it },
            selectedLoras = selectedLoras,
            onLoraClick = { showLoraSheet = true },
            selectedVAE = selectedVAE,
            onVAEClick = { showVAESheet = true },
            selectedEmbeddings = selectedEmbeddings,
            onEmbeddingClick = { showEmbeddingSheet = true }
        )
        
        // ===== 方向四：性能优化设置 =====
        PerformanceSettingsCard(
            expanded = showPerformanceSettings,
            onExpandChange = { showPerformanceSettings = it },
            enableONNX = enableONNX,
            onONNXChange = { enableONNX = it },
            selectedProvider = selectedONNXProvider,
            onProviderClick = { showONNXProviderSheet = true },
            enableFP16 = enableFP16,
            onFP16Change = { enableFP16 = it },
            cpuThreads = 4,
            onCpuThreadsChange = { }
        )
        
        // ===== 生成按钮 =====
        GenerateButton(
            isGenerating = isGenerating,
            enabled = positivePrompt.isNotBlank(),
            batchSize = batchSize,
            enableHiresFix = enableHiresFix,
            onGenerate = {
                isGenerating = true
                progress = 0f
                currentStep = 0
                
                scope.launch {
                    val params = GenerationParams(
                        positivePrompt = positivePrompt,
                        negativePrompt = negativePrompt,
                        width = selectedWidth,
                        height = selectedHeight,
                        steps = steps,
                        guidanceScale = guidanceScale,
                        seed = seed.toLongOrNull() ?: -1,
                        scheduler = selectedScheduler,
                        batchSize = batchSize,
                        clipSkip = clipSkip,
                        baseModel = selectedBaseModel,
                        enableHiresFix = enableHiresFix,
                        hiresScale = hiresScale,
                        hiresSteps = hiresSteps,
                        hiresDenoise = hiresDenoise,
                        hiresUpscaler = selectedHiresUpscaler,
                        enableRefiner = enableRefiner,
                        enableControlNet = enableControlNet,
                        controlNetType = selectedControlNetType,
                        controlNetWeight = controlNetWeight,
                        selectedLoras = selectedLoras,
                        vaeModel = selectedVAE,
                        selectedEmbeddings = selectedEmbeddings,
                        enableONNX = enableONNX,
                        onnxProvider = selectedONNXProvider,
                        enableFP16 = enableFP16
                    )
                    
                    repository.generateImage(params).collect { progressState ->
                        when (progressState) {
                            is GenerationProgress.Status -> {
                                progressMessage = progressState.message
                            }
                            is GenerationProgress.Progress -> {
                                currentStep = progressState.currentStep
                                progress = progressState.percent
                            }
                            is GenerationProgress.Completed -> {
                                isGenerating = false
                                progress = 1f
                            }
                            is GenerationProgress.Error -> {
                                isGenerating = false
                                progressMessage = "错误: ${progressState.message}"
                            }
                            else -> {}
                        }
                    }
                }
            },
            onCancel = {
                isGenerating = false
            }
        )
        
        Spacer(modifier = Modifier.height(100.dp))
    }
    
    // ===== Bottom Sheets =====
    if (showSchedulerSheet) {
        SchedulerBottomSheet(
            selected = selectedScheduler,
            onSelect = { 
                selectedScheduler = it
                showSchedulerSheet = false 
            },
            onDismiss = { showSchedulerSheet = false }
        )
    }
    
    if (showResolutionSheet) {
        ResolutionBottomSheet(
            selectedWidth = selectedWidth,
            selectedHeight = selectedHeight,
            isSDXL = selectedBaseModel.supportsSDXL,
            onSelect = { w, h, is4K ->
                selectedWidth = w
                selectedHeight = h
                showResolutionSheet = false
            },
            onDismiss = { showResolutionSheet = false }
        )
    }
    
    if (showBaseModelSheet) {
        BaseModelBottomSheet(
            selected = selectedBaseModel,
            onSelect = {
                selectedBaseModel = it
                showBaseModelSheet = false
            },
            onDismiss = { showBaseModelSheet = false }
        )
    }
    
    if (showHiresUpscalerSheet) {
        HiresUpscalerBottomSheet(
            selected = selectedHiresUpscaler,
            onSelect = {
                selectedHiresUpscaler = it
                showHiresUpscalerSheet = false
            },
            onDismiss = { showHiresUpscalerSheet = false }
        )
    }
    
    if (showControlNetTypeSheet) {
        ControlNetTypeBottomSheet(
            selected = selectedControlNetType,
            onSelect = {
                selectedControlNetType = it
                showControlNetTypeSheet = false
            },
            onDismiss = { showControlNetTypeSheet = false }
        )
    }
    
    if (showONNXProviderSheet) {
        ONNXProviderBottomSheet(
            selected = selectedONNXProvider,
            onSelect = {
                selectedONNXProvider = it
                showONNXProviderSheet = false
            },
            onDismiss = { showONNXProviderSheet = false }
        )
    }
    
    if (showTemplateSheet) {
        TemplateBottomSheet(
            templates = PresetTemplates.builtInTemplates,
            onSelect = { template ->
                positivePrompt = template.positivePrompt
                negativePrompt = template.negativePrompt
                steps = template.defaultSteps
                guidanceScale = template.defaultGuidance
                selectedBaseModel = template.defaultBaseModel
                showTemplateSheet = false
            },
            onDismiss = { showTemplateSheet = false }
        )
    }
}

/**
 * 方向五：图像预览卡片
 */
@Composable
private fun ImagePreviewCard(
    images: List<Bitmap>,
    isGenerating: Boolean,
    progress: Float,
    progressMessage: String,
    currentStep: Int,
    totalSteps: Int,
    onImageClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .padding(16.dp)
            .clickable(onClick = onImageClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                isGenerating -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // 进度环
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.size(100.dp),
                                strokeWidth = 8.dp,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Text(
                                "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            progressMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            "步 $currentStep / $totalSteps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                
                images.isNotEmpty() -> {
                    // 网格显示多张图片
                    LazyRow(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(images.size) { index ->
                            Image(
                                bitmap = images[index].asImageBitmap(),
                                contentDescription = "生成的图像 ${index + 1}",
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
                
                else -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "输入提示词开始生成",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 提示词输入区域
 */
@Composable
private fun PromptInputSection(
    positivePrompt: String,
    onPositivePromptChange: (String) -> Unit,
    negativePrompt: String,
    onNegativePromptChange: (String) -> Unit,
    onTemplateClick: () -> Unit,
    templates: List<PromptTemplate>,
    onSelectTemplate: (PromptTemplate) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // 正向提示词
        OutlinedTextField(
            value = positivePrompt,
            onValueChange = onPositivePromptChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("✨ 正向提示词") },
            placeholder = { Text("描述你想要的图像...") },
            minLines = 2,
            maxLines = 4,
            trailingIcon = {
                if (positivePrompt.isNotEmpty()) {
                    Row {
                        IconButton(onClick = onTemplateClick) {
                            Icon(Icons.Outlined.AutoAwesome, "模板")
                        }
                        IconButton(onClick = { onPositivePromptChange("") }) {
                            Icon(Icons.Outlined.Clear, "清除")
                        }
                    }
                }
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 负向提示词
        OutlinedTextField(
            value = negativePrompt,
            onValueChange = onNegativePromptChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("🚫 负向提示词") },
            placeholder = { Text("你不想要的内容...") },
            minLines = 1,
            maxLines = 2,
            trailingIcon = {
                if (negativePrompt.isNotEmpty()) {
                    IconButton(onClick = { onNegativePromptChange("") }) {
                        Icon(Icons.Outlined.Clear, "清除")
                    }
                }
            }
        )
    }
}

/**
 * 方向五：预设模板快捷选择
 */
@Composable
private fun TemplateQuickSelect(
    templates: List<PromptTemplate>,
    onSelectTemplate: (PromptTemplate) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            "🎨 快速模板",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(templates) { template ->
                SuggestionChip(
                    onClick = { onSelectTemplate(template) },
                    label = { Text(template.name) },
                    icon = { Text(template.icon) }
                )
            }
        }
    }
}

/**
 * 快捷设置行
 */
@Composable
private fun QuickSettingsRow(
    selectedWidth: Int,
    selectedHeight: Int,
    selectedScheduler: SchedulerType,
    selectedBaseModel: BaseModelType,
    batchSize: Int,
    onResolutionClick: () -> Unit,
    onSchedulerClick: () -> Unit,
    onBaseModelClick: () -> Unit,
    onBatchSizeChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickSettingCard(
            title = "分辨率",
            value = "${selectedWidth}×${selectedHeight}",
            icon = Icons.Outlined.AspectRatio,
            onClick = onResolutionClick,
            modifier = Modifier.weight(1f)
        )
        
        QuickSettingCard(
            title = "调度器",
            value = selectedScheduler.displayName,
            icon = Icons.Outlined.Speed,
            onClick = onSchedulerClick,
            modifier = Modifier.weight(1f)
        )
        
        QuickSettingCard(
            title = "模型",
            value = selectedBaseModel.displayName,
            icon = Icons.Outlined.ModelTraining,
            onClick = onBaseModelClick,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 参数滑块
 */
@Composable
private fun ParameterSliders(
    steps: Int,
    onStepsChange: (Int) -> Unit,
    guidanceScale: Float,
    onGuidanceChange: (Float) -> Unit,
    seed: String,
    onSeedChange: (String) -> Unit,
    clipSkip: Int,
    onClipSkipChange: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // 采样步数
        SettingSlider(
            title = "采样步数",
            value = steps,
            valueRange = 1..50,
            icon = Icons.Outlined.Timeline,
            onValueChange = onStepsChange
        )
        
        // 引导强度
        SettingSlider(
            title = "引导强度",
            value = guidanceScale,
            valueRange = 1f..20f,
            icon = Icons.Outlined.Balance,
            formatValue = { String.format("%.1f", it) },
            onValueChange = { onGuidanceChange(it) }
        )
        
        // 种子
        OutlinedTextField(
            value = seed,
            onValueChange = onSeedChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("🔢 种子 (-1 随机)") },
            singleLine = true,
            trailingIcon = {
                if (seed.isNotEmpty()) {
                    IconButton(onClick = { onSeedChange("-1") }) {
                        Icon(Icons.Outlined.Refresh, "随机")
                    }
                }
            }
        )
        
        // Clip Skip
        SettingSlider(
            title = "Clip Skip",
            value = clipSkip,
            valueRange = 0..3,
            icon = Icons.Outlined.SkipNext,
            onValueChange = onClipSkipChange
        )
    }
}

/**
 * 方向一：更高质量生成设置卡片
 */
@Composable
private fun HighQualitySettingsCard(
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    enableHiresFix: Boolean,
    onHiresFixChange: (Boolean) -> Unit,
    hiresScale: Float,
    onHiresScaleChange: (Float) -> Unit,
    hiresSteps: Int,
    onHiresStepsChange: (Int) -> Unit,
    hiresDenoise: Float,
    onHiresDenoiseChange: (Float) -> Unit,
    selectedHiresUpscaler: HiresUpscaler,
    onHiresUpscalerClick: () -> Unit,
    enableRefiner: Boolean,
    onRefinerChange: (Boolean) -> Unit,
    isSDXL: Boolean,
    onShowResolutionSheet: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandChange(!expanded) }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.HighQuality,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("🖼️ 更高质量生成", fontWeight = FontWeight.Bold)
                        Text(
                            "SDXL 4K · Hires.fix · Refiner",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            
            // Expanded Content
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // SDXL Badge
                    if (isSDXL) {
                        SuggestionChip(
                            onClick = onShowResolutionSheet,
                            label = { Text("⚡ SDXL 模型已选择 - 可用 4K 分辨率") }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    // Hires.fix Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("✨ Hires.fix 超分", fontWeight = FontWeight.Medium)
                            Text(
                                "先生成低分辨率再放大，提高质量",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = enableHiresFix, onCheckedChange = onHiresFixChange)
                    }
                    
                    if (enableHiresFix) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Hires Scale
                        SettingSlider(
                            title = "放大倍数",
                            value = hiresScale,
                            valueRange = 1.0f..4.0f,
                            icon = Icons.Outlined.ZoomIn,
                            formatValue = { String.format("%.1fx", it) },
                            onValueChange = { onHiresScaleChange(it) }
                        )
                        
                        // Hires Steps
                        SettingSlider(
                            title = "Hires 步数",
                            value = hiresSteps,
                            valueRange = 5..30,
                            icon = Icons.Outlined.Timeline,
                            onValueChange = onHiresStepsChange
                        )
                        
                        // Hires Denoise
                        SettingSlider(
                            title = "去噪强度",
                            value = hiresDenoise,
                            valueRange = 0.1f..0.8f,
                            icon = Icons.Outlined.BlurOn,
                            formatValue = { String.format("%.2f", it) },
                            onValueChange = { onHiresDenoiseChange(it) }
                        )
                        
                        // Upscaler Selection
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onHiresUpscalerClick() }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("超分算法", fontWeight = FontWeight.Medium)
                                    Text(
                                        selectedHiresUpscaler.displayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Refiner Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("🎯 Refiner 细化", fontWeight = FontWeight.Medium)
                            Text(
                                "使用细化模型提升细节",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = enableRefiner, onCheckedChange = onRefinerChange)
                    }
                }
            }
        }
    }
}

/**
 * 方向二：更强大控制设置卡片
 */
@Composable
private fun ControlNetSettingsCard(
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    enableControlNet: Boolean,
    onControlNetChange: (Boolean) -> Unit,
    selectedType: ControlNetType,
    onTypeClick: () -> Unit,
    weight: Float,
    onWeightChange: (Float) -> Unit,
    hasControlImage: Boolean,
    onSelectImage: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandChange(!expanded) }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.ControlCamera,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("🎯 更强大控制", fontWeight = FontWeight.Bold)
                        Text(
                            "ControlNet · IP-Adapter",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            
            // Expanded Content
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // ControlNet Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("🕹️ ControlNet 控制", fontWeight = FontWeight.Medium)
                            Text(
                                "通过边缘、深度、姿态等控制生成",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = enableControlNet, onCheckedChange = onControlNetChange)
                    }
                    
                    if (enableControlNet) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Control Type Selection
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTypeClick() }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("控制类型", fontWeight = FontWeight.Medium)
                                    Text(
                                        "${selectedType.icon} ${selectedType.displayName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Weight Slider
                        SettingSlider(
                            title = "控制权重",
                            value = weight,
                            valueRange = 0f..2f,
                            icon = Icons.Outlined.LinearScale,
                            formatValue = { String.format("%.2f", it) },
                            onValueChange = onWeightChange
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Control Image Selection
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectImage() }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (hasControlImage) Icons.Filled.Image else Icons.Outlined.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = if (hasControlImage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        if (hasControlImage) "已选择控制图像" else "添加控制图像",
                                        color = if (hasControlImage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 方向三：更多模型生态设置卡片
 */
@Composable
private fun ModelEcosystemCard(
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    selectedLoras: List<LoraParam>,
    onLoraClick: () -> Unit,
    selectedVAE: String?,
    onVAEClick: () -> Unit,
    selectedEmbeddings: List<String>,
    onEmbeddingClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandChange(!expanded) }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Extension,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("✨ 更多模型生态", fontWeight = FontWeight.Bold)
                        Text(
                            "LoRA · VAE · Embedding",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            
            // Expanded Content
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // LoRA Selection
                    ModelSelectionRow(
                        title = "🎨 LoRA 模型",
                        count = selectedLoras.size,
                        subtitle = if (selectedLoras.isEmpty()) "添加风格化模型" else "${selectedLoras.size} 个模型已选择",
                        onClick = onLoraClick
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // VAE Selection
                    ModelSelectionRow(
                        title = "🔮 VAE 美化",
                        count = if (selectedVAE != null) 1 else 0,
                        subtitle = selectedVAE ?: "自动选择",
                        onClick = onVAEClick
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Embedding Selection
                    ModelSelectionRow(
                        title = "📝 Embedding 嵌入",
                        count = selectedEmbeddings.size,
                        subtitle = if (selectedEmbeddings.isEmpty()) "添加文字嵌入" else "${selectedEmbeddings.size} 个已选择",
                        onClick = onEmbeddingClick
                    )
                }
            }
        }
    }
}

/**
 * 方向四：性能优化设置卡片
 */
@Composable
private fun PerformanceSettingsCard(
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    enableONNX: Boolean,
    onONNXChange: (Boolean) -> Unit,
    selectedProvider: ONNXProvider,
    onProviderClick: () -> Unit,
    enableFP16: Boolean,
    onFP16Change: (Boolean) -> Unit,
    cpuThreads: Int,
    onCpuThreadsChange: (Int) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandChange(!expanded) }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("⚡ 性能优化", fontWeight = FontWeight.Bold)
                        Text(
                            "ONNX · FP16 · 多线程",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            
            // Expanded Content
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // ONNX Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("🚀 ONNX 加速", fontWeight = FontWeight.Medium)
                            Text(
                                "使用ONNX Runtime加速推理",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = enableONNX, onCheckedChange = onONNXChange)
                    }
                    
                    if (enableONNX) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // ONNX Provider Selection
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onProviderClick() }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("加速Provider", fontWeight = FontWeight.Medium)
                                    Text(
                                        selectedProvider.displayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // FP16 Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("🪶 FP16 半精度", fontWeight = FontWeight.Medium)
                            Text(
                                "使用半精度浮点，减少显存占用",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = enableFP16, onCheckedChange = onFP16Change)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // CPU Threads
                    SettingSlider(
                        title = "CPU 线程数",
                        value = cpuThreads,
                        valueRange = 1..8,
                        icon = Icons.Outlined.Memory,
                        onValueChange = onCpuThreadsChange
                    )
                }
            }
        }
    }
}

/**
 * 生成按钮
 */
@Composable
private fun GenerateButton(
    isGenerating: Boolean,
    enabled: Boolean,
    batchSize: Int,
    enableHiresFix: Boolean,
    onGenerate: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Batch & Hires hint
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            if (batchSize > 1) {
                SuggestionChip(
                    onClick = { },
                    label = { Text("📦 批量 $batchSize 张") }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            if (enableHiresFix) {
                SuggestionChip(
                    onClick = { },
                    label = { Text("🖼️ Hires.fix") }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Main Button
        Button(
            onClick = if (isGenerating) onCancel else onGenerate,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = enabled || isGenerating,
            shape = RoundedCornerShape(12.dp),
            colors = if (isGenerating) {
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            } else {
                ButtonDefaults.buttonColors()
            }
        ) {
            Icon(
                if (isGenerating) Icons.Default.Stop else Icons.Default.AutoAwesome,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (isGenerating) "停止生成" else "🎨 开始生成",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

// ===== 辅助组件 =====

@Composable
private fun QuickSettingCard(
    title: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SettingSlider(
    title: String,
    value: Int,
    valueRange: IntRange,
    icon: ImageVector,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(80.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            modifier = Modifier.weight(1f)
        )
        Text("$value", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun SettingSlider(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    icon: ImageVector,
    formatValue: (Float) -> String = { String.format("%.1f", it) },
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(80.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.weight(1f)
        )
        Text(formatValue(value), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun ModelSelectionRow(
    title: String,
    count: Int,
    subtitle: String,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (count > 0) {
                Badge { Text("$count") }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

// ===== 其他 Tab 占位 =====

@Composable
private fun ImageToImageTab(modifier: Modifier, repository: GenerationRepository) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("图生图功能开发中...", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun InpaintTab(modifier: Modifier, repository: GenerationRepository) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("局部重绘功能开发中...", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun UpscaleTab(modifier: Modifier, repository: GenerationRepository) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("超分辨率功能开发中...", style = MaterialTheme.typography.titleMedium)
    }
}

// ===== Bottom Sheets =====

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SchedulerBottomSheet(
    selected: SchedulerType,
    onSelect: (SchedulerType) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("选择调度器", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            SchedulerType.entries.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { scheduler ->
                        FilterChip(
                            selected = scheduler == selected,
                            onClick = { onSelect(scheduler) },
                            label = { Text(scheduler.displayName) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResolutionBottomSheet(
    selectedWidth: Int,
    selectedHeight: Int,
    isSDXL: Boolean,
    onSelect: (Int, Int, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("选择分辨率", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isSDXL) {
                Text("⚡ SDXL 可用更高分辨率", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Resolution.entries.filter { !it.isSDXL || isSDXL }.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { res ->
                        val isSelected = res.width == selectedWidth && res.height == selectedHeight
                        Card(
                            modifier = Modifier.weight(1f).clickable { onSelect(res.width, res.height, res.is4K) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(res.displayName, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                if (res.is4K) Text("4K", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BaseModelBottomSheet(
    selected: BaseModelType,
    onSelect: (BaseModelType) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("选择基础模型", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            BaseModelType.entries.forEach { model ->
                ListItem(
                    headlineContent = { Text(model.displayName) },
                    supportingContent = { Text("最大分辨率: ${model.maxResolution}px") },
                    leadingContent = { Text(if (model.supportsSDXL) "⚡" else "🎨") },
                    trailingContent = { if (model == selected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable { onSelect(model) }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HiresUpscalerBottomSheet(
    selected: HiresUpscaler,
    onSelect: (HiresUpscaler) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("选择超分算法", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            HiresUpscaler.entries.forEach { upscaler ->
                ListItem(
                    headlineContent = { Text(upscaler.displayName) },
                    supportingContent = { Text(upscaler.description) },
                    trailingContent = { if (upscaler == selected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable { onSelect(upscaler) }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ControlNetTypeBottomSheet(
    selected: ControlNetType,
    onSelect: (ControlNetType) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("选择 ControlNet 类型", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            ControlNetType.entries.forEach { type ->
                ListItem(
                    headlineContent = { Text("${type.icon} ${type.displayName}") },
                    supportingContent = { Text(type.description) },
                    trailingContent = { if (type == selected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable { onSelect(type) }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ONNXProviderBottomSheet(
    selected: ONNXProvider,
    onSelect: (ONNXProvider) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("选择 ONNX Provider", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            ONNXProvider.entries.sortedByDescending { it.priority }.forEach { provider ->
                ListItem(
                    headlineContent = { Text(provider.displayName) },
                    trailingContent = { if (provider == selected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable { onSelect(provider) }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateBottomSheet(
    templates: List<PromptTemplate>,
    onSelect: (PromptTemplate) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("选择预设模板", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            templates.forEach { template ->
                ListItem(
                    headlineContent = { Text("${template.icon} ${template.name}") },
                    supportingContent = { Text(template.category.displayName) },
                    trailingContent = { 
                        if (template.isBuiltIn) SuggestionChip(
                            onClick = { },
                            label = { Text("内置") },
                            modifier = Modifier.height(24.dp)
                        )
                    },
                    modifier = Modifier.clickable { onSelect(template) }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package comkuaihuiai.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import comkuaihuiai.data.model.*
import comkuaihuiai.data.repository.GenerationRepository
import comkuaihuiai.ui.components.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 快绘AI v2.4.0 生成界面
 * 集成所有高级功能
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GenerationScreenV24(
    repository: GenerationRepository,
    onNavigateToGallery: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    // 状态管理
    var positivePrompt by remember { mutableStateOf("") }
    var negativePrompt by remember { mutableStateOf("") }
    var selectedBaseModel by remember { mutableStateOf(BaseModelType.SD_1_5) }
    var selectedResolution by remember { mutableStateOf<Resolution?>(null) }
    var width by remember { mutableStateOf(512) }
    var height by remember { mutableStateOf(512) }
    var steps by remember { mutableStateOf(25) }
    var guidanceScale by remember { mutableStateOf(7.5f) }
    var seed by remember { mutableStateOf(-1L) }
    var batchSize by remember { mutableStateOf(1) }
    var selectedScheduler by remember { mutableStateOf(SchedulerType.DPMSOLVER_PLUS_PLUS_2M_KARRAS) }
    
    // 高级选项
    var showAdvancedOptions by remember { mutableStateOf(false) }
    var enableHiresFix by remember { mutableStateOf(false) }
    var hiresScale by remember { mutableStateOf(1.5f) }
    var hiresSteps by remember { mutableStateOf(15) }
    var hiresUpscaler by remember { mutableStateOf(HiresUpscaler.LANCZOS) }
    var enableControlNet by remember { mutableStateOf(false) }
    var controlNetType by remember { mutableStateOf(ControlNetType.NONE) }
    var controlNetWeight by remember { mutableStateOf(1.0f) }
    var selectedLoras by remember { mutableStateOf<List<LoraParam>>(emptyList()) }
    var selectedVae by remember { mutableStateOf<String?>(null) }
    var selectedEmbeddings by remember { mutableStateOf<List<String>>(emptyList()) }
    var enableONNX by remember { mutableStateOf(false) }
    var onnxProvider by remember { mutableStateOf(ONNXProvider.CPU) }
    var enableFP16 by remember { mutableStateOf(true) }
    
    // UI 状态
    var isGenerating by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<GenerationProgress?>(null) }
    var generatedImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedTab by remember { mutableIntStateOf(0) }
    
    val scope = rememberCoroutineScope()
    
    // 监听生成进度
    LaunchedEffect(progress) {
        when (progress) {
            is GenerationProgress.Completed -> {
                generatedImages = (progress as GenerationProgress.Completed).paths
                isGenerating = false
            }
            is GenerationProgress.Error -> {
                isGenerating = false
            }
            else -> {}
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🎨 快绘AI v2.4.0", fontWeight = FontWeight.Bold)
                        Text(
                            if (selectedBaseModel.supportsSDXL) "SDXL 4K 模式" else "SD 1.5 模式",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, "历史")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "设置")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 模式选择标签
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("📝 文生图") },
                    icon = { Icon(Icons.Default.TextFields, null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("🖼️ 图生图") },
                    icon = { Icon(Icons.Default.Image, null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("✏️ 局部重绘") },
                    icon = { Icon(Icons.Default.Edit, null) }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 模型选择
            ModelSelector(
                selectedModel = selectedBaseModel,
                onModelSelect = { selectedBaseModel = it }
            )
            
            // 提示词输入
            EnhancedPromptInput(
                positivePrompt = positivePrompt,
                onPositivePromptChange = { positivePrompt = it },
                negativePrompt = negativePrompt,
                onNegativePromptChange = { negativePrompt = it }
            )
            
            // 分辨率选择
            if (selectedTab == 0) {
                ResolutionSelector(
                    selectedResolution = selectedResolution,
                    onResolutionSelect = {
                        selectedResolution = it
                        width = it.width
                        height = it.height
                    }
                )
            }
            
            // 参数调整
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 步数
                    EnhancedSlider(
                        value = steps.toFloat(),
                        onValueChange = { steps = it.toInt() },
                        valueRange = 5f..100f,
                        label = "采样步数",
                        valueDisplay = "$steps 步",
                        icon = Icons.Default.Timeline
                    )
                    
                    // 引导强度
                    EnhancedSlider(
                        value = guidanceScale,
                        onValueChange = { guidanceScale = it },
                        valueRange = 1f..20f,
                        label = "引导强度",
                        valueDisplay = String.format("%.1f", guidanceScale),
                        icon = Icons.Default.Speed
                    )
                    
                    // 种子
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SeedInput(
                            seed = seed,
                            onSeedChange = { seed = it },
                            onRandomSeed = { seed = -1L },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // 调度器
                    SchedulerSelector(
                        selectedScheduler = selectedScheduler,
                        onSchedulerSelect = { selectedScheduler = it }
                    )
                }
            }
            
            // 批量生成
            BatchControls(
                batchSize = batchSize,
                onBatchSizeChange = { batchSize = it }
            )
            
            // 高级选项
            AdvancedOptionsPanel(
                expanded = showAdvancedOptions,
                onExpandedChange = { showAdvancedOptions = it }
            ) {
                // Hires.fix
                HiresFixPanel(
                    enabled = enableHiresFix,
                    onEnabledChange = { enableHiresFix = it },
                    scale = hiresScale,
                    onScaleChange = { hiresScale = it },
                    steps = hiresSteps,
                    onStepsChange = { hiresSteps = it },
                    upscaler = hiresUpscaler,
                    onUpscalerChange = { hiresUpscaler = it }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // ControlNet
                ControlNetSection(
                    enabled = enableControlNet,
                    onEnabledChange = { enableControlNet = it },
                    controlType = controlNetType,
                    onControlTypeChange = { controlNetType = it },
                    weight = controlNetWeight,
                    onWeightChange = { controlNetWeight = it }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // ONNX 加速
                ONNXSection(
                    enabled = enableONNX,
                    onEnabledChange = { enableONNX = it },
                    provider = onnxProvider,
                    onProviderChange = { onnxProvider = it },
                    fp16Enabled = enableFP16,
                    onFP16Change = { enableFP16 = it }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // LoRA 管理
                LoRASection(
                    selectedLoras = selectedLoras,
                    onLorasChange = { selectedLoras = it }
                )
            }
            
            // 进度显示
            if (isGenerating || progress != null) {
                GenerationProgressDisplay(progress = progress)
            }
            
            // 生成结果预览
            if (generatedImages.isNotEmpty()) {
                GeneratedImagesPreview(
                    images = generatedImages,
                    onViewAll = onNavigateToGallery
                )
            }
            
            // 生成按钮
            Button(
                onClick = {
                    if (!isGenerating && positivePrompt.isNotBlank()) {
                        isGenerating = true
                        generatedImages = emptyList()
                        
                        val params = GenerationParams(
                            positivePrompt = positivePrompt,
                            negativePrompt = negativePrompt,
                            width = width,
                            height = height,
                            steps = steps,
                            guidanceScale = guidanceScale,
                            seed = seed,
                            scheduler = selectedScheduler,
                            batchSize = batchSize,
                            baseModel = selectedBaseModel,
                            enableHiresFix = enableHiresFix,
                            hiresScale = hiresScale,
                            hiresSteps = hiresSteps,
                            hiresUpscaler = hiresUpscaler,
                            enableControlNet = enableControlNet,
                            controlNetType = controlNetType,
                            controlNetWeight = controlNetWeight,
                            selectedLoras = selectedLoras,
                            vaeModel = selectedVae,
                            selectedEmbeddings = selectedEmbeddings,
                            enableONNX = enableONNX,
                            onnxProvider = onnxProvider,
                            enableFP16 = enableFP16,
                            mode = when (selectedTab) {
                                1 -> GenerationMode.IMG2IMG
                                2 -> GenerationMode.INPAINT
                                else -> GenerationMode.TXT2IMG
                            }
                        )
                        
                        scope.launch {
                            repository.generateImage(params).collectLatest {
                                progress = it
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isGenerating && positivePrompt.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("生成中...")
                } else {
                    Icon(Icons.Default.AutoAwesome, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (batchSize > 1) "批量生成 ×$batchSize" else "开始生成",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * v2.4.0 ControlNet 控制面板
 */
@Composable
private fun ControlNetSection(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    controlType: ControlNetType,
    onControlTypeChange: (ControlNetType) -> Unit,
    weight: Float,
    onWeightChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ControlPointDuplicate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "ControlNet 控制",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }
            
            if (enabled) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "控制类型",
                    style = MaterialTheme.typography.labelMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        listOf(
                            ControlNetType.CANNY to "🔲",
                            ControlNetType.DEPTH to "🗺️",
                            ControlNetType.POSE to "🧍",
                            ControlNetType.SCRIBBLE to "✏️",
                            ControlNetType.NORMAL to "🧊",
                            ControlNetType.LINEART to "📐"
                        )
                    ) { (type, icon) ->
                        FilterChip(
                            selected = controlType == type,
                            onClick = { onControlTypeChange(type) },
                            label = { Text(type.displayName) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                EnhancedSlider(
                    value = weight,
                    onValueChange = onWeightChange,
                    valueRange = 0f..2f,
                    label = "ControlNet 权重",
                    valueDisplay = String.format("%.2f", weight),
                    icon = Icons.Default.Tune
                )
            }
        }
    }
}

/**
 * v2.4.0 ONNX 加速面板
 */
@Composable
private fun ONNXSection(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    provider: ONNXProvider,
    onProviderChange: (ONNXProvider) -> Unit,
    fp16Enabled: Boolean,
    onFP16Change: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "ONNX 加速",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }
            
            if (enabled) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Provider 选择
                var expanded by remember { mutableStateOf(false) }
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = provider.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("加速后端") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        ONNXProvider.entries.forEach { p ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(p.displayName)
                                        Text(
                                            p.description,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                },
                                onClick = {
                                    onProviderChange(p)
                                    expanded = false
                                },
                                leadingIcon = {
                                    if (p.isHardwareAccelerated) {
                                        Icon(
                                            Icons.Default.Bolt,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // FP16 开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "FP16 半精度",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "减少显存占用，加速生成",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = fp16Enabled,
                        onCheckedChange = onFP16Change
                    )
                }
            }
        }
    }
}

/**
 * v2.4.0 LoRA 管理面板
 */
@Composable
private fun LoRASection(
    selectedLoras: List<LoraParam>,
    onLorasChange: (List<LoraParam>) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Layers,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "LoRA 模型",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            if (selectedLoras.isEmpty()) "未选择" else "已选择 ${selectedLoras.size} 个",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, "添加 LoRA")
                }
            }
            
            if (selectedLoras.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                selectedLoras.forEach { lora ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    lora.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "权重: ${String.format("%.2f", lora.weight)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = {
                                onLorasChange(selectedLoras - lora)
                            }) {
                                Icon(Icons.Default.Close, "移除")
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showAddDialog) {
        LoRAAddDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { lora ->
                onLorasChange(selectedLoras + lora)
                showAddDialog = false
            }
        )
    }
}

/**
 * v2.4.0 添加 LoRA 对话框
 */
@Composable
private fun LoRAAddDialog(
    onDismiss: () -> Unit,
    onAdd: (LoraParam) -> Unit
) {
    var loraName by remember { mutableStateOf("") }
    var loraWeight by remember { mutableStateOf(1.0f) }
    var selectedCategory by remember { mutableStateOf(LoraCategory.STYLE) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加 LoRA 模型") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = loraName,
                    onValueChange = { loraName = it },
                    label = { Text("LoRA 名称") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                EnhancedSlider(
                    value = loraWeight,
                    onValueChange = { loraWeight = it },
                    valueRange = 0.1f..2f,
                    label = "权重",
                    valueDisplay = String.format("%.2f", loraWeight),
                    icon = Icons.Default.Speed
                )
                
                Text(
                    "分类",
                    style = MaterialTheme.typography.labelMedium
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(LoraCategory.entries.toList()) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text("${category.icon} ${category.displayName}") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (loraName.isNotBlank()) {
                        onAdd(LoraParam.default(loraName, loraWeight))
                    }
                },
                enabled = loraName.isNotBlank()
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * v2.4.0 生成结果预览
 */
@Composable
private fun GeneratedImagesPreview(
    images: List<String>,
    onViewAll: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "生成结果 (${images.size}张)",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                TextButton(onClick = onViewAll) {
                    Text("查看全部")
                    Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(images) { imagePath ->
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imagePath)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(12.dp)
                            ),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

/**
 * 历史画廊界面
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryGalleryScreen(
    historyItems: List<HistoryItem>,
    onItemClick: (HistoryItem) -> Unit,
    onItemDelete: (HistoryItem) -> Unit,
    onItemFavorite: (HistoryItem) -> Unit,
    onClearAll: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (HistoryItem) -> Unit
) {
    var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }
    var selectedTab by remember { mutableIntStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📸 历史记录") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { /* 搜索 */ }) {
                        Icon(Icons.Default.Search, "搜索")
                    }
                    if (historyItems.isNotEmpty()) {
                        IconButton(onClick = onClearAll) {
                            Icon(Icons.Default.DeleteSweep, "清空")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 筛选标签
            ScrollableTabRow(selectedTabIndex = selectedTab) {
                HistoryFilter.entries.forEachIndexed { index, filter ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            selectedFilter = filter
                        },
                        text = {
                            Text(
                                when (filter) {
                                    HistoryFilter.ALL -> "全部 (${historyItems.size})"
                                    HistoryFilter.TODAY -> "今天"
                                    HistoryFilter.WEEK -> "本周"
                                    HistoryFilter.FAVORITES -> "⭐ 收藏"
                                }
                            )
                        }
                    )
                }
            }
            
            // 历史列表
            val filteredItems = historyItems.filter { item ->
                when (selectedFilter) {
                    HistoryFilter.ALL -> true
                    HistoryFilter.TODAY -> {
                        val today = System.currentTimeMillis() - 86400000
                        item.timestamp > today
                    }
                    HistoryFilter.WEEK -> {
                        val week = System.currentTimeMillis() - 604800000
                        item.timestamp > week
                    }
                    HistoryFilter.FAVORITES -> item.status == HistoryStatus.COMPLETED
                }
            }
            
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "暂无历史记录",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "开始生成你的第一张 AI 图像吧!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredItems) { item ->
                        HistoryItemCard(
                            item = item,
                            onClick = { onItemClick(item) },
                            onDelete = { onItemDelete(item) },
                            onFavorite = { onItemFavorite(item) }
                        )
                    }
                }
            }
        }
    }
}

private enum class HistoryFilter {
    ALL, TODAY, WEEK, FAVORITES
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryItemCard(
    item: HistoryItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { /* 显示菜单 */ }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // 图片预览
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (item.outputPaths.isNotEmpty()) {
                    AsyncImage(
                        model = item.thumbnailPath ?: item.outputPaths.first(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                
                // 状态标记
                when (item.status) {
                    HistoryStatus.COMPLETED -> {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                "✓",
                                modifier = Modifier.padding(4.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    HistoryStatus.FAILED -> {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                "✗",
                                modifier = Modifier.padding(4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    else -> {}
                }
            }
            
            // 信息
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    item.params.positivePrompt.take(50) + if (item.params.positivePrompt.length > 50) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formatTimestamp(item.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${item.params.width}×${item.params.height}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60000 -> "刚刚"
        diff < 3600000 -> "${diff / 60000}分钟前"
        diff < 86400000 -> "${diff / 3600000}小时前"
        else -> "${diff / 86400000}天前"
    }
}

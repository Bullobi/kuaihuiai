package comkuaihuiai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import comkuaihuiai.data.model.*
import comkuaihuiai.data.model.BaseModelType

/**
 * 快绘AI v2.4.0 UI 组件库
 * 高质量、可复用的高级组件
 */

/**
 * v2.4.0 增强版提示词输入组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedPromptInput(
    positivePrompt: String,
    onPositivePromptChange: (String) -> Unit,
    negativePrompt: String,
    onNegativePromptChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    maxLines: Int = 5
) {
    var showNegativePrompt by remember { mutableStateOf(negativePrompt.isNotEmpty()) }
    
    Column(modifier = modifier) {
        // 正向提示词
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "正向提示词",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        "${positivePrompt.length}/500",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                OutlinedTextField(
                    value = positivePrompt,
                    onValueChange = { if (it.length <= 500) onPositivePromptChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = (maxLines * 24).dp),
                    placeholder = { Text("描述你想要生成的图像... (英文效果更好)") },
                    enabled = enabled,
                    maxLines = maxLines,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
                
                // 快捷标签
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PromptTag("masterpiece", Icons.Default.Star)
                    PromptTag("best quality", Icons.Default.ThumbUp)
                    PromptTag("highly detailed", Icons.Default.HighQuality)
                    PromptTag("8k", Icons.Default.Hd)
                    PromptTag("photorealistic", Icons.Default.CameraAlt)
                    PromptTag("anime", Icons.Default.Face)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 负向提示词
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showNegativePrompt = !showNegativePrompt },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = showNegativePrompt,
                onCheckedChange = { showNegativePrompt = it },
                enabled = enabled
            )
            Text(
                "添加负向提示词",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        if (showNegativePrompt) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "负向提示词",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    
                    OutlinedTextField(
                        value = negativePrompt,
                        onValueChange = onNegativePromptChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp, max = 120.dp),
                        placeholder = { Text("排除不需要的元素...") },
                        enabled = enabled,
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.error,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                    
                    // 常用负向标签
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PromptTag("low quality", Icons.Default.Warning, isNegative = true)
                        PromptTag("blurry", Icons.Default.BlurOn, isNegative = true)
                        PromptTag("watermark", Icons.Default.WaterDrop, isNegative = true)
                        PromptTag("deformed", Icons.Default.BrokenImage, isNegative = true)
                        PromptTag("extra limbs", Icons.Default.MoreHoriz, isNegative = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun PromptTag(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isNegative: Boolean = false
) {
    val backgroundColor = if (isNegative) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    }
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        modifier = Modifier.clickable { /* 添加到提示词 */ }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (isNegative) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text,
                style = MaterialTheme.typography.labelSmall,
                color = if (isNegative) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * v2.4.0 模型选择器组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelector(
    selectedModel: BaseModelType,
    onModelSelect: (BaseModelType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedModel.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("基础模型") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            leadingIcon = {
                Icon(
                    when {
                        selectedModel.supportsSDXL -> Icons.Default.HighQuality
                        selectedModel.supportsFlux -> Icons.Default.Bolt
                        else -> Icons.Default.Image
                    },
                    contentDescription = null
                )
            }
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // SD 1.5 系列
            DropdownMenuItem(
                text = {
                    Column {
                        Text("SD 1.5", fontWeight = FontWeight.Bold)
                        Text("通用模型，兼容性好", style = MaterialTheme.typography.bodySmall)
                    }
                },
                onClick = {
                    onModelSelect(BaseModelType.SD_1_5)
                    expanded = false
                },
                leadingIcon = { Icon(Icons.Default.Image, null) }
            )
            
            HorizontalDivider()
            
            // SDXL 系列
            Text(
                "SDXL 系列 (支持 4K)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            BaseModelType.entries
                .filter { it.supportsSDXL }
                .forEach { model ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(model.displayName, fontWeight = FontWeight.Bold)
                                Text(model.memoryRequirement, style = MaterialTheme.typography.bodySmall)
                            }
                        },
                        onClick = {
                            onModelSelect(model)
                            expanded = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.HighQuality, null)
                        }
                    )
                }
            
            HorizontalDivider()
            
            // Flux 系列
            Text(
                "Flux 系列",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            BaseModelType.entries
                .filter { it.supportsFlux }
                .forEach { model ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(model.displayName, fontWeight = FontWeight.Bold)
                                Text(model.memoryRequirement, style = MaterialTheme.typography.bodySmall)
                            }
                        },
                        onClick = {
                            onModelSelect(model)
                            expanded = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Bolt, null)
                        }
                    )
                }
        }
    }
}

/**
 * v2.4.0 分辨率选择器
 */
@Composable
fun ResolutionSelector(
    selectedResolution: Resolution?,
    onResolutionSelect: (Resolution) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            "分辨率",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // SD 1.5 分辨率
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ResolutionChip(
                resolution = Resolution.SQUARE_512,
                isSelected = selectedResolution == Resolution.SQUARE_512,
                onClick = { onResolutionSelect(Resolution.SQUARE_512) }
            )
            ResolutionChip(
                resolution = Resolution.PORTRAIT_512_768,
                isSelected = selectedResolution == Resolution.PORTRAIT_512_768,
                onClick = { onResolutionSelect(Resolution.PORTRAIT_512_768) }
            )
            ResolutionChip(
                resolution = Resolution.LANDSCAPE_768_512,
                isSelected = selectedResolution == Resolution.LANDSCAPE_768_512,
                onClick = { onResolutionSelect(Resolution.LANDSCAPE_768_512) }
            )
            ResolutionChip(
                resolution = Resolution.SQUARE_768,
                isSelected = selectedResolution == Resolution.SQUARE_768,
                onClick = { onResolutionSelect(Resolution.SQUARE_768) }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // SDXL/4K 分辨率
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ResolutionChip(
                resolution = Resolution.SDXL_PORTRAIT,
                isSelected = selectedResolution == Resolution.SDXL_PORTRAIT,
                onClick = { onResolutionSelect(Resolution.SDXL_PORTRAIT) },
                isHighQuality = true
            )
            ResolutionChip(
                resolution = Resolution.SDXL_LANDSCAPE,
                isSelected = selectedResolution == Resolution.SDXL_LANDSCAPE,
                onClick = { onResolutionSelect(Resolution.SDXL_LANDSCAPE) },
                isHighQuality = true
            )
            ResolutionChip(
                resolution = Resolution.SDXL_SQUARE,
                isSelected = selectedResolution == Resolution.SDXL_SQUARE,
                onClick = { onResolutionSelect(Resolution.SDXL_SQUARE) },
                isHighQuality = true
            )
        }
    }
}

@Composable
private fun ResolutionChip(
    resolution: Resolution,
    isSelected: Boolean,
    onClick: () -> Unit,
    isHighQuality: Boolean = false
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isHighQuality -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    resolution.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                if (resolution.is4K) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.Hd,
                        contentDescription = "4K",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            if (isHighQuality) {
                Text(
                    "SDXL",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

/**
 * v2.4.0 增强版参数滑块
 */
@Composable
fun EnhancedSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    label: String,
    valueDisplay: String,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    enabled: Boolean = true,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    valueDisplay,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * v2.4.0 批量生成控制
 */
@Composable
fun BatchControls(
    batchSize: Int,
    onBatchSizeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
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
                    Icons.Default.Collections,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        "批量生成",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "一次生成多张图片",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (batchSize > 1) onBatchSizeChange(batchSize - 1) },
                    enabled = batchSize > 1
                ) {
                    Icon(Icons.Default.Remove, "减少")
                }
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondary
                ) {
                    Text(
                        "×$batchSize",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                }
                
                IconButton(
                    onClick = { if (batchSize < 4) onBatchSizeChange(batchSize + 1) },
                    enabled = batchSize < 4
                ) {
                    Icon(Icons.Default.Add, "增加")
                }
            }
        }
    }
}

/**
 * v2.4.0 高级选项面板
 */
@Composable
fun AdvancedOptionsPanel(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!expanded) }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "高级选项",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开"
                )
            }
            
            if (expanded) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    content = content
                )
            }
        }
    }
}

/**
 * v2.4.0 调度器选择器
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulerSelector(
    selectedScheduler: SchedulerType,
    onSchedulerSelect: (SchedulerType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedScheduler.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("采样调度器") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            supportingText = {
                Row {
                    Text("速度: ${"★".repeat((selectedScheduler.speed).toInt())}")
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("质量: ${"★".repeat((selectedScheduler.quality).toInt())}")
                }
            }
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // 快速调度器
            Text(
                "⚡ 快速调度器",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            listOf(
                SchedulerType.LCM to "LCM - 最快速度",
                SchedulerType.LCM_FAST to "LCM Fast - 极速",
                SchedulerType.TURBO to "Turbo - 快速稳定"
            ).forEach { (scheduler, desc) ->
                DropdownMenuItem(
                    text = { Text(desc) },
                    onClick = {
                        onSchedulerSelect(scheduler)
                        expanded = false
                    }
                )
            }
            
            HorizontalDivider()
            
            // 平衡调度器
            Text(
                "⚖️ 平衡调度器",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            listOf(
                SchedulerType.DPMSOLVER_PLUS_PLUS_2M_KARRAS to "DPM-Solver++ 2M Karras - 推荐",
                SchedulerType.DPMSOLVER_PLUS_PLUS_2M to "DPM-Solver++ 2M - 快速",
                SchedulerType.EULER_ANCESTRAL to "Euler A - 经典"
            ).forEach { (scheduler, desc) ->
                DropdownMenuItem(
                    text = { Text(desc) },
                    onClick = {
                        onSchedulerSelect(scheduler)
                        expanded = false
                    }
                )
            }
            
            HorizontalDivider()
            
            // 高质量调度器
            Text(
                "✨ 高质量调度器",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            listOf(
                SchedulerType.TCD to "TCD - 最高质量",
                SchedulerType.DPMSOLVER_SDE_KARRAS to "DPM-Solver++ SDE Karras",
                SchedulerType.DDIM to "DDIM - 精细控制"
            ).forEach { (scheduler, desc) ->
                DropdownMenuItem(
                    text = { Text(desc) },
                    onClick = {
                        onSchedulerSelect(scheduler)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * v2.4.0 Hires.fix 控制面板
 */
@Composable
fun HiresFixPanel(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    scale: Float,
    onScaleChange: (Float) -> Unit,
    steps: Int,
    onStepsChange: (Int) -> Unit,
    upscaler: HiresUpscaler,
    onUpscalerChange: (HiresUpscaler) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
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
                        Icons.Default.HighQuality,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Hires.fix 超分辨率",
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
                
                // 放大倍数
                EnhancedSlider(
                    value = scale,
                    onValueChange = onScaleChange,
                    valueRange = 1.0f..2.5f,
                    label = "放大倍数",
                    valueDisplay = "${String.format("%.1f", scale)}x",
                    icon = Icons.Default.ZoomIn
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Hires 步数
                EnhancedSlider(
                    value = steps.toFloat(),
                    onValueChange = { onStepsChange(it.toInt()) },
                    valueRange = 5f..30f,
                    label = "Hires 步数",
                    valueDisplay = "$steps 步",
                    icon = Icons.Default.Timeline
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 超分算法选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("超分算法")
                    Text(
                        upscaler.displayName,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(HiresUpscaler.entries.toList()) { upscalerOption ->
                        FilterChip(
                            selected = upscaler == upscalerOption,
                            onClick = { onUpscalerChange(upscalerOption) },
                            label = { Text(upscalerOption.displayName) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * v2.4.0 进度显示组件
 */
@Composable
fun GenerationProgressDisplay(
    progress: GenerationProgress?,
    modifier: Modifier = Modifier
) {
    when (progress) {
        is GenerationProgress.Status -> {
            Card(
                modifier = modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        progress.message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        is GenerationProgress.Progress -> {
            Card(
                modifier = modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "生成进度",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "${(progress.percent * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        progress = { progress.percent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "步骤 ${progress.currentStep}/${progress.totalSteps}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (progress.etaMs > 0) {
                            Text(
                                "预计剩余: ${progress.etaMs / 1000}秒",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
        
        is GenerationProgress.Completed -> {
            Card(
                modifier = modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "生成完成!",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "已生成 ${progress.paths.size} 张图片",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        
        is GenerationProgress.Error -> {
            Card(
                modifier = modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        progress.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        
        else -> {
            // 其他状态
        }
    }
}

/**
 * v2.4.0 种子输入组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeedInput(
    seed: Long,
    onSeedChange: (Long) -> Unit,
    onRandomSeed: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textValue by remember(seed) { mutableStateOf(if (seed < 0) "" else seed.toString()) }
    
    OutlinedTextField(
        value = textValue,
        onValueChange = { value ->
            textValue = value
            if (value.isEmpty()) {
                onSeedChange(-1)
            } else {
                value.toLongOrNull()?.let { onSeedChange(it) }
            }
        },
        modifier = modifier,
        label = { Text("种子") },
        placeholder = { Text("随机") },
        leadingIcon = {
            Icon(Icons.Default.FilterVintage, "种子")
        },
        trailingIcon = {
            IconButton(onClick = {
                onRandomSeed()
                textValue = ""
            }) {
                Icon(Icons.Default.Casino, "随机种子")
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

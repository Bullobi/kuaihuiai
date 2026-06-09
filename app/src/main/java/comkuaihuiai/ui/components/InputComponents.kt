package comkuaihuiai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import comkuaihuiai.data.model.*

/**
 * Prompt Input Component with positive and negative prompts
 */
@Composable
fun PromptInput(
    positivePrompt: String,
    negativePrompt: String,
    onPositiveChange: (String) -> Unit,
    onNegativeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showNegativePrompt by remember { mutableStateOf(false) }
    
    Column(modifier = modifier.fillMaxWidth()) {
        // Positive Prompt
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFF22C55E),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "正向提示词",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                BasicTextField(
                    value = positivePrompt,
                    onValueChange = onPositiveChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    ),
                    decorationBox = { innerTextField ->
                        Box {
                            if (positivePrompt.isEmpty()) {
                                Text(
                                    text = "请输入你想要的画面内容...",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Toggle Negative Prompt
        TextButton(
            onClick = { showNegativePrompt = !showNegativePrompt },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = if (showNegativePrompt) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (showNegativePrompt) "隐藏负向提示词" else "添加负向提示词",
                fontSize = 13.sp
            )
        }
        
        // Negative Prompt
        if (showNegativePrompt) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "负向提示词",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    BasicTextField(
                        value = negativePrompt,
                        onValueChange = onNegativeChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp),
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        ),
                        decorationBox = { innerTextField ->
                            Box {
                                if (negativePrompt.isEmpty()) {
                                    Text(
                                        text = "你不想要的内容...",
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        fontSize = 14.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Resolution Selector
 */
@Composable
fun ResolutionSelector(
    selectedResolution: Resolution,
    onResolutionSelected: (Resolution) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AspectRatio,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "分辨率",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // First row - square resolutions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Resolution.values().take(4).forEach { resolution ->
                ResolutionChip(
                    resolution = resolution,
                    isSelected = resolution == selectedResolution,
                    onClick = { onResolutionSelected(resolution) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Second row - portrait/landscape
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Resolution.values().drop(4).forEach { resolution ->
                ResolutionChip(
                    resolution = resolution,
                    isSelected = resolution == selectedResolution,
                    onClick = { onResolutionSelected(resolution) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ResolutionChip(
    resolution: Resolution,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.border(
                    2.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(8.dp)
                ) else Modifier
            ),
        color = if (isSelected) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = resolution.displayName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

/**
 * Aspect Ratio Selector
 */
@Composable
fun AspectRatioSelector(
    selectedRatio: AspectRatio,
    onRatioSelected: (AspectRatio) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Crop,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "宽高比",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AspectRatio.entries.forEach { ratio ->
                AspectRatioButton(
                    ratio = ratio,
                    isSelected = ratio == selectedRatio,
                    onClick = { onRatioSelected(ratio) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AspectRatioButton(
    ratio: AspectRatio,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.border(
                    2.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(8.dp)
                ) else Modifier
            ),
        color = if (isSelected) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = ratio.displayName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

/**
 * Advanced Settings Panel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettings(
    steps: Int,
    guidanceScale: Float,
    seed: Int,
    scheduler: SchedulerType,
    onStepsChange: (Int) -> Unit,
    onGuidanceChange: (Float) -> Unit,
    onSeedChange: (Int) -> Unit,
    onSchedulerChange: (SchedulerType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        TextButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.Settings else Icons.Default.Tune,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (expanded) "隐藏高级设置" else "高级设置",
                fontSize = 13.sp
            )
        }
        
        if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Steps Slider
            SettingSlider(
                label = "采样步数",
                value = steps.toFloat(),
                valueRange = 1f..50f,
                onValueChange = { onStepsChange(it.toInt()) },
                valueDisplay = "$steps"
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Guidance Scale Slider
            SettingSlider(
                label = "引导强度",
                value = guidanceScale,
                valueRange = 1f..20f,
                onValueChange = { onGuidanceChange(it) },
                valueDisplay = String.format("%.1f", guidanceScale)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Seed Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "随机种子",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(0.3f)
                )
                
                OutlinedTextField(
                    value = if (seed == -1) "" else seed.toString(),
                    onValueChange = { value ->
                        onSeedChange(if (value.isEmpty()) -1 else value.toIntOrNull() ?: -1)
                    },
                    modifier = Modifier.weight(0.5f),
                    placeholder = { Text("随机", fontSize = 12.sp) },
                    singleLine = true
                )
                
                IconButton(
                    onClick = { onSeedChange(-1) },
                    modifier = Modifier.weight(0.2f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = "随机",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Scheduler Dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "调度器",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(0.3f)
                )
                
                var schedulerExpanded by remember { mutableStateOf(false) }
                
                ExposedDropdownMenuBox(
                    expanded = schedulerExpanded,
                    onExpandedChange = { schedulerExpanded = it },
                    modifier = Modifier.weight(0.7f)
                ) {
                    OutlinedTextField(
                        value = scheduler.displayName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = schedulerExpanded)
                        },
                        modifier = Modifier.menuAnchor(),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    
                    ExposedDropdownMenu(
                        expanded = schedulerExpanded,
                        onDismissRequest = { schedulerExpanded = false }
                    ) {
                        SchedulerType.values().forEach { sched ->
                            DropdownMenuItem(
                                text = { Text(sched.displayName, fontSize = 13.sp) },
                                onClick = {
                                    onSchedulerChange(sched)
                                    schedulerExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueDisplay: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = valueDisplay,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}

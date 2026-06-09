package comkuaihuiai.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import comkuaihuiai.data.model.*
import comkuaihuiai.data.repository.GenerationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Settings Screen v2.3.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: GenerationRepository,
    onNavigateBack: () -> Unit = {}
) {
    var storageSize by remember { mutableStateOf(0L) }
    var cacheSize by remember { mutableStateOf(0L) }
    var selectedEngine by remember { mutableStateOf("CPU") }
    var enableFP16 by remember { mutableStateOf(true) }
    var enableONNX by remember { mutableStateOf(false) }
    var memoryMode by remember { mutableStateOf(MemoryMode.BALANCED) }
    
    LaunchedEffect(Unit) {
        storageSize = repository.getStorageSize()
        cacheSize = repository.getCacheSize()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚙️ 设置") },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 版本信息卡片
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("快绘AI v2.3.0", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("本地 Stable Diffusion 推理引擎", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SuggestionChip(
                                onClick = { },
                                label = { Text("⚡ SDXL 4K") }
                            )
                            SuggestionChip(
                                onClick = { },
                                label = { Text("🎯 ControlNet") }
                            )
                            SuggestionChip(
                                onClick = { },
                                label = { Text("🚀 ONNX") }
                            )
                        }
                    }
                }
            }
            
            // 推理引擎设置
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🔧 推理引擎", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // 引擎选择
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("推理引擎", style = MaterialTheme.typography.bodyMedium)
                                Text(selectedEngine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Row {
                                FilterChip(
                                    selected = selectedEngine == "CPU",
                                    onClick = { selectedEngine = "CPU" },
                                    label = { Text("CPU") }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                FilterChip(
                                    selected = selectedEngine == "GPU",
                                    onClick = { selectedEngine = "GPU" },
                                    label = { Text("GPU") }
                                )
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        
                        // FP16
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("🪶 FP16 半精度", style = MaterialTheme.typography.bodyMedium)
                                Text("减少显存占用，加速推理", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = enableFP16, onCheckedChange = { enableFP16 = it })
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        
                        // ONNX
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("🚀 ONNX 加速", style = MaterialTheme.typography.bodyMedium)
                                Text("使用 ONNX Runtime 加速推理", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = enableONNX, onCheckedChange = { enableONNX = it })
                        }
                    }
                }
            }
            
            // 内存模式
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("💾 内存模式", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        MemoryMode.entries.forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { memoryMode = mode }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(mode.displayName, style = MaterialTheme.typography.bodyMedium)
                                    Text("${mode.memoryReduction.toInt()}% 显存节省", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                RadioButton(
                                    selected = memoryMode == mode,
                                    onClick = { memoryMode = mode }
                                )
                            }
                        }
                    }
                }
            }
            
            // 存储管理
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("💿 存储管理", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        ListItem(
                            headlineContent = { Text("生成图片") },
                            supportingContent = { Text(formatSize(storageSize)) },
                            leadingContent = { Icon(Icons.Default.Image, null) }
                        )
                        
                        ListItem(
                            headlineContent = { Text("缓存") },
                            supportingContent = { Text(formatSize(cacheSize)) },
                            leadingContent = { Icon(Icons.Default.Cached, null) },
                            trailingContent = {
                                TextButton(onClick = {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        repository.clearCache()
                                        cacheSize = 0L
                                    }
                                }) {
                                    Text("清理")
                                }
                            }
                        )
                    }
                }
            }
            
            // 性能测试
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📊 性能测试", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            "测试推理引擎在不同配置下的性能表现",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = { /* 运行测试 */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Speed, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("运行基准测试")
                        }
                    }
                }
            }
            
            // 关于
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("ℹ️ 关于", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        ListItem(
                            headlineContent = { Text("快绘AI") },
                            supportingContent = { Text("本地 Stable Diffusion 推理引擎 v2.3.0") },
                            leadingContent = { Icon(Icons.Default.Info, null) }
                        )
                        
                        ListItem(
                            headlineContent = { Text("功能特性") },
                            supportingContent = { Text("SDXL 4K | ControlNet | LoRA | VAE | ONNX加速") },
                            leadingContent = { Icon(Icons.Default.Star, null) }
                        )
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
    }
}

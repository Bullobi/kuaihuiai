package comkuaihuiai.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import comkuaihuiai.data.model.*
import comkuaihuiai.data.repository.GenerationRepository
import comkuaihuiai.ui.screens.*
import comkuaihuiai.ui.theme.KuaiHuiAITheme
import kotlinx.coroutines.launch
import java.io.File

/**
 * 快绘AI v2.3.0 主界面
 * 集成五大方向增强功能
 */
class MainActivity : ComponentActivity() {
    
    private lateinit var repository: GenerationRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        repository = GenerationRepository(this)
        
        setContent {
            KuaiHuiAITheme {
                MainScreen(repository)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        repository.release()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(repository: GenerationRepository) {
    val navController = rememberNavController()
    val historyItems by repository.historyItems.collectAsState()
    
    val items = listOf(
        BottomNavItem("生成", Icons.Default.AutoAwesome, "generation"),
        BottomNavItem("图库", Icons.Default.PhotoLibrary, "gallery"),
        BottomNavItem("历史", Icons.Default.History, "history"),
        BottomNavItem("模型", Icons.Default.ModelTraining, "models"),
        BottomNavItem("设置", Icons.Default.Settings, "settings")
    )
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "generation",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("generation") {
                GenerationScreenV23(
                    repository = repository,
                    onNavigateToGallery = { navController.navigate("gallery") },
                    onNavigateToHistory = { navController.navigate("history") },
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }
            
            composable("gallery") {
                GalleryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { /* 打开详情 */ }
                )
            }
            
            composable("history") {
                HistoryGalleryScreen(
                    historyItems = historyItems,
                    onItemClick = { item ->
                        // 导航到详情
                    },
                    onItemDelete = { item ->
                        // 删除记录
                    },
                    onItemFavorite = { item ->
                        // 收藏
                    },
                    onClearAll = {
                        // 清空全部
                    },
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { item ->
                        // 导航到详情
                    }
                )
            }
            
            composable("models") {
                ModelManagementScreen(
                    repository = repository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable("settings") {
                SettingsScreen(
                    repository = repository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

/**
 * 图库界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    var images by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedTab by remember { mutableIntStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📸 我的图库") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { /* 选择 */ }) {
                        Icon(Icons.Default.CheckCircle, "选择")
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
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("全部") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("收藏") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("批量") }
                )
            }
            
            if (images.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "暂无图片",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 模型管理界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManagementScreen(
    repository: GenerationRepository,
    onNavigateBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Checkpoint", "LoRA", "VAE", "Embedding", "ControlNet")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📦 模型管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { /* 下载模型 */ }) {
                        Icon(Icons.Default.Download, "下载")
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
            ScrollableTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            // 模型列表
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    when (selectedTab) {
                        0 -> "Checkpoint 模型"
                        1 -> "LoRA 模型"
                        2 -> "VAE 模型"
                        3 -> "Embedding 嵌入"
                        else -> "ControlNet 控制网"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

/**
 * 设置界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: GenerationRepository,
    onNavigateBack: () -> Unit
) {
    var storageSize by remember { mutableStateOf(0L) }
    var cacheSize by remember { mutableStateOf(0L) }
    
    LaunchedEffect(Unit) {
        storageSize = repository.getStorageSize()
        cacheSize = repository.getCacheSize()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚙️ 设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📱 快绘AI v2.3.0", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("五大方向全面增强", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row {
                            Text("SDXL 4K", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("|", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ControlNet", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("|", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ONNX", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            
            item {
                Card {
                    ListItem(
                        headlineContent = { Text("存储使用") },
                        supportingContent = { Text("生成图片: ${formatSize(storageSize)}") },
                        leadingContent = { Icon(Icons.Default.Storage, null) }
                    )
                }
            }
            
            item {
                Card {
                    ListItem(
                        headlineContent = { Text("缓存大小") },
                        supportingContent = { Text(formatSize(cacheSize)) },
                        leadingContent = { Icon(Icons.Default.Cached, null) },
                        trailingContent = {
                            val scope = rememberCoroutineScope()
                            TextButton(onClick = {
                                scope.launch {
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
            
            item {
                Card {
                    ListItem(
                        headlineContent = { Text("关于") },
                        supportingContent = { Text("快绘AI - 本地 Stable Diffusion 推理引擎") },
                        leadingContent = { Icon(Icons.Default.Info, null) }
                    )
                }
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

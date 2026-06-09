package comkuaihuiai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import comkuaihuiai.data.repository.GenerationRepository
import comkuaihuiai.data.repository.ModelRepository
import comkuaihuiai.ui.theme.KuaiHuiAITheme

/**
 * Navigation routes
 */
sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Generate : Screen(
        route = "generate",
        title = "生成",
        selectedIcon = Icons.Filled.Brush,
        unselectedIcon = Icons.Outlined.Brush
    )
    
    object MyModels : Screen(
        route = "my_models",
        title = "我的模型",
        selectedIcon = Icons.Filled.Folder,
        unselectedIcon = Icons.Outlined.Folder
    )
    
    object ModelMarket : Screen(
        route = "model_market",
        title = "模型市场",
        selectedIcon = Icons.Filled.Store,
        unselectedIcon = Icons.Outlined.Store
    )
    
    object Settings : Screen(
        route = "settings",
        title = "设置",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}

val bottomNavItems = listOf(
    Screen.Generate,
    Screen.MyModels,
    Screen.ModelMarket,
    Screen.Settings
)

/**
 * Main App Navigation with 4 tabs
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KuaiHUIAIApp(
    context: android.content.Context,
    generationRepository: GenerationRepository,
    modelRepository: ModelRepository
) {
    KuaiHuiAITheme {
        val navController = rememberNavController()
        
        Scaffold(
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { 
                            it.route == screen.route 
                        } == true
                        
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
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
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Generate.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Generate.route) {
                    GenerationScreen(repository = generationRepository)
                }
                
                composable(Screen.MyModels.route) {
                    ModelsScreen(repository = generationRepository)
                }
                
                composable(Screen.ModelMarket.route) {
                    ModelMarketScreen(
                        repository = modelRepository,
                        onDownloadClick = { model ->
                            // Start download service
                            val intent = android.content.Intent(context, comkuaihuiai.service.ModelDownloadService::class.java).apply {
                                action = comkuaihuiai.service.ModelDownloadService.ACTION_START
                                putExtra(comkuaihuiai.service.ModelDownloadService.EXTRA_MODEL_ID, model.id)
                                putExtra(comkuaihuiai.service.ModelDownloadService.EXTRA_MODEL_NAME, model.name)
                                putExtra(comkuaihuiai.service.ModelDownloadService.EXTRA_DOWNLOAD_URL, modelRepository.getDownloadUrl(model))
                                putExtra(comkuaihuiai.service.ModelDownloadService.EXTRA_IS_ZIP, true)
                            }
                            context.startForegroundService(intent)
                        }
                    )
                }
                
                composable(Screen.Settings.route) {
                    SettingsScreen(repository = generationRepository)
                }
            }
        }
    }
}

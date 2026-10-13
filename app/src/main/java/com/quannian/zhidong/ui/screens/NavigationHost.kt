package com.quannian.zhidong.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.quannian.zhidong.ui.theme.Palette
import com.quannian.zhidong.ui.theme.QuanNingTheme

private const val TAB_HOME = "tab_home"
private const val TAB_TRAINING = "tab_training"
private const val TAB_ASSISTANT = "tab_assistant"
private const val TAB_MINE = "tab_mine"
private val TABS = setOf(TAB_HOME, TAB_TRAINING, TAB_ASSISTANT, TAB_MINE)

/**
 * 全局导航（V3 · 统一 4 tab：首页 / 训练 / AI 助手 / 我的）。
 * 4 个 tab 常驻；子页自动隐藏底栏；tab 切换保留各自子页栈。
 *  本文件是**唯一**的底部导航实现，任何页面不得再写独立底部栏。
 */
@Composable
fun NavigationHost(modifier: Modifier = Modifier) {
    QuanNingTheme {
        val nav: NavHostController = rememberNavController()
        val backStackEntry by nav.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        val showBottomBar = currentRoute in TABS

        val switchTab: (String) -> Unit = { target ->
            nav.navigate(target) {
                popUpTo(TAB_HOME) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }

        val tabs = listOf(
            TabDef(TAB_HOME, "首页", Icons.Filled.Home),
            TabDef(TAB_TRAINING, "训练", Icons.Filled.FitnessCenter),
            TabDef(TAB_ASSISTANT, "AI 助手", Icons.Filled.SmartToy),
            TabDef(TAB_MINE, "我的", Icons.Filled.Person)
        )

        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        tabs.forEach { t ->
                            NavigationBarItem(
                                selected = currentRoute == t.route,
                                onClick = { switchTab(t.route) },
                                icon = { Icon(t.icon, t.label) },
                                label = { Text(t.label, fontSize = 12.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Palette.accent, selectedTextColor = Palette.accent,
                                    unselectedIconColor = Palette.inkSoft, unselectedTextColor = Palette.inkSoft,
                                    indicatorColor = Palette.accentSoft)
                            )
                        }
                    }
                }
            }
        ) {
            NavHost(navController = nav, startDestination = "splash", modifier = Modifier.fillMaxSize()) {
                composable("splash") {
                    SplashScreen(onDone = {
                        nav.navigate("onboarding") { popUpTo("splash") { inclusive = true } }
                    })
                }
                composable("onboarding") {
                    OnboardingScreen(onFinish = {
                        nav.navigate("profile") { popUpTo("onboarding") { inclusive = true } }
                    })
                }
                composable("profile") {
                    ProfileScreen(onBack = { nav.popBackStack() }, onDone = { switchTab(TAB_HOME) })
                }
                composable(TAB_HOME) {
                    HomeScreen(
                        onPickAge = { nav.navigate("age/$it") },
                        onMyTraining = { nav.navigate("training") },
                        onAiDiagnose = { nav.navigate("videoPick") },
                        onAssistant = { switchTab(TAB_ASSISTANT) },
                        onWearable = { nav.navigate("wearable") },
                        onProfile = { nav.navigate("profile") },
                        onYouthBodyTest = { nav.navigate("youthBodyTest") },
                        onContinueLast = { nav.navigate("training") }
                    )
                }
                composable(TAB_TRAINING) {
                    TrainingHubScreen(
                        onBodyTest = { nav.navigate("bodyTest") },
                        onVideoPick = { nav.navigate("videoPick") },
                        onWearable = { nav.navigate("wearable") },
                        onTraining = { nav.navigate("training") },
                        onYouthTrainingPlan = { nav.navigate("youthTrainingPlan") },
                        onYouthBodyTestReport = { nav.navigate("youthBodyTestReport") },
                        onTeachingVideo = { asset, title, sub ->
                            nav.navigate("teachingVideo/$asset/$title/$sub")
                        },
                        onProfile = { nav.navigate("profile") }
                    )
                }
                composable(TAB_ASSISTANT) {
                    AssistantScreen(onBack = { switchTab(TAB_HOME) })
                }
                composable(TAB_MINE) {
                    MineScreen(
                        onTraining = { nav.navigate("training") },
                        onWearable = { nav.navigate("wearable") },
                        onProfile = { nav.navigate("profile") },
                        onBodyTest = { nav.navigate("bodyTest") },
                        onYouthBodyTest = { nav.navigate("youthBodyTest") },
                        onYouthTrainingPlan = { nav.navigate("youthTrainingPlan") },
                        onSettings = { nav.navigate("settings") }
                    )
                }
                composable("age/{id}") { bse ->
                    val id = bse.arguments?.getString("id") ?: "youth"
                    AgeScreen(
                        id = id,
                        onBack = { nav.popBackStack() },
                        onPickExercise = { exId -> nav.navigate("exercise/$exId") },
                        onBodyTest = {
                            // 青年 → 七项 体测 模块；中年/银龄 → 旧 5 项 自评
                            if (id == "youth") nav.navigate("youthBodyTest") else nav.navigate("bodyTest")
                        },
                        onAgeHealth = { nav.navigate("ageHealth/$id") }
                    )
                }
                composable("bodyTest") {
                    BodyTestScreen(onBack = { nav.popBackStack() }, onGoTrain = { nav.popBackStack() })
                }

                composable("ageHealth/{id}") { bse ->
                    val id = bse.arguments?.getString("id") ?: "middle"
                    AgeHealthScreen(ageId = id, onBack = { nav.popBackStack() })
                }

                composable("youthBodyTest") {
                    YouthBodyTestScreen(
                        onBack = { nav.popBackStack() },
                        onPickItem = { key -> nav.navigate("youthBodyTestItem/$key") },
                        onWatchItem = { key ->
                            val v = com.quannian.zhidong.repository.TeachingVideoRepository.videoForItem(key)
                            if (v != null && v.assetPath.isNotBlank()) {
                                nav.navigate("teachingVideoItem/$key")
                            } else {
                                nav.navigate("youthBodyTestItem/$key")
                            }
                        },
                        onFollow = { key -> nav.navigate("followItem/$key") },
                        onReport = { nav.navigate("youthBodyTestReport") },
                        onPlan = { nav.navigate("youthTrainingPlan") }
                    )
                }
                composable("youthBodyTestItem/{key}") { bse ->
                    val key = bse.arguments?.getString("key") ?: ""
                    YouthBodyTestDetailScreen(
                        itemKey = key,
                        onBack = { nav.popBackStack() },
                        onTrain = { nav.navigate("youthTrainingPlan") },
                        onFollow = { nav.navigate("followItem/$key") }
                    )
                }
                composable("followItem/{key}") { bse ->
                    val key = bse.arguments?.getString("key") ?: ""
                    FollowAlongItemScreen(
                        itemKey = key,
                        onBack = { nav.popBackStack() }
                    )
                }
                composable("youthTrainingPlan") {
                    YouthTrainingPlanScreen(
                        onBack = { nav.popBackStack() },
                        onPlayVideo = { asset, title, sub -> nav.navigate("teachingVideo/$asset/$title/$sub") },
                        onFollow = { key -> nav.navigate("followItem/$key") }
                    )
                }
                composable("youthBodyTestReport") {
                    YouthBodyTestReportScreen(
                        onBack = { nav.popBackStack() },
                        onPickItem = { key -> nav.navigate("youthBodyTestItem/$key") },
                        onPlan = { nav.navigate("youthTrainingPlan") }
                    )
                }
                composable("teachingVideo/{asset}/{title}/{subtitle}") { bse ->
                    val asset = bse.arguments?.getString("asset") ?: ""
                    val title = bse.arguments?.getString("title") ?: ""
                    val sub = bse.arguments?.getString("subtitle") ?: ""
                    TeachingVideoScreen(
                        assetPath = asset,
                        title = title,
                        subtitle = sub,
                        onBack = { nav.popBackStack() }
                    )
                }
                composable("teachingVideoItem/{key}") { bse ->
                    val key = bse.arguments?.getString("key") ?: ""
                    val v = com.quannian.zhidong.repository.TeachingVideoRepository.videoForItem(key)
                    if (v != null && v.assetPath.isNotBlank()) {
                        TeachingVideoScreen(
                            assetPath = v.assetPath,
                            title = v.title,
                            subtitle = v.summary,
                            itemKey = key,
                            onBack = { nav.popBackStack() },
                            onFollowItem = { fk -> nav.navigate("followItem/$fk") }
                        )
                    } else {
                        // 无动作视频（method-only）→ 回详情/测评录入页
                        TeachingVideoScreen(
                            assetPath = "",
                            title = "无示范视频 · $key",
                            subtitle = "此项为独立测评录入项，暂无动作示范视频",
                            onBack = { nav.popBackStack() }
                        )
                    }
                }
                composable("exercise/{id}") { bse ->
                    val id = bse.arguments?.getString("id") ?: ""
                    ExerciseScreen(
                        exerciseId = id,
                        onBack = { nav.popBackStack() },
                        onStartTeach = { nav.navigate("coach/$id") },
                        onStartFollow = { nav.navigate("follow/$id") }
                    )
                }
                composable("coach/{id}") { bse ->
                    val id = bse.arguments?.getString("id") ?: ""
                    CoachScreen(
                        exerciseId = id,
                        onBack = { nav.popBackStack() },
                        onStartFollow = { nav.navigate("follow/$id") }
                    )
                }
                composable("follow/{id}") { bse ->
                    val id = bse.arguments?.getString("id") ?: ""
                    FollowAlongScreen(
                        exerciseId = id,
                        onBack = { nav.popBackStack() },
                        onFinish = { report ->
                            com.quannian.zhidong.repository.ReportChannel.put(report)
                            nav.navigate("report")
                        }
                    )
                }
                composable("report") {
                    ReportScreen(
                        onAgain = { exId ->
                            nav.popBackStack()
                            nav.navigate("follow/$exId") {}
                        },
                        onHome = { switchTab(TAB_HOME) }
                    )
                }
                composable("training") {
                    MyTrainingScreen(onBack = { nav.popBackStack() })
                }
                composable("wearable") {
                    WearableScreen(onBack = { nav.popBackStack() })
                }
                composable("settings") {
                    SettingsScreen(
                        onBack = { nav.popBackStack() },
                        onProfile = { nav.navigate("profile") },
                        onAssistant = { switchTab(TAB_ASSISTANT) },
                        onWearable = { nav.navigate("wearable") }
                    )
                }
                composable("videoPick") {
                    VideoPickScreen(
                        onBack = { nav.popBackStack() },
                        onStartAnalysis = { kind, name, ageId, uriStr ->
                            com.quannian.zhidong.video.VideoAnalysisChannel.pending(kind, name, ageId, uriStr)
                            nav.navigate("videoAnalysis")
                        }
                    )
                }
                composable("videoAnalysis") {
                    VideoAnalysisScreen(onBack = { nav.popBackStack() }, onDone = { nav.navigate("videoReport") })
                }
                composable("videoReport") {
                    VideoReportScreen(
                        onBack = { nav.popBackStack() },
                        onCorrect = { exId, errorType ->
                            com.quannian.zhidong.video.VideoAnalysisChannel.markCorrection(exId, errorType)
                            nav.popBackStack(TAB_HOME, inclusive = false)
                            nav.navigate("coach/$exId")
                        }
                    )
                }
            }
        }
    }
}

private data class TabDef(val route: String, val label: String, val icon: ImageVector)

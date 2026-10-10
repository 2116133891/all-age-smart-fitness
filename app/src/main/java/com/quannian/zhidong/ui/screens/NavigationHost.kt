package com.quannian.zhidong.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.quannian.zhidong.ui.theme.QuanNingTheme

/**
 * 全局导航。路由：
 *  splash → home → age(:child/:youth/:middle/:senior) → exercise → coach → follow → report
 *  新增入口：onboarding（引导）、profile（年龄性别选择）、bodyTest（体测）、
 *           assistant（AI 小助手）、wearable（运动手环）。
 */
@Composable
fun NavigationHost(modifier: Modifier = Modifier) {
    QuanNingTheme {
        Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
            val nav: NavHostController = rememberNavController()
            NavHost(navController = nav, startDestination = "splash") {
                composable("splash") {
                    SplashScreen(
                        onDone = {
                            nav.navigate("onboarding") { popUpTo("splash") { inclusive = true } }
                        }
                    )
                }

                // 引导页（介绍 App 能做什么，三类人群各讲作用）
                composable("onboarding") {
                    OnboardingScreen(
                        onFinish = {
                            nav.navigate("profile") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        }
                    )
                }

                // 年龄 + 性别选择（写入 ProfileStore，个性化）
                composable("profile") {
                    ProfileScreen(
                        onBack = { nav.popBackStack() },
                        onDone = {
                            nav.navigate("home") {
                                popUpTo("profile") { inclusive = true }
                            }
                        }
                    )
                }

                composable("home") {
                    HomeScreen(
                        onPickAge = { nav.navigate("age/$it") },
                        onMyTraining = { nav.navigate("training") },
                        onAiDiagnose = { nav.navigate("videoPick") },
                        onAssistant = { nav.navigate("assistant") },
                        onWearable = { nav.navigate("wearable") },
                        onProfile = { nav.navigate("profile") }
                    )
                }
                composable("age/{id}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id") ?: "youth"
                    AgeScreen(
                        id = id,
                        onBack = { nav.popBackStack() },
                        onPickExercise = { exId -> nav.navigate("exercise/$exId") },
                        onBodyTest = { nav.navigate("bodyTest") }
                    )
                }
                composable("bodyTest") {
                    BodyTestScreen(
                        onBack = { nav.popBackStack() },
                        onGoTrain = { nav.popBackStack() }
                    )
                }
                composable("exercise/{id}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id") ?: ""
                    ExerciseScreen(
                        exerciseId = id,
                        onBack = { nav.popBackStack() },
                        onStartTeach = { nav.navigate("coach/$id") },
                        onStartFollow = { nav.navigate("follow/$id") }
                    )
                }
                composable("coach/{id}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id") ?: ""
                    CoachScreen(
                        exerciseId = id,
                        onBack = { nav.popBackStack() },
                        onStartFollow = { nav.navigate("follow/$id") }
                    )
                }
                composable("follow/{id}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id") ?: ""
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
                        onHome = { nav.popBackStack("home", inclusive = false) }
                    )
                }
                composable("training") {
                    MyTrainingScreen(onBack = { nav.popBackStack() })
                }

                // AI 小助手（本地离线问答）
                composable("assistant") {
                    AssistantScreen(onBack = { nav.popBackStack() })
                }

                // 运动手环（演示连接 + 数据维度）
                composable("wearable") {
                    WearableScreen(onBack = { nav.popBackStack() })
                }

                // AI 动作诊断（视频复盘闭环，spec §十一~§二十）
                composable("videoPick") {
                    VideoPickScreen(
                        onBack = { nav.popBackStack() },
                        onStartAnalysis = { kind, name, ageId, uriStr ->
                            com.quannian.zhidong.video.VideoAnalysisChannel.pending(
                                kind, name, ageId, uriStr
                            )
                            nav.navigate("videoAnalysis") { popUpTo("home") { inclusive = false } }
                        }
                    )
                }
                composable("videoAnalysis") {
                    VideoAnalysisScreen(
                        onBack = { nav.popBackStack() },
                        onDone = { nav.navigate("videoReport") }
                    )
                }
                composable("videoReport") {
                    VideoReportScreen(
                        onBack = { nav.popBackStack() },
                        onCorrect = { exId, errorType ->
                            com.quannian.zhidong.video.VideoAnalysisChannel.markCorrection(exId, errorType)
                            nav.popBackStack("home", inclusive = false)
                            nav.navigate("coach/$exId")
                        }
                    )
                }
            }
        }
    }
}

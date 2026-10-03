package com.quannian.zhidong.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.quannian.zhidong.ui.theme.QuanNingTheme

/**
 * 全局导航。路由：
 *  splash → home → age(:child/:youth/:senior) → exercise → coach → followalong → report
 */
@Composable
fun NavigationHost(modifier: Modifier = Modifier) {
    QuanNingTheme {
        Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
            val nav: NavHostController = rememberNavController()
            NavHost(navController = nav, startDestination = "splash") {
                composable("splash") {
                    SplashScreen(onDone = { nav.navigate("home") { popUpTo("splash") { inclusive = true } } })
                }
                composable("home") {
                    HomeScreen(
                        onPickAge = { nav.navigate("age/$it") },
                        onMyTraining = { nav.navigate("training") }
                    )
                }
                composable("age/{id}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id") ?: "youth"
                    AgeScreen(
                        id = id,
                        onBack = { nav.popBackStack() },
                        onPickExercise = { exId -> nav.navigate("exercise/$exId") }
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
                        onAgain = {
                            nav.popBackStack()
                            nav.navigate("follow") {}
                        },
                        onHome = { nav.popBackStack("home", inclusive = false) }
                    )
                }
                composable("training") {
                    MyTrainingScreen(onBack = { nav.popBackStack() })
                }
            }
        }
    }
}

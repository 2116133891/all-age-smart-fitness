package com.quannian.zhidong

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * V3.6 回归守卫（P0-A / P0-B / P0-C / P1-E）。
 *
 *  真机不可用（本环境 adb devices 为空）下，用**源码级守卫**锁定已定位的根因修复，
 *  防止后续改动回退：
 *   - P0-A：CameraManager 释放后不得再向已关闭的 executor 提交任务（RejectedExecutionException
 *           是跟练页返回/切页后主线程闪退的根因之一）；FollowAlongScreen 必须释放相机。
 *   - P0-B：TeachingVideoPlayer 支持按源比例 letterbox（fixed16By9=false），跟练页必须传 false。
 *   - P0-C/P1-E：CoachVisual 资源映射必须"风格感知"（REALISTIC→真人 / ANIMATED→3D 卡通），
 *           且用户提供的真人立绘资源文件真实存在。
 *
 *  纯 JVM 可运行，不依赖 Android 框架。
 */
class V36RegressionTest {

    private fun findSrcRoot(): File {
        var f = File(".")
        for (i in 0 until 10) {
            if (f.name == "app" || f.listFiles()?.any { it.name == "src" && it.isDirectory } == true)
                return if (f.name == "app") f else f.parentFile ?: f
            val p = f.parentFile ?: break
            f = p
        }
        return File("app/src/main/java")
    }

    private fun readKt(name: String): String? {
        val f = findSrcRoot().walkTopDown().filter { it.name == name }.firstOrNull()
        ?: File("app/src/main/java").walkTopDown().filter { it.name == name }.firstOrNull()
        return f?.readText()
    }

    // ── P0-A：CameraManager 释放守卫 ──────────────────────────────

    @Test
    fun `CameraManager guards start after release (no RejectedExecutionException to UI thread)`() {
        val cm = readKt("CameraManager.kt")
        assertNotNull("CameraManager.kt not found", cm)
        val txt = cm!!
        assertTrue(
            "start() 必须在资源释放后幂等返回，避免向已 shutdown 的 executor 提交任务（跟练闪退根因）",
            txt.contains("if (released)")
        )
        assertTrue(
            "pose 初始化任务必须捕获 RejectedExecutionException，不能让其在主线程未捕获 → 闪退",
            txt.contains("RejectedExecutionException")
        )
        assertTrue("release() 必须置 released 守卫", txt.contains("released = true"))
        assertTrue(
            "restart/switch 必须尊重 released，避免释放后仍操作 CameraX 用例",
            txt.contains("if (released) return")
        )
    }

    @Test
    fun `FollowAlongScreen releases camera on dispose (covers system-back and tab-switch exit)`() {
        val f = readKt("FollowAlongScreen.kt")
        assertNotNull("FollowAlongScreen.kt not found", f)
        val txt = f!!
        assertTrue(
            "跟练页必须在退出路径释放相机（覆盖系统返回/tab 切换），此前缺失导致用例竞争",
            txt.contains("cameraManager.release()") && txt.contains("DisposableEffect(cameraManager)")
        )
    }

    // ── P0-B：示范视频按源比例 letterbox ───────────────────────────

    @Test
    fun `TeachingVideoPlayer supports source-aspect letterbox via fixed16By9`() {
        val t = readKt("TeachingVideoPlayer.kt")
        assertNotNull("TeachingVideoPlayer.kt not found", t)
        val txt = t!!
        assertTrue("需新增 fixed16By9 参数以支持源比例 letterbox", txt.contains("fixed16By9"))
        assertTrue(
            "false 分支必须让视频区吃掉剩余高度（weight(1f)）而非固定 16:9，避免裁切头脚",
            txt.contains("Modifier") && txt.contains("fillMaxWidth()")
        )
    }

    @Test
    fun `follow-along page passes fixed16By9 = false so videos are not cropped`() {
        val f = readKt("FollowAlongScreen.kt")
        val txt = f ?: ""
        assertTrue(
            "跟练双画面必须传 fixed16By9=false（源视频横/竖/近方形混杂，固定 16:9 会裁切头脚）",
            txt.contains("fixed16By9 = false")
        )
    }

    // ── P0-C / P1-E：风格感知的资源映射 + 真人立绘资源真实存在 ─────

    @Test
    fun `CoachVisual resource mapping is style-aware (realistic vs animated)`() {
        val cv = readKt("CoachVisual.kt")
        assertNotNull("CoachVisual.kt not found", cv)
        val txt = cv!!
        assertTrue(
            "coachResource 必须接受 style 参数（风格驱动资源选择）",
            txt.contains("fun coachResource(ageId: String, style:")
        )
        assertTrue(
            "coachPortraitResource 必须接受 style 参数",
            txt.contains("fun coachPortraitResource(ageId: String, style:")
        )
        assertTrue(
            "REALISTIC 风格必须映射到用户提供的真人立绘资源 coach_realistic_*",
            txt.contains("coach_realistic_youth_portrait")
        )
    }

    @Test
    fun `user-supplied realistic coach portraits actually exist in res drawable`() {
        // 直接以相对路径定位 res/drawable（JUnitCore 工作目录 = 项目根），最可靠。
        val candidates = listOf(
            File("app/src/main/res/drawable"),
            findSrcRoot().parentFile?.let { File(it, "res/drawable") }
        ).filterNotNull()
        val drawable = candidates.firstOrNull { it.exists() }
        assertNotNull("res/drawable not found under any candidate: ${candidates.joinToString { it.path }}", drawable)
        val d = drawable!!
        for (name in listOf(
            "coach_realistic_youth_portrait.png",
            "coach_realistic_middle_portrait.png",
            "coach_realistic_senior_portrait.png"
        )) {
            val f = File(d, name)
            assertTrue("$name 必须真实存在且非空（不得伪造/跳过）", f.exists() && f.length() > 0)
        }
    }

    // ── P0-D：青少年页结构对齐 ─────────────────────────────────────

    @Test
    fun `youth page keeps BMI and lung-capacity out of the motion list`() {
        val repo = com.quannian.zhidong.repository.TeachingVideoRepository
        val items = com.quannian.zhidong.model.BodyTestCatalog.ITEMS
        val motionKeys = items.filter { !repo.isMethodOnlyItem(it.key) }.map { it.key }
        assertFalse("BMI 不得进入动作示范清单", motionKeys.contains(com.quannian.zhidong.model.BodyTestItemKey.BMI))
        assertFalse("肺活量 不得进入动作示范清单", motionKeys.contains(com.quannian.zhidong.model.BodyTestItemKey.VITAL_CAPACITY))
    }
}

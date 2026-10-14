package com.quannian.zhidong

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * V3.4 回归保护：V3.3 重做 OnboardingScreen 时，BoardPageView 的功能列表 Column
 * 误用了 `Modifier.weight(0f)`（Compose 要求 weight > 0），导致冷启动首帧布局
 * 即抛 IllegalArgumentException 闪退。
 *
 * 本测试是源码级守卫：扫描全部 .kt 源文件，禁止再次出现 `weight(0)` 或
 * `weight(0f)` / `weight(0.0f)` / `weight(0.0)` 等无效 weight 调用。
 *
 * 不依赖 Android 框架，纯 JVM 可运行。
 */
class V34OnboardingRegressionTest {

    private fun findSrcRoot(): File {
        // 从 app/src/test/java/com/quannian/zhidong/ 向上找 5 层到项目根
        var f = File(".")
        for (i in 0 until 10) {
            if (f.name == "app" || f.listFiles()?.any { it.name == "src" && it.isDirectory } == true)
                return if (f.name == "app") f else f.parentFile ?: f
            val p = f.parentFile ?: break
            f = p
        }
        // fallback：直接用相对路径
        return File("app/src/main/java")
    }

    @Test
    fun `no weight 0 pattern in any kotlin source file`() {
        val root = findSrcRoot()
        assertTrue("Source root not found: ${root.absolutePath}", root.exists())
        val ktFiles = root.walkTopDown().filter { it.extension == "kt" }.toList()
        assertTrue("No .kt files found under $root", ktFiles.isNotEmpty())

        val bad = ktFiles.filter { f ->
            val txt = f.readText()
            txt.contains("weight(0f)") ||
            txt.contains("weight(0.0f)") ||
            txt.contains("weight(0.0)") ||
            txt.contains("weight(0)")
        }
        assertTrue(
            "Found weight(0) in: ${bad.joinToString(", ") { it.path }}. " +
            "Compose requires weight > 0; use natural height instead of weight(0f).",
            bad.isEmpty()
        )
    }

    @Test
    fun `onboarding BoardPageView does not use weight 0`() {
        val f = findSrcRoot().walkTopDown()
            .filter { it.name == "OnboardingScreen.kt" }
            .firstOrNull()
        assertNotNull("OnboardingScreen.kt not found", f)
        val txt = f!!.readText()
        assertFalse(
            "OnboardingScreen.kt still contains weight(0...) — V3.4 crash root cause not fixed",
            txt.contains("weight(0f)") || txt.contains("weight(0.0f)") ||
            txt.contains("weight(0.0)") || txt.contains("weight(0)")
        )
    }

    @Test
    fun `CoachStyle defaults are valid`() {
        assertEquals(com.quannian.zhidong.model.CoachStyle.REALISTIC,
            com.quannian.zhidong.model.CoachStyle.defaultForAgeGroup("youth"))
        assertEquals(com.quannian.zhidong.model.CoachStyle.ANIMATED,
            com.quannian.zhidong.model.CoachStyle.defaultForAgeGroup("middle"))
        assertEquals(com.quannian.zhidong.model.CoachStyle.ANIMATED,
            com.quannian.zhidong.model.CoachStyle.defaultForAgeGroup("senior"))
        // invalid raw → falls back to youth default, no crash
        assertEquals(com.quannian.zhidong.model.CoachStyle.REALISTIC,
            com.quannian.zhidong.model.CoachStyle.fromRaw("__unknown__"))
    }

    @Test
    fun `CoachAssetRepository returns honest null for missing video`() {
        val repo = com.quannian.zhidong.repository.CoachAssetRepository
        val res = repo.resolveVideo("youth", com.quannian.zhidong.model.CoachStyle.REALISTIC, "__no_such_id__")
        assertNull(res.video)
        assertFalse(res.styleMatched)
        assertTrue(res.note.isNotBlank())
    }

    @Test
    fun `AgeGroupConfig normalizes old and unknown ids to valid bands`() {
        val cfg = com.quannian.zhidong.model.AgeGroupConfig
        assertEquals("youth", cfg.normalizeAgeId("child"))
        assertEquals("youth", cfg.normalizeAgeId("__garbage__"))
        assertEquals("middle", cfg.normalizeAgeId("middle"))
        assertEquals("senior", cfg.normalizeAgeId("senior"))
        assertEquals("youth", cfg.normalizeAgeId("youth"))
    }
}

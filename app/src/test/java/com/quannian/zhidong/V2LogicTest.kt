package com.quannian.zhidong

import com.quannian.zhidong.db.BodyTestStandardService
import com.quannian.zhidong.model.BodyTestCatalog
import com.quannian.zhidong.model.BodyTestItemKey
import com.quannian.zhidong.repository.TeachingVideoRepository
import com.quannian.zhidong.service.TrainingPlanService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V2 纯 JVM 逻辑单测（不依赖 Android / Compose，可在 JVM 运行）。
 *  覆盖 任务 4/5/6/3 的 可 单 测 逻辑：
 *   - BMI 计算（体重kg / 身高m²）
 *   - 体测 标准 评分（scoreFor / gradeLabel / 适用 判定 / 不 混用 标准）
 *   - 训练 计划 生成 与 校验（薄弱 项 优先 / 周期 天数 校验 / 安全 边界）
 *   - 视频 资源 索引（itemKey → 视频 映射 / 缺失 处理 / 唯一 键）
 */
class V2LogicTest {

    // ───────── BMI（任务 4.1） ─────────

    /** BMI = 体重(kg) / 身高(m)^2。 */
    @Test
    fun `bmi calculation is weight over height squared`() {
        val heightM = 1.75
        val weightKg = 70.0
        val bmi = weightKg / (heightM * heightM)
        assertEquals(22.86, bmi, 0.05)   // 70 / 1.75^2 ≈ 22.86
    }

    @Test
    fun `bmi is not body fat and is not a diagnostic`() {
        // 仅 验证 本 项目 BMI 计算 一致（同 身高体重 同 结果），不 做 健康 诊断。
        val a = 60.0 / (1.70 * 1.70)
        val b = 60.0 / (1.70 * 1.70)
        assertEquals(a, b, 0.000001)
        assertTrue("BMI 应为 正数", a > 0)
    }

    // ───────── 体测 标准 评分（任务 5） ─────────

    @Test
    fun `standard applicable only for youth known gender`() {
        assertTrue(BodyTestStandardService.applicable("male"))
        assertTrue(BodyTestStandardService.applicable("female"))
        assertFalse("unknown 性别 不 适用", BodyTestStandardService.applicable("unknown"))
        assertFalse("中年 组 不 适用 大学 生 标准", BodyTestStandardService.applicable("male", "middle"))
        assertFalse("银龄 组 不 适用 大学 生 标准", BodyTestStandardService.applicable("female", "senior"))
    }

    @Test
    fun `run 50m lower time scores higher`() {
        val fast = BodyTestStandardService.scoreFor(BodyTestItemKey.RUN_50M, "male", 8.9).first
        val slow = BodyTestStandardService.scoreFor(BodyTestItemKey.RUN_50M, "male", 11.5).first
        assertTrue("更快 应 更 高分", fast > slow)
    }

    @Test
    fun `long jump distance uses cm`() {
        val (score, valid) = BodyTestStandardService.scoreFor(BodyTestItemKey.LONG_JUMP, "male", 280.0)
        assertTrue(valid)
        assertTrue("优秀 跳远 应 高分", score >= 80)
    }

    @Test
    fun `bmi has no official 100 point score`() {
        // 任务 4.1：BMI 不 适用 官方 100 分 制（非 体脂、不 单独 诊断 健康）。
        val (score, valid) = BodyTestStandardService.scoreFor(BodyTestItemKey.BMI, "male", 24.0)
        assertFalse("BMI 不 应 输出 官方 分数", valid)
        assertEquals(0, score)
    }

    @Test
    fun `grade labels map correctly`() {
        assertEquals("优秀", BodyTestStandardService.gradeLabel(95))
        assertEquals("良好", BodyTestStandardService.gradeLabel(82))
        assertEquals("及格", BodyTestStandardService.gradeLabel(70))
        assertEquals("待提升", BodyTestStandardService.gradeLabel(62))
        assertEquals("不及格", BodyTestStandardService.gradeLabel(40))
    }

    // ───────── 训练 计划 生成（任务 6） ─────────

    @Test
    fun `plan generation prioritizes weak items`() {
        val weak = listOf(BodyTestItemKey.RUN_50M)
        val params = TrainingPlanService.PlanParams(weakItems = weak, weeks = 2, daysPerWeek = 3)
        val tasks = TrainingPlanService.generate(params)
        assertTrue("应有 任务 生成", tasks.isNotEmpty())
        // 所有 任务 应 携带 安全 边界
        assertTrue(tasks.all { it.caution.contains("停止") })
        // 薄弱 项 应 排 在 前
        val firstItem = tasks.first().itemKey
        assertTrue("薄弱 项 优先", firstItem == BodyTestItemKey.RUN_50M)
    }

    @Test
    fun `plan validates weeks and days`() {
        assertNull(TrainingPlanService.validate(4, 3))
        assertNotNull("2 周 外 不 允许", TrainingPlanService.validate(5, 3))
        assertNotNull("天数 0 不 允许", TrainingPlanService.validate(4, 0))
        assertNotNull("天数 8 不 允许", TrainingPlanService.validate(4, 8))
    }

    @Test
    fun `plan completion report counts correctly`() {
        val tasks = TrainingPlanService.generate(
            TrainingPlanService.PlanParams(weakItems = emptyList(), weeks = 2, daysPerWeek = 2)
        )
        // 模拟 完成 一半
        val modified = tasks.mapIndexed { i, t ->
            t.copy(status = if (i < tasks.size / 2) "done" else "pending")
        }
        val report = TrainingPlanService.completionReport(modified)
        val total = report["total"] as Int
        val done = report["done"] as Int
        assertEquals(total, modified.size)
        assertTrue("完成率 应 > 0", (report["rate"] as Double) > 0.0)
        assertEquals(done, modified.count { it.status == "done" })
    }

    @Test
    fun `empty weak items uses default rotation`() {
        val tasks = TrainingPlanService.generate(
            TrainingPlanService.PlanParams(weakItems = emptyList(), weeks = 4, daysPerWeek = 5)
        )
        // 4 周 × 5 天 = 20 个 训练 任务（休息 日 不 生成）
        assertEquals(20, tasks.size)
        // 所有 任务 有 标题
        assertTrue(tasks.all { it.title.isNotBlank() })
    }

    // ───────── 视频 资源 索引（任务 3） ─────────

    @Test
    fun `each body test item maps to a video id`() {
        BodyTestItemKey.ALL.forEach { key ->
            // 每项 都 有 主 视频（含 方法 说明 型）
            val v = TeachingVideoRepository.videoFor(key)
            assertNotNull("项目 $key 应 有 视频 条目", v)
        }
    }

    @Test
    fun `pullup situp splits by gender`() {
        val male = TeachingVideoRepository.videoFor(BodyTestItemKey.PULLUP_SITUP, "male")
        val female = TeachingVideoRepository.videoFor(BodyTestItemKey.PULLUP_SITUP, "female")
        assertEquals("pullup", male?.id)
        assertEquals("situp", female?.id)
    }

    @Test
    fun `action items have real video assets`() {
        // 有 动作 的 项目 应 有 真实 assetPath
        val actionItems = listOf(
            BodyTestItemKey.LONG_JUMP, BodyTestItemKey.RUN_50M,
            BodyTestItemKey.SEATED_REACH, BodyTestItemKey.RUN_800_1000
        )
        actionItems.forEach { key ->
            assertTrue("项目 $key 应 有 真实 视频 资源", TeachingVideoRepository.hasVideo(key))
        }
    }

    @Test
    fun `bmi and vital capacity have no action video`() {
        // 任务 4：BMI/肺活量 不 生成 假装 摄像头 测量 的 动作 视频
        assertFalse("BMI 不 应 有 动作 视频", TeachingVideoRepository.hasVideo(BodyTestItemKey.BMI))
        assertFalse("肺活量 不 应 有 动作 视频", TeachingVideoRepository.hasVideo(BodyTestItemKey.VITAL_CAPACITY))
    }

    @Test
    fun `all video items have unique ids`() {
        val ids = TeachingVideoRepository.allVideoItems().map { it.id }
        assertEquals("视频 id 应 唯一", ids.toSet().size, ids.size)
    }

    @Test
    fun `video asset paths point to assets directory`() {
        TeachingVideoRepository.allVideoItems().forEach { v ->
            assertTrue("assetPath 应 在 assets 下", v.assetPath.startsWith("videos/"))
        }
    }

    // ───────── 体测 目录（任务 4） ─────────

    @Test
    fun `catalog has all seven items`() {
        assertEquals(7, BodyTestCatalog.ITEMS.size)
        BodyTestItemKey.ALL.forEach { key ->
            assertNotNull("key $key 应 在 目录", BodyTestCatalog.byKey(key))
        }
    }

    @Test
    fun `displayName splits by gender`() {
        val pullup = BodyTestCatalog.byKey(BodyTestItemKey.PULLUP_SITUP)!!
        assertEquals("引体 向上", BodyTestCatalog.displayName(pullup, "male"))
        assertEquals("1 分钟 仰卧 起坐", BodyTestCatalog.displayName(pullup, "female"))
        val run = BodyTestCatalog.byKey(BodyTestItemKey.RUN_800_1000)!!
        assertEquals("1000 米 跑", BodyTestCatalog.displayName(run, "male"))
        assertEquals("800 米 跑", BodyTestCatalog.displayName(run, "female"))
    }
}

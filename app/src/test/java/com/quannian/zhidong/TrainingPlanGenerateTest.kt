package com.quannian.zhidong

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
 * P0-3 备考训练计划回归单测（纯 JVM）。
 *
 *  锁定 YouthTrainingPlanScreen 依赖的契约：
 *   - generate() 返回的任务 planId 全部一致（UI 必须用这个真实 planId 去 repo.planTasks()，
 *     旧 bug：UI 自建 pid 与服务 planId 不一致 → planTasks(pid) 恒空 → 任务/报告不显示）。
 *   - 周期 × 每周天数 展开正确：训练日数 = weeks * daysPerWeek；休息日不生成任务。
 *   - 视频解析走 byId（P1 修复保持）：有动作项 videoAsset 非空且指向真实资产；
 *     method-only（bmi/vital_capacity）videoAsset 为空（测量方法，不假放视频）。
 *   - 参数校验：周期 2/4/8、天数 1–7。
 */
class TrainingPlanGenerateTest {

    @Test
    fun `generate returns a single consistent planId across all tasks`() {
        val params = TrainingPlanService.PlanParams(weakItems = emptyList(), weeks = 2, daysPerWeek = 4)
        val tasks = TrainingPlanService.generate(params)
        assertTrue("应有任务", tasks.isNotEmpty())
        val planIds = tasks.map { it.planId }.distinct()
        assertEquals("所有任务必须同一 planId（UI 用此 key 查询）", 1, planIds.size)
        assertNotNull(planIds.first())
    }

    @Test
    fun `training sessions equal weeks times daysPerWeek`() {
        for (weeks in listOf(2, 4, 8)) {
            for (d in 1..7) {
                val tasks = TrainingPlanService.generate(
                    TrainingPlanService.PlanParams(emptyList(), weeks, d)
                )
                assertEquals("周$weeks × 每天$d 应有 ${weeks * d} 个训练任务",
                    weeks * d, tasks.size)
                // dayIndex 升序
                assertTrue("dayIndex 应升序", tasks.zipWithNext { a, b -> a.dayIndex < b.dayIndex }.all { it })
            }
        }
    }

    @Test
    fun `video asset resolved via byId and method-only items have blank asset`() {
        val tasks = TrainingPlanService.generate(
            TrainingPlanService.PlanParams(
                weakItems = listOf(
                    BodyTestItemKey.SEATED_REACH, BodyTestItemKey.BMI, BodyTestItemKey.VITAL_CAPACITY
                ),
                weeks = 2, daysPerWeek = 7
            )
        )
        // 坐位体前屈 → forward_reach 视频（byId 解析，assetPath 非空）
        val reach = tasks.first { it.itemKey == BodyTestItemKey.SEATED_REACH }
        assertTrue("坐位体前屈应有教学视频", reach.videoAsset.isNotBlank())
        assertTrue("视频资产路径应以 videos/ 开头", reach.videoAsset.startsWith("videos/"))
        // BMI / 肺活量 → method-only，videoAsset 为空（不放视频，走测量方法）
        val bmi = tasks.first { it.itemKey == BodyTestItemKey.BMI }
        val vc = tasks.first { it.itemKey == BodyTestItemKey.VITAL_CAPACITY }
        assertEquals("BMI 应为 method-only（无视频）", "", bmi.videoAsset)
        assertEquals("肺活量应为 method-only（无视频）", "", vc.videoAsset)
    }

    @Test
    fun `weak items are scheduled first`() {
        val tasks = TrainingPlanService.generate(
            TrainingPlanService.PlanParams(weakItems = listOf(BodyTestItemKey.RUN_50M), weeks = 2, daysPerWeek = 7)
        )
        val first = tasks.first()
        assertEquals("薄弱项应排在首个训练日", BodyTestItemKey.RUN_50M, first.itemKey)
        assertTrue("首个任务应标记 weakItemKey", first.weakItemKey == BodyTestItemKey.RUN_50M)
    }

    @Test
    fun `byId is the correct lookup for template video ids`() {
        // P1 修复口径：byId(id) 能解析 7 个动作视频；method-only 解析到空 assetPath 的条目。
        listOf("run_50m", "long_jump", "forward_reach", "pullup", "endurance").forEach { id ->
            val v = TeachingVideoRepository.byId(id)
            assertNotNull("byId($id) 应命中", v)
            assertFalse("byId($id) 资产应非空", v!!.assetPath.isBlank())
        }
        assertNull("未知 id byId 返回 null", TeachingVideoRepository.byId("no_such_id"))
        // method-only：条目存在但 assetPath 空
        assertEquals("", TeachingVideoRepository.byId("bmi_method")?.assetPath)
        assertEquals("", TeachingVideoRepository.byId("vital_capacity_method")?.assetPath)
    }

    @Test
    fun `validate rejects out-of-range params`() {
        assertNull("2/4/8 周合法", TrainingPlanService.validate(4, 5))
        assertEquals("周期仅 2/4/8", "备考 周期 仅 支持 2/4/8 周", TrainingPlanService.validate(5, 3))
        assertEquals("天数 1-7", "每周 训练 天数 需 在 1-7 天", TrainingPlanService.validate(4, 0))
        assertEquals("天数 1-7", "每周 训练 天数 需 在 1-7 天", TrainingPlanService.validate(4, 8))
    }
}

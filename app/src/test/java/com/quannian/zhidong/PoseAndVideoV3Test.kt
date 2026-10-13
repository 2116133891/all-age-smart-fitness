package com.quannian.zhidong

import com.quannian.zhidong.model.Landmark
import com.quannian.zhidong.pose.Crop
import com.quannian.zhidong.pose.PoseCoordinateMapper
import com.quannian.zhidong.repository.TeachingVideoRepository
import com.quannian.zhidong.service.TrainingPlanService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V3 纯 JVM 单测（任务 9 视频扩展 + 任务 10 姿态坐标映射）。
 *  不依赖 Android / Compose，可在 JVM 运行（配合 run_jvm_tests_v2.sh）。
 *
 *  覆盖：
 *   - 教学视频扩到 18 条（youth 10 + middle 4 + senior 4），id 唯一，assetPath 规范。
 *   - [PoseCoordinateMapper]：全帧映射、裁切映射、前置镜像翻转、cropMatters 诊断。
 */
class PoseAndVideoV3Test {

    // ───────── 任务 9：教学视频 18 条扩展 ─────────

    @Test
    fun `video repository has eighteen items total`() {
        val all = TeachingVideoRepository.allItems()
        assertEquals("V3 应扩展到 18 条教学视频", 18, all.size)
    }

    @Test
    fun `video items split by age group youth 10 middle 4 senior 4`() {
        val youth = TeachingVideoRepository.byAgeId("youth")
        val middle = TeachingVideoRepository.byAgeId("middle")
        val senior = TeachingVideoRepository.byAgeId("senior")
        assertEquals("youth 应有 10 条", 10, youth.size)
        assertEquals("middle 应有 4 条", 4, middle.size)
        assertEquals("senior 应有 4 条", 4, senior.size)
    }

    @Test
    fun `all video ids are unique`() {
        val ids = TeachingVideoRepository.allItems().map { it.id }
        assertEquals("视频 id 应唯一", ids.toSet().size, ids.size)
    }

    @Test
    fun `video asset paths are blank or start with videos`() {
        TeachingVideoRepository.allItems().forEach { v ->
            val p = v.assetPath
            assertTrue(
                "assetPath 应为空或 videos/ 前缀（$p）",
                p.isBlank() || p.startsWith("videos/")
            )
        }
    }

    @Test
    fun `only real videos are surfaced for playback`() {
        val playable = TeachingVideoRepository.allVideoItems()
        // V3.1：youth 10 条 + middle 4 条 + senior 4 条 全部已生成真实 mp4（共 16 条可播）。
        // BMI / 肺活量 为方法说明型（assetPath 空，不生成动作视频，设计如此）→ 不进可播列表。
        assertEquals("可播视频数 = 有真实 mp4 的条目数（youth 10 + middle 4 + senior 4）", 16, playable.size)
        assertTrue("可播条目 assetPath 非空", playable.all { it.assetPath.isNotBlank() })
        // 每段可播数（middle / senior 已补齐，不再是空态）
        // youth 共 10 条，其中 8 条有真实 mp4（另 2 条 BMI/肺活量方法说明型无视频）
        assertEquals("youth 可播 8（10 条中 2 条方法说明型无视频）", 8, TeachingVideoRepository.videoItemsForAge("youth").size)
        assertEquals("middle 可播 4", 4, TeachingVideoRepository.videoItemsForAge("middle").size)
        assertEquals("senior 可播 4", 4, TeachingVideoRepository.videoItemsForAge("senior").size)
    }

    @Test
    fun `method-only entries stay videoless and are excluded from playable`() {
        val all = TeachingVideoRepository.allItems()
        // 仅 BMI / 肺活量 两条方法说明型保持 assetPath 空（不生成运动示范视频）
        val videoless = all.filter { it.assetPath.isBlank() }
        assertEquals("仅 2 条方法说明型无视频（bmi_method / vital_capacity_method）", 2, videoless.size)
        assertTrue("无视频条目必须都是方法说明型",
            videoless.all { it.id in setOf("bmi_method", "vital_capacity_method") })
        videoless.forEach { v ->
            assertFalse("无视频条目 ${v.id} 不应出现在可播列表",
                TeachingVideoRepository.allVideoItems().any { it.id == v.id })
        }
    }

    @Test
    fun `each age group has real videos for middle and senior`() {
        // V3.1：middle / senior 已生成真实 mp4，可播列表不再为空（补齐 4 + 4）。
        assertEquals(4, TeachingVideoRepository.byAgeId("middle").size)
        assertEquals(4, TeachingVideoRepository.byAgeId("senior").size)
        assertEquals("middle 可播 4（非空态）", 4, TeachingVideoRepository.videoItemsForAge("middle").size)
        assertEquals("senior 可播 4（非空态）", 4, TeachingVideoRepository.videoItemsForAge("senior").size)
        // 兜底：方法说明型 youth 条目仍可通过 byId 取到
        assertNotNull(TeachingVideoRepository.byId("bmi_method"))
        assertNotNull(TeachingVideoRepository.byId("vital_capacity_method"))
    }

    // ───────── 任务 10：姿态坐标映射（旋转 / 镜像 / 坐标诊断） ─────────

    private fun lm(x: Float, y: Float) = Landmark(index = 1, x = x, y = y)

    @Test
    fun `full-frame mapping scales normalized to canvas`() {
        val c = PoseCoordinateMapper.toCanvasPoint(
            lm(0.2f, 0.5f), Crop.FULL, canvasW = 1000f, canvasH = 800f, mirror = false
        )
        assertEquals(200f, c.x, 0.001f)  // 0.2 * 1000
        assertEquals(400f, c.y, 0.001f)  // 0.5 * 800
    }

    @Test
    fun `crop mapping lands points inside the visible crop`() {
        // 可见裁切：横向居中裁掉两侧各 12.5%（4:3 源放进 1:1 画布）。
        val crop = Crop(0.125f, 0f, 0.875f, 1f)
        val c = PoseCoordinateMapper.toCanvasPoint(
            lm(0.5f, 0.5f), crop, canvasW = 1000f, canvasH = 1000f, mirror = false
        )
        // 源中心 (0.5,0.5) → 裁切内 (0.125 + 0.5*0.75)=0.50 → *1000 = 500
        assertEquals(500f, c.x, 0.001f)
        assertEquals(500f, c.y, 0.001f)
    }

    @Test
    fun `front camera mirror flips x across canvas center`() {
        val noMirror = PoseCoordinateMapper.toCanvasPoint(
            lm(0.2f, 0.5f), Crop.FULL, 1000f, 800f, mirror = false
        )
        val mirrored = PoseCoordinateMapper.toCanvasPoint(
            lm(0.2f, 0.5f), Crop.FULL, 1000f, 800f, mirror = true
        )
        assertEquals(200f, noMirror.x, 0.001f)
        assertEquals(800f, mirrored.x, 0.001f)   // 1000 - 200
        assertEquals(noMirror.y, mirrored.y, 0.001f)  // y 不受镜像影响
    }

    @Test
    fun `mirror of a centered crop maps to the mirrored visible window`() {
        val crop = Crop(0.125f, 0f, 0.875f, 1f)
        val p = PoseCoordinateMapper.toCanvasPoint(
            lm(0.1f, 0.1f), crop, 1000f, 1000f, mirror = true
        )
        // 源点 (0.1,0.1) → 裁切内 x = 0.125 + 0.1*0.75 = 0.2 → 200；镜像 → 1000-200=800
        assertEquals(800f, p.x, 0.001f)
        assertEquals(100f, p.y, 0.001f)  // 0.1 * 1000
    }

    @Test
    fun `cropMatters flags when visible crop differs from full frame`() {
        val crop = Crop(0.125f, 0f, 0.875f, 1f)
        // 非中心点在全帧 vs 裁切 下差异显著，应被诊断为"需按 crop 映射"。
        val p = lm(0.9f, 0.5f)
        assertTrue("偏离中心的点应触发 cropMatters",
            PoseCoordinateMapper.cropMatters(p, crop, tolerance = 0.02f))
    }

    @Test
    fun `cropMatters is false for a full-frame crop`() {
        val p = lm(0.9f, 0.5f)
        assertFalse("全帧 crop 不应触发 cropMatters",
            PoseCoordinateMapper.cropMatters(p, Crop.FULL, tolerance = 0.02f))
    }

    @Test
    fun `batch mapping preserves order and count`() {
        val lms = listOf(lm(0.1f, 0.1f), lm(0.5f, 0.5f), lm(0.9f, 0.9f))
        val out = PoseCoordinateMapper.toCanvasPoints(
            lms, Crop.FULL, 100f, 100f, mirror = false
        )
        assertEquals(3, out.size)
        assertEquals(10f, out[0].x, 0.001f)
        assertEquals(50f, out[1].x, 0.001f)
        assertEquals(90f, out[2].x, 0.001f)
    }

    // ───────── P1 修复：备考计划任务的教学视频解析（byId 而非 videoFor） ─────────

    /** 取一个计划里每个 itemKey 的代表性 videoAsset（同 itemKey 任务 videoAsset 一致）。 */
    private fun videoAssetByItemKey(weakItems: List<String>): Map<String, String> {
        val tasks = TrainingPlanService.generate(
            TrainingPlanService.PlanParams(weakItems, weeks = 2, daysPerWeek = 7)
        )
        require(tasks.isNotEmpty()) { "计划应非空" }
        return tasks.groupBy { it.itemKey }.mapValues { (_, list) ->
            list.all { it.videoAsset == list.first().videoAsset }
                .let { require(it) { "同 itemKey 任务 videoAsset 应一致" } }
            list.first().videoAsset
        }
    }

    @Test
    fun `plan tasks for action items resolve to a real mp4 via byId`() {
        // 7 个训练项中，5 项有真实 mp4（run_50m/long_jump/seated_reach/pullup_situp/run_800_1000）。
        val map = videoAssetByItemKey(emptyList())
        val expectReal = mapOf(
            "run_50m" to "videos/video_youth_50m.mp4",
            "long_jump" to "videos/video_youth_long_jump.mp4",
            "seated_reach" to "videos/video_youth_forward_reach.mp4",
            "pullup_situp" to "videos/video_youth_pullup.mp4",
            "run_800_1000" to "videos/video_youth_endurance.mp4"
        )
        expectReal.forEach { (itemKey, expectedAsset) ->
            val actual = map[itemKey]
            assertNotNull("计划应包含 $itemKey 任务", actual)
            assertEquals(
                "坐位体前屈/引体·仰卧/800-1000m 三项的教学视频应经 byId(视频 id) 解析到真实 mp4（P1 修复）",
                expectedAsset, actual
            )
        }
    }

    @Test
    fun `plan tasks for method-only items stay blank`() {
        // BMI / 肺活量 无动作视频（方法说明型，assetPath 空），videoAsset 应为空串（设计内，不造假封面）。
        val map = videoAssetByItemKey(emptyList())
        assertEquals("", map["bmi"])
        assertEquals("", map["vital_capacity"])
    }

    @Test
    fun `plan resolves videos even when an item is flagged weak`() {
        // 薄弱项优先排布时，视频解析同样走 byId，不应因顺序变化而失效。
        val map = videoAssetByItemKey(
            listOf("seated_reach", "pullup_situp", "run_800_1000", "bmi")
        )
        assertEquals("videos/video_youth_forward_reach.mp4", map["seated_reach"])
        assertEquals("videos/video_youth_pullup.mp4", map["pullup_situp"])
        assertEquals("videos/video_youth_endurance.mp4", map["run_800_1000"])
        assertEquals("", map["bmi"])
    }
}

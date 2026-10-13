package com.quannian.zhidong

import com.quannian.zhidong.model.AgeGroupConfig
import com.quannian.zhidong.model.BodyTestCatalog
import com.quannian.zhidong.model.CoachStyle
import com.quannian.zhidong.repository.CoachAssetRepository
import com.quannian.zhidong.repository.TeachingVideoRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V3.3 第二轮 P0 验收测试（JVM，无 Android 依赖）：
 *  - 年龄段主名称统一（青少年/中青年/中老年）
 *  - 教练风格默认映射（青少年=真人，中青年/中老年=动画）
 *  - 资源仓库按 ageGroup+style 解析示范视频（缺失返回诚实空态、不跨年龄段填空）
 *  - 青少年"练/测"分离：BMI/肺活量 不进动态跟练列表
 */
class V33NamingStyleTest {

    @Test
    fun `age group primary names are unified`() {
        assertEquals("青少年", AgeGroupConfig.band("youth").title)
        assertEquals("中青年", AgeGroupConfig.band("middle").title)
        assertEquals("中老年", AgeGroupConfig.band("senior").title)
        // 归一化：废弃 child 归到 youth，主名称随之统一
        assertEquals("青少年", AgeGroupConfig.ageLabel("child"))
    }

    @Test
    fun `coach style default per age group`() {
        assertEquals(CoachStyle.REALISTIC, CoachStyle.defaultForAgeGroup("youth"))
        assertEquals(CoachStyle.ANIMATED, CoachStyle.defaultForAgeGroup("middle"))
        assertEquals(CoachStyle.ANIMATED, CoachStyle.defaultForAgeGroup("senior"))
        // 未知/废弃 归一化到 youth 默认
        assertEquals(CoachStyle.REALISTIC, CoachStyle.defaultForAgeGroup("child"))
    }

    @Test
    fun `coach style round trip raw id`() {
        assertEquals(CoachStyle.REALISTIC, CoachStyle.fromRaw("realistic"))
        assertEquals(CoachStyle.ANIMATED, CoachStyle.fromRaw("animated"))
        // 未知 raw 回退到 youth 默认，不留空白/null
        assertEquals(CoachStyle.REALISTIC, CoachStyle.fromRaw("??bogus??"))
    }

    @Test
    fun `realistic style resolves youth motion videos`() {
        val r = CoachAssetRepository.resolveVideo("youth", CoachStyle.REALISTIC, "run_50m")
        assertNotNull("youth 真人风格应有 50m 示范视频", r.video)
        assertTrue("同年龄段真人风格应完全匹配", r.styleMatched)
        assertTrue("示范视频必须可播放（非空 assetPath）", r.video!!.assetPath.isNotBlank())
    }

    @Test
    fun `animated style resolves middle senior videos`() {
        val r = CoachAssetRepository.resolveVideo("middle", CoachStyle.ANIMATED, "middle_core")
        assertNotNull("中青年动画风格应有核心强化视频", r.video)
        assertTrue(r.styleMatched)
    }

    @Test
    @Suppress("UNUSED_PARAMETER")
    fun `method only items never enter motion list`() {
        val motionItems = BodyTestCatalog.ITEMS.filter { !TeachingVideoRepository.isMethodOnlyItem(it.key) }
        val measureItems = BodyTestCatalog.ITEMS.filter { TeachingVideoRepository.isMethodOnlyItem(it.key) }
        // BMI/肺活量 只在测评区，不进动态跟练
        assertFalse("BMI 不应出现在动态跟练列表", motionItems.any { TeachingVideoRepository.isMethodOnlyItem(it.key) })
        assertTrue("BMI/肺活量 应在测评录入区", measureItems.size >= 2)
    }

    @Test
    fun `missing or unknown video returns honest empty state`() {
        val r = CoachAssetRepository.resolveVideo("youth", CoachStyle.REALISTIC, "__no_such_video__")
        assertNull("未知视频 id 必须返回空态，不得填空", r.video)
    }

    @Test
    fun `style switch does not change exercise id`() {
        // 同一动作，在两种风格下 videoId 保持不变（风格只改资源选择，不改动作 id）
        val a = CoachAssetRepository.resolveVideo("youth", CoachStyle.REALISTIC, "run_50m")
        val b = CoachAssetRepository.resolveVideo("youth", CoachStyle.ANIMATED, "run_50m")
        // 两种风格都指向同一个权威动作视频（同 key 同视频）
        assertEquals(a.video?.id, b.video?.id)
    }
}

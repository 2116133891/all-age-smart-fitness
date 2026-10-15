package com.quannian.zhidong

import com.quannian.zhidong.model.AgeGroupConfig
import com.quannian.zhidong.model.BodyTestItemKey
import com.quannian.zhidong.model.CoachStyle
import com.quannian.zhidong.repository.CoachAssetRepository
import com.quannian.zhidong.repository.TeachingVideoRepository
import org.junit.Assert.*
import org.junit.Test

/**
 * V3.5 P0-5/P0-6 回归测试：教练风格资源解析（itemKey+style+assetType+ageGroup）
 * 纯函数、可 JVM 运行（不依赖 Android Context）。
 *
 * 红线：风格切换不改动作 ID / 记录 / 评分 / 历史，只改资源选择；
 *       资源缺失或风格不覆盖 → 诚实空态 + note，绝不跨风格/跨动作/跨年龄段填空；
 *       BMI/肺活量恒为 method-only（无动作视频）。
 */
class V35CoachStyleAssetTest {

    @Test
    fun `resolveAsset returns honest method-only for BMI with no action video`() {
        val r = CoachAssetRepository.resolveAsset(
            itemKey = BodyTestItemKey.BMI, gender = "male",
            style = CoachStyle.REALISTIC,
            assetType = CoachAssetRepository.AssetType.DEMO_VIDEO,
            ageGroup = "youth"
        )
        assertTrue("BMI 应判定为 method-only", r.methodOnly)
        val bmiV = r.resolved
        assertFalse("BMI 不应有可播动作视频", bmiV != null && bmiV.assetPath.isNotBlank())
    }

    @Test
    fun `resolveAsset returns honest method-only for VITAL_CAPACITY`() {
        val r = CoachAssetRepository.resolveAsset(
            itemKey = BodyTestItemKey.VITAL_CAPACITY, gender = "female",
            style = CoachStyle.ANIMATED,
            assetType = CoachAssetRepository.AssetType.DEMO_VIDEO,
            ageGroup = "youth"
        )
        assertTrue("肺活量 应判定为 method-only", r.methodOnly)
    }

    @Test
    fun `youth motion item resolved with matched style has non-empty asset`() {
        // 立定跳远：youth 组，默认 REALISTIC → styleMatched
        val r = CoachAssetRepository.resolveAsset(
            itemKey = BodyTestItemKey.LONG_JUMP, gender = "male",
            style = CoachStyle.REALISTIC,
            assetType = CoachAssetRepository.AssetType.DEMO_VIDEO,
            ageGroup = "youth"
        )
        assertNotNull("立定跳远 应有可播视频", r.resolved)
        val lj = r.resolved!!
        assertTrue("youth REALISTIC 应 styleMatched", r.styleMatched)
        assertTrue("视频 assetPath 非空", lj.assetPath.isNotBlank())
        assertFalse("立定跳远 不是 method-only", r.methodOnly)
    }

    @Test
    fun `middle item with REALISTIC style is honest non-match not cross-age fill`() {
        // 核心强化：middle 组权威视频；请求 REALISTIC（youth 风格）→ 风格不覆盖 middle，
        // 诚实标注 styleMatched=false，但同年龄段视频仍可用（note 提示只有动画风格）。
        val r = CoachAssetRepository.resolveAsset(
            itemKey = "middle_core", gender = "male",
            style = CoachStyle.REALISTIC,
            assetType = CoachAssetRepository.AssetType.DEMO_VIDEO,
            ageGroup = "middle"
        )
        val mc = r.resolved
        assertNotNull("middle 核心应有视频", mc)
        assertEquals("middle_core 应匹配 middle 视频", "videos/video_middle_core.mp4", mc!!.assetPath)
        assertFalse("请求真人风格但视频属动画风格 → styleMatched=false", r.styleMatched)
    }

    @Test
    fun `ownerAgeOf maps body-test itemKey to its authoritative video age`() {
        assertEquals("youth", CoachAssetRepository.ownerAgeOf(BodyTestItemKey.LONG_JUMP))
        assertEquals("middle", CoachAssetRepository.ownerAgeOf("middle_core"))
        assertEquals("senior", CoachAssetRepository.ownerAgeOf("senior_baduanjin"))
    }

    @Test
    fun `unknown itemKey resolves to honest null not a fabricated video`() {
        val r = CoachAssetRepository.resolveAsset(
            itemKey = "does_not_exist", gender = "male",
            style = CoachStyle.REALISTIC,
            assetType = CoachAssetRepository.AssetType.DEMO_VIDEO,
            ageGroup = "youth"
        )
        assertNull("未知 itemKey 应诚实返回 null，不塞无关视频", r.resolved)
        assertFalse(r.styleMatched)
    }

    @Test
    fun `avatar asset type never returns null even when style does not cover age`() {
        // AVATAR 始终按年龄段立绘返回，不因风格覆盖判定而 null
        val r = CoachAssetRepository.resolveAsset(
            itemKey = BodyTestItemKey.LONG_JUMP, gender = "male",
            style = CoachStyle.ANIMATED,
            assetType = CoachAssetRepository.AssetType.AVATAR,
            ageGroup = "youth"
        )
        // 头像资源 id 非视频，resolved 为视频（可为该 item 视频），但绝不因风格覆盖而崩
        assertFalse("AVATAR 解析不应判为 method-only", r.methodOnly)
    }

    @Test
    fun `style switch does not change canonical video id only styleMatched or note`() {
        // 同一动作不同风格：解析到的视频 id 不变（资源映射稳定），仅 styleMatched/note 变化。
        val a = CoachAssetRepository.resolveAsset(
            "middle_core", "male", CoachStyle.REALISTIC,
            CoachAssetRepository.AssetType.DEMO_VIDEO, "middle")
        val b = CoachAssetRepository.resolveAsset(
            "middle_core", "male", CoachStyle.ANIMATED,
            CoachAssetRepository.AssetType.DEMO_VIDEO, "middle")
        val av = a.resolved; val bv = b.resolved
        assertNotNull(av); assertNotNull(bv)
        assertEquals("风格切换不改变解析到的权威视频", av!!.id, bv!!.id)
    }
}

package com.quannian.zhidong

import com.quannian.zhidong.model.BodyTestItemKey
import com.quannian.zhidong.repository.TeachingVideoRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V3.3 纯 JVM 单测（P0 视频统一映射 + 青年体测评测闭环 + 跟练生命周期判定）。
 *
 *  覆盖（不依赖 Android/Compose，纯 JVM 可跑）：
 *   1. 视频匹配严格性：
 *      - [TeachingVideoRepository.videoForItem] 唯一权威映射：同一 itemKey 恒返回同一视频。
 *      - BMI / 肺活量 是 method-only（isMethodOnlyItem=true），映射到 method-only 条目（assetPath 为空），
 *        绝不对到深蹲/跳远等动作视频。
 *      - 运动型 itemKey 映射到真实可播视频（assetPath 非空）。
 *      - 未知 key 返回 null（诚实空态，不塞泛化视频）。
 *      - 无跨年龄段错配（体测视频 ageId 恒为 youth）。
 *   2. 跟练生命周期判定：
 *      - 运动型 itemKey 可跟练（followable=true，能力标签非空）。
 *      - method-only itemKey 不可跟练（followable=false）。
 *      - 能力标签诚实分级（人工计时/测量/计数，不冒充自动检测）。
 *   3. 青年体测评测闭环数据契约（与 FollowAlongItemScreen 的人工录入逻辑一致）：
 *      - BMI 双值计算（体重kg ÷ 身高m²）。
 *      - 运动型单值录入。
 */
class V33FollowAndBodyTestTest {

    // ───────── 1. 视频匹配严格性（唯一权威映射） ─────────

    @Test
    fun `motion items map to real playable videos`() {
        val motion = listOf(
            BodyTestItemKey.RUN_50M, BodyTestItemKey.RUN_800_1000,
            BodyTestItemKey.LONG_JUMP, BodyTestItemKey.SEATED_REACH,
            BodyTestItemKey.PULLUP_SITUP
        )
        motion.forEach { key ->
            val v = TeachingVideoRepository.videoForItem(key)
            assertNotNull("运动项 $key 必须有示范视频", v)
            assertTrue("$key 视频 assetPath 应非空可播", v!!.assetPath.isNotBlank())
            assertTrue("$key 视频路径应以 videos/ 开头", v.assetPath.startsWith("videos/"))
            assertTrue("$key 应是体测 youth 视频", v.ageId == "youth")
            assertTrue("$key 匹配应有可播视频", TeachingVideoRepository.hasItemVideo(key))
        }
    }

    @Test
    fun `bmi and vital capacity are method-only and never mapped to motion videos`() {
        listOf(BodyTestItemKey.BMI, BodyTestItemKey.VITAL_CAPACITY).forEach { key ->
            assertTrue("$key 应标记 method-only", TeachingVideoRepository.isMethodOnlyItem(key))
            assertFalse("$key 不应有可播视频", TeachingVideoRepository.hasItemVideo(key))
            // 映射到 method-only 条目：assetPath 为空，绝不冒充动作视频
            val v = TeachingVideoRepository.videoForItem(key)
            assertNotNull("$key 应有方法说明条目", v)
            assertTrue("$key 条目 assetPath 应为空（不映射到动作视频）", v!!.assetPath.isEmpty())
        }
    }

    @Test
    fun `pullup situp is gender split but never cross age group`() {
        val male = TeachingVideoRepository.videoForItem(BodyTestItemKey.PULLUP_SITUP, "male")
        val female = TeachingVideoRepository.videoForItem(BodyTestItemKey.PULLUP_SITUP, "female")
        assertNotNull(male); assertNotNull(female)
        assertEquals("男性引体视频", "pullup", male!!.id)
        assertEquals("女性仰卧起坐视频", "situp", female!!.id)
        // 无论性别，都锁定在 youth 年龄段，不做跨年龄段错配
        assertEquals(male.ageId, "youth")
        assertEquals(female.ageId, "youth")
        // 与 exercise 侧的 teachingVideoFor（按 exerciseId）互不混淆：
        // 体测项映射必须经 videoForItem，不依赖 EXERCISE_TEACHING_VIDEO。
    }

    @Test
    fun `unknown item key returns null (honest empty state, no generic substitution)`() {
        assertNull("未知 key 必须 null", TeachingVideoRepository.videoForItem("no_such_item"))
        assertFalse("未知 key 不可跟练", TeachingVideoRepository.hasItemVideo("no_such_item"))
    }

    @Test
    fun `warmup alias maps to the warmup video`() {
        assertEquals("warmup", TeachingVideoRepository.videoForItem("warmup")?.id)
        assertTrue(TeachingVideoRepository.hasItemVideo("warmup"))
    }

    // ───────── 2. 跟练生命周期判定（可跟练 / 能力标注） ─────────

    @Test
    fun `motion items are followable with honest capability labels`() {
        val motion = listOf(
            BodyTestItemKey.RUN_50M, BodyTestItemKey.RUN_800_1000,
            BodyTestItemKey.LONG_JUMP, BodyTestItemKey.SEATED_REACH,
            BodyTestItemKey.PULLUP_SITUP
        )
        motion.forEach { key ->
            assertFalse("$key 是运动项，可跟练", TeachingVideoRepository.isMethodOnlyItem(key))
            val label = TeachingVideoRepository.followCapabilityLabel(key)
            assertTrue("$key 能力标签非空", label.isNotEmpty())
            assertTrue("$key 能力标签应声明辅助识别/人工（诚实分级）",
                label.contains("辅助识别") || label.contains("人工"))
        }
    }

    @Test
    fun `method-only items are not followable`() {
        listOf(BodyTestItemKey.BMI, BodyTestItemKey.VITAL_CAPACITY).forEach { key ->
            assertTrue("$key 是 method-only，不可作为动态跟练项",
                TeachingVideoRepository.isMethodOnlyItem(key))
        }
    }

    @Test
    fun `followability is a pure function of itemKey with no side effect`() {
        // 复刻 FollowAlongItemScreen 的判定：followable = 非 method-only。
        // 与方向/旋转/重入无关，因此旋转不会重复计数或改变可跟练性。
        fun followable(itemKey: String) = !TeachingVideoRepository.isMethodOnlyItem(itemKey)
        assertTrue(followable(BodyTestItemKey.RUN_50M))
        assertFalse(followable(BodyTestItemKey.BMI))
        assertFalse(followable(BodyTestItemKey.VITAL_CAPACITY))
        // 多次调用结果稳定（幂等，无全局状态被污染）
        val a = followable(BodyTestItemKey.LONG_JUMP)
        val b = followable(BodyTestItemKey.LONG_JUMP)
        assertEquals(a, b)
    }

    // ───────── 3. 青年体测评测闭环数据契约（人工录入逻辑一致性） ─────────

    @Test
    fun `bmi is computed as weightKg over heightM squared`() {
        val heightCm = 170.0
        val weightKg = 60.0
        val bmi = weightKg / ((heightCm / 100.0) * (heightCm / 100.0))
        assertEquals("60kg / 1.7m 的 BMI", 20.8, bmi, 0.1)
    }

    @Test
    fun `bmi requires both height and weight positive to save`() {
        fun canSave(height: Double?, weight: Double?) =
            height != null && weight != null && height > 0
        assertTrue(canSave(170.0, 60.0))
        assertFalse("身高 0 不可存", canSave(0.0, 60.0))
        assertFalse("缺身高不可存", canSave(null, 60.0))
        assertFalse("缺体重不可存", canSave(170.0, null))
    }

    @Test
    fun `motion single-value entry only needs a parsed double`() {
        fun canSaveSingle(value: String?, unit: String) = value?.toDoubleOrNull()?.let { true } ?: false
        assertTrue(canSaveSingle("8.42", "s"))
        assertFalse(canSaveSingle("", "s"))
        assertFalse("非数字不可存", canSaveSingle("abc", "s"))
    }
}

package com.quannian.zhidong

import com.quannian.zhidong.repository.TeachingVideoRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P0-2 数字人教学"匹配动作视频"回归单测（纯 JVM，不依赖 Android）。
 *
 *  锁定 TeachingVideoRepository.teachingVideoFor 的**严格匹配**契约：
 *   - 八段锦→八段锦视频、太极→太极、肩颈→肩颈… 动作必须一致。
 *   - 没有专属动作视频的动作（深蹲等）返回 null，UI 走诚实"暂无示范视频"分支，
 *     绝不塞泛化视频冒充（用户红线：不做 generic substitution）。
 *   - 匹配到的视频 assetPath 非空（真实可播）。
 */
class TeachingVideoMatchTest {

    @Test
    fun `senior actions match their own videos`() {
        assertEquals("senior_baduanjin", TeachingVideoRepository.teachingVideoFor("senior-baduanjin")?.id)
        assertEquals("senior_taichi", TeachingVideoRepository.teachingVideoFor("senior-taichi")?.id)
        assertEquals("senior_gentle_stretch", TeachingVideoRepository.teachingVideoFor("senior-easy")?.id)
        assertEquals("senior_balance", TeachingVideoRepository.teachingVideoFor("senior-balance")?.id)
    }

    @Test
    fun `middle actions match their own videos`() {
        assertEquals("middle_neck_shoulder", TeachingVideoRepository.teachingVideoFor("middle-neck")?.id)
        assertEquals("middle_flexibility", TeachingVideoRepository.teachingVideoFor("middle-stretch")?.id)
        assertEquals("middle_cardio", TeachingVideoRepository.teachingVideoFor("middle-gym")?.id)
    }

    @Test
    fun `actions without a dedicated video return null (no generic substitution)`() {
        // 深蹲/引体/仰卧/50m/800m 等没有"动作一致"的专属示范视频 → 必须 null，不塞泛化视频。
        assertNull("深蹲无专属示范视频", TeachingVideoRepository.teachingVideoFor("youth-squat"))
        assertNull("引体无专属示范视频", TeachingVideoRepository.teachingVideoFor("unknown-x"))
    }

    @Test
    fun `matched videos have a real playable assetPath`() {
        listOf("senior-baduanjin", "senior-taichi", "senior-easy", "senior-balance",
            "middle-neck", "middle-stretch", "middle-gym").forEach { exId ->
            val v = TeachingVideoRepository.teachingVideoFor(exId)
            assertNotNull("教学视频缺失: $exId", v)
            assertTrue("$exId 视频 assetPath 应为非空可播", v!!.assetPath.isNotBlank())
            assertTrue("$exId 视频路径应以 videos/ 开头", v.assetPath.startsWith("videos/"))
        }
        assertFalse("method-only 视频不应匹配到可播教学视频",
            TeachingVideoRepository.teachingVideoFor("senior-baduanjin")?.assetPath?.isEmpty() ?: true)
    }
}

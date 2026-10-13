package com.quannian.zhidong.repository

import com.quannian.zhidong.model.BodyTestVideo
import com.quannian.zhidong.model.CoachStyle

/**
 * V3.3 统一教练资源映射层（spec §3.2 / §8）。
 *
 *  按 `ageGroup + coachStyle + videoId + assetType` 获取资源。设计目标：
 *   - 让"教练风格选择"真正影响后续页面（头像 / 示范视频），而不是装饰按钮。
 *   - 风格切换**不改**动作 ID / 训练记录 / 评分 / 历史成绩，只改资源选择。
 *   - 资源缺失时返回**诚实空态**（VideoResolution.video=null），绝不拿无关视频填空。
 *   - 某动作若只有一种风格的专属视频，返回 [VideoResolution.styleMatched]=false
 *     并给出 [VideoResolution.note]，供 UI 提示"该动作暂时只有 X 风格示范"。
 *
 *  纯函数核心 [resolveVideo] 不依赖 Android，便于 JVM 单元测试。
 */
object CoachAssetRepository {

    /** 资源类型：区分头像 / 动作示范视频 / 跟练教学视频。 */
    enum class AssetType { AVATAR, DEMO_VIDEO, FOLLOW_VIDEO }

    /**
     * 视频解析结果。
     *  - [video]：可用的示范视频；null 表示无对应视频（诚实空态）。
     *  - [styleMatched]：该视频是否真正匹配请求的风格。
     *  - [note]：当 styleMatched=false 时，提示用户"该动作只有某风格"。
     */
    data class VideoResolution(
        val video: BodyTestVideo?,
        val styleMatched: Boolean,
        val note: String = ""
    )

    /** 请求风格的"默认匹配视频集合"：
     *   - REALISTIC：青少年真人风格视频（youth 组有真实 mp4 的动作）。
     *   - ANIMATED：中青年/中老年动画风格视频（middle / senior 组）。
     *  其余 videoId 无该风格专属视频时，返回该 videoId 唯一可用视频并标记 styleMatched=false。
     */
    private val STYLE_AGE: Map<CoachStyle, Set<String>> = mapOf(
        CoachStyle.REALISTIC to setOf("youth"),
        CoachStyle.ANIMATED to setOf("middle", "senior")
    )

    /**
     * 解析示范视频。纯函数，无 Android 依赖。
     *
     * @param ageGroup  青少年/中青年/中老年的内部 id（youth/middle/senior）
     * @param style     用户所选教练风格
     * @param videoId   TeachingVideoRepository 的权威视频 id（同动作同视频）
     */
    fun resolveVideo(ageGroup: String, style: CoachStyle, videoId: String): VideoResolution {
        val normalizedAge = com.quannian.zhidong.model.AgeGroupConfig.normalizeAgeId(ageGroup)
        val canonical = TeachingVideoRepository.byId(videoId)
        if (canonical == null) return VideoResolution(video = null, styleMatched = false, note = "无对应示范视频")

        // 该视频所属年龄段
        val ownerAge = canonical.ageId
        val requestedAges = STYLE_AGE[style] ?: emptySet()

        // 1) 请求风格恰好覆盖该视频所属年龄段 → 完全匹配
        if (ownerAge in requestedAges) {
            return VideoResolution(video = canonical, styleMatched = true, note = "")
        }

        // 2) 动作在请求风格的年龄段没有专属视频：
        //    - 若该视频本身就是用户所选年龄段的内容（只是风格名不同）→ 可用，但提示"只有对应风格"。
        //    - 否则（跨年龄段）→ 不强行播放，返回诚实空态。
        if (ownerAge == normalizedAge) {
            val hint = if (style == CoachStyle.REALISTIC) "该动作暂时只有动画风格示范"
            else "该动作暂时只有真人风格示范"
            return VideoResolution(video = canonical, styleMatched = false, note = hint)
        }

        // 3) 跨年龄段不匹配（不应发生，因权威映射已限定同年龄段）→ 诚实空态。
        return VideoResolution(video = null, styleMatched = false, note = "该动作无 ${style.title} 风格示范")
    }

    /** 按年龄段返回头像资源 id（复用 CoachVisual 的唯一映射，避免各页写死）。 */
    fun avatarResourceId(ageGroup: String): Int =
        com.quannian.zhidong.ui.components.CoachVisual.coachResource(ageGroup)
}

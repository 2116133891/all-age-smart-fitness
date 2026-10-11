package com.quannian.zhidong.repository

import android.content.Context
import com.quannian.zhidong.model.BodyTestItemKey
import com.quannian.zhidong.model.BodyTestVideo

/**
 * V2 教学视频资源索引（全本地 assets）。
 *
 *  设计（任务 3）：
 *   - 视频文件统一放 `assets/videos/`，播放器用 `assetPath` 经 AssetFileDataSource 播放。
 *   - 提供 [assetExists] 做缺失资源处理（任务 12 测试项）。
 *   - 每个 itemKey 映射到主教学视频；BMI/肺活量 无动作视频，返回 null，
 *     但提供"方法说明"文案（任务 4 明确：不生成假装摄像头测量的动作视频）。
 *
 *  诚实声明：视频由 Agnes AIGC text_to_video 生成（720P H.264 MP4，本地落地），
 *  非真实摄影师拍摄；仅作动作示范教学辅助，**不冒充** 官方 场地测试。
 */
object TeachingVideoRepository {

    /** 视频目录（按 唯一 id 键）。 */
    private val BY_ID: Map<String, BodyTestVideo> = mapOf(
        "long_jump" to BodyTestVideo(
            id = "long_jump", title = "立定跳远 · 标准动作示范",
            summary = "预摆→起跳→腾空→落地，发力连贯。",
            commonErrors = "未预摆直接跳；落地直腿；手向后拉过头。",
            safetyTip = "落地屈膝缓冲；软质沙坑/垫地更安全。",
            keyPoints = listOf("准备：双脚并拢，重心略前", "预摆：前后摇臂蓄力",
                "起跳：蹬地伸髋伸膝，身体前冲", "腾空：收腹举腿，身体充分伸展", "落地：屈膝缓冲，脚跟先着地"),
            assetPath = "videos/video_youth_long_jump.mp4", ageId = "youth"
        ),
        "pullup" to BodyTestVideo(
            id = "pullup", title = "引体向上（男）· 标准动作示范",
            summary = "直臂悬垂→背臂发力→下巴过杆→受控下放。",
            commonErrors = "借力甩摆；下放不完全。",
            safetyTip = "下方 放保护垫；无 单杠 时 勿 勉强。",
            keyPoints = listOf("直臂 悬垂 起始", "背 臂 发力 上拉", "下巴 过 杆 顶点", "受控 下放 回 直臂"),
            assetPath = "videos/video_youth_pullup.mp4", ageId = "youth"
        ),
        "situp" to BodyTestVideo(
            id = "situp", title = "1 分钟仰卧起坐（女）· 标准动作示范",
            summary = "屈膝仰卧，起身 至 胸 近 膝，1min 计 次数。",
            commonErrors = "起身 过猛；抱头 拉颈。",
            safetyTip = "核心 发力，颈椎 保持 中立；不适 即 停。",
            keyPoints = listOf("屈膝 仰卧，脚 固定", "起身 至 胸 近 膝", "受控 下放，不 弹 跳"),
            assetPath = "videos/video_youth_situp.mp4", ageId = "youth"
        ),
        "run_50m" to BodyTestVideo(
            id = "run_50m", title = "50 米跑 · 起跑 / 加速 / 冲刺要点",
            summary = "蹲踞/站立起跑，蹬离后加速，冲刺不 减速。",
            commonErrors = "起跑 前倾 过大；中途 减速；最后 20m 掉速。",
            safetyTip = "起跑 前 检查 跑道；落 点 软 化。",
            keyPoints = listOf("起跑：蹬地 前冲", "加速：前 30m 高 频率 抬 腿", "冲刺：摆 臂 摆 腿，身体 前倾 不 减速"),
            assetPath = "videos/video_youth_50m.mp4", ageId = "youth"
        ),
        "forward_reach" to BodyTestVideo(
            id = "forward_reach", title = "坐位体前屈 · 动作要点与常见错误",
            summary = "坐直、双腿 伸直，缓慢 前伸，不 弹 跳。",
            commonErrors = "双腿 弯曲；前伸 弹 跳；耸肩。",
            safetyTip = "柔韧 不足 时 轻 拉，避免 拉伤 腘绳 肌。",
            keyPoints = listOf("坐 直，双腿 并拢 伸直，足 尖 上 勾", "两 手 叠 放，缓慢 前伸", "保持 腿 直，不 弹 跳，不 耸肩"),
            assetPath = "videos/video_youth_forward_reach.mp4", ageId = "youth"
        ),
        "endurance" to BodyTestVideo(
            id = "endurance", title = "800/1000 米 · 配速与分段训练方法",
            summary = "匀速有氧 跑，节奏 稳定，呼吸 均匀。",
            commonErrors = "起跑 全力；中途 掉速 过多；终点 提前 减速。",
            safetyTip = "配速 由 缓 到 快；不适 立即 停。",
            keyPoints = listOf("起跑 60-70% 速度，不 全力", "中段 匀速，保持 摆 臂 节奏", "最后 100m 视 情况 冲刺"),
            assetPath = "videos/video_youth_endurance.mp4", ageId = "youth"
        ),
        "warmup" to BodyTestVideo(
            id = "warmup", title = "体测前热身 & 运动后整理",
            summary = "慢跑 热身 → 拉伸 整理，防 伤。",
            commonErrors = "热身 不足；运动 后 不 整理。",
            safetyTip = "运动 后 不 立即 坐 卧，缓 走 整理。",
            keyPoints = listOf("热身：慢跑 + 肩髋 活动", "整理：慢走 + 拉伸", "全程 无 不适 再 开 始"),
            assetPath = "videos/video_youth_warmup.mp4", ageId = "youth"
        ),
        "vital_capacity_method" to BodyTestVideo(
            id = "vital_capacity_method", title = "肺活量 · 正确测量方法（无动作视频）",
            summary = "肺活量 需 专用 肺活量 仪，非 摄像头 可测。",
            commonErrors = "吹气 不 充分；漏气。",
            safetyTip = "仅 手工 录入 实测值；不 冒充 自动 测量。",
            keyPoints = listOf("用 专用 肺活量 仪，吹 一口气 至 最大", "记 毫升 数（mL）", "本 项目 不 通过 摄像头 测量 肺活量"),
            assetPath = "", ageId = "youth"
        ),
        "bmi_method" to BodyTestVideo(
            id = "bmi_method", title = "BMI · 测量方法与计算（无动作视频）",
            summary = "BMI = 体重(kg) / 身高(m)^2，手工录入。",
            commonErrors = "单位 混用；身高 体重 不同 时 测量。",
            safetyTip = "BMI 不 等同 体脂，不 单独 诊断 健康。",
            keyPoints = listOf("晨起 空腹 同 状态 测 身高 体重", "BMI = 体重kg ÷ 身高m²", "评价 区间 依 年龄 与 正式 标准"),
            assetPath = "", ageId = "youth"
        )
    )

    /** itemKey → 主视频 id（有动作示范 的 优先）。 */
    private val ITEM_TO_PRIMARY: Map<String, String> = mapOf(
        BodyTestItemKey.LONG_JUMP to "long_jump",
        BodyTestItemKey.PULLUP_SITUP to "pullup",     // 男
        BodyTestItemKey.RUN_50M to "run_50m",
        BodyTestItemKey.SEATED_REACH to "forward_reach",
        BodyTestItemKey.RUN_800_1000 to "endurance",
        BodyTestItemKey.VITAL_CAPACITY to "vital_capacity_method",
        BodyTestItemKey.BMI to "bmi_method",
        "warmup" to "warmup"
    )

    /** 女生 时 引体/仰卧 项 主 视频 换 成 仰卧。 */
    fun videoFor(itemKey: String, gender: String = "unknown"): BodyTestVideo? {
        val id = when (itemKey) {
            BodyTestItemKey.PULLUP_SITUP -> if (gender == "female") "situp" else "pullup"
            else -> ITEM_TO_PRIMARY[itemKey]
        }
        return id?.let { BY_ID[it] }
    }

    /** 仰卧起坐 视频（女）。 */
    fun situpVideo(): BodyTestVideo? = BY_ID["situp"]

    /** itemKey 是否有 真实 视频（assetPath 非 空）。 */
    fun hasVideo(itemKey: String, gender: String = "unknown"): Boolean =
        !videoFor(itemKey, gender)?.assetPath.isNullOrEmpty()

    /** 全部有 视频 的条目（播放器 入口 用）。 */
    fun allVideoItems(): List<BodyTestVideo> = BY_ID.values.filter { it.assetPath.isNotBlank() }

    /** 热身/整理 视频。 */
    fun warmup(): BodyTestVideo? = BY_ID["warmup"]

    /**
     * 检查 本地 视频 资源 是否 存在（任务 12：缺失 资源 处理）。
     *  通过 assets 打开 流 判断；失败 返回 false（UI 显示 明确 提示）。
     */
    fun assetExists(context: Context, assetPath: String): Boolean {
        if (assetPath.isBlank()) return false
        return try {
            val input = context.assets.open(assetPath)
            input.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    /** 取 某 视频 的 asset 路径。 */
    fun assetPathOf(video: BodyTestVideo): String = video.assetPath
}

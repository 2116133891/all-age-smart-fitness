package com.quannian.zhidong.repository

import android.content.Context
import com.quannian.zhidong.model.BodyTestItemKey
import com.quannian.zhidong.model.BodyTestVideo

/**
 * V2/V3 教学视频资源索引（全本地 assets）。
 *
 *  设计（任务 3 / 任务 9）：
 *   - 视频文件统一放 `assets/videos/`，播放器用 [BodyTestVideo.assetPath] 经 AssetFileDataSource 播放。
 *   - 提供 [assetExists] 做缺失资源处理（任务 12 测试项）。
 *   - 每个 itemKey 映射到主教学视频；BMI/肺活量 无动作视频，返回方法说明型条目，
 *     但提供"方法说明"文案（任务 4 明确：不生成假装摄像头测量的动作视频）。
 *   - V3（任务 9）：扩展到 18 条基础集（youth 10 + middle 4 + senior 4）。
 *     其中 7 条 youth 已有真实 mp4；其余 11 条 assetPath=""（pending，
 *     AIGC 生成后填入 `assets/videos/`，VIDEO_ASSET_MANIFEST.md 追踪状态）。
 *     [allVideoItems] 过滤 assetPath 非空；[allItems] 返回全部 18 条；
 *     [byAgeId] 按年龄段归组（推荐/列表用）。
 *
 *  诚实声明：视频由 Agnes AIGC text_to_video 生成（720P H.264 MP4，本地落地），
 *  非真实摄影师拍摄；仅作动作示范教学辅助，**不冒充** 官方 场地测试。
 */
object TeachingVideoRepository {

    /** 视频目录（按 唯一 id 键）。共 18 条：youth 10 / middle 4 / senior 4。 */
    private val BY_ID: Map<String, BodyTestVideo> = mapOf(
        // ── 已有真实 mp4（7 条，youth）──
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
        ),
        // ── V3 扩展（任务 9）：youth +1，middle +4，senior +4；合计补齐到 18 条基础集 ──
        // 现有 7 条 youth 有真实 mp4；本段新增 youth 1 + middle 4 + senior 4，
        // 其余 youth 方法说明型（bmi/vital_capacity）保留。assetPath="" 表示视频文件
        // pending（AIGC 生成后填入 assets/videos/，VIDEO_ASSET_MANIFEST.md 追踪）。
        // allVideoItems() 自动过滤空 assetPath；UI 空态不崩。
        "youth_plank" to BodyTestVideo(
            id = "youth_plank", title = "平板支撑 · 核心稳定示范（青年）",
            summary = "俯身平板，肘在肩下，躯干成直线，保持 30s。",
            commonErrors = "塌腰；臀部过高；肘超出肩线。",
            safetyTip = "核心收紧，不适立即停止。",
            keyPoints = listOf("前臂撑地，肘在肩正下方", "双脚并拢，脚尖内扣",
                "躯干保持一条直线，不塌腰", "保持 30 秒，呼吸均匀"),
            assetPath = "videos/video_youth_plank.mp4", ageId = "youth"
        ),
        "middle_core" to BodyTestVideo(
            id = "middle_core", title = "中年核心强化 · 下班后 15 分钟",
            summary = "死虫 + 鸟狗 + 平板，三式循环，强化核心稳定。",
            commonErrors = "腰部塌下沉；动作过快；憋气。",
            safetyTip = "有腰伤者先咨询医生；动作从轻缓开始。",
            keyPoints = listOf("死虫：仰卧，对侧手脚交替伸出，核心收紧",
                "鸟狗：四点支撑，对侧手脚同时伸出",
                "平板：30s × 2 组，组间休息 20s"),
            assetPath = "videos/video_middle_core.mp4", ageId = "middle"
        ),
        "middle_flexibility" to BodyTestVideo(
            id = "middle_flexibility", title = "中年柔韧性恢复 · 坐姿拉伸",
            summary = "坐姿前屈 + 坐姿扭转 + 侧弯，每次 3 分钟。",
            commonErrors = "起身过猛；扭转过幅；憋气。",
            safetyTip = "腰椎不适者避免大幅度扭转；有伤先咨询。",
            keyPoints = listOf("坐姿双腿伸直，缓慢前屈保持 30s",
                "坐姿扭转，双手抱肩，保持 30s",
                "侧弯：右手举过头顶，身体侧弯"),
            assetPath = "videos/video_middle_flexibility.mp4", ageId = "middle"
        ),
        "middle_cardio" to BodyTestVideo(
            id = "middle_cardio", title = "中年有氧减脂 · 间歇走跑",
            summary = "3min 快走 + 1min 慢跑 × 5 轮，心率 60-70% 最大。",
            commonErrors = "快起步太猛；间歇休息过长；不监测心率。",
            safetyTip = "有心血管疾病先咨询；胸闷立即停。",
            keyPoints = listOf("快走 3min → 慢跑 1min → 休息 30s",
                "循环 5 轮", "用运动手表 / 手环监测心率"),
            assetPath = "videos/video_middle_cardio.mp4", ageId = "middle"
        ),
        "middle_neck_shoulder" to BodyTestVideo(
            id = "middle_neck_shoulder", title = "肩颈放松 · 工位 5 分钟版（中年）",
            summary = "头颈侧倾 + 肩绕 + 上拉 + 后收，工位即做。",
            commonErrors = "甩头；动作过快；耸肩。",
            safetyTip = "落枕急性期避免大幅度；每天 1 次比猛练好。",
            keyPoints = listOf("头向一侧倾斜保持 15s，换边",
                "肩画圈 10 圈，正反向各 10 圈",
                "双手上拉保持 10s，后收下巴 10 次"),
            assetPath = "videos/video_middle_neck_shoulder.mp4", ageId = "middle"
        ),
        "senior_baduanjin" to BodyTestVideo(
            id = "senior_baduanjin", title = "八段锦 · 第一至三式示范（银龄）",
            summary = "两脚开立，缓做三式，配合呼吸，舒展经络。",
            commonErrors = "动作过快；呼吸不配合；重心不稳。",
            safetyTip = "有基础病量力而行；头晕立即停。",
            keyPoints = listOf("两脚开立与肩同宽，含胸拔背",
                "第一式：双手托天理三焦",
                "第二式：左右开弓似射雕",
                "第三式：调理脾胃须单举"),
            assetPath = "videos/video_senior_baduanjin.mp4", ageId = "senior"
        ),
        "senior_taichi" to BodyTestVideo(
            id = "senior_taichi", title = "太极 · 起势与云手示范（银龄）",
            summary = "松肩沉肘，重心平稳，动作连绵，养心调息。",
            commonErrors = "重心起伏大；动作突然；呼吸不配合。",
            safetyTip = "平衡不佳者扶椅；头晕立即停。",
            keyPoints = listOf("起势：双脚与肩同宽，缓慢下蹲",
                "云手：重心转移，手臂划圆",
                "全程保持松沉，呼吸自然"),
            assetPath = "videos/video_senior_taichi.mp4", ageId = "senior"
        ),
        "senior_balance" to BodyTestVideo(
            id = "senior_balance", title = "平衡防跌 · 串联走与单腿站（银龄）",
            summary = "脚跟脚尖串联走 10 步 + 扶物单腿站 10s × 2。",
            commonErrors = "走路太快；单腿站时间过长；不扶物。",
            safetyTip = "全程在硬地板或垫子上；扶物更安全。",
            keyPoints = listOf("脚跟顶脚尖走 10 步，扶椅",
                "单腿站保持 10s，换腿",
                "闭眼单腿站 5s（可扶物）"),
            assetPath = "videos/video_senior_balance.mp4", ageId = "senior"
        ),
        "senior_gentle_stretch" to BodyTestVideo(
            id = "senior_gentle_stretch", title = "银龄舒缓拉伸 · 4 分钟全身活动",
            summary = "头、肩、腰、腿依次缓慢活动，各 30s，配合呼吸。",
            commonErrors = "动作过快；幅度过大；憋气。",
            safetyTip = "关节不适者减小幅度；保持呼吸。",
            keyPoints = listOf("头缓慢旋转 5 圈 + 侧倾 15s",
                "肩部画圈 10 圈",
                "站立体前屈保持 30s，缓慢起身"),
            assetPath = "videos/video_senior_gentle_stretch.mp4", ageId = "senior"
        )
    )

    /**
     * 教学页（数字人教学）exerciseId → 匹配动作的视频 id（P0-2：替代旧火柴人示范）。
     *
     *  严格匹配原则（用户要求：八段锦→八段锦视频、太极→太极，不做泛化替代）：
     *  只把"视频动作 == 该 exercise 动作"的映射写进来；没有专属动作视频
     *  的 exercise 不强行塞一个泛化视频，而是走诚实"暂无该动作示范视频"卡片。
     */
    private val EXERCISE_TEACHING_VIDEO: Map<String, String> = mapOf(
        // 银龄（4 条全匹配）
        "senior-baduanjin" to "senior_baduanjin",
        "senior-taichi" to "senior_taichi",
        "senior-easy" to "senior_gentle_stretch",
        "senior-balance" to "senior_balance",
        // 中年
        "middle-neck" to "middle_neck_shoulder",   // 肩颈缓解 → 肩颈放松（动作一致）
        "middle-stretch" to "middle_flexibility",   // 久坐恢复（坐姿拉伸）→ 柔韧恢复
        "middle-gym" to "middle_cardio",            // 下班力量·燃脂塑力 → 间歇走跑燃脂
        // 青年（仅瑜伽与柔韧视频动作一致，匹配；深蹲/肩颈无专属视频 → 走诚实卡片）
        "youth-yoga" to "forward_reach"             // 瑜伽（柔韧）→ 坐位体前屈（柔韧）
    )

    /**
     * 教学页按 exerciseId 取"匹配动作"的真实示范视频（P0-2）。
     *  无匹配（该动作没有专属视频）返回 null，UI 走诚实"暂无示范视频"分支，
     *  不用泛化视频冒充（用户红线：不 generic substitution）。
     */
    fun teachingVideoFor(exerciseId: String): BodyTestVideo? =
        EXERCISE_TEACHING_VIDEO[exerciseId]?.let { BY_ID[it] }

    // ======================================================================
    // V3.3：青年体测 项目项 itemKey → 教学/示范视频 的**严格**统一映射（唯一权威来源）。
    //  红线：BMI/肺活量 是方法型项目（无动作视频），绝不映射到深蹲/跳远等无关动作视频；
    //       训练详情 / 跟练 / 备考计划 全应用共用这一套映射。
    // ======================================================================

    /** 是否"仅方法说明、无可播视频"的项目（BMI / 肺活量）。这类项目不进入动态跟练列表。 */
    fun isMethodOnlyItem(itemKey: String): Boolean =
        itemKey == BodyTestItemKey.BMI || itemKey == BodyTestItemKey.VITAL_CAPACITY

    /**
     * 体测项目 itemKey（+ 性别）→ 匹配的教学/示范视频（P0 统一映射）。
     *  - 运动型项目 → 真实动作视频；BMI / 肺活量 → method-only 条目（assetPath 为空）。
     *  - 未知 key → null（UI 走诚实空态，不塞泛化视频）。
     */
    fun videoForItem(itemKey: String, gender: String = "unknown"): BodyTestVideo? =
        when (itemKey) {
            BodyTestItemKey.RUN_50M -> BY_ID["run_50m"]
            BodyTestItemKey.RUN_800_1000 -> BY_ID["endurance"]
            BodyTestItemKey.LONG_JUMP -> BY_ID["long_jump"]
            BodyTestItemKey.SEATED_REACH -> BY_ID["forward_reach"]
            BodyTestItemKey.PULLUP_SITUP -> if (gender == "female") BY_ID["situp"] else BY_ID["pullup"]
            BodyTestItemKey.BMI -> BY_ID["bmi_method"]                // method-only（无视频）
            BodyTestItemKey.VITAL_CAPACITY -> BY_ID["vital_capacity_method"] // method-only（无视频）
            "warmup" -> BY_ID["warmup"]
            else -> null
        }

    /** 该方法型项目是否有可播视频（BMI/肺活量 → false）。 */
    fun hasItemVideo(itemKey: String, gender: String = "unknown"): Boolean =
        !videoForItem(itemKey, gender)?.assetPath.isNullOrEmpty()

    /**
     * 跟练能力标注（诚实分级，不把人工成绩包装成"AI 自动检测"）：
     *  运动测量项没有专属可靠自动计数/测距算法 → 一律"辅助识别 + 人工"。
     */
    fun followCapabilityLabel(itemKey: String): String =
        when (itemKey) {
            BodyTestItemKey.RUN_50M -> "辅助识别 · 人工计时"
            BodyTestItemKey.RUN_800_1000 -> "辅助识别 · 人工计时"
            BodyTestItemKey.LONG_JUMP -> "辅助识别 · 人工测量距离"
            BodyTestItemKey.SEATED_REACH -> "辅助识别 · 人工测量"
            BodyTestItemKey.PULLUP_SITUP -> "辅助识别 · 人工计数"
            else -> "辅助识别"
        }

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

    /** 直接按 id 取（用于 18 条全量索引 / 清单）。 */
    fun byId(id: String): BodyTestVideo? = BY_ID[id]

    /** 仰卧起坐 视频（女）。 */
    fun situpVideo(): BodyTestVideo? = BY_ID["situp"]

    /** itemKey 是否有 真实 视频（assetPath 非 空）。 */
    fun hasVideo(itemKey: String, gender: String = "unknown"): Boolean =
        !videoFor(itemKey, gender)?.assetPath.isNullOrEmpty()

    /** 全部有 视频 的条目（播放器 入口 用；自动 过滤 assetPath 为 空 的 pending）。 */
    fun allVideoItems(): List<BodyTestVideo> = BY_ID.values.filter { it.assetPath.isNotBlank() }

    /** 全部 18 条（含 方法 说明 与 pending 视频，供 清单 / 后台 管理 用）。 */
    fun allItems(): List<BodyTestVideo> = BY_ID.values.toList()

    /** 按 年龄段 归组（youth / middle / senior），供 推荐 列表 与 视频 中心 用。 */
    fun byAgeId(ageId: String): List<BodyTestVideo> = BY_ID.values.filter { it.ageId == ageId }

    /** 某 年龄段 中 已 有 真实 视频 的 条目。 */
    fun videoItemsForAge(ageId: String): List<BodyTestVideo> =
        byAgeId(ageId).filter { it.assetPath.isNotBlank() }

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

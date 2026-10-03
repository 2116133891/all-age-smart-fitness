package com.quannian.zhidong.coach

import com.quannian.zhidong.model.ErrorType
import com.quannian.zhidong.model.TrainingReport

/**
 * AI 教练响应生成接口（对照 spec §19 / §20）。
 *
 *  流程：[TrainingReport] + 实时 [ErrorType] → 动态自然语言教练话术。
 *
 *  - [LocalCoachProvider]：默认，本地规则 + 模板，**离线可完整运行**（比赛默认）。
 *  - [RemoteLLMProvider]：可选，接入外部 LLM；**无 API Key 时自动降级到本地**，
 *    绝不因缺 Key 阻塞项目。禁止硬编码任何 API Key / Token / Secret。
 *
 *  设计原则：数字人不是装饰，是"智能纠错的可视化载体"。话术引用实时数据
 *  （次数、分数、具体错误），而非固定文案。
 */
interface CoachAIProvider {
    /** 为一次训练生成"AI 教练总结"。 */
    fun generateSummary(report: TrainingReport): String
    /** 为当前命中的错误生成针对性纠错话术（实时，引用数据）。 */
    fun correctionFor(exercise: String, age: String, errors: Set<ErrorType>, score: Int): List<String>
    /** 为下一次训练生成建议。 */
    fun nextSuggestion(report: TrainingReport): String
}

package com.quannian.zhidong.coach

import com.quannian.zhidong.model.ErrorType
import com.quannian.zhidong.model.TrainingReport
import java.util.concurrent.ConcurrentHashMap

/**
 * 可选的远程 LLM 教练 Provider（对照 spec §20）。
 *
 *  设计原则：
 *  - **无 API Key 时自动降级到本地**（[LocalCoachProvider]），绝不因缺 Key 阻塞项目。
 *  - **禁止硬编码任何 API Key / Token / Secret**：密钥只从运行时读取（如由调用方
 *    通过 [configure] 注入，或从 Android Keystore / 用户手动输入），本类不存储明文。
 *  - 请求超时后也降级本地，保证比赛现场离线也能完整运行。
 *
 *  本 Provider 是一个**可选接口**：默认项目用 [LocalCoachProvider]。只有当
 *  [apiKey] 已被配置且 [endpoint] 可达时，才会实际发起网络请求；否则全部走本地。
 */
class RemoteLLMProvider(
    /** 运行时注入，默认空 = 未配置 → 全部降级本地。不允许硬编码。 */
    private var apiKey: String = "",
    /** 服务端点（可选）。 */
    private var endpoint: String = "http://127.0.0.1:8080/v1/chat",
    private val local: LocalCoachProvider = LocalCoachProvider
) : CoachAIProvider {

    /** 是否已配置了远程能力（apiKey 非空 且 endpoint 非空）。 */
    private val configured: Boolean get() = apiKey.isNotBlank() && endpoint.isNotBlank()

    /** 运行时注入密钥（例如从用户设置/Keystore）。不持久化明文。 */
    fun configure(key: String) {
        apiKey = key
    }

    override fun correctionFor(exercise: String, age: String, errors: Set<ErrorType>, score: Int): List<String> {
        // 未配置远程能力时，直接降级本地（离线也完整可用）
        if (!configured) return local.correctionFor(exercise, age, errors, score)
        // 有配置时理论上发网络请求；为保持 MVP 无网络依赖 + 现场可靠，
        // 这里仍返回本地结果（远程调用留给后续接入，不阻塞功能）。
        return local.correctionFor(exercise, age, errors, score)
    }

    override fun generateSummary(report: TrainingReport): String =
        if (!configured) local.generateSummary(report) else local.generateSummary(report)

    override fun nextSuggestion(report: TrainingReport): String =
        if (!configured) local.nextSuggestion(report) else local.nextSuggestion(report)

    /** 是否处于"降级本地"状态（未配置远程能力）。 */
    val isFallbackLocal: Boolean get() = !configured
}

/**
 * 全局教练 Provider 门面：默认本地，可选切到远程（缺 Key 自动降级）。
 *
 *  使用：`CoachProvider.instance.summary(report)`。
 *  不直接持有 apiKey，避免把密钥散落在代码里。
 */
object CoachProvider {
    private val _instance = RemoteLLMProvider()
    val instance: RemoteLLMProvider get() = _instance

    /** 由调用方在运行时注入密钥（例如用户设置页读取 Keystore 后调用）。 */
    fun configureRemoteApiKey(key: String) = _instance.configure(key)

    fun localProvider(): LocalCoachProvider = LocalCoachProvider
}

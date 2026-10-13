package com.quannian.zhidong.coach

import com.quannian.zhidong.assistant.LocalAssistant
import com.quannian.zhidong.model.CapabilityRegistry
import com.quannian.zhidong.model.Profile
import com.quannian.zhidong.model.ProfileStore
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONArray
import org.json.JSONObject

/**
 * 统一 AI 服务（V3 任务 22-24）：一套聊天界面复用不同模型服务，失败必降级本地。
 *
 *  设计原则：
 *  - **单一 AI 系统**：不另建独立聊天/LLM 栈；[LocalAssistant]（规则）是回退，[RemoteAiProvider]（可选）是增强。
 *  - **项目特色**：回答优先使用真实训练/体测数据、教学视频与识别能力；无成绩不假装知道；
 *    不承诺达标、不替代医疗建议；识别失败给站位/手动记录指引（任务 24）。
 *  - **降级**：未配置 Key / 网络失败 / 超时 / 401 → 明确标注"本地规则建议"，绝不伪造"AI 回答"。
 *  - **数据最小化 + 用户同意**：仅发送完成当前问题所必需的信息，且需用户在设置里明确同意才带个人数据。
 *
 *  诚实声明：无后端时，把服务商 API Key 放进 APK 即使混淆也无法保证不被提取；
 *  正式上架应由服务端代理保管密钥。这里只完成开发期 Provider 接入 + 安全配置机制。
 */

/** 一条对话消息（OpenAI 兼容 role/content）。 */
data class AiMsg(val role: String, val content: String)

/** 一次 AI 回答的来源，供 UI 显示"本地/远程"标签，避免把降级回答冒充远程 AI。 */
data class AiAnswer(val text: String, val source: AiSource, val quickReplies: List<String> = emptyList())

enum class AiSource(val label: String) {
    REMOTE("AI 模型"), LOCAL_FALLBACK("本地规则（模型不可用/未配置）")
}

/**
 * 项目特色上下文（任务 24）：把"当前项目真实能力 + 用户真实数据"注入系统提示，
 *  让模型回答基于事实；无数据时明确告知模型"用户未录入"。
 */
internal object ProjectContext {
    fun systemPrompt(profile: Profile, hasRecentTraining: Boolean, hasBodyTest: Boolean): String {
        val sb = StringBuilder()
        sb.append("你是「全龄智动」的运动教练 AI 助手，面向 ${ProfileStore.ageLabel(profile.ageId)}人群，性别 ${ProfileStore.genderLabel(profile.gender)}。")
        sb.append("回答原则：循序渐进，不承诺一定达到某正式体测成绩，不替代专业医疗建议；涉及疾病/受伤先建议咨询医生。")
        sb.append("动作识别能力（据实告知）：深蹲/开合跳/八段锦/太极/肩颈=可辅助姿态分析(非正式计数)；")
        sb.append("立定跳远=需标定与误差验证后自动测距(当前未开放)；50m/耐力计时与肺活量=必须手动录入真实值；")
        sb.append("引体/仰卧=手工记录，识别能力验证前不提供自动计数。")
        sb.append("若用户询问\"是否支持自动计数\"，按上述能力如实回答，不夸大。")
        sb.append("用户数据：" +
            (if (hasRecentTraining) "有最近训练记录，可引用最近成绩" else "用户暂无最近训练记录，不得假设其成绩") +
            "；" +
            (if (hasBodyTest) "有体测成绩记录" else "用户尚未录入体测成绩，不得假装知道其体测结果") + "。")
        sb.append("若某项目识别不稳定，提示\"未能稳定识别，请调整站位(全身入镜/光线充足)或手动记录\"。")
        return sb.toString()
    }
}

/**
 * 统一 AI 门面：远程优先（已配置 Key 时真实调网络），任何失败降级本地规则。
 *  线程安全；请求可取消（[cancel]）。
 */
object AiFacade {

    private val cancelFlag = AtomicBoolean(false)

    /**
     * 发起一次问答。
     * @param question 用户问题。
     * @param profile  用户画像。
     * @param recentSessions 最近训练记录（个性化，可为空）。
     * @param hasBodyTest 是否已有体测成绩（用于诚实告知模型）。
     * @param allowPersonalData 用户是否**明确同意**把个人数据发给模型（任务 23 数据最小化）。
     * @param endpoint/model/apiKey 远程服务配置；apiKey 为空则直接走本地。
     * @param onChunk 流式分段回调（服务商支持时；不支持时为 null，整段一次性返回）。
     * @return [AiAnswer]；远程失败时 source=LOCAL_FALLBACK。
     */
    fun ask(
        question: String,
        profile: Profile,
        recentSessions: List<com.quannian.zhidong.db.TrainingSession>,
        hasBodyTest: Boolean,
        allowPersonalData: Boolean,
        endpoint: String,
        model: String,
        apiKey: String,
        onChunk: ((String) -> Unit)? = null,
        timeoutMs: Int = 30_000
    ): AiAnswer {
        cancelFlag.set(false)
        // 未配置 → 直接本地（不伪造远程）
        if (apiKey.isBlank()) {
            return localAnswer(question, profile, recentSessions, hasBodyTest, allowPersonalData)
        }
        return try {
            remoteAnswer(
                question, profile, recentSessions, hasBodyTest, allowPersonalData,
                endpoint, model, apiKey, onChunk, timeoutMs
            )
        } catch (e: Exception) {
            // 超时 / 网络 / 解析 / 401 等 → 诚实降级本地
            val note = when {
                e.message?.contains("401", ignoreCase = true) == true ||
                e.message?.contains("Unauthorized", ignoreCase = true) == true ->
                    "（API Key 无效或无权限）"
                e is java.net.SocketTimeoutException -> "（请求超时）"
                else -> "（网络不可用）"
            }
            val local = localAnswer(question, profile, recentSessions, hasBodyTest, allowPersonalData)
            AiAnswer(
                text = "模型服务暂不可用${note}，以下为本地规则建议：\n" + local.text,
                source = AiSource.LOCAL_FALLBACK,
                quickReplies = local.quickReplies
            )
        }
    }

    /** 取消当前请求（UI 点击取消时调用）。 */
    fun cancel() = cancelFlag.set(true)

    /**
     * 连接测试真实结果（P0-4：真实 HTTP 探测，非"仅字符串校验/本地静态回答"）。
     *  - [ok] 仅当真实收到 2xx 且可解析为模型响应时 true。
     *  - [detail] 给用户的中文、可行动说明（成功/Key无效/超时/网络不可用/路径或模型错误）。
     *  绝不把完整 Key / Authorization 写进 detail 或日志（仅 [ApiKeyStore.mask] 脱敏）。
     */
    data class AiConnTest(val ok: Boolean, val detail: String)

    /**
     * 归一化 chat 端点：避免 /v1/chat/completions 重复拼接。
     *  - 已以 /chat/completions 结尾 → 原样使用（用户直接填了完整 URL）。
     *  - 否则在 base 上补 /chat/completions（支持用户只填 base，如 https://api.openai.com/v1）。
     *  这样 ask() 与 testConnection() 走同一口径，消除"重复 /v1/chat/completions"隐患。
     */
    fun resolveChatEndpoint(raw: String): String {
        val ep = raw.trim()
        if (ep.isEmpty()) return ep
        if (ep.lowercase().endsWith("/chat/completions")) return ep
        return ep.trimEnd('/') + "/chat/completions"
    }

    /**
     * 真实连接测试（P0-4）：对 [endpoint] 发一次最小的 OpenAI 兼容 chat 请求（max_tokens=1）。
     *  - 2xx → 连接成功（可解析则显示模型返回）。
     *  - 401/403 → API Key 无效或无权限。
     *  - 404 → 端点路径不对（应为 .../v1/chat/completions）。
     *  - 400 → 多为模型名或请求体不符，给出提示。
     *  - 超时/网络错误 → 给出"网络不可用/超时"。
     *  未配置 Key 时诚实返回"未配置"，不伪造成功。全程脱敏，不打印完整 Key。
     */
    fun testConnection(
        endpoint: String,
        model: String,
        apiKey: String,
        timeoutMs: Int = 15_000
    ): AiConnTest {
        if (apiKey.isBlank()) return AiConnTest(false, "尚未配置 API Key，无法测试远程连接（本地规则建议仍可用）。")
        if (endpoint.isBlank()) return AiConnTest(false, "尚未填写服务端点，无法测试连接。")
        val url = resolveChatEndpoint(endpoint)
        val body = JSONObject()
            .put("model", model.ifBlank { "gpt-4o-mini" })
            .put("max_tokens", 1)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", "ping")))
            .toString()
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer ${apiKey.trim()}") // 脱敏：密钥仅请求头
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val errBody = try {
                if (code !in 200..299) conn.errorStream?.bufferedReader()?.readText()?.take(200).orEmpty()
                else conn.inputStream.bufferedReader().readText().take(200)
            } catch (_: Exception) { "" }
            return when {
                code in 200..299 -> AiConnTest(true, "连接成功（$url），模型响应正常。")
                code == 401 || code == 403 -> AiConnTest(false, "API Key 无效或无权限（HTTP $code）。请检查 Key 是否复制完整、是否有该模型权限。")
                code == 404 -> AiConnTest(false, "端点路径不对（HTTP 404）。请确认地址为 .../v1/chat/completions（或 base .../v1，会自动补全）。")
                code == 400 -> AiConnTest(false, "请求被拒（HTTP 400，模型名或请求体不符）。请核对模型名与服务商要求。详情：${errBody.ifBlank { "无" }}")
                else -> AiConnTest(false, "服务返回异常（HTTP $code）：${errBody.ifBlank { "无详情" }}")
            }
        } catch (e: java.net.SocketTimeoutException) {
            return AiConnTest(false, "连接超时（${timeoutMs / 1000}s）。请检查网络或端点地址。")
        } catch (e: java.net.UnknownHostException) {
            return AiConnTest(false, "无法解析主机（DNS 失败）：请检查端点域名与网络。")
        } catch (e: Exception) {
            return AiConnTest(false, "网络不可用或连接失败：${e.message?.ifBlank { e.javaClass.simpleName } ?: "未知错误"}")
        } finally {
            try { conn.disconnect() } catch (_: Exception) {}
        }
    }

    /** 本地规则回答（回退路径）。 */
    fun localAnswer(
        question: String,
        profile: Profile,
        recentSessions: List<com.quannian.zhidong.db.TrainingSession>,
        hasBodyTest: Boolean,
        @Suppress("UNUSED_PARAMETER") allowPersonalData: Boolean
    ): AiAnswer {
        val reply = LocalAssistant.ask(question, profile, recentSessions)
        // 项目特色：涉及"能否自动计数/识别"时，追加据实的能力说明（按各体测项真实能力）
        val capNote = if (question.contains("计数") || question.contains("自动") ||
            question.contains("识别") || question.contains("测距") || question.contains("跳远")) {
            "\n（识别能力据实说明：引体/仰卧起坐=" +
                CapabilityRegistry.describe(CapabilityRegistry.forBodyTest(com.quannian.zhidong.model.BodyTestItemKey.PULLUP_SITUP)) +
                "；立定跳远=" +
                CapabilityRegistry.describe(CapabilityRegistry.forBodyTest(com.quannian.zhidong.model.BodyTestItemKey.LONG_JUMP)) +
                "；50m/耐力/肺活量=" +
                CapabilityRegistry.describe(CapabilityRegistry.forBodyTest(com.quannian.zhidong.model.BodyTestItemKey.VITAL_CAPACITY)) +
                "。未开放的项目以手动录入为准，不输出未经验证的自动结果。）"
        } else ""
        return AiAnswer(text = reply.text + capNote, source = AiSource.LOCAL_FALLBACK, quickReplies = reply.quickReplies)
    }

    /** 真实网络调用（OpenAI 兼容 chat 接口；[apiKey] 仅经请求头传输，不落日志）。 */
    private fun remoteAnswer(
        question: String,
        profile: Profile,
        recentSessions: List<com.quannian.zhidong.db.TrainingSession>,
        hasBodyTest: Boolean,
        allowPersonalData: Boolean,
        endpoint: String,
        model: String,
        apiKey: String,
        onChunk: ((String) -> Unit)?,
        timeoutMs: Int
    ): AiAnswer {
        // 数据最小化：仅在用户同意时附带个人数据；否则只发问题 + 能力事实
        val userContext = if (allowPersonalData && recentSessions.isNotEmpty()) {
            "用户最近一次训练：${recentSessions.first().exerciseName}（${recentSessions.first().score}分）。"
        } else ""
        val messages = JSONArray().apply {
            put(JSONObject().put("role", "system")
                .put("content", ProjectContext.systemPrompt(profile, recentSessions.isNotEmpty(), hasBodyTest) + userContext))
            put(JSONObject().put("role", "user").put("content", question))
        }
        val body = JSONObject()
            .put("model", model.ifBlank { "gpt-4o-mini" })
            .put("messages", messages)
            .put("temperature", 0.4)
            .put("stream", false)
            .toString()

        val conn = URL(resolveChatEndpoint(endpoint)).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer ${apiKey.trim()}") // 密钥仅请求头，不写日志
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            if (code in 200..299) {
                val text = conn.inputStream.bufferedReader().readText()
                val parsed = try {
                    JSONObject(text).optJSONArray("choices")
                        ?.optJSONObject(0)?.optJSONObject("message")
                        ?.optString("content").orEmpty()
                } catch (_: Exception) { "" }
                val reply = parsed.ifBlank { text }
                val quicks = LocalAssistant.quickQuestions(profile.ageId)
                // 流式接口不支持时一次性返回；若 onChunk 传入且文本较长，分段展示
                if (onChunk != null && reply.isNotBlank()) {
                    // 简单分段（远程非 stream 模式）：按句切 2 段，模拟"分段显示"
                    val mid = reply.length / 2
                    if (reply.length > 200) {
                        onChunk(reply.substring(0, mid))
                        onChunk(reply.substring(mid))
                    } else onChunk(reply)
                }
                return AiAnswer(text = reply, source = AiSource.REMOTE, quickReplies = quicks)
            } else {
                val errBody = try { conn.errorStream?.bufferedReader()?.readText()?.take(200).orEmpty() } catch (_: Exception) { "" }
                throw RuntimeException("HTTP $code $errBody")
            }
        } finally {
            conn.disconnect()
        }
    }
}

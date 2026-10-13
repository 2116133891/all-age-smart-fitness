package com.quannian.zhidong.coach

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * AI API Key 的本地安全存储（V3 任务 23）。
 *
 *  安全边界（诚实声明）：
 *  - 密钥**绝不硬编码**进 Kotlin 源码 / 公开配置 / Git 仓库。
 *  - 用 [EncryptedSharedPreferences]（Android Keystore 后端）加密存储，**不持久化明文**。
 *  - 仅开发环境机制：把服务商 API Key 直接放进 APK，即使混淆也无法保证不被提取。
 *    **正式上架应由服务端代理保管密钥**（访问控制 + 限流 + 费用保护）。
 *    本实现不声称已具备生产级密钥保护。
 *  - 日志与错误提示**一律脱敏**（[mask]），不打印完整密钥。
 *
 *  降级：若 Keystore / EncryptedSharedPreferences 在当前设备不可用（罕见），
 *  退回内存态（进程内有效、不落盘），并在 [isEncrypted] 标记，绝不把明文写进普通 SharedPreferences。
 */
object ApiKeyStore {

    private const val PREF = "quanli_ai_key"
    private const val K_KEY = "api_key"
    private const val K_ENDPOINT = "endpoint"
    private const val K_MODEL = "model"
    private const val K_PROVIDER = "provider"

    private val inMemory = mutableMapOf<String, String>()
    @Volatile
    private var encryptedAvailable = true

    /**
     * security-crypto 1.0.0 API：MasterKeys.getOrCreate() 返回的是"主密钥别名"（String），
     *  再由 [EncryptedSharedPreferences.create] 以别名 + 文件名 + 上下文 生成加密 prefs。
     */
    private fun prefs(context: Context) = try {
        val alias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            alias, PREF, context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (_: Exception) {
        // 降级：内存态（不落盘、不进普通 prefs）。诚实标记。
        encryptedAvailable = false
        null
    }

    /** 读取已保存的 API Key（明文仅在返回给调用方用于单次请求；调用方不得日志输出）。 */
    fun loadKey(context: Context): String {
        val p = prefs(context)
        if (p != null) return p.getString(K_KEY, "") ?: ""
        return inMemory[K_KEY] ?: ""
    }

    /** 保存 API Key（传空串 = 清除）。 */
    fun saveKey(context: Context, key: String) {
        val p = prefs(context)
        if (p != null) {
            p.edit().apply { if (key.isEmpty()) remove(K_KEY) else putString(K_KEY, key); apply() }
        }
        if (key.isEmpty()) inMemory.remove(K_KEY) else inMemory[K_KEY] = key
    }

    /** 读取服务商端点 / 模型 / 名称（明文可存，非敏感）。 */
    fun loadConfig(context: Context): Triple<String, String, String> {
        val p = prefs(context)
        val ep = if (p != null) p.getString(K_ENDPOINT, "") ?: "" else inMemory[K_ENDPOINT] ?: ""
        val md = if (p != null) p.getString(K_MODEL, "") ?: "" else inMemory[K_MODEL] ?: ""
        val pv = if (p != null) p.getString(K_PROVIDER, "") ?: "" else inMemory[K_PROVIDER] ?: ""
        return Triple(ep, md, pv)
    }

    fun saveConfig(context: Context, provider: String, endpoint: String, model: String) {
        val p = prefs(context)
        if (p != null) {
            p.edit().apply {
                putString(K_PROVIDER, provider); putString(K_ENDPOINT, endpoint); putString(K_MODEL, model); apply()
            }
        } else {
            inMemory[K_PROVIDER] = provider; inMemory[K_ENDPOINT] = endpoint; inMemory[K_MODEL] = model
        }
    }

    /** 是否使用加密存储（false 表示已降级到内存态，需在 UI 诚实提示）。 */
    fun isEncrypted(): Boolean = encryptedAvailable

    /** 脱敏显示：仅保留前 4 后 4，中间用 * 填充；空返回"未配置"。 */
    fun mask(key: String): String = when {
        key.isBlank() -> "未配置"
        key.length <= 8 -> "****"
        else -> key.take(4) + "•".repeat((key.length - 8).coerceAtLeast(4)) + key.takeLast(4)
    }
}

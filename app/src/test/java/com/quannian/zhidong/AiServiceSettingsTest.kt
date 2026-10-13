package com.quannian.zhidong

import com.quannian.zhidong.coach.AiFacade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P0-4 AI 服务设置回归单测（纯 JVM，不联网）。
 *
 *  锁定：
 *   - resolveChatEndpoint：base 补全 /chat/completions；已带则不重复（消除"重复 /v1/chat/completions"）。
 *   - testConnection 诚实性：未配置 Key / 未填端点 → 直接返回失败说明（不联网、不伪造成功）。
 */
class AiServiceSettingsTest {

    @Test
    fun `resolveChatEndpoint appends path when missing`() {
        assertEquals(
            "https://api.openai.com/v1/chat/completions",
            AiFacade.resolveChatEndpoint("https://api.openai.com/v1")
        )
        assertEquals(
            "https://api.openai.com/v1/chat/completions",
            AiFacade.resolveChatEndpoint("https://api.openai.com/v1/")
        )
    }

    @Test
    fun `resolveChatEndpoint does not duplicate when already present`() {
        val full = "https://api.openai.com/v1/chat/completions"
        assertEquals(full, AiFacade.resolveChatEndpoint(full))
        // 大小写不敏感（路径统一小写判断）
        assertEquals(full, AiFacade.resolveChatEndpoint("https://api.openai.com/v1/Chat/Completions".lowercase()))
    }

    @Test
    fun `resolveChatEndpoint trims and keeps custom base`() {
        assertEquals(
            "https://host:8080/v1/chat/completions",
            AiFacade.resolveChatEndpoint("  https://host:8080/v1  ")
        )
        assertEquals("", AiFacade.resolveChatEndpoint("   "))
    }

    @Test
    fun `testConnection is honest when key or endpoint is missing (no network, no fake success)`() {
        // 无 Key：guard 在构建请求体前返回，纯离线，不联网、不伪造成功。
        val noKey = AiFacade.testConnection("https://api.openai.com/v1", "gpt-4o-mini", "")
        assertFalse("无 Key 不应报告连接成功", noKey.ok)
        assertTrue("无 Key 应给中文说明", noKey.detail.isNotBlank())
        assertTrue("无 Key 说明应提示去配置", noKey.detail.contains("API Key"))

        // 无端点：guard 在构建请求体前返回。
        val noEp = AiFacade.testConnection("", "gpt-4o-mini", "sk-1234567890")
        assertFalse("无端点不应报告连接成功", noEp.ok)
        assertTrue("无端点应给中文说明", noEp.detail.isNotBlank())
    }

    @Test
    fun `ApiKeyStore mask never echoes the full key`() {
        val store = com.quannian.zhidong.coach.ApiKeyStore
        val full = "sk-abcdefghij9876543210"
        val m = store.mask(full)
        assertFalse("脱敏结果不得含完整 Key", m.contains(full))
        assertTrue("脱敏应保留前 4 位", m.startsWith(full.take(4)))
        assertTrue("脱敏应保留后 4 位", m.endsWith(full.takeLast(4)))
        assertEquals("空 Key 显示未配置", "未配置", store.mask(""))
        assertEquals("短 Key 显示占位", "****", store.mask("abcd"))
    }
}

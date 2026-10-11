package com.quannian.zhidong.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 设计系统：浅色科技风（白/浅灰底 + 青绿·深蓝·白 为主 + 卡片化）。
 * 三大成年人群各有主色（V3 统一），保持统一设计语言但区分气质：
 *  青年 = 科技蓝（现代·体测备考）
 *  中年 = 沉稳紫（高效·减压·平衡）
 *  银龄 = 沉稳青绿（稳重·康养防跌）
 */
object Palette {
    // 全局
    val bg = Color(0xFFF5F7FB)          // 浅灰蓝底
    val card = Color(0xFFFFFFFF)
    val cardSoft = Color(0xFFEEF2F9)
    val ink = Color(0xFF1B2233)        // 主文字
    val inkSoft = Color(0xFF6B7688)    // 次级文字
    val line = Color(0xFFE4E9F2)       // 描边
    val accent = Color(0xFF2F7BE8)     // 主科技蓝
    val accentSoft = Color(0xFFE7F0FE)
    val good = Color(0xFF21B57A)
    val warn = Color(0xFFF5A623)
    val bad = Color(0xFFE5484D)

    // 三大成年人群配色（V3：青绿 / 深蓝 / 白 为主，保留中年紫、银龄青绿区分气质）
    val youth = Color(0xFF3B6BE8)       // 科技蓝（深蓝）
    val youthSoft = Color(0xFFE9F0FE)
    // 中年
    val middle = Color(0xFF8A56C8)      // 沉稳紫
    val middleSoft = Color(0xFFF1E9FB)
    // 银龄
    val senior = Color(0xFF2E9C8E)      // 青绿
    val seniorSoft = Color(0xFFE4F4F0)

    fun ageColor(id: String): Color = when (com.quannian.zhidong.model.AgeGroupConfig.normalizeAgeId(id)) {
        "middle" -> middle
        "senior" -> senior
        else -> youth
    }

    fun ageSoft(id: String): Color = when (com.quannian.zhidong.model.AgeGroupConfig.normalizeAgeId(id)) {
        "middle" -> middleSoft
        "senior" -> seniorSoft
        else -> youthSoft
    }
}

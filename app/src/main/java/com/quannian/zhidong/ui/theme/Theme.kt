package com.quannian.zhidong.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 设计系统：浅色科技风（白/浅灰底 + 青蓝科技色 + 卡片化）。
 * 年龄段各有主色（儿童/青年/中年/银龄），保持统一设计语言但区分气质：
 *  儿童 = 活泼珊瑚橙（明亮运动感）
 *  青年 = 科技蓝（现代简洁）
 *  中年 = 沉稳紫（高效·减压·平衡）
 *  银龄 = 沉稳青绿（稳重健康）
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

    // 儿童
    val child = Color(0xFFF2713D)       // 珊瑚橙
    val childSoft = Color(0xFFFFEFE4)
    // 青年
    val youth = Color(0xFF3B6BE8)       // 科技蓝
    val youthSoft = Color(0xFFE9F0FE)
    // 中年
    val middle = Color(0xFF8A56C8)      // 沉稳紫
    val middleSoft = Color(0xFFF1E9FB)
    // 银龄
    val senior = Color(0xFF2E9C8E)      // 青绿
    val seniorSoft = Color(0xFFE4F4F0)

    fun ageColor(id: String): Color = when (id) {
        "child" -> child
        "middle" -> middle
        "senior" -> senior
        else -> youth
    }

    fun ageSoft(id: String): Color = when (id) {
        "child" -> childSoft
        "middle" -> middleSoft
        "senior" -> seniorSoft
        else -> youthSoft
    }
}

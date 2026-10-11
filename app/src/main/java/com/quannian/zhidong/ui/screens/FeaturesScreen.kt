package com.quannian.zhidong.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quannian.zhidong.ui.theme.Palette

/** 功能宫格项。 */
data class Feature(val title: String, val sub: String, val icon: ImageVector, val color: Color)

/**
 * "功能" tab（企业级 · 对标主流 App 功能聚合页）：宫格入口，把分散能力集中露出。
 *  6 项：体测 / AI 诊断 / 运动手环 / AI 助手 / 我的训练 / 个性化。静态 3×2 网格，无 experimental API。
 */
@Composable
fun FeaturesScreen(
    modifier: Modifier = Modifier,
    onBodyTest: () -> Unit = {},
    onVideoPick: () -> Unit = {},
    onWearable: () -> Unit = {},
    onAssistant: () -> Unit = {},
    onTraining: () -> Unit = {},
    onProfile: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.bg)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Text("功能中心", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Palette.ink)
        Spacer(Modifier.height(14.dp))

        val r1 = listOf(
            Feature("体测自评", "测 · 按年龄定制", Icons.Filled.FitnessCenter, Palette.youth) to onBodyTest,
            Feature("AI 动作诊断", "视频 · 前后对比", Icons.Filled.Insights, Palette.accent) to onVideoPick,
            Feature("运动手环", "同步 · 联训", Icons.Filled.Sync, Palette.middle) to onWearable
        )
        val r2 = listOf(
            Feature("AI 小助手", "离线问答", Icons.Filled.SmartToy, Palette.good) to onAssistant,
            Feature("我的训练", "记录 · 曲线", Icons.Filled.History, Palette.warn) to onTraining,
            Feature("个性化", "年龄 · 性别", Icons.Filled.Person, Palette.senior) to onProfile
        )

        FeatureRow(r1)
        Spacer(Modifier.height(12.dp))
        FeatureRow(r2)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun FeatureRow(cells: List<Pair<Feature, () -> Unit>>) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        cells.forEach { (f, action) ->
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                FeatureTile(feature = f, onClick = { action() })
            }
        }
    }
}

@Composable
private fun FeatureTile(feature: Feature, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.card, RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(vertical = 18.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(feature.color.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(feature.icon, null, tint = feature.color, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(feature.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Palette.ink,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(feature.sub, fontSize = 11.sp, color = Palette.inkSoft,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

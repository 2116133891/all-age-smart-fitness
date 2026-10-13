package com.quannian.zhidong.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quannian.zhidong.repository.TeachingVideoRepository
import com.quannian.zhidong.ui.components.TeachingVideoPlayer
import com.quannian.zhidong.ui.theme.Palette

/**
 * 独立 教学 视频 播放 页（任务 3）。
 *  从 训练 计划 / 体测 详情 进入，专注 播放 单一 视频（支持 全屏）。
 *  页面 离开 时 [TeachingVideoPlayer] 内部 DisposableEffect 释放 ExoPlayer。
 *  V3.3：当视频对应某个体测项目（itemKey 非空）时，提供「开始跟练」入口，
 *       直接进入双栏跟练页 followItem/{key}，实现 教学视频→跟练 的导航闭环。
 */
@Composable
fun TeachingVideoScreen(
    assetPath: String,
    title: String,
    subtitle: String = "",
    itemKey: String = "",
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onFollowItem: (String) -> Unit = {}
) {
    val followable = itemKey.isNotEmpty() && !TeachingVideoRepository.isMethodOnlyItem(itemKey)

    Column(
        modifier = modifier.fillMaxSize().background(Palette.bg),
        content = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "返回", tint = Palette.ink) }
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TeachingVideoPlayer(assetPath = assetPath, title = title, subtitle = subtitle)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, fontSize = 13.sp, color = Palette.inkSoft, lineHeight = 18.sp)
                }
                // V3.3 教学视频 → 跟练 闭环入口（仅运动型体测项）
                if (followable) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Palette.accent, RoundedCornerShape(14.dp))
                            .clickable { onFollowItem(itemKey) }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("开始跟练（双栏：示范 + 实时摄像头）", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                    Text(
                        "能力标注：${TeachingVideoRepository.followCapabilityLabel(itemKey)}（人工录入成绩，不冒充自动测量）",
                        fontSize = 11.sp, color = Palette.inkSoft, modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    )
}

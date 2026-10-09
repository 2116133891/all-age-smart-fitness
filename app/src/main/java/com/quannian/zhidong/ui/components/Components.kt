package com.quannian.zhidong.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quannian.zhidong.ui.theme.Palette

// ============================================================================
// 数字教练头像（统一入口，全项目唯一视觉源 = CoachVisual）
// ============================================================================
//
//  Phase 1：三类数字人（儿童/青年/银龄）统一使用**用户提供的高质量 3D 数字教练
//  PNG**（coach_child / coach_youth / coach_senior，480x960 透明背景）作为主视觉。
//
//  本顶层 [CoachAvatar] 只是 **转发** 到 [CoachVisual.CoachAvatar]（唯一实现与资源
//  映射点），保证：
//   - 全项目资源映射唯一（ageId -> drawable），后续替换 3D/帧序列只改 CoachVisual。
//   - 人物**静止站立**：无上下跳动 / 无缩放 / 无闪烁（不创建任何循环动画）。
//   - 竖向容器 + ContentScale.Fit：480x960 竖图全身可见，头脚不被裁切。
//
//  旧的 Canvas 火柴人 / 分层骨骼人（drawChildCoach / drawYouthCoach / drawSeniorCoach）
//  及 [CoachSpriteRenderer] 帧序列在本参赛版已停用；教学/跟练改用真实 PNG + 分运动
//  文字示范（Phase 2 再评估分阶段动作素材 / 真 3D）。

/** 数字教练头像（唯一实现委托给 [CoachVisual.CoachAvatar]）。 */
@Composable
fun CoachAvatar(
    ageId: String,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp
) {
    CoachVisual.CoachAvatar(ageId = ageId, modifier = modifier, size = size)
}

// ============================================================================
// 通用 UI 组件
// ============================================================================

/** 通用卡片容器。 */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    background: Color = Palette.card,
    borderColor: Color = Palette.line,
    shape: Dp = 20.dp,
    content: @Composable (ColumnScope) -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(shape))
            .background(background, RoundedCornerShape(shape))
            .border(1.dp, borderColor.copy(alpha = 0.6f), RoundedCornerShape(shape))
            .padding(16.dp)
    ) {
        content(this)
    }
}

/** 小标签。 */
@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 11.sp, color = Color.White)
    }
}

/** 带 emoji 的运动卡片。 */
@Composable
fun ExerciseCard(
    icon: String,
    title: String,
    tagline: String,
    duration: String,
    level: String,
    accent: Color,
    accentSoft: Color,
    detected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.card, RoundedCornerShape(18.dp))
            .border(1.dp, accent.copy(alpha = 0.14f), RoundedCornerShape(18.dp))
            .padding(16.dp)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentSoft, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 26.sp)
            }
            Column {
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                Spacer(Modifier.height(2.dp))
                Text(tagline, fontSize = 12.sp, color = Palette.inkSoft)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Pill(if (detected) "已支持实时检测" else "姿态识别", accent)
            Spacer(Modifier.height(6.dp))
            Text("$duration · $level", fontSize = 11.sp, color = Palette.inkSoft)
        }
    }
}

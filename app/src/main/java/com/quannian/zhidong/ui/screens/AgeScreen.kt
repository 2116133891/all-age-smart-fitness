package com.quannian.zhidong.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quannian.zhidong.model.AgeGroup
import com.quannian.zhidong.repository.ExerciseRepository
import com.quannian.zhidong.ui.components.Card
import com.quannian.zhidong.ui.components.CoachAvatar
import com.quannian.zhidong.ui.components.ExerciseCard
import com.quannian.zhidong.ui.theme.Palette

/**
 * 年龄段页：数字人教练 + 该年龄的"练 / 测"双模块。
 *  - "练"：该年龄段运动项目列表（进教学 / 跟练）。
 *  - "测"：体测自评入口（[onBodyTest]），与"练"形成 测→练→再测 闭环。
 */
@Composable
fun AgeScreen(
    id: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onPickExercise: (String) -> Unit,
    onBodyTest: () -> Unit = {}
) {
    val age = AgeGroup.entries.firstOrNull { it.id == id } ?: AgeGroup.YOUTH
    val color = Palette.ageColor(age.id)
    val soft = Palette.ageSoft(age.id)
    val exercises = ExerciseRepository.byAgeGroup(age)

    // 练 / 测 切换
    var tab by remember { mutableStateOf(0) } // 0=练 1=测

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.bg),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 头部：返回 + 数字人 + 标题
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "返回", tint = Palette.ink)
                }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(soft, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    CoachAvatar(ageId = age.id, size = 44.dp)
                }
                Column(Modifier.weight(1f)) {
                    Text(age.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                    Text(age.subtitle, fontSize = 12.sp, color = Palette.inkSoft)
                }
            }
        }

        // 练 / 测 双模块切换
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Palette.cardSoft)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("练 · 运动", "测 · 体测").forEachIndexed { idx, label ->
                    val sel = tab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (sel) color else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(11.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { tab = idx }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (sel) androidx.compose.ui.graphics.Color.White else Palette.ink
                        )
                    }
                }
            }
        }

        if (tab == 0) {
            // 练模块：运动列表
            item {
                Text("选择今天想练习的项目", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.ink)
            }
            items(exercises) { ex ->
                ExerciseCard(
                    icon = ex.icon,
                    title = ex.name,
                    tagline = ex.tagline,
                    duration = ex.duration,
                    level = ex.level,
                    accent = color,
                    accentSoft = soft,
                    detected = ex.analysisKind != null,
                    onClick = { onPickExercise(ex.id) }
                )
            }
        } else {
            // 测模块：体测自评入口
            item {
                Card() {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(soft, RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.FitnessCenter, "体测", tint = color, modifier = Modifier.size(30.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text("${age.title.split(" ")[0]} · 体测自评", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                            Spacer(Modifier.height(3.dp))
                            Text("5 项自评 → 综合分 + 等级 + 针对性建议，与「练」形成闭环", fontSize = 12.sp, color = Palette.inkSoft)
                        }
                    }
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(color, RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onBodyTest() }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("开始体测", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        color = androidx.compose.ui.graphics.Color.White)
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

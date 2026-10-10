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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quannian.zhidong.model.Profile
import com.quannian.zhidong.model.ProfileStore
import com.quannian.zhidong.ui.components.CoachAvatar
import com.quannian.zhidong.ui.theme.Palette

/** 年龄段选项（含新增"中年"）。 */
data class AgeOption(
    val id: String,
    val title: String,
    val desc: String,
    val coachHint: String
)

/**
 * 年龄 + 性别选择窗口（onboarding / 首页"个性化"入口）。
 *  选定后写入 [ProfileStore]，供 AI 助手 / 体测 / 运动推荐个性化。
 */
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onDone: (Profile) -> Unit
) {
    val context = LocalContext.current
    val saved = remember { ProfileStore.load(context) }

    var ageId by remember { mutableStateOf(saved.ageId.ifBlank { "youth" }) }
    var gender by remember { mutableStateOf(saved.gender.ifBlank { "unknown" }) }

    val ages = listOf(
        AgeOption("child", "儿童 (6-12)", "活泼 · 明亮 · 运动感", "轻运动 · 快乐成长"),
        AgeOption("youth", "青年 (18-35)", "科技 · 简洁 · 现代", "科学运动 · 体态纠正"),
        AgeOption("middle", "中年 (36-59)", "高效 · 减压 · 平衡", "下班健身 · 肩颈缓解"),
        AgeOption("senior", "银龄 (60+)", "稳重 · 温和 · 健康", "舒缓运动 · 康养防跌")
    )
    val genders = listOf("female" to "女", "male" to "男", "unknown" to "不限")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.bg),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "返回", tint = Palette.ink)
                }
                Text("个性化设置", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                Spacer(Modifier.weight(1f))
            }
            Text("告诉我你的信息，数字教练会更懂你", fontSize = 13.sp, color = Palette.inkSoft)
        }

        // 年龄
        item {
            Text("选择年龄段", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.ink)
            Spacer(Modifier.height(8.dp))
        }
        items(ages) { opt ->
            val sel = opt.id == ageId
            val color = Palette.ageColor(opt.id)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (sel) Palette.ageSoft(opt.id) else Palette.card, RoundedCornerShape(18.dp))
                    .padding(16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { ageId = opt.id },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Palette.ageSoft(opt.id), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CoachAvatar(ageId = opt.id, size = 44.dp)
                }
                Column(Modifier.weight(1f)) {
                    Text(opt.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                    Text(opt.desc, fontSize = 11.sp, color = Palette.inkSoft)
                    Spacer(Modifier.height(4.dp))
                    Text(opt.coachHint, fontSize = 11.sp, color = color, fontWeight = FontWeight.SemiBold)
                }
                if (sel) Text("✓", fontSize = 20.sp, color = color, fontWeight = FontWeight.Bold)
            }
        }

        // 性别
        item {
            Text("性别（可选）", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.ink)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                genders.forEach { (gid, label) ->
                    val sel = gid == gender
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (sel) Palette.accent else Palette.cardSoft, RoundedCornerShape(14.dp))
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (sel) androidx.compose.ui.graphics.Color.White else Palette.ink,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { gender = gid }
                        )
                    }
                }
            }
        }

        item {
            val profile = Profile(ageId = ageId, gender = gender, nickname = saved.nickname)
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Palette.accent, RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        ProfileStore.save(context, profile)
                        onDone(profile)
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("保存并进入 · ${ProfileStore.ageLabel(ageId)}/${ProfileStore.genderLabel(gender)}",
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = androidx.compose.ui.graphics.Color.White)
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

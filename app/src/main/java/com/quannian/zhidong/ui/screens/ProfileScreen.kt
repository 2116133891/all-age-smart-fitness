package com.quannian.zhidong.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircle
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quannian.zhidong.model.AgeGroupConfig
import com.quannian.zhidong.model.CoachStyle
import com.quannian.zhidong.model.CoachStyleStore
import com.quannian.zhidong.model.Profile
import com.quannian.zhidong.model.ProfileStore
import com.quannian.zhidong.ui.components.CoachAvatar
import com.quannian.zhidong.ui.theme.Palette

/**
 * V3.3 个性化设置（spec §3）：三个明确区域。
 *  1) 选择年龄段：青少年 / 中青年 / 中老年（主名称统一，不出现年龄范围括号）。
 *  2) 选择性别：男 / 女（沿用现有业务规则，从统一配置读取）。
 *  3) 选择教练风格：真人风格 / 动画数字教练（真正影响后续页面资源，非装饰）。
 *
 *  仅"保存并进入"才落盘；返回上一步保留原选择；已有配置不强制每次重选。
 */
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onDone: (Profile) -> Unit
) {
    val context = LocalContext.current
    val saved = remember { ProfileStore.load(context) }

    var ageId by remember {
        mutableStateOf(AgeGroupConfig.normalizeAgeId(saved.ageId.ifBlank { "youth" }))
    }
    var gender by remember {
        mutableStateOf(saved.gender.takeIf { it == "male" || it == "female" } ?: "male")
    }
    var style by remember { mutableStateOf(CoachStyleStore.load(context)) }

    val ages = AgeGroupConfig.BANDS.map { AgeOption(it.id, it.title, it.desc, it.coachHint) }
    val genders = AgeGroupConfig.GENDERS

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
                Column(Modifier.weight(1f)) {
                    Text("个性化设置", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                    Text("告诉我你的信息，数字教练会更懂你", fontSize = 13.sp, color = Palette.inkSoft)
                }
            }
        }

        // ① 年龄段（V3.3 主名称统一：青少年/中青年/中老年，不显示年龄范围括号）
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
                    .border(
                        width = if (sel) 2.dp else 1.dp,
                        color = if (sel) color else Palette.cardSoft,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { ageId = opt.id },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CoachAvatar(ageId = opt.id, size = 48.dp, modifier = Modifier.width(72.dp))
                Column(Modifier.weight(1f)) {
                    Text(opt.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                    Spacer(Modifier.height(3.dp))
                    Text(opt.coachHint, fontSize = 11.sp, color = color, fontWeight = FontWeight.SemiBold)
                }
                if (sel) {
                    Icon(Icons.Filled.CheckCircle, "已选", tint = color, modifier = Modifier.size(24.dp))
                }
            }
        }

        // ② 性别（从统一配置 AgeGroupConfig.GENDERS 读取，不逐页判断）
        item {
            Text("性别", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.ink)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                genders.forEach { (gid, label) ->
                    val sel = gid == gender
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (sel) Palette.accentSoft else Palette.cardSoft, RoundedCornerShape(14.dp))
                            .border(
                                width = if (sel) 2.dp else 1.dp,
                                color = if (sel) Palette.accent else Color.Transparent,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (sel) Palette.accent else Palette.ink,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { gender = gid }
                        )
                    }
                }
            }
        }

        // ③ 教练风格（V3.3 新增；真正影响后续页面资源）
        item {
            Text("选择教练风格", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.ink)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CoachStyle.entries.forEach { s ->
                    StyleOptionCard(style = s, selected = s == style, avatarAgeId = ageId, onClick = { style = s })
                }
            }
        }

        // 保存并进入（落盘 年龄/性别/风格；重启仍有效）
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
                        CoachStyleStore.save(context, style, ageId)
                        onDone(profile)
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("保存并进入 · ${ProfileStore.ageLabel(ageId)} / ${ProfileStore.genderLabel(gender)}",
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = Color.White)
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

/** 年龄段选项（V3.3：主名称统一，不出现年龄范围括号）。 */
data class AgeOption(
    val id: String,
    val title: String,
    val desc: String,
    val coachHint: String
)

/** 教练风格选项卡：醒目代表图 + 标题 + 一句说明；选中态描边+勾选，非蓝底蓝字。 */
@Composable
private fun StyleOptionCard(
    style: CoachStyle,
    selected: Boolean,
    avatarAgeId: String,
    onClick: () -> Unit
) {
    val color = if (style == CoachStyle.ANIMATED) Palette.middle else Palette.accent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) Palette.card else Palette.cardSoft, RoundedCornerShape(18.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) color else Color.Transparent,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(14.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 代表性图：真人风格 → 青少年真人头像；动画风格 → 中青年/中老年动画头像
        CoachAvatar(
            ageId = if (style == CoachStyle.REALISTIC) avatarAgeId.ifBlank { "youth" } else "middle",
            size = 44.dp,
            modifier = Modifier.width(66.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(style.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
            Spacer(Modifier.height(2.dp))
            Text(style.subtitle, fontSize = 11.sp, color = Palette.inkSoft)
        }
        if (selected) {
            Icon(Icons.Filled.CheckCircle, "已选", tint = color, modifier = Modifier.size(24.dp))
        }
    }
}

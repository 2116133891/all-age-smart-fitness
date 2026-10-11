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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quannian.zhidong.db.TrainingRepository
import com.quannian.zhidong.ui.theme.Palette

/**
 * 设置中心（企业级 · 对标京东健康"我的-设置"）：
 *  - 个性化（年龄性别）
 *  - AI 小助手（常用入口）
 *  - 运动手环
 *  - 语音教练开关
 *  - 数据管理（导出/清空训练记录）
 *  - 隐私政策 / 关于
 *  全本地：开关走 SharedPreferences，数据走 Room，无网络。
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onProfile: () -> Unit = {},
    onAssistant: () -> Unit = {},
    onWearable: () -> Unit = {}
) {
    val context = LocalContext.current
    val pref = remember { context.getSharedPreferences("quanli_settings", android.content.Context.MODE_PRIVATE) }
    var voiceOn by remember { mutableStateOf(pref.getBoolean("voice_coach", false)) }
    var showPrivacy by remember { mutableStateOf(false) }
    var clearing by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.bg),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.ArrowBack, "返回", tint = Palette.ink)
                Text("设置", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Palette.ink)
                Spacer(Modifier.weight(1f))
            }
        }

        if (showPrivacy) {
            item { PrivacyPanel() }
        } else {
            // 个性化
            item {
                Text("账户", fontSize = 13.sp, color = Palette.inkSoft, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 6.dp))
            }
            item {
                SettingsGroup {
                    SettingsRow("个性化 · 年龄性别", Icons.Filled.Apps, Palette.accent,
                        "去设置", { onProfile() })
                }
            }

            // 智能
            item {
                Text("智能服务", fontSize = 13.sp, color = Palette.inkSoft, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 6.dp))
            }
            item {
                SettingsGroup {
                    SettingsRow("AI 小助手", Icons.Filled.SmartToy, Palette.good,
                        "离线问答", { onAssistant() })
                    SettingsRow("运动手环", Icons.Filled.Sync, Palette.middle,
                        "配对同步", { onWearable() })
                    // 语音教练开关
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("语音教练", fontSize = 15.sp, color = Palette.ink)
                            Text("跟练时数字教练朗读纠错与鼓励（默认关）", fontSize = 12.sp, color = Palette.inkSoft)
                        }
                        Switch(
                            checked = voiceOn,
                            onCheckedChange = { voiceOn = it; pref.edit().putBoolean("voice_coach", it).apply() },
                            colors = SwitchDefaults.colors(Palette.accent)
                        )
                    }
                }
            }

            // 数据
            item {
                Text("数据与隐私", fontSize = 13.sp, color = Palette.inkSoft, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 6.dp))
            }
            item {
                SettingsGroup {
                    SettingsRow("隐私政策", Icons.Filled.Lock, Palette.senior, "", { showPrivacy = true })
                    SettingsRow("关于全龄智动", Icons.Filled.Info, Palette.inkSoft, "v1.0", {})
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Palette.bad.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            clearing = true
                            com.quannian.zhidong.ui.screens.clearTrainingData(context.applicationContext)
                            clearing = false
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.Delete, null, tint = Palette.bad, modifier = Modifier.size(22.dp))
                    Column(Modifier.weight(1f)) {
                        Text("清空训练记录", fontSize = 15.sp, color = Palette.ink, fontWeight = FontWeight.SemiBold)
                        Text(if (clearing) "已清空全部本地训练数据" else "删除全部本地训练记录（不可恢复）", fontSize = 12.sp, color = Palette.inkSoft)
                    }
                }
            }

            // 版本信息
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("全龄智动 v1.0", fontSize = 12.sp, color = Palette.inkSoft,
                        textAlign = TextAlign.Center)
                    Text("数据本地存储 · 摄像头姿态识别 · 不上云", fontSize = 11.sp, color = Palette.line,
                        textAlign = TextAlign.Center)
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.card, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp)
    ) {
        content()
    }
}

@Composable
private fun SettingsRow(label: String, icon: ImageVector, color: Color, sub: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 15.sp, color = Palette.ink, fontWeight = FontWeight.SemiBold)
            val s = sub
            if (s.isNotEmpty()) Text(s, fontSize = 12.sp, color = Palette.inkSoft)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = Palette.line, modifier = Modifier.size(22.dp))
    }
}

/** 隐私政策内页（企业级上架必备，本地文案，无网络）。 */
@Composable
private fun PrivacyPanel() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.card, RoundedCornerShape(18.dp))
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Text("隐私政策", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
        Spacer(Modifier.height(10.dp))
        listOf(
            "全龄智动 致力于保护你的隐私。本 App 的所有数据处理均在设备本地完成：",
            "· 摄像头姿态识别：MediaPipe 在设备端推理，画面与骨骼数据不上传任何服务器。",
            "· 训练记录：仅保存在本机 Room 数据库，不会同步到云端。",
            "· AI 小助手：本地规则引擎，离线运行，不收集、不上传你的提问。",
            "· 运动手环（演示）：未连接真实设备前无任何数据；Phase 2 接入后，数据仅用于本地训练强度建议。",
            "我们不使用任何账号体系，不收集手机号、位置等个人信息。",
            "你可以随时在\"设置 → 清空训练记录\"删除全部本地数据。"
        ).forEach { p ->
            Text(p, fontSize = 13.sp, color = Palette.ink, lineHeight = 20.sp, modifier = Modifier.padding(vertical = 5.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text("最后更新：2026-10-10", fontSize = 11.sp, color = Palette.inkSoft)
    }
}

/** 清空训练记录（Room 本地，供设置页调用）。 */
fun clearTrainingData(context: android.content.Context) {
    try {
        kotlinx.coroutines.runBlocking {
            com.quannian.zhidong.db.QuanNingDatabase.get(context).trainingSessionDao().clearAll()
        }
    } catch (_: Exception) {
    }
}

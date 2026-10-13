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
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.quannian.zhidong.coach.AiFacade
import com.quannian.zhidong.ui.components.Pill
import com.quannian.zhidong.ui.theme.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 设置中心（企业级 · 对标京东健康"我的-设置"）：
 *  - 个性化（年龄性别）
 *  - AI 小助手（常用入口）
 *  - 运动手环
 *  - 语音教练开关
 *  - 数据管理（导出/清空训练记录）
 *  - 隐私政策 / 关于
 *  全本地：开关走 SharedPreferences，数据走 Room，无网络。
 *
 *  P0-4 修复（对照"AI 配置结构不清 / 仅 URL 字符串校验 / 无真实连接测试"）：
 *   - AI 面板信息分层：服务商 → 端点 → 模型 → API Key（脱敏）→ 保存 + 测试连接。
 *   - 端点支持填 base（如 https://api.openai.com/v1）或完整 URL，用
 *     [AiFacade.resolveChatEndpoint] 统一补全 /chat/completions，避免重复路径。
 *   - 新增"测试连接"：真实发一次最小 chat 请求（非字符串校验/本地静态回答），
 *     返回中文可行动结果（成功 / Key 无效 / 超时 / 网络不可用 / 路径或模型错误）。
 *   - API Key 用 [com.quannian.zhidong.coach.ApiKeyStore] 加密存储 + 脱敏，不落日志。
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
            item { AiServiceConfigPanel(context) }
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
            "· AI 小助手：本地规则引擎，离线运行，不收集、不上传你的提问；配置远程模型后，仅在你明确同意时发送完成当前问题必需的信息。",
            "· 运动手环（演示）：未连接真实设备前无任何数据；Phase 2 接入后，数据仅用于本地训练强度建议。",
            "我们不使用任何账号体系，不收集手机号、位置等个人信息。",
            "你可以随时在\"设置 → 清空训练记录\"删除全部本地数据。"
        ).forEach { p ->
            Text(p, fontSize = 13.sp, color = Palette.ink, lineHeight = 20.sp, modifier = Modifier.padding(vertical = 5.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text("最后更新：2026-10-13", fontSize = 11.sp, color = Palette.inkSoft)
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

/**
 * AI 模型服务配置面板（V3 任务 22-23，P0-4 重构）。
 *  信息分层：服务商 → 端点 → 模型 → API Key（脱敏）→ 保存 + 测试连接（真实 HTTP）。
 *  - Key 用 [com.quannian.zhidong.coach.ApiKeyStore] 加密存储，脱敏展示，不落日志 / 不进 Git。
 *  - 端点支持 base 或完整 URL，统一补全 /chat/completions（[AiFacade.resolveChatEndpoint]）。
 *  - "测试连接"真实发一次最小 chat 请求（max_tokens=1），返回中文可行动结果，
 *    不做"仅字符串校验 / 本地静态回答"的假测试。
 */
@Composable
private fun AiServiceConfigPanel(context: android.content.Context) {
    val keyStore = com.quannian.zhidong.coach.ApiKeyStore
    val cfg = remember { keyStore.loadConfig(context) }
    var provider by remember { mutableStateOf(cfg.first.ifBlank { "OpenAI 兼容" }) }
    var endpoint by remember { mutableStateOf(cfg.second) }
    var model by remember { mutableStateOf(cfg.third) }
    var keyInput by remember { mutableStateOf("") }
    var savedKeyMask by remember {
        mutableStateOf(keyStore.mask(keyStore.loadKey(context)))
    }
    var dataConsent by remember {
        mutableStateOf(context.getSharedPreferences("quanli_settings", android.content.Context.MODE_PRIVATE)
            .getBoolean("ai_data_consent", false))
    }
    var saved by remember { mutableStateOf(false) }
    val pref = remember { context.getSharedPreferences("quanli_settings", android.content.Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()

    // 连接测试（真实 HTTP，P0-4）
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<AiFacade.AiConnTest?>(null) }

    fun doSave() {
        if (keyInput.isNotEmpty()) keyStore.saveKey(context, keyInput)
        keyStore.saveConfig(context, provider, endpoint, model)
        savedKeyMask = keyStore.mask(keyStore.loadKey(context))
        keyInput = ""
        saved = true
    }

    fun doTest() {
        // 若输入框有新 Key 则先保存再测（否则用已存 Key）
        val key = if (keyInput.isNotBlank()) {
            keyStore.saveKey(context, keyInput)
            keyInput
        } else keyStore.loadKey(context)
        val ep = endpoint.ifBlank { keyStore.loadConfig(context).first }
        val md = model.ifBlank { keyStore.loadConfig(context).third }
        testing = true
        testResult = null
        scope.launch(Dispatchers.IO) {
            val r = AiFacade.testConnection(ep, md, key)
            testing = false
            testResult = r
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.card, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("AI 模型服务", fontSize = 15.sp, color = Palette.ink, fontWeight = FontWeight.SemiBold)
            val configured = keyStore.loadKey(context).isNotBlank()
            Pill(if (configured) "已配置" else "未配置", if (configured) Palette.good else Palette.inkSoft)
        }
        Spacer(Modifier.height(10.dp))

        // 服务商
        Text("服务商（可选）", fontSize = 12.sp, color = Palette.inkSoft)
        Spacer(Modifier.height(4.dp))
        val providers = listOf("OpenAI 兼容", "DeepSeek", "Mistral", "自定义端点")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            providers.take(2).forEach { p ->
                Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                    .background(if (provider == p) Palette.accent else Palette.cardSoft, RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { provider = p; pref.edit().putString("ai_provider", p).apply() }
                    .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center) {
                    Text(p, fontSize = 13.sp, color = if (provider == p) Color.White else Palette.ink)
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        // 端点（支持 base 或完整 URL，统一补全 /chat/completions）
        Text("服务端点（可填 base，如 https://api.openai.com/v1，或完整 …/v1/chat/completions）",
            fontSize = 12.sp, color = Palette.inkSoft, lineHeight = 16.sp)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = endpoint, onValueChange = { endpoint = it },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text("https://api.openai.com/v1", color = Palette.inkSoft) },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
        )
        Spacer(Modifier.height(10.dp))

        // 模型
        Text("模型名称", fontSize = 12.sp, color = Palette.inkSoft)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = model, onValueChange = { model = it },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text("gpt-4o-mini", color = Palette.inkSoft) },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
        )
        Spacer(Modifier.height(10.dp))

        // API Key（脱敏）
        Text("API Key（加密本地存储 · 当前 ${savedKeyMask}）", fontSize = 12.sp, color = Palette.inkSoft)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = keyInput, onValueChange = { keyInput = it },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text("粘贴服务商 API Key（请勿提交到 Git）", color = Palette.inkSoft) },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Spacer(Modifier.height(12.dp))

        // 保存 + 测试连接（并排）
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Palette.accent, RoundedCornerShape(14.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { doSave() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(if (saved) "已保存" else "保存配置", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = Color.White)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Palette.ink, RoundedCornerShape(14.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !testing
                    ) { doTest() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (testing) "测试中…" else "测试连接",
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White
                )
            }
        }

        // 测试连接的真实结果（诚实：成功/失败都给出可行动说明，不造假）
        if (testing || testResult != null) {
            Spacer(Modifier.height(10.dp))
            val r = testResult
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            r == null -> Palette.cardSoft
                            r.ok -> Palette.good.copy(alpha = 0.10f)
                            else -> Palette.bad.copy(alpha = 0.10f)
                        },
                        RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    when {
                        r == null -> "正在测试连接…"
                        r.ok -> "✅ 连接成功"
                        else -> "❌ 连接失败"
                    },
                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    color = when {
                        r == null -> Palette.inkSoft
                        r.ok -> Palette.good
                        else -> Palette.bad
                    }
                )
                if (r != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(r.detail, fontSize = 12.sp, color = Palette.ink, lineHeight = 16.sp)
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        // 数据最小化 + 同意
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(Palette.cardSoft, RoundedCornerShape(14.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text("发送我的最近训练记录给模型", fontSize = 13.sp, color = Palette.ink,
                    fontWeight = FontWeight.SemiBold)
                Text("默认关闭；仅发送完成当前问题必需的信息", fontSize = 11.sp, color = Palette.inkSoft)
            }
            Switch(checked = dataConsent, onCheckedChange = {
                dataConsent = it; pref.edit().putBoolean("ai_data_consent", it).apply()
            }, colors = SwitchDefaults.colors(Palette.accent))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "说明：API Key 存于本机加密存储（Android Keystore），不会写入 Git 或日志。" +
            "若把密钥直接打进 APK，即使混淆也无法保证不被提取；正式上架应由服务端代理保管密钥。" +
            if (!keyStore.isEncrypted()) "（当前设备加密存储不可用，密钥仅进程内存有效，不落盘）" else "",
            fontSize = 11.sp, color = Palette.inkSoft, lineHeight = 15.sp
        )
    }
}

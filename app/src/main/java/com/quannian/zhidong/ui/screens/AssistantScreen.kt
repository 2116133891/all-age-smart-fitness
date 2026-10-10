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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quannian.zhidong.assistant.LocalAssistant
import com.quannian.zhidong.db.TrainingRepository
import com.quannian.zhidong.model.ProfileStore
import com.quannian.zhidong.ui.theme.Palette

/** 一条对话。 */
data class ChatMsg(val text: String, val fromUser: Boolean)

/**
 * AI 小助手（本地离线问答）：输入框 + 快捷提问 + 气泡对话，回复引用用户画像与最近训练。
 *  全程本地规则引擎（[LocalAssistant]），无网络、无 API Key。
 */
@Composable
fun AssistantScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val profile = remember { ProfileStore.load(context) }

    val messages = remember { mutableStateListOf<ChatMsg>() }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var recent: List<com.quannian.zhidong.db.TrainingSession> by remember { mutableStateOf(emptyList()) }
    val quick = remember(profile.ageId) { LocalAssistant.quickQuestions(profile.ageId) }

    // 预置最近训练数据（给助手个性化用）
    LaunchedEffect(Unit) {
        try {
            recent = TrainingRepository(context).recent(5)
        } catch (_: Exception) {
            recent = emptyList()
        }
        messages.add(ChatMsg("你好，我是全龄智动 AI 教练助手，${ProfileStore.ageLabel(profile.ageId)}专属。", fromUser = false))
    }

    val sendScope = rememberCoroutineScope()

    fun send(q: String) {
        val question = q.trim()
        if (question.isEmpty() || sending) return
        messages.add(ChatMsg(question, fromUser = true))
        sending = true
        input = ""
        // 本地引擎即时返回；用一个小延迟模拟"思考"，体验更像 AI
        sendScope.launch {
            kotlinx.coroutines.delay(400)
            val reply = LocalAssistant.ask(question, profile, recent)
            messages.add(ChatMsg(reply.text, fromUser = false))
            sending = false
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.bg),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                Text("AI 小助手", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Palette.ink)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Palette.accentSoft)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("本地离线", fontSize = 11.sp, color = Palette.accent, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 对话
        items(messages) { m ->
            Bubble(m, accent = Palette.ageColor(profile.ageId))
        }

        // 快捷提问（仅首屏）
        if (messages.size <= 1) {
            item {
                Column {
                    Text("你可以问我：", fontSize = 12.sp, color = Palette.inkSoft)
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        quick.forEach { qq ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Palette.cardSoft)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { send(qq) }
                                    .padding(horizontal = 14.dp, vertical = 11.dp)
                            ) {
                                Text(qq, fontSize = 14.sp, color = Palette.ink)
                            }
                        }
                    }
                }
            }
        }

        // 输入区（固定在 LazyColumn 末尾；用 fillMaxSize 视觉下沉）
        item {
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("输入你的运动问题…", color = Palette.inkSoft) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Palette.accent, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            send(input)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Send, "发送", tint = Color.White)
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun Bubble(msg: ChatMsg, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.fromUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(
                    if (msg.fromUser) RoundedCornerShape(16.dp, 4.dp, 16.dp, 4.dp)
                    else RoundedCornerShape(4.dp, 16.dp, 4.dp, 16.dp)
                )
                .background(if (msg.fromUser) accent.copy(alpha = 0.16f) else Palette.card)
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Text(
                text = msg.text,
                fontSize = 14.sp,
                color = if (msg.fromUser) accent else Palette.ink,
                lineHeight = 21.sp
            )
        }
    }
}

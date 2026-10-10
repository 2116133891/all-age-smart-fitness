## ⚠️ 本轮（10-10）参赛版大功能扩展 + 双 bug 修复

**目标：新增 6 大能力（引导页 / 年龄性别选择 / 练·测双模块 / AI 小助手 / 运动手环 / 中年组），并修复 2 个真机反馈的 bug（视频分析闪退、数字人不会动）。BUILD SUCCESSFUL（:app:compileDebugKotlin + :app:assembleDebug，APK 71.6MB）。**

### A. 新增功能（全部本地离线，可编译）
| 能力 | 文件 | 说明 |
|---|---|---|
| 引导页（可滑动） | `ui/screens/OnboardingScreen.kt` | 6 张卡（青年×2/中年×2/银龄×2），逐张讲"能得到什么"，末尾进 App |
| 年龄性别选择 | `ui/screens/ProfileScreen.kt` + `model/Profile.kt` | 4 组年龄 + 性别，写 `ProfileStore`(SharedPreferences)，供助手/体测/推荐个性化 |
| 中年组（健身/肩颈/减压） | `model/PoseModels.kt`(MIDDLE) + `repository/ExerciseRepository.kt`(middle) + `score/AgeProfile.kt` + `ui/theme/Theme.kt` | 新增 middle 配色 / 权重 / 3 个运动（下班力量·肩颈缓解·久坐恢复） |
| 练 / 测 双模块 | `ui/screens/AgeScreen.kt` + `ui/screens/BodyTestScreen.kt` | AgeScreen 加"练·测"切换；体测页按年龄段定制 5 项自评→综合分+等级+短板建议 |
| AI 小助手 | `assistant/LocalAssistant.kt` + `ui/screens/AssistantScreen.kt` | 本地规则+知识库问答（引用画像/最近训练），快捷提问，离线无 Key |
| 运动手环 | `ui/screens/WearableScreen.kt` | 配对/同步 + 步数/心率/睡眠/卡路里数据维度 + 联训闭环（**诚实标注：演示连接，Phase 2 接真实 BLE**） |

### B. 两个真机 bug 修复
1. **AI 动作诊断上传视频后闪退**：根因在 `pose/PoseDetector.kt` 旧实现用 `ByteArray(input.available()) + 单次 read` 读 5.7MB 模型——`available()` 对 assets 流不可靠，模型字节被**截断** → `createFromOptions` 在 **native 层 SIGSEGV**（Kotlin `catch(Exception)` 接不住 native crash）→ 整应用闪退。修复：改 `input.readBytes()`（循环读到 EOF，保证完整）+ `catch(Throwable)` 兜底降级。
2. **数字人教学/跟练不会动（固定站立）**：根因是 `CoachSpriteView` 主视觉走静态 `CoachFigure`(PNG)，只有文字在切。修复：重写 `ui/components/CoachSpriteView.kt`——教学页用 `CoachMotionEngine.teachingSequence` + `CoachMotionView`(2D 关节人形)，相邻两步 600ms 关节插值平滑过渡（不同运动姿态肉眼可区分）；跟练页直接渲染实时 `livePose`。保留小 `CoachAvatar` 做身份识别。

### C. 核心能力保护（回归核对，未推翻既有架构）
导航路由、applicationId/namespace、Room 结构、FollowAlongViewModel 会话清理、CameraX 生命周期、MediaPipe 33 关键点、9 个分析器与独立实例、深蹲状态机/计数/评分/纠错、训练结束即存本地记录 —— 均保留。未升级 Kotlin/Gradle/Compose/CameraX/MediaPipe 版本。

### D. 验证分级（本环境约束）
- ✅ 编译 + 打包：`:app:compileDebugKotlin` 与 `:app:assembleDebug` 均 **BUILD SUCCESSFUL**（本机 JDK17 + Android SDK + 预下载 Gradle 8.3）。
- ⏳ 真机运行验收（引导页滑动、体测打分、助手问答、手环演示、视频分析不闪退、数字人动起来）需安装到 MI 9 真机实测（MIUI 需开"USB 安装"）。

### E. 文件变更清单（本轮）
新增：`assistant/LocalAssistant.kt`、`model/Profile.kt`、`ui/screens/{Onboarding,Profile,BodyTest,Assistant,Wearable}Screen.kt`
修改：`model/PoseModels.kt`、`repository/ExerciseRepository.kt`、`score/AgeProfile.kt`、`ui/theme/Theme.kt`、`ui/screens/{NavigationHost,Home,Age}Screen.kt`、`ui/components/CoachSpriteView.kt`、`pose/PoseDetector.kt`、`ui/components/CoachVisual.kt`（10-09 遗留）

---

# PROJECT_STATUS.md

> 鍏ㄩ緞鏅哄姩 路 鏅鸿兘杩愬姩鎸囧 Android MVP锛堝煄甯傚ぇ璧涢」鐩級
> 鏇存柊鏃堕棿锛?026-10-09 路 涓荤▼锛欰gnes-3.0-flash锛圕laude Code锛?> 椤圭洰鏍癸細`C:\Users\23163\Desktop\鍩庡競澶ц禌椤圭洰` 路 杩滅▼ `github.com/2116133891/all-age-smart-fitness`
> 鍖呭悕锛歚com.quannian.zhidong` 路 applicationId 鍚岋紙**鏈疆鏈敼鍖呭悕/applicationId/namespace**锛?
## 鈿狅笍 鏈疆锛?0-09锛夊弬璧涚増瑙嗚鍗囩骇涓庝綋楠屼紭鍖?**鐩爣锛氶椤电ǔ瀹氥€佹暟瀛椾汉缁熶竴銆佸姩浣滅ず鑼冪洿瑙傘€丄pp 鍥炬爣閲嶅仛銆佹牳蹇冭兘鍔涗笉鐮村潖銆備互涓嬪潎涓?浠ｇ爜宸叉敼銆佸彲缂栬瘧棰勬湡"绾у埆锛涙瀯寤?鍗曟祴/鐪熸満鍦ㄦ湰鐜鏈墽琛岋紙瑙?搂0锛夈€?*

### 0. 楠岃瘉璇氬疄鍒嗙骇锛堟湰鐜绾︽潫锛?- 鏈細璇濊繍琛岀幆澧?**鏃?JDK 17銆佹棤 Android SDK銆佹棤 Gradle銆佹棤 adb**锛歚local.properties` 浠嶆寚鍚戜笉瀛樺湪鐨?`C:\Users\Admin\.android-build\android-sdk`锛宍JAVA_HOME`=JDK 8銆?- 鍥犳 **APK 鐢熸垚銆丟radle 鍗曟祴銆佺湡鏈烘憚鍍忓ご/瑙嗚楠屾敹鍧?鏈湪鏈幆澧冩墽琛?**锛涙敼鍔ㄤ负绮剧‘浠ｇ爜淇敼锛岄渶鍦ㄥ叿澶?JDK17+SDK 鐨勬満鍣ㄤ笂鎵ц `.\gradlew :app:assembleDebug` 澶嶆牳銆?- 闈欐€佹牳瀵癸紙宸插畬鎴愶級锛氣憼 鏁板瓧浜轰綋绯?*涓嶅啀瀛樺湪浠讳綍 `infiniteRepeatable`/`rememberInfiniteTransition` 璋冪敤鐐?*锛坓rep 纭锛夛紱鈶?鎵€鏈?`CoachAvatar` 璋冪敤鐐逛粎浼?`ageId`+`size`锛堜笌鏂扮鍚嶅尮閰嶏級锛涒憿 鏃х殑 Canvas 鐏煷浜?/ 甯у簭鍒楃鍙锋棤鎮┖寮曠敤锛涒懀 App 鍥炬爣寮曠敤閾撅紙`mipmap-anydpi-v26` 鈫?`@drawable/ic_launcher_{background,foreground}`锛夊畬濂斤紝涓?*鏃犲瘑搴?mipmap 鐩綍**锛岀煝閲忔浛鎹㈡棤閲嶅璧勬簮鍐茬獊銆?
### 1. 淇敼鏂囦欢娓呭崟锛堟湰杞?7 澶勶級
| 鏂囦欢 | 鍙樻洿 |
|---|---|
| `ui/components/CoachVisual.kt` | 閲嶅啓涓?*鍞竴鏁板瓧浜鸿瑙夋簮**锛涘垹闄?2s 鏃犻檺鍛煎惛寰幆锛堣烦鍔ㄦ牴鍥狅級锛沗CoachAvatar` 鏀圭珫鍚戝鍣?+ `ContentScale.Fit`锛堜慨澶磋剼琚?Crop 瑁佸垏銆侀€忔槑鑳屾櫙涓嶆樉榛戝潡锛夛紱`CoachFigure`/`TeachingCoach` 榛樿闈欐绔欑珛锛岃瘹瀹炴爣娉?Phase 1锛堥潪 3D锛?|
| `ui/components/Components.kt` | 椤跺眰 `CoachAvatar` 鏀逛负**杞彂**鍒?`CoachVisual.CoachAvatar`锛堟秷闄ゅ弻瀹炵幇/鍙岃祫婧愭槧灏勶級锛涘垹闄ゆ浠ｇ爜 Canvas 鐏煷浜轰笌鏈敤鍔ㄧ敾 import锛?*淇濈暀** `Card`/`Pill`/`ExerciseCard` |
| `ui/screens/HomeScreen.kt` | P0 绋冲畾锛氬崱鐗囦竴娆℃€?220ms 娣″叆锛坄graphicsLayer` alpha锛屾寜 `age.id` key锛屼笉鍙犲姞锛? 鏈夌晫 ripple锛堝厠鍒舵寜鍘嬪弽棣堬紝涓嶆敼甯冨眬锛夛紱P1 棣栭〉鍏ュ彛鍥炬爣缁熶竴涓?Material 鍥炬爣锛堝幓 emoji 娣锋惌锛?|
| `ui/components/CoachSpriteView.kt` | 鏂板 `coachState` 褰㈠弬骞堕€忎紶缁?`CoachFigure`锛涜窡缁冮〉鏁板瓧浜烘寜鏁欑粌鐘舵€佸仛**鍏嬪埗鍙嶉**锛屼笌鐢ㄦ埛瀹炴椂楠ㄦ灦鏄庣‘鍖哄垎 |
| `ui/screens/FollowAlongScreen.kt` | 璺熺粌椤?`CoachSpriteView` 璋冪敤澶勮ˉ `coachState = coachState`锛堝崟鐐广€佺紪璇戝畨鍏級 |
| `res/drawable/ic_launcher_background.{png鈫抶ml}` | 閲嶅仛涓?*鐭㈤噺**鑷€傚簲鍥炬爣鑳屾櫙锛氭繁钃濃啋闈掔豢瀵硅娓愬彉 + 椤堕儴楂樺厜锛堝搧鐗岀鎶€閰嶈壊锛?|
| `res/drawable/ic_launcher_foreground.{png鈫抶ml}` | 閲嶅仛涓?*鐭㈤噺**鑷€傚簲鍥炬爣鍓嶆櫙锛氱櫧鑹茶繍鍔ㄤ汉褰?+ 濮挎€佽瘑鍒叧閿偣锛堥潚鐐癸級+ 璇嗗埆妗嗚鏍囷紝钀藉湪 108dp 瀹夊叏鍖哄唴 |

### 2. 鏍稿績鑳藉姏淇濇姢锛堝洖褰掓牳瀵癸紝鏈敼鍔ㄤ笟鍔￠€昏緫锛?- 瀵艰埅璺敱銆乣applicationId`/`namespace`銆丷oom 缁撴瀯銆乣FollowAlongViewModel` 浼氳瘽娓呯悊銆丆ameraX 鐢熷懡鍛ㄦ湡銆丮ediaPipe 33 鍏抽敭鐐广€? 涓垎鏋愬櫒涓庣嫭绔嬪疄渚嬨€佹繁韫茬姸鎬佹満/璁℃暟/璇勫垎/绾犻敊銆佽缁冪粨鏉熷嵆瀛樻湰鍦拌褰?鈥斺€?**鏈疆鍧囨湭瑙︾**锛屼粎鏀硅瑙夊眰缁勪欢涓庡浘鏍囪祫婧愩€?- 鏈崌绾?Kotlin/Gradle/Compose/CameraX/MediaPipe 鐗堟湰锛涙湭鍒犻櫎宸茬ǔ瀹氬垎鏋愬櫒锛涙湭鏀规暟鎹簱/鍖呭悕銆?
### 3. 鏁板瓧浜虹礌鏉愭帴鍏ヤ笌璧勬簮鏄犲皠璇存槑锛堜氦浠橀」 2锛?- 璧勬簮鍛藉悕锛歚coach_child` / `coach_youth` / `coach_senior`锛?80脳960 楂樿川閲?3D 閫忔槑鑳屾櫙锛岄鏍肩粺涓€锛屽悓鍝佺墝涓夎 logo锛夆啋 浣嶄簬 `res/drawable/`锛?*鍞竴**鏄犲皠鐐瑰湪 `CoachVisual.coachResource(ageId)`銆?- 鍔ㄤ綔绀鸿寖锛堣瘹瀹?Phase 1锛夛細鏁欏/璺熺粌 = **鍒嗗勾榫勯潤鎬?PNG + 鍒嗚繍鍔ㄦ枃瀛楃ず鑼?+ 鐘舵€佸井鍙嶉**锛沗CoachMotionEngine.teachingSequence` 鎸夎繍鍔ㄧ敓鎴愪笉鍚屾枃妗堝簭鍒楋紙娣辫共/渚ф媺浼?寮€鍚堣烦/鍏閿?8 寮忊€︼級锛岄伩鍏?鎵€鏈夎繍鍔ㄥ悓涓€鍥?銆?*涓嶅啋鍏呯湡瀹炶繛缁?3D 鍔ㄧ敾**锛涘垎闃舵鍔ㄤ綔绱犳潗 / 鐪?3D锛團ilament锛変负 Phase 2 棰勭暀銆?
### 4. 浠嶅瓨鍦ㄧ殑闂涓庡悗缁缓璁?- [ ] **P1 鍘?emoji锛堝凡閮ㄥ垎瀹屾垚锛?*锛氫粎棣栭〉宸叉崲 Material 鍥炬爣锛沗Age/Exercise/Coach/Report/MyTraining` 灞忓唴浠嶆湁绾?9 澶?emoji锛堭煄煋婐煉○煄ゐ煍婐煍団喕鉁擄級锛屽彈"鏃犳硶缂栬瘧楠岃瘉"绾︽潫鏈疆**鏈叏閲忔浛鎹?*锛屽缓璁湪鍙瀯寤烘満涓婄粺涓€涓?Material 鍥炬爣銆?- [ ] 璺熺粌椤佃嫢闇€鏁板瓧浜?*閫愬抚璺熼殢鍔ㄤ綔**锛堣€岄潪鐘舵€佸井鍙嶉锛夛紝闇€ Phase 2 鍒嗛樁娈靛姩浣滅礌鏉愶紙鍒囧浘/甯у簭鍒楋級銆?- [ ] `local.properties` 浠嶆寚鍚戜笉瀛樺湪鐨?Admin 璺緞锛涙瀯寤哄墠闇€鎸夊綋鍓嶆満鍣ㄥ啓 `sdk.dir`锛堟垨閰?`ANDROID_HOME`锛? JDK17銆?- [ ] 寤鸿鍦ㄥ彲鏋勫缓鏈轰笂璺?`:app:assembleDebug` + JVM 鍗曟祴锛屽苟鐪熸満楠岃瘉锛氶椤靛仠鐣?鈮?0s 鏁欑粌闈欐銆佸娆¤繘鍑烘棤鍔ㄧ敾鍙犲姞銆佸浘鏍囨樉绀恒€佽窡缁冭仈鍔ㄣ€佸浘鏍囧湪妗岄潰娓呮櫚搴︺€?- [ ] App 鍥炬爣鐭㈤噺宸叉帴鍏ワ紝浣?`mipmap-anydpi-v26` 浠呰鐩?API 26+锛?*褰撳墠鏃犲瘑搴?mipmap 鍏滃簳 PNG**锛岃嫢闇€鏀寔 API 24/25 闇€琛?PNG锛堢煝閲?adaptive icon 鍦?<26 浼氭樉绀轰负绌?绯荤粺榛樿锛夈€?
## 鈿狅笍 鏈疆锛?0-03锛夌姸鎬佷慨姝?**鏈枃浠?10-01 鐗堢殑"P0 鍏ㄥ畬鎴?/ P1 鏈仛"鎻忚堪宸茶繃鏃躲€傜湡瀹炰唬鐮佸簱宸茶繙瓒呰鎻忚堪銆?*
閫愭枃浠舵牳瀵瑰悗鐨勭湡瀹炵姸鎬侊紙瑙?`FINAL_IMPLEMENTATION_REPORT.md`锛夛細

- **9 涓笓灞炲垎鏋愬櫒鍏ㄩ儴宸插疄鐜?*锛坰quat/shoulder/yoga/jumprope/stretch/jack/baduanjin/taichi/easy锛夛紝
  `ExerciseAnalyzerFactory` 宸茶矾鐢憋紝**涓嶅啀鏄?鍏ㄩ儴澶嶇敤 ArmReach"**銆?- **鏁板瓧浜轰笁骞撮緞娈靛樊寮傚寲**宸插疄鐜帮紙Canvas 鐭㈤噺锛屽ご韬瘮/鍙戝瀷/鑰佽姳闀?鍛煎惛鑺傚鍚勫紓锛? CoachState 鐘舵€佹満銆?- **Room 璁粌璁板綍 + 鎴愰暱鏇茬嚎 + 鎴戠殑璁粌椤?* 宸插疄鐜帮紙`db/`銆乣MyTrainingScreen`锛夈€?- **鏈湴鏁欑粌锛圠ocalCoachProvider锛? 鍙€?RemoteLLM锛堟棤 Key 闄嶇骇锛?* 宸插疄鐜般€?- **Android TTS锛圱tsCoach锛岄粯璁ゅ叧锛?* 宸插疄鐜般€?- **鏈疆淇鐨勭湡 bug**锛氬紑鍚堣烦鍒嗚吙瑙掑弽瑙掞紙`JumpingJackAnalyzer.legAngle` 鏀圭偣绉笁鐐瑰す瑙掞級锛?  4 涓崟娴嬪す鍏峰嚑浣曢噸鍐欍€?- **鍗曟祴锛欵xerciseAnalyzerTest 16/16 + ScoreEngineTest 7/7 鍏ㄨ繃**锛坄java -cp` 鎵嬭窇锛?  鍥犱腑鏂囪矾寰勪笅 Gradle test 浼?ClassNotFoundException锛夈€?- **缂栬瘧 + 瀹夎 + 鐪熸満杩涚▼鏃犲穿婧?宸茬‘璁?*锛涙憚鍍忓ご鍑虹敾闈?/ MediaPipe 鍒濆鍖栨棩蹇?/ 鐪熶汉璁℃暟
  灞?PARTIALLY VERIFIED锛堥渶鐪熶汉鍦ㄨ窡缁冮〉閰嶅悎锛夈€?
涓嬫柟涓?10-01 鍘熷璁板綍锛堜繚鐣欏巻鍙诧級锛屼笌褰撳墠鐪熷疄浠ｇ爜鍙兘鏈夊嚭鍏ワ紝**浠ヤ唬鐮?+ `FINAL_IMPLEMENTATION_REPORT.md` 涓哄噯**銆?
---

## 1. 褰撳墠宸插畬鎴愮殑宸ヤ綔

### 1.1 Android 宸ョ▼楠ㄦ灦锛圥0 鍏ㄩ噺瀹屾垚锛屽彲缂栬瘧鍙畨瑁咃級
- 瀹屾暣 Gradle 宸ョ▼锛歚settings.gradle`銆乣build.gradle`銆乣app/build.gradle`銆乣gradle-wrapper.properties`銆乣gradle.properties`銆乣gradlew`/`gradlew.bat`
- 鍖呯粨鏋勶細`ui/screens`銆乣ui/components`銆乣ui/theme`銆乣camera`銆乣pose`銆乣analyzer`銆乣score`銆乣model`銆乣repository`
- `MainActivity` + `NavigationHost`锛歴plash 鈫?home 鈫?age 鈫?exercise 鈫?coach 鈫?followalong 鈫?report 鍏ㄩ摼璺?7 椤靛鑸?- 璧勬簮锛歚values/strings.xml`銆乣values/colors.xml`銆乣values/themes.xml`銆佽嚜閫傚簲鍚姩鍥炬爣锛坄mipmap-anydpi-v26/ic_launcher.xml` + `drawable/ic_launcher_foreground.xml` + `drawable/ic_launcher_background.xml`锛?- `AndroidManifest.xml`锛欳AMERA 鏉冮檺 + `MainActivity` 鍏ュ彛

### 1.2 鏁板瓧浜?+ 瑙嗚浣撶郴锛圥0 瀹屾垚锛?- `ui/theme/Palette.kt`锛氫笁骞撮緞娈甸厤鑹诧紙鍎跨鐝婄憵 / 闈掑勾绉戞妧钃?/ 閾堕緞闈掔豢锛? 鍏ㄥ眬娴呰壊绉戞妧椋?- `ui/components/Components.kt`锛?  - `CoachAvatar(ageId, size)` 鈥?Canvas 缁樺埗鐨勬娊璞℃暟瀛椾汉锛堟诞鍔ㄥ姩鐢伙紝鏃犲閮ㄧ礌鏉愶級
  - `Card / Pill / ExerciseCard` 閫氱敤鍗＄墖缁勪欢
- `ui/components/SkeletonOverlay.kt`锛氬湪鎽勫儚澶寸敾闈箣涓婄粯鍒?33 鍏抽敭鐐?+ 楠ㄦ灦杩炵嚎锛坈yan 绾?+ yellow 鐐癸級

### 1.3 涓変釜骞撮緞娈甸〉闈紙P0 瀹屾垚锛?- `HomeScreen.kt`锛氬搧鐗屾爣棰?+ 銆岄€夋嫨鎮ㄧ殑杩愬姩闄粌銆? 涓夊勾榫勫叆鍙ｅ崱鐗?- `AgeScreen.kt`锛氬勾榫勬爣棰?+ 璇ュ勾榫?3 椤硅繍鍔ㄥ崱鐗?- `ExerciseScreen.kt`锛氬姩浣滃悕 / 姝ラ / 娉ㄦ剰 / 鏁欑粌寮€鍦虹櫧 + 銆屽紑濮嬫暀瀛︺€嶃€屽紑濮嬭窡缁冦€嶆寜閽?- `CoachScreen.kt`锛氭暟瀛椾汉鑸炲彴 + 杞挱鏁欑粌鍙拌瘝 + 鍔ㄤ綔瑕侀閫熻 + 鏁欏/璺熺粌鎸夐挳
- 杩愬姩鍐呭浠撳簱 `repository/ExerciseRepository.kt`锛氬効绔ワ紙璺崇怀/鎷変几/寮€鍚堣烦锛夈€侀潚骞达紙娣辫共/鑲╅鎷変几/鐟滀冀锛夈€侀摱榫勶紙鍏閿?澶瀬/鑸掔紦鎷変几锛夊悇 3 椤癸紝鍚暟瀛椾汉寮€鍦虹櫧銆佹楠ゃ€佹敞鎰忋€佺洰鏍囨鏁?
### 1.4 鎽勫儚澶?+ 濮挎€佽瘑鍒?+ 鍔ㄤ綔璇勫垎 + 绾犻敊锛圥0 鏍稿績 Demo 瀹屾垚锛?- `camera/CameraManager.kt`锛欳ameraX `PreviewView` 棰勮 + `ImageAnalysis` 鎶藉抚 鈫?缂╂斁 鈫?鍥炶皟
- `pose/PoseDetector.kt`锛歁ediaPipe Pose Landmarker锛堟湰鍦?`pose_landmarker.task` 妯″瀷锛?.7MB锛屽凡 `assets/pose_landmarker.task` 鎵撳寘锛?- `analyzer/` 涓変釜鍒嗘瀽鍣細
  - `SquatAnalyzer`锛堟繁韫茬姸鎬佹満锛歴tanding鈫抎own鈫抌ottom鈫抲p锛屽畬鎴愭鏁板垽瀹氾級
  - `ArmReachAnalyzer`锛堟姮鑷傚箙搴﹂€氱敤锛岀敤浜庤偐棰?鍏閿︽墭澶?鍎跨寮€鍚堣烦锛?  - `JumpRopeAnalyzer`锛堣烦缁宠捣璺虫鏋讹紝璇氬疄鏍囨敞涓?璧疯烦杩戜技"锛岀暀鍗囩骇鎺ュ彛锛?- `score/ScoreCalculator.kt`锛氬洓缁存潈閲嶏紙骞呭害 40 / 濮挎€?30 / 绋冲畾 20 / 瀹屾暣 10锛?- `score/CorrectionEngine.kt`锛氳鍒欏紩鎿?鈫?8 绫婚敊璇紙armsTooLow / bodyTooForward / kneeAngleInvalid / unstablePose / rangeTooShallow / speedTooFast / noPerson / postureDrift锛夆啋 鑷劧璇█寤鸿
- `ui/screens/FollowAlongViewModel.kt`锛氬疄鏃剁姸鎬佹祦锛坙andmarks / phase / score / stateText / corrections / repCount锛?- `ui/screens/FollowAlongScreen.kt`锛氭憚鍍忓ご + 楠ㄩ + 瀹炴椂鏁版嵁鍗?+ 绾犻敊 + 瀹屾垚鎸夐挳
- `ui/screens/ReportScreen.kt`锛氳缁冩姤鍛婏紙鐜舰璇勫垎 + 琛ㄧ幇鑹ソ + 寤鸿鏀硅繘 + 鍐嶆璁粌/杩斿洖棣栭〉锛?- `repository/ReportChannel.kt`锛氳窡缁?鈫?鎶ュ憡鐨勬暟鎹€氶亾
- `model/PoseModels.kt`銆乣model/ScoreModels.kt`锛氭暟鎹粨鏋?
### 1.5 JVM 鍗曟祴锛圥0 楠岃瘉閫氳繃锛?- `test/java/.../ScoreEngineTest.kt`锛氳瘎鍒?绾犻敊閫昏緫 7 椤规柇瑷€锛屽叏杩囷紙宸茬敤 `java -cp` 鐩磋窇楠岃瘉锛?
### 1.6 鐩戞帶 / 瀹堟姢绯荤粺锛堟湰杞柊澧烇紝鍏ㄥ眬浣?Token 鏂规锛?- `backup/custom-guard-backup/` 鈥?鏃╂湡鑷埗鍥涘眰鐩戞帶锛堥」鐩骇锛夋暣浣撳綊妗ｅ仠鐢紝**鏈垹闄?*
- `~/.claude/agents/project-supervisor.md` 鈥?鐢ㄦ埛绾у彧璇?Supervisor agent锛圧ead/Grep/Glob only锛屾寜闇€璋冪敤锛?- `~/.claude/watchdog/` 鈥?鍏ㄥ眬 AGNES Resilience Watchdog锛堢嫭绔?PowerShell锛屼笉渚濊禆 AGNES/妯″瀷锛夛細
  - `watchdog.ps1` 鈥?60s 杞锛?5723 绔彛鎺㈡祴 + 蹇冭烦 + 杩涚▼ + AGNES 鐘舵€?+ build 鐘舵€?鈫?11 绉嶅尯鍒嗙姸鎬?  - `agnes-error-hook.ps1` 鈥?鍏ㄥ眬 PostToolUseFailure Hook锛岀函鏂囨湰鎵弿 502/503/upstream/timeout锛屽啓 `state/last-action.json`
  - `start.ps1` / `stop.ps1` 鈥?涓€閿惎鍋?watchdog
  - `pscheck.ps1` 鈥?PowerShell 璇硶鑷鍣?  - `USAGE.md` 鈥?浣跨敤璇存槑
- `~/.claude/settings.json` 鈥?澧炲姞 `hooks.PostToolUseFailure` 鍏ㄥ眬婵€娲?`agnes-error-hook.ps1`锛宍enabledPlugins` 鍚敤 ralph-loop
- **Ralph Loop** 宸插畨瑁咃細`claude plugin install ralph-loop@claude-plugins-official`锛堝畼鏂规彃浠讹級

---

## 2. 鍒涘缓 / 淇敼杩囩殑鍏抽敭鏂囦欢

### 2.1 涓氬姟锛堥」鐩唴锛?| 璺緞 | 浣滅敤 |
|------|------|
| `settings.gradle` / `build.gradle` / `app/build.gradle` | 宸ョ▼ / 渚濊禆 |
| `gradle-wrapper.properties` / `gradlew` / `gradlew.bat` / `gradle/wrapper/gradle-wrapper.jar` | Gradle 8.3 |
| `gradle.properties` | `android.overridePathCheck=true`锛堥潪 ASCII 璺緞锛?|
| `local.properties` | `sdk.dir=C:\Users\Admin\.android-build\android-sdk` |
| `app/src/main/AndroidManifest.xml` | 鏉冮檺 + 鍏ュ彛 |
| `app/src/main/assets/pose_landmarker.task` | 5.7MB 濮挎€佹ā鍨嬶紙鏈湴鎺ㄧ悊锛?|
| `app/src/main/res/{values,mipmap-anydpi-v26,drawable}/*` | 璧勬簮 |
| `app/src/main/java/com/quannian/zhidong/MainActivity.kt` | 鍏ュ彛 |
| `app/src/main/java/com/quannian/zhidong/model/{PoseModels,ScoreModels}.kt` | 鏁版嵁妯″瀷 |
| `app/src/main/java/com/quannian/zhidong/repository/{ExerciseRepository,ReportChannel}.kt` | 鍐呭 + 閫氶亾 |
| `app/src/main/java/com/quannian/zhidong/analyzer/{PoseAnalyzer,SquatAnalyzer,ArmReachAnalyzer,JumpRopeAnalyzer}.kt` | 鍔ㄤ綔鍒嗘瀽 |
| `app/src/main/java/com/quannian/zhidong/score/{ScoreCalculator,CorrectionEngine}.kt` | 璇勫垎 + 绾犻敊 |
| `app/src/main/java/com/quannian/zhidong/pose/PoseDetector.kt` | MediaPipe 灏佽 |
| `app/src/main/java/com/quannian/zhidong/camera/CameraManager.kt` | CameraX 灏佽 |
| `app/src/main/java/com/quannian/zhidong/ui/theme/Palette.kt` | 璁捐绯荤粺 |
| `app/src/main/java/com/quannian/zhidong/ui/components/{Components,SkeletonOverlay}.kt` | 鏁板瓧浜?鍗＄墖/楠ㄩ |
| `app/src/main/java/com/quannian/zhidong/ui/screens/*.kt` | 7 灞?+ `FollowAlongViewModel` + `NavigationHost` |
| `app/src/test/java/com/quannian/zhidong/ScoreEngineTest.kt` | JVM 鍗曟祴 |
| `README.md` | 椤圭洰璇存槑 |

### 2.2 瀹堟姢锛堢敤鎴风骇 + 椤圭洰绾э級
| 璺緞 | 浣滅敤 |
|------|------|
| `backup/custom-guard-backup/` | 鏃╂湡鑷埗鍥涘眰鐩戞帶褰掓。锛?*鍋滅敤**锛屽惈 `README.md` 璇存槑锛?|
| `~/.claude/agents/project-supervisor.md` | 鐢ㄦ埛绾у彧璇?Supervisor |
| `~/.claude/watchdog/{watchdog,start,stop,agnes-error-hook,pscheck}.ps1` + `USAGE.md` | 鍏ㄥ眬 AGNES 瀹堟姢 |
| `~/.claude/watchdog/state/{last-action.json,heartbeat.json,agnes-watchdog-state.json}` | 杩愯鏃剁姸鎬?|
| `~/.claude/settings.json` | 鍏ㄥ眬 Hook + ralph-loop 鍚敤 |

### 2.3 鐜锛堥潪 git 绠＄悊锛屾柊浼氳瘽闇€閲嶅缓锛?| 璺緞 | 浣滅敤 |
|------|------|
| `C:\Users\Admin\.android-build\jdk-17.0.10+7\` | Temurin JDK 17 |
| `C:\Users\Admin\.android-build\android-sdk\` | Android SDK锛坧latform 34 + build-tools 33/34 + platform-tools锛?|

---

## 3. 褰撳墠鍙洿鎺ヤ娇鐢ㄧ殑鍔熻兘

- 鉁?**缂栬瘧 / 鎵撳寘 / 瀹夎**锛歚.\gradlew :app:assembleDebug` 浜у嚭 `app\build\outputs\apk\debug\app-debug.apk`锛?7MB锛屽惈 MediaPipe 4-ABI 鍘熺敓搴?+ 濮挎€佹ā鍨嬶級
- 鉁?**鍏ㄩ摼璺氦浜?*锛氬惎鍔?鈫?棣栭〉 鈫?涓夊勾榫?鈫?杩愬姩 鈫?鏁欏 鈫?璺熺粌 鈫?鎶ュ憡锛堟瘡椤靛彲鐐瑰嚮璺宠浆锛?- 鉁?**鐪熸満璺熺粌闂幆**锛堥渶 ARM 鐪熸満锛屾ā鎷熷櫒鏃犳憚鍍忓ご锛夛細寮€鎽勫儚澶?鈫?MediaPipe 瀹炴椂濮挎€佽瘑鍒?鈫?楠ㄩ缁樺埗 鈫?鍏宠妭瑙掑害 鈫?鍔ㄤ綔闃舵 鈫?瀹炴椂寰楀垎锛?鈥?00锛夆啋 鈮? 绫荤籂閿欏缓璁?鈫?瀹屾垚娆℃暟 鈫?璁粌鎶ュ憡
- 鉁?**JVM 鍗曟祴**锛氳瘎鍒?绾犻敊閫昏緫 7/7 閫氳繃
- 鉁?**鍏ㄥ眬 AGNES 瀹堟姢**锛?  - `powershell -File "%USERPROFILE%\.claude\watchdog\start.ps1"` 鍚姩 60s 杞 watchdog
  - 妫€娴嬪埌 Claude 杩涚▼宸查€€鍑?+ 15723 鍦ㄧ洃鍚?+ 鏈?session 鏃讹紝鑷姩 `claude --continue` resume锛?min 闃叉姈锛?  - AGNES 杩炵画 502 鎸?20s/60s/120s 閫€閬?- 鉁?**Ralph Loop 闀夸换鍔?*锛歚/ralph-loop "浠诲姟鎻忚堪" --max-iterations 10`锛堜笉浼氶粯璁ゅ父椹伙級
- 鉁?**Supervisor 澶嶆牳**锛歚claude agents` 鍙彂鐜?`project-supervisor`锛涗富 Agent 浠诲姟瀹屾垚鏃?/ 鎬€鐤戝崱浣忔椂鏄惧紡璋冪敤

---

## 4. 褰撳墠鏈畬鎴愮殑鍔熻兘

- **璺崇怀鐪熷疄璁℃暟**锛氬綋鍓?`JumpRopeAnalyzer` 鏄?璧疯烦鐘舵€佽繎浼?锛堣啙瑙掗槇鍊硷級锛岄潪鐪熷疄缁充綋璁℃暟銆傚崌绾ф帴鍙ｅ凡鐣欙紙鎵嬭厱杞ㄨ抗 + 鍛ㄦ湡淇″彿 + MediaPipe z 楂樺害锛?- **鍏閿?/ 澶瀬閫愬紡璇嗗埆**锛氱洰鍓嶅叓娈甸敠鐢?`ArmReachAnalyzer` 閫氱敤鎶噦锛屾湭鍋氶€愬紡锛堜袱鎵嬫墭澶?宸﹀彸寮€寮?浣撹浆鍙屽叧鈥︼級
- **鏁板瓧浜哄崌绾?*锛氬綋鍓嶆槸 Canvas 鎶借薄浜哄舰 + 杞挱鍙拌瘝锛涙湭鍋?Lottie / 3D / 鍙ｅ瀷椹卞姩
- **鍘嗗彶鏁版嵁缁熻 / 鎴愰暱鏇茬嚎 / 涓汉妗ｆ**锛氭棤 Room / 鍥捐〃
- **澶фā鍨嬬籂閿欑敓鎴?*锛氬綋鍓嶇籂閿欐槸棰勮鏂囨锛屾湭鎺?LLM
- **璐﹀彿 / 浜戝悓姝?/ 鍚庣**锛歅2 鏈仛
- **UI 鍔ㄧ敾 / 澶у睆瑙嗚缁嗚妭**锛歅1 鏈仛
- **鏇村鍔ㄤ綔锛堢憸浼?/ 寮€鍚堣烦 / 鍩虹浣撹兘锛?*锛氬唴瀹逛粨搴撳凡鏈夛紝浣嗘瘡涓兘澶嶇敤 `ArmReachAnalyzer`锛屾湭鍋氬姩浣滀笓灞炲垎鏋?
---

## 5. 褰撳墠宸茬煡闂

1. **AGNES 闂存瓏 502 / rate-limit**锛氭湰鍦?inference gateway `127.0.0.1:15723` 浼氶棿姝?`API Error: 502` 涓?`claude-sonnet-4-6[1M] is temporarily unavailable (rate-limited)`銆傛湰杞紑鍙戝凡鏁版鍥犳涓柇銆?   - 瀹堟姢鏂规锛堝叏灞€ watchdog + error hook锛夊凡閮ㄧ讲锛屽彲鍦?Claude 閫€鍑哄悗鑷姩 `--continue` 鎭㈠锛屼絾**涓嶄繚璇?*鑷姩閲嶈繛 100% 鎴愬姛
2. **闈?ASCII 椤圭洰璺緞**锛歚鍩庡競澶ц禌椤圭洰` 涓枃璺緞瑙﹀彂 AGP 璀﹀憡锛屽凡 `gradle.properties` 鍔?`android.overridePathCheck=true`
3. **JVM 鍗曟祴 classpath 闄愬埗**锛歚gradle test` 鍦?Windows + 闈?ASCII 璺緞涓嬩細 `ClassNotFoundException`锛涙敼鐢ㄦ墜璺?`java -cp` 楠岃瘉锛堣瘎鍒嗛€昏緫 7/7 杩囷級銆傚悗缁璺?Gradle 鍗曟祴闇€鍏堟妸椤圭洰鎸埌 ASCII 璺緞
4. **Reikor-Arg/claude-agent-monitor 鏄?VS Code 鎵╁睍锛岄潪 Claude Code 鎻掍欢**锛歚claude plugin install agent-monitor` 鎵句笉鍒帮紙鏃?`marketplace.json`锛夈€傚綋鍓嶆柟妗?*涓嶈**杩欎釜锛屾敼鐢?Claude Code 鍘熺敓 task-notification + 鍙€?VS Code 鎵╁睍
5. **鑷姩缂栬瘧 Hook 鐨?JAVA_HOME 缁ф壙 bug**锛堝凡淇絾鐣欑棔锛夛細`auto-build.ps1` 鏃╂湡鐗堟湰鎶?`$env:JAVA_HOME` 鍙栧埌瀛楅潰 `'True'`锛屽鑷村瓙杩涚▼ Gradle 鎶?`JAVA_HOME is not set`銆傜幇宸茬敤 `[string]::IsNullOrWhiteSpace` 瀹堝崼 + 榛樿璺緞锛屼絾**鑷埗绯荤粺宸插仠鐢ㄥ綊妗?*锛屾 bug 涓嶅啀褰卞搷

---

## 6. 褰撳墠姝ｅ湪杩涜浣嗗皻鏈畬鎴愮殑浠诲姟

**鏈疆"鍏ㄥ眬浣?Token 瀹堟姢鏂规"宸插熀鏈畬鎴?*锛屽墿浣欐敹灏鹃」锛?
- [ ] 璺戜竴娆?watchdog 瀹屾暣鑷祴锛坄start.ps1` 鍚庡彴璧锋潵 鈫?妯℃嫙 502 鈫?鐪?`agnes-watchdog-state.json` 鐘舵€佸彉鍖?鈫?`stop.ps1`锛?- [ ] 楠岃瘉 `claude agents` 鑳藉惁鍒楀嚭 `project-supervisor`锛堜箣鍓?`claude agents --json` 杩斿洖 `[]`锛屾€€鐤戠敤鎴风骇 agent 鍦?subagent 涓婁笅鏂囨湭鍔犺浇锛岄渶鍦ㄤ氦浜掑紡浼氳瘽閲岀‘璁わ級
- [ ] 纭 `~/.claude/settings.json` 鐨?PostToolUseFailure Hook 鍦ㄥ疄闄?Claude Code 浼氳瘽閲岀湡鐨勮Е鍙戯紙闇€瑕佷竴娆″伐鍏峰け璐ユ墠鑳介獙璇侊級
- [ ] 鍐欎竴浠?`HOW_TO_RESUME_THIS_PROJECT.md`锛堢粰涓嬩竴涓細璇濈殑閫熸煡鍗★紝瑙?搂8锛?
**娌℃湁**鍏朵粬鏈畬鎴愪换鍔°€?
---

## 7. 涓嬩竴姝ユ渶鎺ㄨ崘鎵ц鐨勪换鍔?
鎸夌敤鎴?鏆傚仠涓氬姟寮€鍙?鐨勬寚绀猴紝**鐜板湪涓嶈鍐嶅啓涓氬姟浠ｇ爜**銆傛渶鍚堢悊鐨勪笅涓€姝ワ細

1. **锛堟帹鑽愶紝鏈€灏忥級** 璺戜竴閬?搂6 閲岀殑 4 椤瑰畧鎶よ嚜娴嬶紝纭鍏ㄥ眬鏂规鐪熺殑鐢熸晥
2. **锛堝鏋滅敤鎴锋槑纭鎭㈠寮€鍙戯級** 浼樺厛鎶?**璺崇怀浠?璧疯烦杩戜技"鍗囩骇涓虹湡瀹炶鏁?*锛堟墜鑵曡建杩?+ 鍛ㄦ湡淇″彿锛夛紝杩欐槸 P0 閲屽敮涓€"鐣欎簡鎺ュ彛浣嗘病鍋氱湡"鐨勫姩浣?3. **锛堝鏋滅敤鎴疯婕旂ず锛?* 瑁?`adb` 杩炰竴鍙?ARM 鐪熸満璺戦€?娣辫共璺熺粌"闂幆锛岃繖鏄瘎濮旀渶鍏冲績鐨?Demo
4. **涓嶅缓璁?* 鍦?AGNES 涓嶇ǔ瀹氱殑鎯呭喌涓嬪啀鎷夐暱鏃堕棿浠诲姟锛汻alph Loop 涓婇檺璁?10~20 杞氨澶?
---

## 8. 濡傞渶缁х画褰撳墠浠诲姟锛屾柊浼氳瘽闇€瑕佺煡閬撶殑鍏ㄩ儴涓婁笅鏂?
### 8.1 蹇€熷惎鍔紙鐜锛?```powershell
# JDK / SDK锛堥兘鍦ㄧ敤鎴风洰褰曪紝闈?git 绠＄悊锛?$env:JAVA_HOME   = "C:\Users\Admin\.android-build\jdk-17.0.10+7"
$env:ANDROID_HOME= "C:\Users\Admin\.android-build\android-sdk"
cd C:\Users\Admin\Desktop\鍩庡競澶ц禌椤圭洰

# 缂栬瘧
.\gradlew.bat :app:assembleDebug --no-daemon
# 浜х墿锛歛pp\build\outputs\apk\debug\app-debug.apk

# 鐪熸満瀹夎
& "C:\Users\Admin\.android-build\android-sdk\platform-tools\adb.exe" install -r .\app\build\outputs\apk\debug\app-debug.apk
```

### 8.2 瀹堟姢绯荤粺锛堝叏灞€锛岃法椤圭洰锛?```powershell
# 鍚姩 AGNES watchdog锛?0s 杞锛屽悗鍙帮級
powershell -NoProfile -ExecutionPolicy Bypass -File "$env:USERPROFILE\.claude\watchdog\start.ps1"

# 鍋滄
powershell -NoProfile -ExecutionPolicy Bypass -File "$env:USERPROFILE\.claude\watchdog\stop.ps1"

# 鐪嬪綋鍓嶇姸鎬?Get-Content "$env:USERPROFILE\.claude\watchdog\state\agnes-watchdog-state.json" -Raw

# 妯℃嫙涓€娆?AGNES 502 缁?hook
echo '{"tool_name":"Bash","tool_error":"502 Bad Gateway from upstream"}' |
  powershell -NoProfile -ExecutionPolicy Bypass -File "$env:USERPROFILE\.claude\watchdog\agnes-error-hook.ps1"
```

### 8.3 闀夸换鍔?/ 澶嶆牳
```
/ralph-loop "<鏄庣‘鐨勪笅涓€娈典换鍔℃弿杩?" --max-iterations 10
/cancel-ralph
```
- 鎬€鐤戜富 Agent 鍋忕鐩爣 / 鍗′綇 / 杩炵画缂栬瘧澶辫触鏃讹紝璁╀富 Agent 鏄惧紡璋冪敤 `project-supervisor`锛圧ead/Grep/Glob 鍙锛屾寜闇€娑堣€?token锛?
### 8.4 椤圭洰鐘舵€?- **P0 鍏ㄥ畬鎴?*锛堝惈缂栬瘧銆佸畨瑁呫€佽窡缁冮棴鐜€佽瘎鍒嗐€佺籂閿欍€佹姤鍛婏級
- **P1 鏈仛**锛堟洿澶氬姩浣溿€佹暟瀛椾汉 Lottie/3D銆乁I 鍔ㄧ敾銆佹暟鎹粺璁★級
- **P2 鏈仛**锛堜簯銆佽处鍙枫€佸ぇ妯″瀷锛?- 璇﹁ `README.md`

### 8.5 鍏抽敭璁捐鍐崇瓥锛堟柊浼氳瘽鍒帹缈伙級
1. **MediaPipe 0.10.14 API**锛歚PoseLandmarker.createFromOptions` + `BaseOptions.setModelAssetBuffer(ByteBuffer)` + `RunningMode.VIDEO`锛沗BitmapImageBuilder(bitmap).build()` 鈫?`detectForVideo(mpImage, ts)` 鈫?`result.landmarks()` 杩斿洖 `List<List<NormalizedLandmark>>`銆傛ā鍨?`pose_landmarker.task` 鍦?`assets/`锛?.7MB锛?2. **CameraX 1.3.0**锛歚ImageProxy.toBitmap()`锛沗ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888`锛沗PreviewView` 缁戝畾鐢?`preview.setSurfaceProvider { req -> view.getSurfaceProvider().onSurfaceRequested(req) }`
3. **璇勫垎**锛歚ScoreCalculator` 鍥涚淮锛堝箙搴?40 / 濮挎€?30 / 绋冲畾 20 / 瀹屾暣 10锛夛紝鐙珛妯″潡锛屽姞鏂板姩浣滃彧鍔?`analyzer/` 瑙勫垯涓嶆帹缈绘灦鏋?4. **绾犻敊**锛歚CorrectionEngine` 棰勮 8 绫婚敊璇?鈫?鑷劧璇█锛涙湭鏉ュ彲鎹?LLM
5. **璺崇怀璇氬疄鏍囨敞**锛氬綋鍓嶆槸璧疯烦杩戜技锛屼笉鏄湡璁℃暟锛屽埆鍦ㄦ紨绀洪噷鍐掑厖
6. **瀹堟姢绯荤粺鍒绘剰浣?Token**锛歸atchdog 鏄函 PowerShell 鏈湴鑴氭湰锛? token锛夛紝Supervisor 鎸夐渶璋冪敤锛?*涓嶈**瑁呭涓悓绫?monitor / 涓嶈缁?Supervisor 鍔?Write/Edit 鏉冮檺

### 8.6 鏈疆韪╄繃鐨勫潙锛堝埆鍐嶈俯锛?- **PowerShell 璇?stdin**锛欳laude Code Hook 鐢?`[Console]::In.ReadToEnd()`锛屼笉鑳界敤 `$input`锛坄param(ValueFromRemainingArguments)` 浼氬悶鎺夌閬撹緭鍏ワ級
- **PowerShell 5.x 榛樿 GBK**锛氫换浣曞甫涓枃鐨?`.ps1` 蹇呴』瀛?**UTF-8 BOM**锛屽惁鍒欒В鏋愬櫒鎶婁腑鏂囧綋涔辩爜瀵艰嚧璇硶閿欒
- **闈?ASCII 璺緞 + classpath**锛歚gradle test` 鍦ㄤ腑鏂囪矾寰勪笅浼?`ClassNotFoundException`锛屾墜璺?`java -cp` 缁曡繃
- **AGNES 502 鏃?Bash 琚嫆**锛歛uto-mode classifier 鍦?AGNES 鎸傛椂浼氭嫆缁?Bash / Edit锛屾鏃跺彧鍋?Read 绫绘搷浣滐紝绛?AGNES 鎭㈠鍐嶇户缁?- **`gradlew.bat` 蹇呴』 `--no-daemon`**锛歞aemon 鍦?VS Code / 涓枃璺緞涓嬬粡甯稿崱浣?
### 8.7 鏂囦欢浣嶇疆閫熸煡
- 椤圭洰鏍癸細`c:\Users\Admin\Desktop\鍩庡競澶ц禌椤圭洰`
- APK锛歚app\build\outputs\apk\debug\app-debug.apk`
- 濮挎€佹ā鍨嬶細`app\src\main\assets\pose_landmarker.task`
- 鍏ㄥ眬瀹堟姢锛歚C:\Users\Admin\.claude\watchdog\`
- 鐢ㄦ埛绾?Supervisor锛歚C:\Users\Admin\.claude\agents\project-supervisor.md`
- 鑷埗鐩戞帶褰掓。锛歚c:\Users\Admin\Desktop\鍩庡競澶ц禌椤圭洰\backup\custom-guard-backup\`

### 8.8 涓€涓槑纭殑"鏈獙璇?澹版槑
**AGNES 鑷姩 `claude --continue` 鎭㈠ 灏氭湭鍦ㄧ湡瀹炵殑 AGNES 涓柇鍦烘櫙涓嬭窇閫?*銆傛湰杞?watchdog 鍙獙璇佷簡锛?- 502 妫€娴嬶紙妯℃嫙娉ㄥ叆 last-action.json锛夆渽
- 鐘舵€佸垎绫伙紙AGNES_UPSTREAM_ERROR / NORMAL / GATEWAY_OFFLINE锛夆渽
- 闃叉姈閫€閬块€昏緫锛?0s/60s/120s锛夆渽
浣?`Start-Process claude --continue` 鍦?Claude 鐪熷疄鍥?502 閫€鍑哄悗鑳藉惁鐪熺殑缁笂 session锛?*娌℃湁鐜板満楠岃瘉杩?*銆傚鏋滄柊浼氳瘽瑕佷緷璧栬繖涓兘鍔涳紝**绗竴娆＄湡瀹炶Е鍙戞椂鍔″繀鎵嬪姩纭**銆?
---

## 9. 涓€鍙ヨ瘽鎬荤粨

**P0 鍏ㄥ畬鎴愩€佸彲缂栬瘧銆佸彲瀹夎銆佸彲婕旂ず**锛堥渶 ARM 鐪熸満锛夛紱**鍏ㄥ眬浣?Token 瀹堟姢宸查儴缃?*锛坵atchdog + AGNES 閿欒 Hook + 鐢ㄦ埛绾?Supervisor + Ralph Loop锛夛紱**涓氬姟浠ｇ爜鏈疆鏈啀鏀瑰姩**銆備笅涓€姝ユ渶鎺ㄨ崘锛氳窇瀹屽畧鎶よ嚜娴嬪悗锛岃涔堣繛鐪熸満婕旀繁韫查棴鐜紝瑕佷箞鎶婅烦缁充粠杩戜技鍗囩骇涓虹湡瀹炶鏁般€?
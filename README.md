# 臂伴 ArmPal

> **给爸妈的空间康复陪伴——真人比例示范、手势实时纠正、子女远程看到"今天练了没有"。**

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

臂伴是一款面向居家上肢康复与老年防跌倒场景的 PICO 空间应用:训练者戴上头显,在客厅固定"训练位"跟随真人比例的示范者做坐姿训练;应用通过手部追踪实时对比动作轨迹,给颜色 + 语音反馈;训练结束生成今日卡片,让不在身边的子女安心。

**参赛**:PICO Space Program 2026 · Spatial 空间应用创作赛道(Kotlin + PICO Spatial SDK)

> ⚠️ 合规边界:本产品不做医疗诊断、不宣称疗效,定位为训练陪伴与动作引导,不替代专业医疗建议。

## 技术栈

- **PICO Spatial SDK 6.2.8**(Kotlin + Jetpack Compose 声明式 Spatial UI)
- **Full Space(Stage)**:手部追踪(`com.picovr.permission.HAND_TRACKING`)与空间锚点仅在 Full Space 可用
- **三窗架构**:Full Space 内自开 3 个 WindowContainer(训练主窗 / 动作纠正侧窗 / 报告窗),通过 `Placement` 相对主窗左右摆放
- **追踪**:`HandTrackingProvider`(手部 26 关节)+ `HMDTrackingProvider`(头显位姿,用于肩部估计与 yaw 旋转)
- 目标平台:PICO Space Pro · PICO OS 6;开发验证以 PICO Emulator 为主

## 当前实现状态(2026-11-14 W2 进行中)

| 模块 | 状态 | 说明 |
|---|---|---|
| 三窗渲染 + 摆放 | ✅ | 声明 + 顺序打开 + `Placement.left/right` 相对主窗摆放 |
| 追踪接入 | ✅ | 手部 + 头显 Provider;权限授权后自动重启;丢失 >500ms 降级 |
| 单动作闭环 | ✅ 核心链路 | 抬举角 → 反馈色 → 状态机计数 → 三窗联动(示范者仍为文案/仪表,3D 真人模型 W3) |
| 无真机演示 | ✅ | 6 秒无有效手部数据自动切"演示模式",按脚本注入达标/不足/不对称三种轨迹 |
| 动作模板库 | ✅ 加载器 | `exercises.json` → `ExerciseRepository`;JSON 可扩展(见下) |
| 本地存储 / 报告持久化 | ⏳ W2.5 | 当前报告窗为内存态 |
| 语音/空间音频 | ⏳ W3 | |
| 空间锚点 streak | ⏳ W3 | |

算法与追踪均为端侧处理;`analysis/` 包无 SDK 依赖,JVM 单测 31 个全绿。

## 架构

```
┌──────────────────────────── Full Space (Stage) ─────────────────────────────┐
│  ┌─ 训练主窗 ────────┐  ┌─ 纠正侧窗 ─────────┐  ┌─ 报告窗 ───────────┐        │
│  │ 动作/节奏/次数反馈  │  │ 抬举角仪表/对称性   │  │ 今日结果/免责声明   │        │
│  └─────────┬─────────┘  └──────────┬─────────┘  └──────────┬─────────┘        │
│            └───────────────┬───────┴───────────────────────┘                  │
│                    SessionState (StateFlow,单一状态中枢)                        │
│                    TrainingSessionController                                    │
│              ┌──────────────┴───────────────┐                                 │
│              │  Analysis(纯 Kotlin,可单测)  │                                 │
│              │  BodyFrameEstimator / AngleMetrics / RepCounter / DTW          │
│              └──────────────┬───────────────┘                                 │
│                    TrackingSource(TrackingFrame 流)                            │
│              ┌──────────────┴───────────────┐                                 │
│              │ PicoTrackingSource(手+HMD)  │  DemoTrackingSource(脚本注入)   │
│              └──────────────────────────────┘                                 │
└───────────────────────────────────────────────────────────────────────────────┘
```

单一状态中枢:三窗共享同一个 `StateFlow<SessionState>`,窗口之间不直接通信;
追踪层把 SDK 数据转换为 `TrackingFrame`(米制 `Vec3`),analysis 层只吃纯 Kotlin 数据,
因此算法可单测、也可用 `DemoTrackingSource` 注入合成轨迹做无真机演示。

## 仓库结构

```
app/src/main/java/cn/armpal/rehab/
├─ platform/      # Application / LaunchActivity(Stage 元数据)
├─ content/       # HomeStage(Stage 内容)+ ExerciseCatalog(exercises.json 加载)
├─ windows/       # 训练主窗 / 纠正侧窗 / 报告窗 + 适老色板
├─ tracking/      # TrackingSource 抽象、PicoTrackingSource(手+HMD)、DemoTrackingSource、权限运行时
├─ analysis/      # 核心算法(纯 Kotlin,可单测):角度/状态机计数/DTW/肩部估计
├─ session/       # TrainingSessionController(状态中枢)+ SessionState
├─ anchor/        # (W3)训练位空间锚点
├─ data/          # (W2.5)Room / DataStore
├─ audio/         # (W3)语音引导 + 空间音频
└─ ui/            # (W4)主题/通用组件
```

## 快速开始

### 环境

- macOS(Apple Silicon)/ Windows;pico-cli 0.7.0+
- Node.js 20+;Android SDK(API 35);PICO Emulator(按需下载)
- **JDK 17+**(推荐 pico-cli 由 primer 托管的 JDK 21):`pico-cli project detect` 会自动处理;
  手动 `./gradlew` 构建时需显式设置 `JAVA_HOME` 指向 JDK 17+。
- `local.properties`(含 `sdk.dir` / `spatial.tools.dir`)由 pico-cli 生成且**不提交**;
  手动构建前请确认该文件存在,否则 Gradle 会报 SDK 路径错误。
- 建议把工程放在**纯英文路径**下(本次开发目录含中文,沙箱/无 Kotlin daemon 环境下会触发
  Kotlin 2.1 的 argfile 转义问题;正常桌面构建不受影响)。

### 构建

```bash
# 方式一:pico-cli(推荐)
pico-cli project detect        # 确认工程被识别
pico-cli emulator install      # 下载 PICO Emulator(首次)
pico-cli emulator start        # 启动模拟器
pico-cli app install           # 构建并安装到模拟器
pico-cli app launch            # 启动应用
pico-cli capture screenshot    # 截图验证

# 方式二:Gradle 直接构建
./gradlew assembleDebug

# 单测(算法/模板)
./gradlew testDebugUnitTest
```

### 演示模式(无真机/模拟器)

模拟器没有真实手势,应用会自动兜底:

1. 启动后先尝试真实手部 + 头显追踪;
2. **6 秒内没有有效手腕数据** → 自动切换 `DemoTrackingSource`;
3. 演示脚本每 15 秒轮换:`GOOD`(对称抬到 85°,计数+绿色)→ `INSUFFICIENT`(只到 55°,黄色"再抬高一点")→ `ASYMMETRIC`(左右差明显);
4. 三窗会明确标注"演示模式:模拟手部轨迹",不冒充真实追踪。

开发调试可把 `tracking/TrackingRuntime.kt` 的 `FORCE_DEMO_MODE` 改为 `true` 强制演示模式。

### 权限

应用仅申请:
- `com.picovr.permission.HAND_TRACKING` — 手部追踪(Full Space)
- `SPATIAL_ANCHOR` — 训练位锚点(完整版启用,W3)

手部追踪与训练数据全部**端侧处理、本地存储**,不联网上传;`android:allowBackup="false"`。

## 开源与二次开发

- 许可:Apache-2.0
- **动作模板 JSON 可扩展**:在 `app/src/main/assets/exercises.json` 中按模板字段新增动作即可扩展动作库,格式见下。
- 核心算法(`analysis/`)为纯 Kotlin 无 SDK 依赖,可独立复用与单测。

### 如何新增一个动作模板

`app/src/main/assets/exercises.json` 顶层是 `{ "version": 1, "exercises": [...] }`。新增动作 = 在 `exercises` 数组里追加一条:

```json
{
  "id": "seated_arm_raise_l",
  "name": "坐姿左臂抬举",
  "targetJoint": "WRIST_L",
  "targetElevationDeg": 80.0,
  "reps": 12,
  "holdMs": 500,
  "sets": 2,
  "restSec": 30,
  "description": "换到左臂,动作与右侧一致",
  "templateTrajectory": [[0.0, -0.3, 0.0], [0.5, 0.1, 0.0]]
}
```

字段说明:

| 字段 | 说明 |
|---|---|
| `id` | 唯一标识(用作动作库 key) |
| `name` | 主窗展示名 |
| `targetJoint` | 作用侧:`WRIST_R` / `WRIST_L`(以右手为主,后续可扩展 `HEAD`) |
| `targetElevationDeg` | 达标抬举角,**0°=自然垂下,90°=侧平举,180°=过头** |
| `reps` / `sets` / `restSec` | 目标次数 / 组数 / 组间休息(秒) |
| `holdMs` | 顶点保持时长(毫秒);状态机要求 ≥ 该值才计一次 |
| `templateTrajectory` | 标准轨迹点(归一化空间,用于 W2.5 报告页 DTW 质量分) |
| `description` | 可选,适老提示文案 |

加载失败(JSON 损坏/字段缺失)时动作库返回空列表并降级,不会崩溃;未知字段会被忽略,便于向后兼容。
`targetElevationDeg` 与 `AngleMetrics`、`RepCounter` 使用同一角度约定,新增动作时不要引入第二套语义。

## 技术验证记录(WIP)

> 开发过程中的关键验证结论与踩坑记录,持续更新。

| 日期 | 验证项 | 结论 |
|---|---|---|
| 2026-11-14 | 工具链 | pico-cli 0.7.0:工程模板(SDK 6.2.8)自带 PICO CDN 下载,无需 Android Studio;便携 Java 21 由 primer 托管 |
| 2026-11-14 | 网络 | github.com 主站克隆受阻(api.github.com 可用),官方模板/CDN/npmmirror 均正常 |
| 2026-11-14 | 构建 | stage 模板 + 三窗骨架 `assembleDebug` 通过(AGP 8.13.2 / Kotlin 2.1.20 / Spatial SDK BOM 6.2.8);Gradle 走腾讯镜像 + 官方 sha256 校验 |
| 2026-11-14 | 算法单测 | analysis 包 20 个 JVM 单测全绿(RepCounter 状态机 / 角度 / DTW / 校准估计) |
| 2026-11-14 | API 实测 | 6.2.8 API:`HandTrackingProvider`(DataProvider 流)、`SupportState` 含 WITHOUT_PERMISSION/NOT_IN_FULL_SPACE、`WindowContainer(name, Form, WindowContainerSize(w,h,d,LengthUnit))`、`DataListener.onProvideData` |
| 2026-11-14 | **多窗 Spike A** | **三窗(训练/纠正/报告)在模拟器 Full Space 渲染成功**。关键坑:**声明 ≠ 打开** —— `WindowContainer(id=...)` 只注册声明,必须 `LocalSpatialNavigator.openWindowContainer(id)` 显式打开(参照官方 WelcomeSpace);尺寸用 Dp 构造 |
| 2026-11-14 | **手部追踪 Spike B** | 权限链路端到端通:manifest 声明 → `WITHOUT_PERMISSION` → 系统弹窗 → `pm grant` → `start=SUCCESS support=SUPPORTED`,腕关节数据流入(模拟器无真实手,坐标 0,0,0 属"有限手势"预期)。自动化授权用 `adb shell pm grant` |
| 2026-11-14 | 模拟器启动 | pico-cli `emulator start` 在本机死锁,绕过方案:直接跑 bundle 内标准 emulator 二进制;PICO 魔改的 searchPaths 按 `sdk_root/<sysdir 项>` 独立拼接,需 `sdk_root/6.2` 符号链接;内核用 `-kernel` 显式指定 |
| 2026-11-14 | 稳定性坑 | 对前台 Full Space 应用 `adb install -r` 会挂死模拟器(QEMU CPU 线程 hang)——重装前先 `am force-stop` |
| 2026-11-14 | **W2 算法修正** | 抬举角统一为"0°=垂下/180°=过头";RepCounter 增加释放阈值(顶点保持 3s 由 4 次修正为 1 次,回归用例锁定);T-pose 校准不再把腕距当肩宽(肩宽用解剖学先验 + 臂长反推) |
| 2026-11-14 | **W2 追踪修正** | 权限授权后由巡检自动 `ensureStarted()`(无需重启应用);加入 `HMDTrackingProvider` 提供头显位置/yaw;`isSane()` 过滤 (0,0,0)/NaN;DataListener 不回调主线程的约束改为轮询 `latestData` |
| 2026-11-14 | **W2 单测** | 31 个 JVM 单测全绿(含 P0 回归:顶点保持不重复计数、未释放不计数、校准不压缩角度量程、JSON 目录解析) |
| 2026-11-14 | **W2 闭环端到端验收 ✅** | 模拟器实测:真实源启动(手+HMD SUPPORTED)→ 6s 无手数据自动切演示模式 → 三脚本 15s 轮换,计数器实测跑到 9/12 次(顶点 84° ≥ 目标 80°),侧窗实时角度仪表(67°/目标线 80°)+ 颜色反馈,报告窗同步进度;`Placement.left/right` 三窗摆放生效。证据:`docs/images/w2-closed-loop-demo.png` |
| 2026-11-14 | W2.5 依赖 | Room 2.8.5 + KSP(2.1.20-1.0.32;KSP2 2.0.1 与 AGP 8.13.2 的 javaPreCompile NPE 不兼容)+ DataStore 1.1.7;`repo.maven.apache.org`/`maven.aliyun.com` 直连 TLS 均被断 → Gradle 走本机代理(~/.gradle/gradle.properties)+ 阿里云镜像兜底 |
| 2026-11-14 | **W2.5 落库端到端 ✅** | Room 持久化链路实测:演示会话完整跑完(24 次 = 12×2 组)→ `session completed` 落库 → 报告窗显示"1 场训练";组间休息 30s 自动推进到第 2 组;**force-stop 重启后历史仍在、免责声明不再弹**(DataStore) |
| 2026-11-14 | W2.5 合规 | 首次启动免责声明 gate:确认前只开训练主窗渲染"使用须知",确认后才开三窗。踩坑:① Stage 内容层不渲染纯 Compose 覆盖(gate 放进训练窗);② `remember(true)` 初始值导致开窗竞态(改 null 三态);③ 未确认时 bind 也别启动(演示源会提前计数) |
| 2026-11-14 | W2.5 动作质量 | DTW 质量分接入会话链路(`RepQualityScorer`):每次 RepCompleted 对腕轨迹 vs 模板打分,模板语义 = 肩相对偏移(与 exercises.json 一致);报告窗显示本场平均分 |
| 2026-11-14 | **30 分钟 soak ✅** | 模拟器连续 30 分钟演示模式全速跑:pid 全程存活、crash buffer 0 条(准入红线达标; soak 期间勿 install/install 会杀进程导致 soak 无效——第一次就栽在这) |
| 2026-11-14 | **W3 扩展落地 ✅** | 动作库 5 个(含颈部 HEAD_YAW/HEAD_PITCH,头显姿态驱动计数,模拟器实测 42°≥40° 计数)、动作选择 chips(选中高亮+rebind+持久化)、锚点训练位 UI(创建/找回/降级)、TTS 语音引导(模拟器无 TTS 引擎静默降级)、3D 几何体示范者(与仪表同源驱动) |
| 2026-11-14 | **W4 家属卡片 ✅** | Canvas 原生绘制 1080×1440 品牌卡片 → MediaStore Pictures/ArmPal → 系统分享面板(模拟器实测:文件落盘 89KB、Chooser 弹出);含免责声明与"数据仅存本机" |
| 2026-11-14 | **W5 素材草稿 ✅** | `tools/gen_assets.py`(uv+Pillow)程序化生成符合 VibeLab 规格的图标/海报/详情图(56~239KB,均在限制内);终稿可替换后重跑 |
| (待) | 示范者视觉调优 | 机制就位(编译+静默降级),面板摆放/比例需 W4 视觉审计专时段迭代 |
| (待) | TTS/锚点真机验证 | 模拟器无 TTS 引擎(静默降级);锚点找回需真实感知环境 |

## Roadmap

- [x] W1:环境 + stage 模板工程 + 三窗/手部追踪/坐标转换三个 spike
- [x] W2:单动作核心闭环(坐姿抬臂):跟随 → 纠正 → 计数 → 反馈(示范者为文案/仪表,3D 真人模型 W3)
- [x] W2.5:本地存储 + 报告窗持久化 + 30 分钟稳定性(11/25 提交占位版)
- [x] W3:动作库 3–5 个 + 空间锚点打卡 streak + 语音引导 + 3D 示范者
- [ ] W4:适老化视觉打磨 + 家属报告卡片导出
- [ ] W5:素材/视频/文档 + 冻结提交

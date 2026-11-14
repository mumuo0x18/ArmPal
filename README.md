# 臂伴 ArmPal

> **给爸妈的空间康复陪伴——真人比例示范、手势实时纠正、子女远程看到"今天练了没有"。**

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

臂伴是一款面向居家上肢康复与老年防跌倒场景的 PICO 空间应用:训练者戴上头显,在客厅固定"训练位"跟随真人比例的示范者做坐姿训练;应用通过 26 关节手部追踪实时对比动作轨迹,给颜色 + 语音反馈;训练结束生成今日卡片,让不在身边的子女安心。

**参赛**:PICO Space Program 2026 · Spatial 空间应用创作赛道(Kotlin + PICO Spatial SDK)

> ⚠️ 合规边界:本产品不做医疗诊断、不宣称疗效,定位为训练陪伴与动作引导,不替代专业医疗建议。

## 技术栈

- **PICO Spatial SDK 6.2.8**(Kotlin + Jetpack Compose 声明式 Spatial UI)
- **Full Space(Stage)**:手部追踪(`com.picovr.permission.HAND_TRACKING`)与空间锚点仅在 Full Space 可用
- **三窗架构**:Full Space 内自开 3 个 WindowContainer(训练主窗 / 动作纠正侧窗 / 报告窗)
- 目标平台:PICO Space Pro · PICO OS 6;开发验证以 PICO Emulator 为主

## 架构

```
┌────────────────────────── Full Space (Stage) ──────────────────────────┐
│  ┌─ 训练主窗 ───────┐  ┌─ 纠正侧窗 ───────┐  ┌─ 报告窗 ────────┐        │
│  │ 示范者/节奏/组数  │  │ 轨迹对比/角度仪表 │  │ 趋势/streak/分享 │        │
│  └────────┬────────┘  └────────┬────────┘  └───────┬────────┘        │
│  ┌────────▼────────────────────▼───────────────────▼────────┐          │
│  │              TrainingSessionViewModel (StateFlow)        │          │
│  └──────┬──────────────┬───────────────────┬───────────────┘          │
│  ┌──────▼─────┐ ┌──────▼────────┐ ┌────────▼──────┐ ┌─────────────┐   │
│  │ HandTracker│ │ MotionAnalyzer│ │ SessionStore  │ │ AnchorManager│  │
│  └────────────┘ └───────────────┘ └───────────────┘ └─────────────┘   │
└────────────────────────────────────────────────────────────────────────┘
```

单一状态中枢:三窗共享一个 ViewModel,追踪数据 → 分析 → 状态 → 各窗订阅渲染,窗口间不直接通信。

## 仓库结构

```
app/src/main/java/cn/armpal/rehab/
├─ platform/      # Application / LaunchActivity(Stage 元数据)
├─ stage/         # Full Space Stage 生命周期、场景/光照
├─ windows/       # 三个 WindowContainer:训练主窗 / 纠正侧窗 / 报告窗
├─ tracking/      # HandTracker 封装、坐标转换、追踪健康监测与降级
├─ analysis/      # 核心算法(纯 Kotlin,可单测):角度/状态机计数/DTW
├─ anchor/        # 训练位空间锚点(创建/持久化/识别)
├─ data/          # Room 实体与 DAO、仓库层、周报聚合
├─ content/       # HomeStage + exercises.json 动作模板库
├─ audio/         # 语音引导 + 空间音频方向反馈
└─ ui/            # 主题(适老大字/高对比)与组件(角度仪表/轨迹视图)
```

## 快速开始

### 环境

- macOS(Apple Silicon)/ Windows;pico-cli 0.7.0+
- Node.js 20+;Android SDK(API 35);PICO Emulator(按需下载)

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
```

### 权限

应用仅申请:
- `com.picovr.permission.HAND_TRACKING` — 手部追踪(Full Space)
- `SPATIAL_ANCHOR` — 训练位锚点(完整版启用)

手部追踪与训练数据全部**端侧处理、本地存储**,不联网上传。

## 开源与二次开发

- 许可:Apache-2.0
- **动作模板 JSON 可扩展**:在 `app/src/main/assets/exercises.json` 中按模板字段新增动作即可扩展动作库(详见后续 README 的「如何新增一个动作模板」)
- 核心算法(`analysis/`)为纯 Kotlin 无 SDK 依赖,可独立复用与单测

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
| 2026-11-14 | **手部追踪 Spike B** | 权限链路端到端通:manifest 声明 → `SupportState.WITHOUT_PERMISSION` → 系统弹窗 → `pm grant` → `start=SUCCESS support=SUPPORTED`,腕关节数据流入(模拟器无真实手,坐标 0,0,0 属"有限手势"预期)。自动化授权用 `adb shell pm grant` |
| 2026-11-14 | 模拟器启动 | pico-cli `emulator start` 在本机死锁,绕过方案:直接跑 bundle 内标准 emulator 二进制;PICO 魔改的 searchPaths 按 `sdk_root/<sysdir 项>` 独立拼接,需 `sdk_root/6.2` 符号链接;内核用 `-kernel` 显式指定 |
| 2026-11-14 | 稳定性坑 | 对前台 Full Space 应用 `adb install -r` 会挂死模拟器(QEMU CPU 线程 hang)——重装前先 `am force-stop` |
| (待) | 坐标转换 Spike C | 随 W2 CoordinateMapper 一并验证(模拟器腕坐标恒 0,无法单独验证) |

## Roadmap

- [x] W1:环境 + stage 模板工程 + 三窗/手部追踪/坐标转换三个 spike
- [ ] W2:单动作核心闭环(坐姿抬臂):示范 → 跟随 → 纠正 → 计数 → 反馈
- [ ] W2.5:本地存储 + 报告窗 + 降级链路 + 30 分钟稳定性(11/25 提交占位版)
- [ ] W3:动作库 3–5 个 + 空间锚点打卡 streak + 语音引导
- [ ] W4:适老化视觉打磨 + 家属报告卡片导出
- [ ] W5:素材/视频/文档 + 冻结提交

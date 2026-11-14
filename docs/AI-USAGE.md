# AI 工具使用说明(参赛提交材料)

> 按赛事规则要求填写:AI IDE、主要模型及说明、PICO CLI 使用记录。

## AI IDE / 工具链

| 工具 | 用途 |
|---|---|
| **ZCode**(GLM 系列模型) | 全程主力 AI Agent:规划、编码、代码评审、构建调试、模拟器端到端验证、文档 |
| **PICO CLI 0.7.0**(`@picoxr/pico-cli`) | 工程创建(`project create --sdk-type spatial --template stage`)、SDK 6.2.8 模板获取、模拟器管理(emulator install/create)、安装启动(`app install/launch`)、截图(`capture screenshot --spatial`)、语义树验证(`interact query/act`)、相机控制(`camera move`) |
| **uv + Pillow 脚本** | 提交素材草稿程序化生成(`tools/gen_assets.py`) |

## 主要模型

- **GLM(智谱)**:本仓库全部 Kotlin 代码、算法、UI、测试与文档的生成与迭代;任务规划与验收驱动(每批次 Definition of Done)。

## 使用方式(可复现)

1. 环境搭建全程由 AI 驱动:`npm i -g @picoxr/pico-cli` → `pico-cli project create` → Gradle 构建(过程中 AI 自主处理了 KSP2 兼容性、Maven TLS 网络、模拟器 searchPaths 等环境问题,见 README 技术验证记录)。
2. 开发闭环:AI 编码 → `./gradlew testDebugUnitTest assembleDebug` → 安装到 PICO Emulator → `pico-cli interact query` 断言三窗语义树 → `capture screenshot` 存档 → 修复 → 提交。
3. 代码评审:每个里程碑批次由 AI 做一轮 P0 问题评审(如 RepCounter 重复计数、对比度不足、授权后 Provider 不重启),修复后以回归测试锁定。
4. 算法资产(analysis 包)为纯 Kotlin + JUnit 表驱动测试,AI 生成的实现与测试互为校验。

## 人机分工说明

- 产品定位、PRD、评分对齐策略:由团队制定,AI 依文档执行
- 全部代码/测试/文档:AI 生成,团队验收
- 需要真机/报名/提交的操作:人工执行(AI 给出清单与材料)

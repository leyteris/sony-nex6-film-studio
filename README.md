# Film Studio NEX-6 / 胶片工坊 NEX-6

**中文** · [English](README.en.md)

面向 **Sony NEX-6** 的独立、非官方胶片风格应用。它把 Film Studio 的 15 个风格用 NEX-6 支持的运行时相机参数实现，在实时取景上叠加自绘控制界面，并在退出时恢复相机原始参数。

> **当前状态：`0.2.12-alpha`（versionCode 14），尚未在 NEX-6 实机上安装或运行。** 已完成构建、API 10 静态验收和 Android 37 模拟器冒烟测试；`camera_tested=false`。在取得实机证据前，本项目不声称兼容或可用。

## 项目边界

- **目标机型**：Sony NEX-6，固件 1.03，Android 2.3.7 / API 10。
- **独立应用**：包名 `com.filmstudio.nex6`，可与旧的 a5100 Film Studio 并存安装。
- **仅静态照片**：没有录像界面；PLAY、DISP、MOVIE 键被忽略。
- **运行时参数**：只写 `Camera.Parameters`，退出或暂停时恢复；不改固件、不写相机设置存储、不做初始化。
- **参数级近似**：NEX-6 没有矩阵/Gamma 或 3D LUT 通道，观感不会与 a5100 版逐像素一致。

## 功能

- 15 个风格（10 个富士参考、5 个理光参考），4 档强度（30 / 50 / 70 / 100%）。
- 左右键或拨轮切换风格，上下键切换强度，取景实时变化。
- 三阶段界面：完整面板 → 左下角小胶囊 → 仅取景；DELETE 或 SK2 循环切换。
- 简体中文、繁体中文、英文界面，跟随系统语言；内置 Noto Sans 简/繁子集，解决固件无中日韩字形的问题。
- S1 半按自动对焦，取景中央显示应用自绘的对焦框（白=搜索、绿=合焦、红=失败）。
- 中央键或快门拍摄；拍后检查 `DCIM` 是否出现新 JPEG，并写入日志。
- 上次使用的风格和强度会在下次启动时恢复。
- 卡上日志 `/sdcard/FSNEX6/FSLOG.TXT`，不含 IP 地址或序列号。

## 机型兼容性

| 机型 / 固件 | 本项目状态 |
| --- | --- |
| **NEX-6，固件 1.03** | 开发目标；**尚未实机验证** |
| NEX-6 其他固件 | 未评估，不能从 1.03 推定 |
| a5100 / ILCE-5100 | 不适用；请使用 a5100 版 Film Studio |
| 其他 PMCA 机型 | 未评估；本项目不承诺可用 |

PMCA（PlayMemories Camera Apps）是安装本应用所需的机内平台。安装成功不等于功能可用；首次实机测试清单见 [VALIDATION.md](docs/VALIDATION.md)。

## 按键

| 按键 | 功能 |
| --- | --- |
| 左 / 右、拨轮、转盘 | 切换风格 |
| 上 / 下 | 强度 + / − |
| 中央键、快门 | 拍摄 |
| 半按快门 S1 | 自动对焦；松开取消 |
| DELETE / SK2 | 界面阶段循环（完整 → 胶囊 → 隐藏） |
| MENU / SK1 | 恢复参数并退出 |
| BACK | 恢复参数并退出 |
| PLAY / DISP / MOVIE | 忽略 |

## 15 个风格

| # | 标签 | 名称 |
| --- | --- | --- |
| 0 | PROVIA | Fuji PROVIA Standard |
| 1 | VELVIA | Fuji Velvia Vivid |
| 2 | ASTIA | Fuji ASTIA Soft |
| 3 | CHROME | Fuji Classic Chrome |
| 4 | REALA | Fuji REALA ACE |
| 5 | PRONEG | Fuji PRO Neg. Std |
| 6 | CNEG | Fuji Classic Negative |
| 7 | ETERNA | Fuji ETERNA Cinema |
| 8 | BLEACH | Fuji ETERNA Bleach |
| 9 | ACROS | Fuji ACROS Mono |
| 10 | GR+ | Ricoh GR Positive |
| 11 | GR- | Ricoh Negative Film |
| 12 | HCBW | Ricoh Hi-Contrast B&W |
| 13 | MORI | Ricoh Moriyama Mono |
| 14 | CROSS | Ricoh Cross Process |

每个风格对应的创意风格、饱和、对比、锐度、白平衡和 DRO 参数见 [ARCHITECTURE.md](docs/ARCHITECTURE.md#5-15-个风格)。名称只用于说明风格取向，不代表与富士或理光官方色彩逐像素一致。

## 安装

前置条件：

1. NEX-6 的「应用程序」入口已通过 **sony-nex6-app-menu-unlock** 项目的流程恢复；
2. 相机已启用 Wi-Fi ADB（Sony-PMCA-RE / OpenMemories: Tweak）；
3. 电脑已安装 Android Platform-Tools。

在 Releases 页面下载 `FilmStudioNex6-0.2.12-alpha.apk`（发布后），或使用本地构建产物 `output/FilmStudioNex6-0.2.12-alpha.apk`，然后：

```sh
adb connect CAMERA_IP:5555
adb -s CAMERA_IP:5555 install -r FilmStudioNex6-0.2.12-alpha.apk
```

`CAMERA_IP` 只是占位符，必须替换为相机当前显示的地址，并保留 `:5555` 端口。分享截图或日志时请遮住真实 IP。

当前构建的 SHA-256：

```text
51889474607a71f8b2f0db1ad2aab74f924d6db7c50a72bfaa2a5df0d2f2e349
```

完整步骤、首次检查项、日志取回和故障排查见 **[中文安装教程](docs/INSTALL.zh-CN.md)**。

> **不要**通过相机的「设置 → 初始化」排查问题。CN2 预设会重新关闭应用入口，只能重新走解锁流程。解锁参数仅适用于 NEX-6 1.03 / `CX76001_CN2`，不得套用到其他机型或区域。

## 日志

应用把诊断信息写入存储卡：

```text
/sdcard/FSNEX6/FSLOG.TXT
```

目录和文件名都满足 Sony FUSE 的 DOS 8.3 限制。若目录创建失败，才回退到卡根 `FSLOG.TXT`。日志包含机型、固件、预览尺寸、按键扫描码、参数应用/丢弃、拍摄、落卡检查、对焦结果和异常；**不记录 IP 地址、序列号或照片内容**。

取回方式：

```sh
adb -s CAMERA_IP:5555 pull /sdcard/FSNEX6/FSLOG.TXT
```

如果文件是全 NUL 或内容不完整，先退出应用再拉取，或在相机上把文件压缩成 zip 后传输。

## 构建

需要 JDK 17、Android SDK build-tools 36.0.0、`platforms/android-37.0/android.jar`、OpenSSL 和 Python 3。

```sh
python3 tools/build.py
```

产物写入 `output/FilmStudioNex6-0.2.12-alpha.apk`，同时生成 `validation/FilmStudioNex6-0.2.12-alpha.json`。签名默认使用本地 `.private/signing.pem`；该文件不提交、不上传。缺失时脚本会用一次性调试密钥签名，签名不同，不能覆盖安装已发布版本。

重新生成简/繁字体子集（可选，需要网络与 `fonttools`）：

```sh
python3 -m pip install fonttools
python3 tools/fetch_fonts.py
```

更完整的构建、API 10 门禁、模拟器测试和发布流程见 [DEVELOPMENT.md](docs/DEVELOPMENT.md)。

## 仓库结构

```text
app/            应用源码、Manifest、资源与内置字体
docs/           安装、架构、开发、兼容性、验证与来源说明
tools/          构建、字体生成、API 10 签名工具
validation/     每个版本的构建元数据与 SHA-256（camera_tested=false）
output/         本地构建产物（Git 忽略）
inputs/         本地工具链与第三方输入（Git 忽略）
.private/       签名私钥（Git 忽略，绝不提交）
```

## 已知限制

- 尚未实机验证；无法确认取景、拍摄、落卡、对焦回调和退出恢复在 NEX-6 上的实际行为。
- 参数级近似，不是 a5100 版的矩阵/Gamma 或 LUT 移植。
- 风格只在应用运行期间生效；退出后原生相机不带风格。
- 某些机身可能丢弃色温、微调或 DRO 参数，应用会回退到核心参数并记录 `DROPPED`。
- 对焦框是应用自绘的居中指示，不是机身选择的对焦点。
- 仅静态照片；没有录像功能。
- 预览比例和方向可能需要根据实机再调整。

## 许可与致谢

本项目源自并感谢 [ukiki0718-netizen/sony-a5100-film-studio](https://github.com/ukiki0718-netizen/sony-a5100-film-studio)：a5100 版 Film Studio 定义了本项目的 15 个风格与验证方法，NEX-6 外壳是在其基础上重写的独立实现。

本项目新写代码与文档采用 [PolyForm Noncommercial 1.0.0](LICENSE)。`tools/sign_apk.py` 源自 bonyback1/sony-pmca-ricoh-mod，保持 Apache-2.0；内置 Noto Sans 子集采用 SIL OFL 1.1。Recipe Lab 与 sony-lut-camera 为 GPL-3.0，仅作行为/接口参考，未复制源码。

本仓库不包含 Sony 固件、Sony 基础应用、富士官方 LUT、理光矩阵参数或签名私钥。Sony、PlayMemories、FUJIFILM、Ricoh 等商标归各自权利人所有，本项目与其无隶属、认证或背书关系。

分别见 [LICENSING.md](LICENSING.md)、[NOTICE](NOTICE) 和 [SOURCES.md](docs/SOURCES.md)。

## 文档

- [安装教程（中文）](docs/INSTALL.zh-CN.md) · [Installation guide (English)](docs/INSTALL.en.md)
- [架构与实现](docs/ARCHITECTURE.md)
- [开发与构建](docs/DEVELOPMENT.md)
- [机型兼容性与限制](docs/COMPATIBILITY.md)
- [验证记录](docs/VALIDATION.md)
- [参考来源](docs/SOURCES.md)
- [版本历史](CHANGELOG.md)

# 安装与使用：Film Studio NEX-6（中文）

[项目首页](../README.md) · [English](INSTALL.en.md)

本教程适用于 **Sony NEX-6，固件 1.03，Android 2.3.7 / API 10**。当前版本 `0.2.16-alpha` **尚未在实机上验证**；下面同时给出首轮检查项。日志优先用 DISP 在相机屏上查看，卡上的 `REPORT.TXT` 只作为补充。

> **不要**用相机的「设置 → 初始化」排查问题。CN2 预设会重新关闭「应用程序」入口，只能重新走解锁流程。解锁参数仅适用于 NEX-6 1.03 / `CX76001_CN2`，不得套用到其他机型或区域。

## 0. 开始之前

需要准备：

- 已恢复「应用程序」入口的 NEX-6（见 `sony-nex6-app-menu-unlock` 项目，仅支持 Windows）；
- 已启用 Wi-Fi ADB 的相机（Sony-PMCA-RE / OpenMemories: Tweak）；
- 电脑上的 [Android Platform-Tools / adb](https://developer.android.com/tools/releases/platform-tools)；
- 充足电量、已备份的存储卡，以及可正常读写的 USB 连接；
- APK：`FilmStudioNex6-0.2.16-alpha.apk`。

本项目只安装普通 PMCA 应用，不写固件、不写相机设置存储、不修改解锁用的 Backup 属性。安装 APK 与解锁流程相互独立。

## 1. 下载与校验

从 Releases 页面下载 `FilmStudioNex6-0.2.16-alpha.apk`（tag `0.2.16` 触发的 CI 构建），或使用本地构建产物 `output/FilmStudioNex6-0.2.16-alpha.apk`。

当前本地构建的 SHA-256：

```text
5cc4e4da07772b87de8a177f3c76fe567cdd2d328c1b2e635fd728bf69013001
```

> Release 中的 APK 由 CI 用一次性调试密钥签名，SHA-256 与上面的本地构建不同，校验时请使用该 Release 附带的 `SHA256SUMS.txt`。两种签名不能互相覆盖安装；需要与文档校验值一致时使用本地构建。

macOS / Linux：

```sh
shasum -a 256 FilmStudioNex6-0.2.16-alpha.apk
```

Windows PowerShell：

```powershell
Get-FileHash -Algorithm SHA256 .\FilmStudioNex6-0.2.16-alpha.apk
```

校验的是文件一致性，不是兼容性或法律许可保证。

## 2. 连接相机

在相机的 Tweak → Developer 中查看当前 Wi-Fi ADB 地址。`CAMERA_IP` 只是占位符，必须替换为你自己的地址，并保留 `:5555` 端口：

```sh
adb connect CAMERA_IP:5555
adb devices
```

`adb devices` 中应出现 `CAMERA_IP:5555	device`。如果显示 `offline` 或 `unauthorized`，断开后重新连接，或在相机上重新开关 Wi-Fi ADB。

**隐私**：公开截图、日志或命令记录前，请遮住真实 IP。本项目日志不记录 IP 或序列号。

## 3. 安装

```sh
adb -s CAMERA_IP:5555 install -r FilmStudioNex6-0.2.16-alpha.apk
```

成功时显示 `Success`。包名是新的 `com.filmstudio.nex6`，可以与 a5100 版 Film Studio 并存，不需要先卸载旧应用。

如果出现 `INSTALL_FAILED_UPDATE_INCOMPATIBLE`，说明相机上已有一个签名不同的 `com.filmstudio.nex6`。只卸载这个包后重装：

```sh
adb -s CAMERA_IP:5555 uninstall com.filmstudio.nex6
adb -s CAMERA_IP:5555 install FilmStudioNex6-0.2.16-alpha.apk
```

**不要卸载任何 Sony 系统包或其他不认识的包。** 如果只是签名不同，使用同一份 `.private/signing.pem` 重新构建即可覆盖安装；该私钥不随仓库分发。

## 4. 首次启动与检查

在相机的应用列表打开 **Film Studio NEX-6**。如果列表未刷新，重启相机后再看。

> **先把模式拨盘置于 P / A / S / M。** iAuto 与 SCN 由相机接管创意风格和照片效果，黑白会出彩色、其他风格也可能被覆盖。完整面板底部右侧的橙色 `P/A/S/M:风格` 就是这条提醒；拨盘不在 P/A/S/M 时它也会显示。

按顺序检查并记录：

1. **取景**：实时取景是否出现，覆盖界面是否持续显示，是否会被固件切回原生界面；
2. **风格**：左/右或拨轮切换风格，画面是否实时变化；上/下切换强度 30/50/70/100%；确认底部右侧提示 `P/A/S/M:风格` 完整显示且不与左侧按键提示重叠；
3. **界面阶段**：按一次 DELETE 或 SK2，是否变为左下角胶囊；再按一次是否只剩取景；再按一次回到完整面板；
4. **对焦**：半按快门 S1，取景中央是否出现对焦框（白 → 绿/红），松开是否消失；
5. **拍摄**：中央键或快门是否保存 JPEG；日志应从 `SHOOT requested` 开始，约 2.5–10 秒后出现 `SAVED <文件名> <字节数>` 或 `SAVED no new JPEG`；
6. **语言**：相机系统语言为中文时，界面应显示简体或繁体；日志应有 `FONT ... ok script=zh-CN/zh-TW`；
7. **屏内日志**：按 DISP 打开日志叠加层，标题行应为 `LOG n 起-止`，右上角提示 `DISP:关闭 上下:翻页`；再按 DISP 或 BACK 关闭；
8. **黑白回读**：选到 ACROS / HCBW / MORI 时，右上角应显示 `CM mono FX richtone-mono`（ACROS）或 `CM mono FX rough-mono`（HCBW/MORI）；只要 `CM` 或 `FX` 中有一项是 `mono` 系值，画面就应走灰阶。若显示 `CM standard FX off`，说明两条路径都被固件拒绝，请把这一行连同风格名记录下来；
9. **退出恢复**：按 MENU 或 BACK，日志应出现 `EXIT menu` → `RESTORE ok` → `PAUSE released finishing=true` → `END app` → `EXPORT ...`；回到原生取景后确认颜色和参数已恢复。

首轮检查不需要逐项改相机设置。保持默认「对焦区域」即可；对焦框是应用自绘的居中指示，不代表机身实际选择的 AF 点。

## 5. 日志

**首选：屏内日志。** 0.2.14 起按 DISP 可以直接在相机屏上显示内存中的最近 600 行日志，不依赖存储卡，也不需要电脑：

- 日志打开时，上/下键翻页，DISP 或 BACK 关闭；MENU 始终退出应用；
- 日志打开时其余按键被吞掉；按 MENU 仍会退出应用；
- 顶部 `LOG n 起-止` 表示缓冲区共 n 行、当前显示范围；
- `PROBE ... size=… zero=… text=…` 行说明上次会话写入的 `REPORT.TXT` / `FSLOG.TXT` 读回是否正常：`text=0` 表示读到的全是 NUL。

卡上仍会写两个文件，作为取卡的补充：

```text
/sdcard/FSNEX6/REPORT.TXT   退出/崩溃时生成的快照
/sdcard/FSNEX6/FSLOG.TXT    运行期间的追加日志（崩溃残留）
```

只有目录创建失败时，才回退到卡根。Sony FUSE 不允许同时读写同一个文件：一直追加的 `FSLOG.TXT` 在应用存活时读回会是全 NUL；`REPORT.TXT` 是退出/崩溃时一次性写入并关闭的全新文件。0.2.14 在写入关闭后立刻回读，并在 `FSLOG.TXT` 里记下 `EXPORT-VERIFY size=… zero=… text=…`，下次启动再记录一次 `PROBE`。

如果 `PROBE` 显示 `text=0`，说明文件在卡上读回就是 NUL，或者读取路径有问题：**用读卡器直接取卡上的 `FSNEX6/REPORT.TXT`**，不要用相机 USB/MTP 拖拽，也不要只依赖电脑上的临时副本；正常退出应用后再拔卡。

如果 adb 可用：

```sh
adb -s CAMERA_IP:5555 pull /sdcard/FSNEX6/REPORT.TXT
```

## 5.1 日志事件

日志主要事件：

| 事件 | 含义 |
| --- | --- |
| `START` / `BODY` | 应用启动；机型、固件、平台、Android 版本（不含序列号） |
| `OPEN` / `CAP` / `PREVIEW` | 相机打开方式、支持的参数列表、预览尺寸与重试 |
| `APPLY` / `DROPPED` / `STYLE` / `EFFECT` / `MONO` | 风格参数应用结果、被固件丢弃的 key、`cm=`（color-mode 回读）、`fx=`（picture-effect 回读）；`MONO` 是两路都失败后的重试结论 |
| `PROBE` / `EXPORT-VERIFY` | 启动时回读上次卡上日志、导出后立即回读的结果（`zero`/`text` 字节数） |
| `FOCUS` | `requested` / `locked` / `failed` / `FOCUS-FAIL` |
| `SHOOT` / `SAVED` | 拍摄请求；`DCIM` 中是否出现新 JPEG |
| `UI` | `full` / `pill` / `hidden` |
| `KEY` / `KEYUP` | 每个按键首次按下/抬起的扫描码与 keyCode |
| `RESTORE` / `EXIT` / `PAUSE` / `EXPORT` | 退出、参数恢复与快照导出 |
| `ALIVE` | 每 60 秒心跳 |
| `CRASH` | 未捕获异常 |

日志不记录 IP 地址、序列号或照片内容。

## 6. 更新与卸载

覆盖安装新版本：

```sh
adb -s CAMERA_IP:5555 install -r FilmStudioNex6-<新版本>.apk
```

如果签名相同，可直接覆盖。卸载：

```sh
adb -s CAMERA_IP:5555 uninstall com.filmstudio.nex6
```

卸载只移除应用和它的偏好设置，不会删除 `DCIM` 中的照片，也不会改变解锁状态。

## 7. 故障排查

| 现象 | 可能原因 / 处理 |
| --- | --- |
| 应用列表中没有 Film Studio NEX-6 | 应用入口未解锁，或列表未刷新；重启相机后再看，必要时重新走解锁流程 |
| 打开后只看到原生界面 / 界面不出现 | 按 DISP 看屏内日志；查看 `START`、`OPEN`、`CRASH`、`PREVIEW-FAIL` |
| 找不到日志 | 按 DISP 直接看；卡上文件在 `/sdcard/FSNEX6/REPORT.TXT`、`FSLOG.TXT`，正常退出后再取 |
| 取景黑屏 | 查看 `OPEN` / `PREVIEW` / `PREVIEW-FAIL`；退出重开应用，确认没有其他应用占用相机 |
| 风格切换无变化 | 查看 `APPLY`、`DROPPED`、`STYLE`、`EFFECT` 与右上角 `CM ... FX ...`；某些参数可能被固件拒绝，应用会回退核心参数 |
| 黑白风格仍显示彩色 | **先确认模式拨盘在 P / A / S / M**：iAuto / SCN 下相机接管风格，黑白必然出彩色，这是固件行为。拨盘正确后再看右上角是否为 `CM mono` 或 `FX <单色效果>`。若两项都不是 mono 系值（如 `CM standard FX off`），说明固件同时拒绝了创意风格和照片效果，请记录该行与 `MONO` / `EFFECT` 日志 |
| 快门没有保存 JPEG | 查看 `SHOOT` 与 `SAVED`；`SAVED no new JPEG` 表示相机未在约 10 秒内落卡 |
| 对焦框一直红色 | 光线或对比不足；确认相机「对焦区域」设置；实机红色路径尚未验证 |
| 安装报签名冲突 | 使用同一签名密钥构建，或只卸载 `com.filmstudio.nex6` 后重装 |
| 卡上日志是全 NUL | 先按 DISP 看屏内日志；`PROBE` / `EXPORT-VERIFY` 会显示 `zero`/`text` 字节数。若 `text=0`，用读卡器直接取卡，不要用 MTP 拖拽 |
| 色彩或效果回读显示 `?` | 固件没有暴露对应参数的读回；以 `APPLY` 行记录的目标值为准 |

## 8. 安全与隐私

- 不修改固件，不写相机设置存储，不做工厂重置或初始化。
- 首次实机测试前备份存储卡并保证电量充足。
- 解锁参数仅适用于 NEX-6 1.03 / `CX76001_CN2`；不要套用到其他机型、固件或区域。
- 不要卸载 Sony 系统包，也不要安装来源不明的 APK。
- 本项目是实验软件，不承诺“不会出问题”；在实机确认前，不声称兼容或可用。
- 分享日志前可快速搜索 `IP`、`serial`，但本项目不会写入这些字段；命令记录中的 `CAMERA_IP` 请手动打码。

取得实机结果后，请把机型、固件、应用版本、测试项和 `REPORT.TXT` 一并反馈。只有真实 NEX-6 证据才能把 `camera_tested` 改为 `true`。

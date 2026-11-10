# 架构与实现：Film Studio NEX-6

状态：`0.2.18-alpha` 独立外壳已实现，静态验收和模拟器冒烟测试通过；0.2.13 已在用户的 NEX-6 上启动并写卡，但回传日志全 NUL，**各项行为在实机上均未验证**（`camera_tested=false`）。用户在 0.2.15 实机上确认：拨盘在 iAuto / SCN 时黑白出彩色，P/A/S/M 下正常——0.2.16 因此加入底部 `P/A/S/M` 提示。0.2.17 增加两个徕卡参考 Look（经典、自然），风格总数为 17。0.2.18 修复色温写入越界（曾把 K×100 发给相机），风格不再锁色温，改为 AWB + A-B 微调。目标机型为 **Sony NEX-6，固件 1.03，Android 2.3.7 / API 10**。

> **English summary.** Film Studio NEX-6 is a standalone PMCA app for the first-generation Sony NEX-6. The a5100 Film Studio engine depends on `CameraEx.setRGBMatrix` and the extended gamma table, which the NEX-6 firmware does not expose. This app therefore keeps the Film Studio look list but expresses each look with runtime `Camera.Parameters` the NEX-6 does accept. It is a plain `Activity` + `SurfaceView` + hand-drawn `Canvas` overlay, with every Sony class reached through reflection. It is an approximation, not a LUT or matrix/gamma port.

## 1. 目标与约束

- **目标**：在 NEX-6 上获得可用的 Film Studio 取景与拍摄体验，而不修改固件、不写相机设置存储、不做工厂重置。
- **显示**：640×480 横屏；固件字体不含中日韩字形。
- **运行时**：Android 2.3.7 / API 10；只能用公开 SDK 编译，Sony 接口全部反射。
- **安全边界**：只写 `Camera.Parameters` 运行时参数；退出或暂停时恢复进入前的值。
- **诚实边界**：NEX-6 没有矩阵/Gamma 色彩通道，因此观感不会与 a5100 版逐像素一致。

## 2. 为什么采用独立外壳

a5100 版 `RicohHook.applyHook()` 的观感来自：

- `CameraEx$ParametersModifier.setRGBMatrix(int[])`：15 个风格 × 4 档强度的 3×3 颜色矩阵；
- `CameraEx.createGammaTable()` → `CameraEx.setExtendedGammaTable(table)`：1024 点色调曲线。

NEX-6 固件 1.03 的静态 API 审计（sony-lut-camera 的兼容性资料）显示这些接口缺失：

```text
CameraEx.createGammaTable
CameraEx.getExtendedGammaTable
CameraEx.setExtendedGammaTable
CameraEx$GammaTable.getSize / write / read / release / setPictureEffectGammaForceOff
CameraEx$ParametersModifier.setRGBMatrix
native_color_preview_available: false
```

因此继续给 a5100 APK 打补丁无法恢复完整引擎；独立外壳只使用 NEX-6 确实提供的接口，是更稳妥的路线。Recipe Lab 已在 NEX-6 上验证过 Activity + SurfaceView + 自绘覆盖层 + `Camera.Parameters` 的结构，本项目沿用这一结构，但代码全部新写。

## 3. NEX-6 可用的运行时参数

这些 key 的名称和格式逐项对照过 Recipe Lab 在 NEX-6 上的实现；它们同时作用于实时取景和机内 JPEG。

| 能力 | 运行时 key | 取值 |
| --- | --- | --- |
| 创意风格 | `color-mode` | standard / vivid / neutral / portrait / landscape / mono / sunset / sepia 等 |
| 对比度 | `contrast` | -3..+3 |
| 饱和度 | `saturation` | -3..+3 |
| 锐度 | `sharpness` | -3..+3 |
| 白平衡 | `whitebalance` | auto（0.2.18 起风格不锁色温） |
| 色温 | `color-temperture-white-balance` | 真实 K，如 `4000`（Sony 固件拼写就是 `temperture`）；风格不再写该键，仅随原参数捕获与恢复 |
| 白平衡微调 | `light-balance-for-white-balance`（A-B）<br>`color-compensation-for-white-balance`（G-M） | -7..+7；相机以琥珀为正（A-B）、以洋红为正（G-M），表内绿为正、写入取反 |
| DRO | `dro-mode` / `dro-level` | off / auto / 1..5 |
| 图片效果 | `picture-effect` | off / richtone-mono / rough-mono（单色） |
| 画质 | `storage-fmt` / `jpeg-quality` | jpeg；50 |
| 矩阵模式 | `rgb-matrix-mode` | 仅在相机暴露该 key 时写 `false` |

`color-mode` 不再把固件未列出的值降级为 `standard`：Sony 对未知值是静默忽略而不是抛错，降级会让 `mono` 永远变成彩色。现在按风格表发送目标值（含 `mono`/`bw`/`blackwhite` 等别名匹配），`setParameters()` 后回读 `color-mode`。

黑白风格再加一条独立路径：ACROS 同时写 `picture-effect=richtone-mono`，HCBW / MORI 写 `rough-mono`。Picture Effect 由相机自己的图像管线处理，不依赖 Creative Style 是否接受 `mono`；只要两路中有一路被保留，成片就应是灰阶。两路都回读并显示在右上角（`CM mono FX richtone-mono`）。效果开启时相机忽略创意风格滑块，因此低强度对对比/锐度的缩放在效果路径下可能不生效——这是为保证黑白而接受的取舍。`rgb-matrix-mode` 未知时不写，避免整个参数包被拒绝。

## 4. 应用结构

```text
MainActivity (Activity)
├─ SurfaceView            相机实时取景
├─ UiView (Canvas)        顶栏 / 强度 / 5 张风格卡 / 胶囊 / 提示 / 对焦框 / 日志叠加层
├─ Cam                    CameraEx 反射 + Camera.Parameters
│  ├─ open()              CameraEx.open(0, null) → getNormalCamera()
│  ├─ apply()             写入风格参数，失败时退化到核心参数
│  ├─ focus()             Camera.autoFocus(callback)
│  ├─ shoot()             Camera.takePicture(null, null, null)
│  └─ restore()           按进入时捕获的原值逐项恢复
└─ Log8                   内存环形缓冲（600 行）+ REPORT.TXT（快照）+ FSLOG.TXT（追加）
```

源码位于 `app/src/com/filmstudio/nex6/`：

| 文件 | 职责 |
| --- | --- |
| `MainActivity.java` | Activity、按键分发、UI 阶段、拍摄与落卡检查、日志心跳 |
| `Cam.java` | Sony 相机打开/参数/对焦/拍摄/恢复 |
| `UiView.java` | 全部界面绘制，无图片资源 |
| `Looks.java` | 17 个风格的参数表与强度缩放 |
| `Lang.java` | 简/繁/英界面文本与内置字体选择 |
| `Log8.java` | 8.3 安全路径上的尽力日志、回读探针与内存快照 |

编译后共 22 个 class（含内部类与匿名回调），只引用 35 个 `android.*` 类型，全部存在于 Android 2.3.3 stub。

## 5. 17 个风格

| # | 标签 | 名称 | 创意风格 | 饱和 | 对比 | 锐度 | 白平衡 | DRO |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 0 | PROVIA | Fuji PROVIA Standard | standard | +1 | 0 | 0 | AWB | auto |
| 1 | VELVIA | Fuji Velvia Vivid | vivid | +2 | +1 | +1 | AWB | auto |
| 2 | ASTIA | Fuji ASTIA Soft | portrait | 0 | -1 | -1 | AWB | auto |
| 3 | CHROME | Fuji Classic Chrome | neutral | -1 | +1 | 0 | AWB A+1 | auto |
| 4 | REALA | Fuji REALA ACE | standard | +1 | +1 | 0 | AWB | auto |
| 5 | PRONEG | Fuji PRO Neg. Std | neutral | -1 | -1 | 0 | AWB | auto |
| 6 | CNEG | Fuji Classic Negative | neutral | -1 | +1 | 0 | AWB A+2 | auto |
| 7 | ETERNA | Fuji ETERNA Cinema | neutral | -2 | -1 | -1 | AWB | auto |
| 8 | BLEACH | Fuji ETERNA Bleach | neutral | -3 | +2 | +1 | AWB | off |
| 9 | ACROS | Fuji ACROS Mono | mono | 0 | +1 | +1 | AWB | auto |
| 10 | GR+ | Ricoh GR Positive | vivid | +1 | +1 | +1 | AWB | auto |
| 11 | GR- | Ricoh Negative Film | neutral | -1 | +1 | 0 | AWB A+1 | auto |
| 12 | HCBW | Ricoh Hi-Contrast B&W | mono | 0 | +3 | +2 | AWB | off |
| 13 | MORI | Ricoh Moriyama Mono | mono | 0 | +3 | +3 | AWB | off |
| 14 | CROSS | Ricoh Cross Process | vivid | +3 | +2 | 0 | AWB A+2 | off |
| 15 | LEICA-C | Leica Look Classic | neutral | -1 | -1 | 0 | AWB | auto |
| 16 | LEICA-N | Leica Look Natural | standard | 0 | 0 | 0 | AWB | auto |

强度为 30% / 50% / 70% / 100%。以 100% 为完整值，低强度对饱和、对比、锐度和 A-B/G-M 做线性缩放并四舍五入；创意风格与 DRO 档位不插值。黑白风格在低强度下仍是黑白；若单色 Picture Effect 生效，对比/锐度滑块可能被相机忽略，灰阶由效果本身决定。

ACROS 另写 `picture-effect=richtone-mono`；HCBW / MORI 写 `rough-mono`（高反差黑白）。

两个徕卡 Look 参考 [Chai-Yu/sony-a7r2-film-studio](https://github.com/Chai-Yu/sony-a7r2-film-studio) 的命名与机身回退映射：Natural 用 `standard`；Classic 用 `neutral`，再用少量降饱和/降对比近似其回退观感。它们没有使用该项目的 LUT、矩阵或曲线数据。

> 这是参数级近似，不是 LUT 移植。风格名称只用于说明取向，不代表与富士、理光或徕卡官方色彩逐像素一致。

## 6. 参数应用与回退

每次切换风格或强度：

1. 计算当前强度下的参数集；
2. `color-mode` 按风格表的目标值发送；若 `color-mode-values` 列出的拼写不同，尝试别名（如 `mono`），但**不**因为未列出就降级；黑白风格同时按 `picture-effect-values` 解析单色效果拼写（`richtone-mono` / `rough-mono` 及其别名）；
3. 完整写入 `color-mode`、饱和、对比、锐度、WB/Kelvin、A-B/G-M、DRO、`picture-effect`（彩色风格写 `off`，黑白风格写单色效果）、`storage-fmt=jpeg`、`jpeg-quality=50`；
4. `setParameters()` 后回读，把被相机丢弃的 key 写入 `DROPPED`；
5. 黑白风格两路都不是 mono 系值时，先单独重写 `color-mode` 并逐个尝试 mono 拼写，再单独重写单色效果并逐个尝试拼写；每次重试都回读，最终结论写入 `MONO` 与右上角 `CM … FX …`；
6. 回读结果与目标不一致的其它参数只记录，不阻塞后续拍摄；
7. 完整写入抛异常时，退化为只含风格、饱和、对比、锐度、图片效果和 JPEG 设置的核心子集；再失败则界面显示「参数被拒绝」。

退出、返回或暂停时，按进入时捕获的 13 个 key 逐项恢复。恢复失败写 `RESTORE-FAIL`，不会让应用崩溃。

## 7. 按键与界面阶段

| 按键 | 扫描码 | 功能 |
| --- | --- | --- |
| 左 / 右 | 105 / 106 | 上一个 / 下一个风格 |
| 拨轮、转盘 | 522/523、525/526、528/529、634/635 | 切换风格 |
| 上 / 下 | 103 / 108 | 强度 + / − |
| 中央键 | 232 | 拍摄 |
| 快门 S2 | 518 | 拍摄（抬起时调用 `cancelTakePicture`） |
| 半按 S1 | 516 | 自动对焦；抬起取消 |
| DELETE / SK2 | 595 / 513 | 界面阶段循环（抬起触发） |
| MENU / SK1 | 514 / 229 | 恢复参数并退出（抬起触发） |
| BACK | — | 恢复参数并退出 |
| DISP | 608 | 打开 / 关闭屏内日志；打开时上/下翻页，BACK 关闭，MENU 仍退出 |
| PLAY / MOVIE | 207 / 515 | 吞掉，避免误触 |

没有 Sony 扫描码时，`fallback()` 把标准 Android keyCode 映射到同一功能；`KEYCODE_FOCUS`（80）映射到 S1，`KEYCODE_CAMERA` 映射到 S2，`KEYCODE_INFO`（165）映射到 DISP，便于模拟器测试。实机扫描码优先。

界面有三阶段，短按 DELETE 或 SK2 依次切换：

1. **full**：顶部状态条（应用名、当前风格、强度、参数摘要、右上角 `CM <color-mode> FX <picture-effect>`）、强度分段、固定 5 张风格卡、底部提示行；彩色风格只显示 `CM`。底部提示行左侧是按键说明，右侧同一基线右对齐一条橙色 `P/A/S/M:风格`（英 `P/A/S/M: STYLE`）模式提示；
2. **pill**：左下角小胶囊，显示风格标签 + 中文简称 + 强度；错误信息显示在胶囊上方；
3. **hidden**：只保留实时取景；切换风格/强度时仍有约 1.4 秒的居中提示。

三阶段下风格、强度、拍摄、对焦和 MENU 退出均已实现。应用重启从 full 开始。DISP 可随时覆盖一层只读日志，打开时其它按键（除 MENU 退出、BACK 关闭）被吞掉。

### 模式拨盘

iAuto 与 SCN 下 NEX-6 由相机接管创意风格和照片效果，应用写入的 `color-mode` / `picture-effect` 不落地，黑白会出彩色。完整面板右侧的 `P/A/S/M:风格` 提示只提醒用户把拨盘放在 P/A/S/M；它不读取拨盘位置，也不会因模式错误而阻止操作。该现象来自用户实机观察（0.2.15），不是可读日志证据。

### 对焦框

S1 半按调用 `Camera.autoFocus`，`UiView` 在取景中央绘制四角框：搜索中白色、合焦绿色、失败红色；松开立即消失。NEX-6 的 CameraEx 只提供对焦开始/完成监听，**不暴露真实 AF 坐标**，所以这是应用自绘的居中指示框，不是机身选择的对焦点。实际对焦区域仍由相机的「对焦区域」设置决定。

## 8. 语言与字体

界面语言跟随系统 `Locale`：

- `zh-TW`、`zh-HK`、`zh-MO` → 繁体中文；
- 其他 `zh` → 简体中文；
- 其余 → 英文。

固件字体没有中日韩字形，因此 APK 内置两份 Noto Sans 子集（仅 ASCII 和界面用字，SIL OFL 1.1，附许可文件）：

- `app/assets/fonts/NotoSansSC-UI.ttf`
- `app/assets/fonts/NotoSansTC-UI.ttf`

字体由 `tools/fetch_fonts.py` 生成；脚本用 fontTools 校验每个请求字符都在 cmap 中。英文沿用机身字体。风格名、参数摘要和异常详情保持英文，便于与日志对照。

## 9. 日志与隐私

Sony 相机通过 FUSE 层访问存储卡，只接受 DOS 8.3 文件名，且不允许同时读写同一个文件。因此 `Log8` 用两个文件：

- `FSLOG.TXT`：运行期间逐行追加的日志。文件一直被打开追加，应用存活时从外部读回会是全 NUL；它的作用是崩溃残留。
- `REPORT.TXT`：退出（`onDestroy`）或崩溃时，把内存环形缓冲（最近 600 行）一次性写入一个全新文件，`flush` + `getFD().sync()` 后立即关闭、不再打开。快照在写入前先记一条 `EXPORT`，因此文件末尾能看到 `END app` → `EXPORT <路径>`，自证导出完成。同一进程内再次打开应用时，缓冲会保留上一次会话的行，`REPORT.TXT` 因而包含最近几次启动的记录（上限 600 行）。

**卡上文件不是唯一通道。** 0.2.13 的 `REPORT.TXT` 在 Android 37 模拟器上读回为纯文本（0 个 NUL），但两次从真实 NEX-6 经相机 USB/MTP 复制出来的 `REPORT.TXT` 和 `FSLOG.TXT` 都是 100% NUL，文件大小正常且 `FSLOG` 持续增长，说明写入确实发生，问题在读取/传输路径或 FUSE 回写上。因此 0.2.14 增加三条自证渠道：

- **屏内日志**：内存环形缓冲由 `UiView` 直接绘制，DISP 打开，上/下翻页，不经过存储卡；
- **回读探针**：启动时 `PROBE` 回读上次的 `REPORT.TXT`/`FSLOG.TXT`，导出关闭后立即回读并写 `EXPORT-VERIFY`，输出 `size/head/zero/text/other`；
- **路径日志**：`PATH` 记录最终选中的日志路径。

若 `text=0`，应把存储卡取出用读卡器直接复制 `FSNEX6/REPORT.TXT`，不要依赖相机 USB/MTP 或电脑临时目录。

日志目录 `FSNEX6`、追加文件 `FSLOG.TXT`，依次尝试：

```text
Environment.getExternalStorageDirectory()/FSNEX6/FSLOG.TXT
/sdcard/FSNEX6/FSLOG.TXT
/mnt/sdcard/FSNEX6/FSLOG.TXT
/android/mnt/sdcard/FSNEX6/FSLOG.TXT
/storage/sdcard0/FSNEX6/FSLOG.TXT
/sdcard/DCIM/FSNEX6/FSLOG.TXT
/sdcard/FSLOG.TXT        （仅目录创建失败时的最后回退）
```

`REPORT.TXT` 写在选中的 `FSLOG.TXT` 所在目录。主要事件：`START`、`PATH`、`BODY`、`RESUME`、`OPEN`、`CAP`、`PREVIEW`、`ORIGINAL`、`APPLY`、`DROPPED`、`STYLE`、`EFFECT`、`MONO`、`PROBE`、`SHOOT`、`SAVED`、`FOCUS`、`UI`、`KEY`、`KEYUP`、`FONT`、`RESTORE`、`EXIT`、`PAUSE`、`EXPORT`、`EXPORT-VERIFY`、`ALIVE`、`CRASH`。

- `BODY` 只记录机型、固件、平台和 Android 版本，不记录序列号。
- 不记录 IP 地址、照片内容或用户隐私。
- 拍摄后按“拍摄前已有文件名集合”判断 `DCIM` 中是否出现新 JPEG，不依赖 FUSE 的 `lastModified`；重试约 10 秒。

## 10. 风险与已知边界

| 风险 | 影响 | 处理 |
| --- | --- | --- |
| NEX-6 无矩阵/Gamma | 观感与 a5100 版不同 | 文档明确标注“参数级近似” |
| `takePicture(null,null,null)` 未在实机确认 | 可能不保存 JPEG | 首轮实机测试项；`SAVED` 行给出结果 |
| 风格名、微调或 DRO 被固件丢弃 | 某风格无效 | 回读检测 + `DROPPED` + 核心参数降级 |
| 对焦框不是机身 AF 点 | 构图判断可能偏差 | UI 与文档均说明这是居中指示 |
| 预览比例/方向 | 画面可能拉伸 | 沿用 Recipe Lab 的 SurfaceView 方案，实机再调 |
| 退出后不保留风格 | 原生相机不带风格 | 拍摄请在应用内完成；写设置存储不在本版范围 |
| 拨轮方向 | 可能与机身标注相反 | 扫描码固定映射，实机可再调 |
| 快门后预览停止 | 需要恢复取景 | 1.2 秒后只调 `startPreview()` 并重应用参数，最多重试 24×250 ms |

本应用**不修改固件、不写相机设置存储、不做工厂重置**。但它仍是实验软件，不承诺“不会出问题”；实机测试前请备份存储卡并保证电量充足。

## 11. 构建与验证

- 构建：`python3 tools/build.py`；
- 源码：`app/`；签名：`tools/sign_apk.py` + `.private/signing.pem`（不提交）；
- 详细步骤与 API 10 门禁见 [DEVELOPMENT.md](DEVELOPMENT.md)；
- 当前 APK 的静态验收、模拟器证据与 `camera_tested=false` 状态见 [VALIDATION.md](VALIDATION.md)。

## 12. 与 a5100 项目的关系

Film Studio NEX-6 是独立项目，包名 `com.filmstudio.nex6`，可以与 a5100 版 Film Studio 并存安装。

- a5100 版使用矩阵/Gamma 和 3D LUT 拟合参数，本项目只使用 NEX-6 支持的运行时参数；
- 早期“给 a5100 APK 打补丁”的 NEX-6 路线（0.3.x-alpha-nex6）已停止，作为历史资料保存在本地 `legacy/a5100/`，不在本仓库发布；
- 两个项目的签名相同，但包名不同，因此互不覆盖。

许可边界：

- 本项目新写代码与文档：PolyForm Noncommercial 1.0.0；
- `tools/sign_apk.py`：Apache-2.0（源自 bonyback1 的项目）；
- 内置 Noto Sans 子集：SIL OFL 1.1；
- Recipe Lab 与 sony-lut-camera：GPL-3.0，仅作为行为/接口参考，**未复制源码**；
- 不打包 Sony 固件、Sony 基础应用、富士官方 LUT 或理光矩阵参数。

详见 [LICENSING.md](../LICENSING.md)、[NOTICE](../NOTICE) 与 [SOURCES.md](SOURCES.md)。

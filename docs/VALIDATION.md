# 验证记录 / Validation

当前发布候选：`FilmStudioNex6-0.2.16-alpha.apk`（versionCode 18）

```text
SHA-256 5cc4e4da07772b87de8a177f3c76fe567cdd2d328c1b2e635fd728bf69013001
size    79,339 bytes
```

> **`camera_tested=false`。** 0.2.13 已在用户的 NEX-6 上启动过，卡上确实生成了 `REPORT.TXT` 与 `FSLOG.TXT`；但两次经相机 USB/MTP 和远程桌面复制出来的文件都是 100% NUL，无法读取内容，因此取景、拍摄、落卡、对焦、色彩与退出恢复在实机上**均未被验证**。下面的静态检查和模拟器结果不能证明这些行为在相机上可用。
>
> 0.2.15 在用户实机上有一个口头确认的观察：拨盘在 **iAuto / SCN** 时黑白风格依旧出彩色，转到 **P** 后正常出灰阶——这两种自动模式由相机接管创意风格与照片效果。这是用户报告，不是可读日志证据；0.2.16 据此在完整面板底部右侧加入 `P/A/S/M` 提示。`camera_tested` 仍为 `false`。

## At a glance (English)

- **Build**: `com.filmstudio.nex6`, `0.2.16-alpha`, versionCode 18, minSdk/targetSdk 10, dex 035, 11 APK entries, 79,339 bytes, v1 JAR signature accepted for min-sdk 10.
- **Static**: all sources recompiled against the Android 2.3.3 (API 10) stub — 22 classes; the final dex references 35 `android.*` types and all 35 exist in the 2.3.3 stub; the resource table contains only `ic_launcher` and `app_name`; the APK contains dex code only, with no native libraries. Font assets are unchanged from 0.2.14.
- **Emulator**: Android 37 at 640×480/160 installed this exact artifact (with `--bypass-low-target-sdk-block`) and opened the fallback camera. The full panel shows the new right-aligned orange `P/A/S/M: STYLE` hint (`P/A/S/M:风格` in the Simplified Chinese UI) on the bottom tips line without overlapping the left hints. ACROS reported both mono paths read back (`APPLY ACROS 100% mono cm=mono fx=richtone-mono`, top bar `CM mono FX richtone-mono`). DISP opened the in-memory log overlay (`LOG 42 11-42`, hint `DISP:关闭 上下:翻页`); MENU exited and restored. The resulting `FSNEX6/REPORT.TXT` was readable (1,089 bytes in the single Chinese session, 0 NUL); `FSLOG.TXT` recorded `EXPORT-VERIFY size=1089 head=1089 zero=0 text=1064 other=25`. These results only prove the read-back/display chain — the emulator's fake camera echoes any value and cannot prove the NEX-6 firmware accepts either mono path or the hint's wording suits the camera.
- **Hardware**: two user-supplied NEX-6 sessions produced plausible card files (`REPORT.TXT` 2,853 B, `FSLOG.TXT` 21,562 B) but every copied byte was NUL. A later 0.2.15 session produced the mode-dial observation above (user report only). No behavior is log-verified; `camera_tested=false`.

## 1. 当前构建

| 项目 | 结果 |
| --- | --- |
| 包名 | `com.filmstudio.nex6` |
| 版本 | `0.2.16-alpha` / versionCode 18 |
| 显示名 | Film Studio NEX-6 |
| minSdk / targetSdk | 10 / 10 |
| dex | `dex\n035` |
| APK 条目 | 11 |
| 大小 | 79,339 bytes |
| SHA-256 | `5cc4e4da07772b87de8a177f3c76fe567cdd2d328c1b2e635fd728bf69013001` |
| 签名 | v1 JAR，SHA-1，RSA；`apksigner verify --min-sdk-version 10` 通过 |
| 证书 | `CN=SonyPMCADebug`（本地开发证书，SHA-256 `72f30a1a…d133`） |
| 资源表 | 仅 `drawable/ic_launcher`、`string/app_name` |
| 字体 | `NotoSansSC-UI.ttf` 32,196 B；`NotoSansTC-UI.ttf` 35,212 B；各附 OFL 1.1 |
| `camera_tested` | **false** |

`0.2.16` 只改界面提示与版本号，取景、参数应用和日志逻辑与 `0.2.15` 相同：

1. **模式提示**：完整面板的底部提示行右侧新增右对齐的橙色 `P/A/S/M:风格`（繁体 `P/A/S/M:風格`，英文 `P/A/S/M: STYLE`），与左侧按键提示同一基线；胶囊和隐藏阶段不显示；
2. **用字复用**：`风` / `風` / `格` 已在 0.2.14 的字体子集中，无需重新生成 Noto 子集，字体字节未变；
3. **原因**：用户实机确认拨盘在 iAuto / SCN 时相机接管创意风格与照片效果，黑白会出彩色；P/A/S/M 下黑白正常。提示用于避免把模式拨盘误判为应用缺陷；
4. **版本**：Manifest 升至 `0.2.16-alpha` / versionCode 18，`tools/build.py` 与 CI 产物名同步；`validation/` 只保留本版 JSON。

`0.2.15` 的黑白双路径保持不变：ACROS/HCBW/MORI 同时写 `color-mode=mono` 和单色 `picture-effect`（ACROS 为 `richtone-mono`，HCBW/MORI 为 `rough-mono`），两值都回读；未单色时先轮换 `mono` 别名重试 `color-mode`，仍失败再重试效果。照片效果开启时 Sony 忽略创意风格滑块，黑白强度可能不再改变对比，这是为保证灰阶接受的取舍。

下面的静态检查和模拟器结果均针对最终 `5cc4e4da…3001` 执行。

## 2. 静态验收

### 2.1 API 10 编译门

全部源码用 Android 2.3.3 公开 stub 重新编译：

```text
javac -source 8 -target 8
-bootclasspath /tmp/android-2.3.3.jar:$ANDROID_HOME/platforms/android-37.0/android.jar
app/src/com/filmstudio/nex6/*.java
```

结果：22 个 class（含内部类），编译通过。`0.2.16` 只新增 `Lang.modeHint()` 与两处绘制调用，不新增类。任何 API 11+ 的 `android.*` 类型或方法都会在这一步失败；此前的 `KEYCODE_FORWARD_DEL`（API 11）就是这样被发现并移除的。

### 2.2 dex 类型审计

对最终 `classes.dex` 运行 `dexdump -d`，提取全部 `android.*` 类型并与 2.3.3 stub 的 `classes.jar` 比对：

```text
android refs: 35
missing in 2.3.3 stub: 0
```

35 个类型包括 `Activity`、`Context`、`SharedPreferences`、`PackageManager`、`AssetManager`、`Camera` 及其 `AutoFocusCallback`/`Parameters`/`PictureCallback`/`ShutterCallback`/`Size`、`Canvas`、`Paint`、`Rect`、`RectF`、`Typeface`、`Build.VERSION`、`Bundle`、`Environment`、`Handler`、`SystemClock`、`Log`、`KeyEvent`、`SurfaceHolder`/`Callback`、`SurfaceView`、`View`、`ViewGroup.LayoutParams`、`Window`、`FrameLayout`/`LayoutParams`。

### 2.3 资源与清单

- `aapt dump badging`：包名、版本、minSdk/targetSdk 和标签如上表；
- 清单不含 `android:compileSdkVersion` / `platformBuildVersion`（`aapt2 --no-compile-sdk-metadata`）；
- 资源表只有启动图标和应用名，无 framework 私有 ID 引用；
- `classes.dex` 魔数 `dex\n035`；
- v1 签名文件 `META-INF/MANIFEST.MF`、`CERT.SF`、`CERT.RSA` 均存在。

### 2.4 字体

`Lang.java` 共使用 93 个不同汉字。子集按简/繁分别生成：

```text
NotoSansSC-UI.ttf  32,196 B  161 请求字符（95 ASCII + 66 简体）
NotoSansTC-UI.ttf  35,212 B  162 请求字符（95 ASCII + 67 繁体）
```

逐字校验：SC 字体包含全部简体用字，TC 字体包含全部繁体用字；SC 字体只缺繁体变体，TC 字体只缺简体变体（预期）。0.2.14 新增的日志提示字 `关`/`闭`/`翻`/`页` 与 `關`/`閉`/`翻`/`頁` 仍全部在对应 cmap 中，且 APK 内字体与 `app/assets/` 字节一致。0.2.15 和 0.2.16 未改字体资产；0.2.16 的模式提示只用到已有的 `风`/`風`/`格`，已逐字确认在对应 cmap 中。0.2.14 的简体启动记录仍然适用：

```text
FONT fonts/NotoSansSC-UI.ttf ok script=zh-CN
```

0.2.14 已验证简体界面与 DISP 叠加层提示 `DISP:关闭 上下:翻页` 渲染正常、无豆腐块；0.2.16 在简体中文界面下复测了完整面板与 DISP 叠加层，右下角 `P/A/S/M:风格` 与左侧提示无重叠、无豆腐块。繁体字体只做了字符覆盖校验，未在繁体系统语言下截图。

## 3. 模拟器冒烟测试（最终构建）

环境：Android 37 模拟器 `emulator-5554`，`wm size 640x480`、`wm density 160`。安装命令带 `--bypass-low-target-sdk-block`，因为 Android 37 会拒绝 targetSdk 10 的普通安装；这只是模拟器的限制，不是 NEX-6 的要求。

### 3.1 启动、取景与界面提示

安装后以简体中文启动，`REPORT.TXT` 中的本次会话（节选）：

```text
START FilmStudio NEX-6 0.2.16-alpha ui=zh-CN
FONT fonts/NotoSansSC-UI.ttf ok script=zh-CN
PATH /storage/emulated/0/FSNEX6/FSLOG.TXT
BODY model=? fw=? platform=? android=17 sdk=37
OPEN android.hardware.Camera
ORIGINAL captured 13
CAP color-mode=null
CAP picture-effect=null
CAP whitebalance=auto
CAP rgb-matrix-mode=null
APPLY ACROS 100% mono cm=mono fx=richtone-mono
PROBE REPORT.TXT size=1032 head=1032 zero=0 text=1008 other=24
PROBE FSLOG.TXT size=4648 head=4096 zero=0 text=4012 other=84
PREVIEW size=1280x720
APPLY ACROS 100% mono cm=mono fx=richtone-mono
```

截图确认完整面板底部：

- 左侧仍是 `左右:风格 上下:强度 中央:拍摄 DEL:界面 MENU:退出`；
- 右侧同一基线新增橙色 `P/A/S/M:风格`，右对齐到面板边距，与左侧提示之间留有空隙，没有重叠或截断；
- 两行提示都在 640×480 下完整显示，无豆腐块。

![完整面板：右下角 P/A/S/M:风格（Android 37 模拟器，简体中文）](images/ui-full.png)

`APPLY ... cm=mono fx=…` 是两条路径的回读：模拟器假相机原样接收并回显写入值，屏幕上同时显示 `CM mono FX richtone-mono`。它只验证了写入、回读与显示链路，**不能**证明 NEX-6 固件接受 `mono` 或照片效果；假相机也不公布 `picture-effect-values`（`CAP picture-effect=null`）。本次会话没有触发 Kelvin 白平衡，未出现 `DROPPED`。

### 3.2 屏内日志

按 DISP（模拟器映射 keyCode 165）打开叠加层，截图确认：

- 标题 `LOG 42 11-42`，右上角提示 `DISP:关闭 上下:翻页`（简体中文界面）；
- 覆盖层显示两次 0.2.16 会话共 42 行，包括 `START ... 0.2.16-alpha ui=zh-CN`、`APPLY ACROS ... cm=mono fx=richtone-mono`、`EXPORT-VERIFY size=1089 ...`、`KEY scan=608 keyCode=165`；
- 再按 DISP 关闭，回到主界面；MENU 退出仍生效。提示与翻页文案沿用 0.2.14 的实现，本次未改代码。

日志打开时，UP/DOWN 用于翻页（长按 ×3），MENU 退出、BACK 关闭，其余按键由 `onKeyDown` 吞掉，避免误改风格。

![DISP 屏内日志（Android 37 模拟器）](images/log-overlay.png)

### 3.3 MENU 退出、恢复与日志回读

```text
KEY scan=514 keyCode=82
KEYUP scan=514 keyCode=82
EXIT menu
RESTORE ok
PAUSE released finishing=true
END app
EXPORT /storage/emulated/0/FSNEX6/REPORT.TXT
```

`dumpsys window` 显示前台回到 launcher，应用正常结束。单次中文会话的 `REPORT.TXT` 为 1,089 字节、25 行、0 个 NUL；第二次会话结束后为 2,168 字节、50 行、0 个 NUL（两份快照连续存放）。`adb pull /sdcard/FSNEX6/REPORT.TXT` 取回的 UTF-8 文本以 `END app` → `EXPORT <路径>` 结尾；`FSLOG.TXT` 末尾：

```text
EXPORT-VERIFY size=2168 head=2168 zero=0 text=2118 other=50
```

这验证了导出与回读链路本身在 Android 37 上通过；它**不**代表 Sony FUSE 或相机 USB/MTP 的读取行为。

### 3.4 模拟器不能验证的内容

- NEX-6 的真实 Sony 扫描码、拨轮方向和 S1/S2 行为；模拟器没有 DISP 的原始扫描码，只能用 keyCode 165 经 `fallback()` 映射为 608（日志仍记 `scan=608`）；
- 真实拨盘位置与 iAuto/SCN 下相机接管风格的固件行为；模拟器没有模式拨盘，`P/A/S/M` 提示的语义只能由实机确认；
- CameraEx 的 `takePicture` 落卡时序与 `cancelTakePicture`；
- `color-mode=mono` 与 `picture-effect=richtone-mono/rough-mono` 在 NEX-6 固件上的实际接受情况（模拟器假相机会回显任意值，且不公布效果值）；
- 色温、A-B/G-M、DRO 在 NEX-6 固件上的实际接受情况；
- 预览比例、方向和快门后的恢复；
- 对焦失败（红色）路径；
- 退出后原生取景的颜色恢复；
- 繁体系统语言下的实际字体渲染（仅做字符覆盖校验）；
- 640×480 以外的排版；
- Sony FUSE 上 `REPORT.TXT` / `FSLOG.TXT` 的读取结果（这正是 0.2.14 起的 `PROBE` 要在实机上回答的问题）。

## 4. 实机证据

**行为未验证。** `2026-11-09`，用户在 NEX-6 上启动过 0.2.13：

- 卡上生成了 `FSNEX6/REPORT.TXT`（2,853 B）和 `FSNEX6/FSLOG.TXT`（21,562 B，较上一轮 18,710 B 增长）；
- 两份文件经相机 USB/MTP 与 Windows 远程桌面复制到电脑后，逐字节检查均为 100% NUL，无可打印文本；
- 因此无法确认取景、覆盖层、风格、拍摄、落卡、对焦或退出恢复中的任何一项。

文件大小合理且持续增长，说明应用在实机上确实运行并写入了内容；问题出在读取/传输路径或 Sony FUSE 的回写行为。`camera_tested` 必须保持 `false`。

### 4.1 用户实机观察：模式拨盘（0.2.15）

`2026-11-09`，用户在 NEX-6 上安装 0.2.15 后反馈黑白风格仍出彩色；把拨盘从 **iAuto / SCN** 转到 **P** 后，黑白正常显示灰阶。结论：这两种自动模式由相机接管创意风格与照片效果，应用写入的 `color-mode=mono` 与单色效果不会落地；这是固件行为，不是参数写入失败。

这条观察来自用户口述与屏上画面，不是可读日志，因此：

- 它**是**目前唯一指向“P/A/S/M 下风格参数实际生效”的实机线索；
- 它**不足以**把 `camera_tested` 改为 `true`，落卡、对焦、拍摄与退出恢复仍未验证；
- 0.2.16 因此在完整面板底部右侧加入 `P/A/S/M:风格` 提示，并在 README / INSTALL / COMPATIBILITY 中写明拨盘要求。

### 4.2 0.2.16 实机首轮检查

0. 先把模式拨盘置于 **P / A / S / M**，确认底部右侧显示 `P/A/S/M:风格`；iAuto / SCN 下提示仍在，但风格不会生效；
1. 启动后按 DISP，确认屏内日志出现，并记录 `CAP picture-effect=…`；
2. 依次选择 ACROS / HCBW / MORI，记录右上角的 `CM … FX …`，并对屏内日志拍照：
   - 两者都是单色（如 `CM mono FX richtone-mono`）：两路至少一路生效；
   - `CM standard FX off`：两路都被固件丢弃，记录 `APPLY`、`DROPPED`，看是否有 `MONO retry …`；
   - 只有其一为单色：另一路被丢弃，仍应以灰阶为准；
3. 记录 `PROBE REPORT.TXT ... text=…` 与 `PROBE FSLOG.TXT ... text=…`；
4. 遍历风格/强度，记录 `APPLY` / `DROPPED` / `STYLE` / `EFFECT`；注意照片效果开启时创意风格滑块被忽略，黑白强度可能不改变对比；
5. 半按 S1、中央键/快门拍摄，检查成片是否为灰阶，记录 `FOCUS` 与 `SAVED`；
6. 三阶段 UI、简/繁中文；
7. MENU/BACK 退出并恢复；
8. 若 `PROBE` 显示 `text=0`，用读卡器直接复制卡上的 `FSNEX6/REPORT.TXT`。

只有完成并保存证据后，才能把 `camera_tested` 改为 `true`，并在 README 中使用“已验证”“可用”等表述。

## 5. 版本历史

| 版本 | versionCode | 大小 | SHA-256（前 12 位） | 主要内容 |
| --- | --- | --- | --- | --- |
| 0.1.0-alpha | 1 | 22,057 | `4cc9a6f6c678` | 首个独立外壳；15 风格、拍摄、日志 |
| 0.2.0-alpha | 2 | 61,764 | `3afd5a379ec4` | 三阶段 UI、简/繁/英、`FSNEX6/` 日志 |
| 0.2.1-alpha | 3 | 71,700 | `8166b779da31` | 紧凑面板、本地化风格名 |
| 0.2.2-alpha | 4 | 71,766 | `1a187e51a96d` | 胶囊本地化 |
| 0.2.3-alpha | 5 | 71,763 | `de0ea4e42b09` | 切换浮层本地化 |
| 0.2.4-alpha | 6 | 71,834 | `ccdd319b1de4` | hidden 阶段强度反馈 |
| 0.2.5-alpha | 7 | 71,853 | `be6e0f2d8bc3` | 胶囊移到左下角 |
| 0.2.6-alpha | 8 | 71,932 | `ca0f06744d4f` | 字体加载诊断日志 |
| 0.2.7-alpha | 9 | 71,926 | `d6f188c780c4` | 底部提示行居左 |
| 0.2.8-alpha | 10 | 71,959 | `ae36d1284373` | 强度整行居左 |
| 0.2.9-alpha | 11 | 71,935 | `35ae4b99976e` | 左右切风格、上下切强度 |
| 0.2.10-alpha | 12 | 71,937 | `e27706edc403` | 风格卡更扁、与提示行拉开 |
| 0.2.11-alpha | 13 | 72,030 | `4b3273c577b7` | 卡片上下间距对称 |
| 0.2.12-alpha | 14 | 73,408 | `51889474607a` | S1 半按对焦框；目录重组后重建 |
| 0.2.13-alpha | 15 | 74,199 | `6c7121259561` | 修复日志全 NUL：退出/崩溃导出可读 REPORT.TXT 快照 |
| 0.2.14-alpha | 16 | 78,302 | `95e0292f7314` | DISP 屏内日志、回读探针、color-mode 回读与重试 |
| 0.2.15-alpha | 17 | 79,207 | `eccb2b1e31f5` | 黑白双路径：`mono` + 单色照片效果、别名解析与回读重试 |
| **0.2.16-alpha** | **18** | **79,339** | **`5cc4e4da0777`** | **完整面板底部右侧 `P/A/S/M` 模式提示（iAuto/SCN 会接管风格）** |

当前版本的完整元数据在 `validation/FilmStudioNex6-0.2.16-alpha.json`，记录 `camera_tested=false`。历史版本只保留上表摘要，完整旧 JSON 存放在本地 `legacy/validation-history/`，不随仓库发布。

早期“给 a5100 APK 打补丁”的 NEX-6 路线（0.3.x-alpha-nex6）不属于本仓库，源码和验证资料保存在本地 `legacy/a5100/`，不随新仓库发布。

## 6. 日志与复现

- 首选：相机屏上按 DISP 查看内存日志（最近 600 行）；
- 卡上：`/sdcard/FSNEX6/REPORT.TXT`（退出/崩溃快照）；`FSLOG.TXT` 为运行期间追加日志；
- 回读：启动 `PROBE`、导出后 `EXPORT-VERIFY`，报告 `size/head/zero/text/other`；
- 取回：`adb pull /sdcard/FSNEX6/REPORT.TXT`；若为全 NUL，用读卡器直接取卡；
- 构建：`python3 tools/build.py`；
- API 10 门禁与模拟器步骤：见 [DEVELOPMENT.md](DEVELOPMENT.md)。

日志不含 IP 地址或序列号。模拟器结果不替代实机验证。

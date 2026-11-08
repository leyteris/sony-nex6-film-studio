# 验证记录 / Validation

当前发布候选：`FilmStudioNex6-0.2.12-alpha.apk`（versionCode 14）

```text
SHA-256 51889474607a71f8b2f0db1ad2aab74f924d6db7c50a72bfaa2a5df0d2f2e349
size    73,408 bytes
```

> **`camera_tested=false`。没有任何 NEX-6 运行过此构建。** 下面的静态检查和模拟器结果不能证明相机上的取景、拍摄、落卡、对焦或退出恢复可用。

## At a glance (English)

- **Build**: `com.filmstudio.nex6`, `0.2.12-alpha`, versionCode 14, minSdk/targetSdk 10, dex 035, 11 APK entries, 73,408 bytes, v1 JAR signature accepted for min-sdk 10.
- **Static**: all sources recompiled against the Android 2.3.3 (API 10) stub — 21 classes; the final dex references 35 `android.*` types and all 35 exist in the 2.3.3 stub; the resource table contains only `ic_launcher` and `app_name`; the APK contains dex code only, with no native libraries.
- **Emulator**: Android 37 at 640×480/160 installed this exact artifact (with `--bypass-low-target-sdk-block`), started with `ui=zh-CN`, loaded the Simplified Chinese font, opened the fallback camera, switched looks and strength, cycled all three UI stages, held S1 for `FOCUS requested` → `FOCUS locked`, requested a capture, ran the `SAVED` check, and exited with MENU while logging `RESTORE ok` and returning to the launcher.
- **Hardware**: none. The emulator has no CameraEx, no real Sony scan codes, no DCIM output and a fake camera that always reports focus lock. No compatibility or usability claim follows.

## 1. 当前构建

| 项目 | 结果 |
| --- | --- |
| 包名 | `com.filmstudio.nex6` |
| 版本 | `0.2.12-alpha` / versionCode 14 |
| 显示名 | Film Studio NEX-6 |
| minSdk / targetSdk | 10 / 10 |
| dex | `dex\n035` |
| APK 条目 | 11 |
| 大小 | 73,408 bytes |
| SHA-256 | `51889474607a71f8b2f0db1ad2aab74f924d6db7c50a72bfaa2a5df0d2f2e349` |
| 签名 | v1 JAR，SHA-1，RSA；`apksigner verify --min-sdk-version 10` 通过 |
| 证书 | `CN=SonyPMCADebug`（本地开发证书，SHA-256 `72f30a1a…d133`） |
| 资源表 | 仅 `drawable/ic_launcher`、`string/app_name` |
| 字体 | `NotoSansSC-UI.ttf` 31,032 B；`NotoSansTC-UI.ttf` 33,572 B；各附 OFL 1.1 |
| `camera_tested` | **false** |

同一份 `0.2.12` 源码在目录重组前构建为 `b7fe23f7…ace3`；重组后重新构建得到 `51889474…e349`，源码与功能未变，差异仅来自 APK 内的时间戳。下面的静态检查和模拟器结果均针对最终 `51889474…e349` 重新执行。

## 2. 静态验收

### 2.1 API 10 编译门

全部源码用 Android 2.3.3 公开 stub 重新编译：

```text
javac -source 8 -target 8
-bootclasspath /tmp/android-2.3.3.jar:$ANDROID_HOME/platforms/android-37.0/android.jar
app/src/com/filmstudio/nex6/*.java
```

结果：21 个 class，编译通过。任何 API 11+ 的 `android.*` 类型或方法都会在这一步失败；此前的 `KEYCODE_FORWARD_DEL`（API 11）就是这样被发现并移除的。

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

`2026-11-08` 对 `Lang.java` 的全部界面汉字逐字校验：31 组简繁对与 `SCRIPT ==` 直返分支共 86 个汉字，SC 字体（157 字）和 TC 字体（158 字）均无缺字；APK 内字体与 `app/assets/` 字节一致。模拟器启动日志分别出现：

```text
FONT fonts/NotoSansSC-UI.ttf ok script=zh-CN
FONT fonts/NotoSansTC-UI.ttf ok script=zh-TW
```

## 3. 模拟器冒烟测试（最终构建）

环境：Android 37 模拟器 `emulator-5554`，`wm size 640x480`、`wm density 160`。安装命令带 `--bypass-low-target-sdk-block`，因为 Android 37 会拒绝 targetSdk 10 的普通安装；这只是模拟器的限制，不是 NEX-6 的要求。

### 3.1 启动与取景

清除旧日志后安装并启动，日志按序：

```text
START FilmStudio NEX-6 0.2.12-alpha ui=zh-CN
BODY model=? fw=? platform=? android=17 sdk=37
FONT fonts/NotoSansSC-UI.ttf ok script=zh-CN
RESUME open=false surface=false
OPEN-CAMERAEX java.lang.ClassNotFoundException: com.sony.scalar.hardware.CameraEx
OPEN android.hardware.Camera
ORIGINAL captured 13
CAP color-mode=null
CAP whitebalance=auto
CAP rgb-matrix-mode=null
APPLY-FULL java.lang.RuntimeException: setParameters failed
DROPPED whitebalance=auto color-temperture-white-balance=null ...
APPLY CHROME 100% neutral
PREVIEW size=1280x720
```

截图确认实时取景、顶部状态条、强度分段、5 张风格卡和底部提示行同时显示；简体中文无豆腐块。

模拟器的假相机不接受色温白平衡，完整参数写入失败后按设计退化到核心参数，并记录 `DROPPED`。这验证了回退路径，不代表 NEX-6 也会拒绝这些 key。

### 3.2 风格、强度与三阶段

- RIGHT（fallback keyCode 22，扫描码 106）把 CHROME 切到 REALA，日志 `APPLY REALA 100% standard`；
- UP（keyCode 19，扫描码 103）时强度已是 100%，不再变化；
- DELETE（keyCode 67，扫描码 595）依次写 `UI pill`、`UI hidden`、`UI full`；
- 截图确认 pill 位于左下角，hidden 阶段只剩取景，full 阶段恢复完整面板。

### 3.3 S1 对焦

`input keyevent --duration 4000 80`（标准 `KEYCODE_FOCUS`；模拟器没有 Sony 扫描码 516，`fallback()` 将其映射到 S1）：

```text
KEY scan=516 keyCode=80
FOCUS requested
FOCUS locked
KEYUP scan=516 keyCode=80
```

保持半按时的截图显示取景中央的绿色四角框；松开后消失。模拟器假相机总是返回合焦，**红色失败路径没有被触发**，需实机确认。

### 3.4 拍摄与落卡检查

CENTER（keyCode 23，扫描码 232）触发：

```text
KEY scan=232 keyCode=23
SHOOT requested
KEYUP scan=232 keyCode=23
APPLY REALA 100% standard
SAVED no new JPEG (known 2)
```

模拟器的 `takePicture(null,null,null)` 不产生 `DCIM` 文件，所以结果是 `no new JPEG`。检测逻辑此前用两个测试文件验证过正例：拍摄后新增 `DSC00002.JPG` 时记录 `SAVED DSC00002.JPG 21`。NEX-6 的 CameraEx 是否自行落卡仍需实机确认。

### 3.5 MENU 退出与恢复

```text
KEY scan=514 keyCode=82
KEYUP scan=514 keyCode=82
EXIT menu
RESTORE ok
PAUSE released finishing=true
END app
```

`dumpsys activity activities` 显示前台回到 `com.google.android.apps.nexuslauncher/.NexusLauncherActivity`，应用正常结束。

### 3.6 模拟器不能验证的内容

- NEX-6 的真实 Sony 扫描码、拨轮方向和 S1/S2 行为；
- CameraEx 的 `takePicture` 落卡时序与 `cancelTakePicture`；
- `color-mode-values`、色温、A-B/G-M、DRO 在 NEX-6 固件上的实际接受情况；
- 预览比例、方向和快门后的恢复；
- 对焦失败（红色）路径；
- 退出后原生取景的颜色恢复；
- 640×480 以外的排版与不同固件语言环境。

## 4. 实机证据

**无。** 截至 `2026-11-08`，没有任何 NEX-6 安装或运行过本项目。`camera_tested=false`，`cameras_tested=[]`。

首次实机测试需要：

1. 安装并启动，确认取景和覆盖层持续显示；
2. 遍历 15 个风格和 4 档强度，记录 `APPLY` / `DROPPED` / `STYLE`；
3. 半按 S1，确认对焦框和 `FOCUS` 结果；
4. 中央键/快门拍摄，确认 `SAVED <文件名> <字节数>`；
5. DELETE/SK2 三阶段；
6. 简/繁中文显示；
7. MENU/BACK 退出并恢复；
8. 取回 `/sdcard/FSNEX6/FSLOG.TXT`。

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
| **0.2.12-alpha** | **14** | **73,408** | **`51889474607a`** | **S1 半按对焦框；目录重组后重建** |

每个版本的完整元数据在 `validation/FilmStudioNex6-*.json`，均记录 `camera_tested=false`。

早期“给 a5100 APK 打补丁”的 NEX-6 路线（0.3.x-alpha-nex6）不属于本仓库，源码和验证资料保存在本地 `legacy/a5100/`，不随新仓库发布。

## 6. 日志与复现

- 日志：`/sdcard/FSNEX6/FSLOG.TXT`；
- 取回：退出应用后 `adb pull /sdcard/FSNEX6/FSLOG.TXT`；
- 构建：`python3 tools/build.py`；
- API 10 门禁与模拟器步骤：见 [DEVELOPMENT.md](DEVELOPMENT.md)。

日志不含 IP 地址或序列号。模拟器结果不替代实机验证。

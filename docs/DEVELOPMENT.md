# 开发与构建

本文说明如何从源码构建、验证、签名和测试 Film Studio NEX-6。当前目标版本为 `0.2.18-alpha`，源码在 `app/`，构建入口是 `tools/build.py`。

> 实机验证状态见 [VALIDATION.md](VALIDATION.md)。在真实 NEX-6 上运行并取回日志之前，`camera_tested` 必须保持 `false`。

## 1. 环境

| 组件 | 版本 / 路径 |
| --- | --- |
| Python | 3.10+（只用标准库） |
| JDK | 17；脚本优先使用 `inputs/amazon-corretto-17.jdk/Contents/Home`，其次 `JAVA_HOME`，最后 `PATH` |
| Android SDK | build-tools `36.0.0`；platform `android-37.0` |
| 签名 | OpenSSL + `.private/signing.pem` |
| 字体（可选） | `fonttools` |

默认 SDK 路径：

```text
$HOME/Library/Android/sdk
```

不是这个路径时，修改 `tools/build.py` 顶部的 `SDK`、`BUILD_TOOLS`、`ANDROID_JAR`。

准备 JDK（示例，按实际下载包调整）：

```text
inputs/amazon-corretto-17.jdk/
```

`inputs/`、`output/`、`build-local/`、`.private/` 都被 `.gitignore` 忽略。

## 2. 构建

在仓库根目录：

```sh
python3 tools/build.py
```

脚本依次执行：

1. `aapt2 compile` 编译 `app/res/`；
2. `aapt2 link` 生成 `build-local/app/unsigned.apk`，写入 Manifest，打包 `app/assets/`，并加 `--no-compile-sdk-metadata`；
3. `javac -source 8 -target 8` 编译 `app/src/`；
4. `d8 --min-api 10` 生成 `classes.dex`；
5. 把 `classes.dex` 加入未签名 APK；
6. `tools/sign_apk.py` 生成 v1 JAR 签名（OpenSSL `smime -noattr -binary`，SHA-1）；
7. 用 `aapt dump badging`、`apksigner verify --min-sdk-version 10` 检查；
8. 写入 `validation/FilmStudioNex6-0.2.18-alpha.json`。

产物：

```text
output/FilmStudioNex6-0.2.18-alpha.apk
validation/FilmStudioNex6-0.2.18-alpha.json
```

构建结束会打印包名、版本、dex 版本、SHA-256 和大小。验证记录里的 `camera_tested` 固定为 `false`，只有实机证据才能修改。

## 3. 签名

- 正式覆盖安装必须使用同一份 `.private/signing.pem`；
- 该文件权限应为 `600`，离线备份，绝不提交或上传；
- 缺失时脚本会用一次性调试密钥签名，打印 `WARNING`；产物可以安装，但不能覆盖已发布版本；
- 旧相机只接受 v1 JAR 签名；`tools/sign_apk.py` 已针对 API 10 显式使用 SHA-1 和 JAR 清单折行；
- 签名文件本身属于敏感材料，不属于 PolyForm 授权内容。

检查证书：

```sh
JAVA_HOME="$PWD/inputs/amazon-corretto-17.jdk/Contents/Home" \
  "$HOME/Library/Android/sdk/build-tools/36.0.0/apksigner" \
  verify --min-sdk-version 10 --print-certs \
  output/FilmStudioNex6-0.2.18-alpha.apk
```

## 4. API 10 编译门

NEX-6 运行 Android 2.3.7 / API 10。仅设置 `minSdkVersion=10` 不能发现源码里误用的高版本 API；需要用 Android 2.3.3 的公开 stub 重新编译全部源码。

下载 stub（只需一次）：

```sh
curl -L -o /tmp/android-2.3.3.jar \
  https://repo1.maven.org/maven2/com/google/android/android/2.3.3/android-2.3.3.jar
```

编译：

```sh
rm -rf /tmp/nex6-api10-classes
mkdir -p /tmp/nex6-api10-classes
inputs/amazon-corretto-17.jdk/Contents/Home/bin/javac \
  -source 8 -target 8 \
  -bootclasspath "/tmp/android-2.3.3.jar:$HOME/Library/Android/sdk/platforms/android-37.0/android.jar" \
  -encoding UTF-8 \
  -d /tmp/nex6-api10-classes \
  $(find app/src -name '*.java')
```

- 必须同时把 `android-37.0/android.jar` 放在 bootclasspath 后面，否则 `java.lang` 等核心类无法解析；
- stub 在前，所以任何高于 API 10 的 `android.*` 类型或方法都会在这里失败；
- 通过时 `0.2.18` 生成 22 个 class（含内部类）。

再检查最终 dex 的 `android.*` 类型都能在 stub 中找到：

```sh
rm -rf /tmp/nex6-dexcheck && mkdir -p /tmp/nex6-dexcheck
unzip -p output/FilmStudioNex6-0.2.18-alpha.apk classes.dex > /tmp/nex6-dexcheck/classes.dex
"$HOME/Library/Android/sdk/build-tools/36.0.0/dexdump" -d /tmp/nex6-dexcheck/classes.dex > /tmp/nex6-dexcheck/dump.txt
python3 - <<'PY'
import re, zipfile
text = open('/tmp/nex6-dexcheck/dump.txt', encoding='utf-8', errors='replace').read()
refs = sorted(set(re.findall(r'Landroid/[A-Za-z0-9_/$]+;', text)))
jar = zipfile.ZipFile('/tmp/android-2.3.3.jar')
have = set('L' + n[:-6] + ';' for n in jar.namelist() if n.endswith('.class'))
missing = [r for r in refs if r not in have]
print('android refs:', len(refs), 'missing:', len(missing))
for r in missing:
    print('MISSING', r)
PY
```

`0.2.18` 的结果是 35 个 `android.*` 类型、0 个缺失。`tools/api10_public_ids.txt` 是另一份资源 ID 清单（AOSP android-2.3.7_r1 `public.xml`），用于检查资源层是否引用框架私有 ID；当前资源表只有 `ic_launcher` 和 `app_name`。

## 5. 字体

界面汉字由两份 Noto Sans 子集绘制。修改 `Lang.java` 的界面文本后，要同步更新 `tools/fetch_fonts.py` 里的 `SC_TEXT` / `TC_TEXT`，再运行：

```sh
python3 -m pip install fonttools
python3 tools/fetch_fonts.py
```

脚本从 Google Fonts 的 `text=` 接口获取 TTF，并用 fontTools 校验每个字符都在 cmap 中；同时下载 OFL 许可到 `app/assets/fonts/`。产物已提交，正常构建不需要网络。

## 6. 模拟器测试

模拟器只能验证安装、生命周期、界面和按键路径，不能替代 NEX-6 实机验证。

1. 启动 Android 37 模拟器；
2. 设为 NEX-6 的屏幕尺寸：

```sh
adb shell wm size 640x480
adb shell wm density 160
```

3. 安装并启动：

```sh
adb install -r output/FilmStudioNex6-0.2.18-alpha.apk
adb shell am start -n com.filmstudio.nex6/.MainActivity
```

4. 常用按键：

| 操作 | 命令 |
| --- | --- |
| 左 / 右 | `adb shell input keyevent 21` / `22` |
| 上 / 下 | `adb shell input keyevent 19` / `20` |
| 中央键 | `adb shell input keyevent 23` |
| DELETE | `adb shell input keyevent 67` |
| MENU | `adb shell input keyevent 82` |
| BACK | `adb shell input keyevent 4` |
| 半按 S1（保持 3 秒） | `adb shell input keyevent --duration 3000 80` |
| DISP | `adb shell input keyevent 165`（模拟器专用映射；实机扫描码 608） |

模拟器没有 NEX-6 的原始扫描码；`fallback()` 把 `KEYCODE_INFO`（165）映射为 `K_DISP`（608），因此日志仍显示 `scan=608 keyCode=165`。这条路径只用于在模拟器上走通日志叠加层。

5. 截图与日志：

```sh
adb exec-out screencap -p > /tmp/nex6.png
adb shell input keyevent 165          # 打开屏内日志
adb shell input keyevent 82           # MENU 退出并导出 REPORT.TXT
adb pull /sdcard/FSNEX6/REPORT.TXT
```

0.2.14 起优先检查屏内日志：DISP 打开后应看到 `LOG n 起-止` 标题、右上角关闭/翻页提示和 `APPLY ... cm=` 行；0.2.15 的黑白风格还会显示 `FX <picture-effect>`（如 `CM mono FX richtone-mono`），启动时 `CAP picture-effect=…` 记录固件公布的效果值，必要时 `EFFECT` / `MONO` 记录别名解析与重试。0.2.16 还需检查完整面板底部右侧的橙色 `P/A/S/M:风格`：与左侧按键提示同一基线、无重叠；简体中文 `P/A/S/M:风格`、英文 `P/A/S/M: STYLE`。0.2.17 还需切到 `LEICA-C` / `LEICA-N`，确认顶部 `徕卡 经典` / `徕卡 自然`、参数行 `NEUTRAL S-1 C-1 R+0` / `STANDARD S+0 C+0 R+0` 与日志 `APPLY LEICA-…`。0.2.18 还需切到 `CHROME` / `GR-` / `CNEG` / `CROSS`，确认参数行显示 `AWB A+1` / `AWB A+2`、日志不再出现 `color-temperture-white-balance` 写入，且此前反馈的偏黄（CHROME/GR-）与偏蓝（CNEG）消失；若 `DROPPED` 里出现 `light-balance-for-white-balance`，说明固件丢弃 A-B 偏移，记录即可。上/下翻页、再按 DISP 或 BACK 关闭。`REPORT.TXT` 是退出/崩溃时生成的一次性快照，可读；`FSLOG.TXT` 是运行期间的追加日志，在应用存活时读回可能是全 NUL（Sony FUSE 不允许同时读写同一文件）。导出后 `FSLOG.TXT` 会记录 `EXPORT-VERIFY size=… zero=… text=…`，下次启动记录 `PROBE` 行，用来自证卡上文件是否真的可读。

语言测试可在系统设置中为应用单独选择语言（Android 13+）；在 AOSP 模拟器上也可用 `adb shell cmd locale set-device-locale zh-Hans-CN`（重启应用后生效）。日志的 `START` 行会记录 `ui=zh-CN` / `ui=zh-TW` / `ui=en`。

模拟器的已知差异：

- 没有 `CameraEx`，应用回退到 `android.hardware.Camera`；
- 假相机没有 `color-mode`、`picture-effect` 和 `rgb-matrix-mode`，且会原样回显任意写入值；某些微调或 DRO 参数会被拒绝，应用退到核心参数并记录 `DROPPED`；
- 假相机总是返回合焦，红色对焦失败路径不会被触发；
- `takePicture(null,null,null)` 不产生 `DCIM` 文件，`SAVED` 只能是 `no new JPEG`；
- 不能验证 NEX-6 的扫描码、落卡时序、预览比例和退出恢复。

## 7. 版本与验证记录

发布新版本时：

1. 修改 `app/AndroidManifest.xml` 的 `versionCode` 和 `versionName`；
2. 修改 `tools/build.py` 的 `OUTPUT` 和 `VALIDATION` 文件名；
3. 构建并运行第 4 节的 API 10 门禁；
4. 更新 `README.md` / `README.en.md` 的版本、SHA-256 和 `docs/INSTALL.*` 的校验值；
5. 在 `CHANGELOG.md` 增加条目；
6. 在 `docs/VALIDATION.md` 记录静态结果和模拟器证据，保持 `camera_tested=false`，直到取得实机证据；
7. 确认当前版本的 `validation/FilmStudioNex6-<version>.json` 被 Git 跟踪，APK 不被跟踪；旧版本的 JSON 不提交；
8. 推送 `main` 和与版本对应的 tag（如 `0.2.18`），确认 tag 触发的 workflow 通过并生成预发布 Release。

`validation/` 只保留当前发布候选的 JSON，由构建脚本生成，包含包名、版本、SHA-256、大小、签名方式、限制和 `camera_tested`。历史版本的摘要保留在 `docs/VALIDATION.md`，完整旧文件存放在本地 `legacy/validation-history/`。不要手工伪造实机结果。

## 8. 发布

推送 tag（例如 `0.2.18`，不必带 `v`）会触发 `.github/workflows/build.yml` 的 `release` job：构建、签名，并创建带 APK、`SHA256SUMS.txt` 和 `validation/FilmStudioNex6-<version>.json` 的预发布 Release。CI 不持有 `.private/signing.pem`，因此 Release APK 由一次性调试密钥签名，SHA-256 与本地构建不同，也不能覆盖安装本地同版本；需要与文档校验值一致的 APK 时使用 `output/` 下的本地构建。

发布前检查：

- 不包含 `.private/`、`.env`、`inputs/`、`build-local/`、APK 以外的私钥或日志；
- 不包含 Sony 固件、Sony 基础应用、富士官方 LUT 或理光矩阵数据；
- `README`、`NOTICE`、`LICENSING.md` 的措辞与当前验证状态一致；
- `camera_tested` 仍为 `false` 时，不使用“已验证”“可用”“兼容”等表述。

## 9. 目录约定

```text
app/                应用源码、Manifest、资源、字体
.github/            GitHub Actions 构建工作流
build-local/        中间产物（忽略）
docs/               文档
inputs/             本地 JDK、第三方输入（忽略）
output/             签名 APK（忽略）
tools/              构建、字体、签名、API 10 清单
validation/         当前版本元数据（跟踪）
.private/           签名私钥（忽略，绝不提交）
legacy/             a5100 旧项目资料与旧验证 JSON（本地保留，忽略）
```

## 10. GitHub Actions

`.github/workflows/build.yml` 在 push 到 `main`、pull request、手动触发和推送任意 tag 时构建 APK：

1. 安装 JDK 17、`platforms;android-37.0` 和 `build-tools;36.0.0`；
2. 下载 Android 2.3.3 stub 并执行第 4 节的 API 10 编译门；
3. 执行 `python3 tools/build.py`；
4. 把 APK、`SHA256SUMS.txt` 和当前 `validation/*.json` 暂存到 `dist/`，上传为 workflow artifact。

tag 触发时另有 `release` job：下载 artifact，并用 `gh release create --verify-tag --prerelease` 创建预发布 Release，附上同样三个文件。tag 名不必带 `v`。

CI 不持有 `.private/signing.pem`，因此 APK 由构建脚本生成的一次性调试密钥签名。该产物适合在模拟器或测试机上验证构建流程，**不能覆盖安装**本地同版本或正式签名的 APK；其 SHA-256 也不等于文档中本地构建的值。

`tools/build.py` 通过 `ANDROID_SDK_ROOT` / `ANDROID_HOME` 查找 SDK，也接受 `ANDROID_BUILD_TOOLS` 和 `ANDROID_JAR` 覆盖；本地 macOS 默认仍使用 `~/Library/Android/sdk`。

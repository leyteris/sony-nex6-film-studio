# 参考来源

查阅日期：2026-11-08

本项目只把下列资料用于接口行为、兼容性事实和工具链参考。**没有复制 Recipe Lab、sony-lut-camera 或其他 GPL 项目的源码。** 仓库内不包含 Sony 固件、Sony 基础应用、富士官方 LUT、理光矩阵参数或签名私钥。

## 1. 相机行为与接口参考

| 来源 | 用途 | 许可 / 说明 |
| --- | --- | --- |
| `recipe-lab-sony-pmca`（本地参考检出 `../recipe-lab-sony-pmca`） | NEX-6 上已验证的 Activity + SurfaceView + Canvas 结构、`Camera.Parameters` key、扫描码、FUSE 8.3 日志实践 | GPL-3.0；仅作行为/接口参考，未复制源码 |
| `sony-lut-camera`（本地参考检出 `../sony-lut-camera`） | NEX-6 固件 1.03 的静态 API 审计：缺少矩阵/Gamma 与 3D LUT 通道 | GPL-3.0；只引用兼容性结论，未复制源码 |
| `sony-nex6-app-menu-unlock`（本地 `../sony-nex6-app-menu-unlock`） | 恢复 NEX-6「应用程序」入口的前置流程 | 本项目相关项目；仅支持 Windows；不写固件 |
| [Sony-PMCA-RE](https://github.com/ma1co/Sony-PMCA-RE) | PMCA 应用安装与设备连接 | 上游许可见其仓库 |
| [OpenMemories: Tweak](https://github.com/ma1co/OpenMemories-Tweak) | 启用 Wi-Fi ADB / Developer 设置 | 上游许可见其仓库 |

Recipe Lab 和 sony-lut-camera 的 GPL-3.0 许可不能并入本项目的 PolyForm 工程。本项目只使用从设备行为中观察到的接口事实，并以独立代码实现；如果未来需要复用其源码，必须单独处理 GPL 合规。

## 2. 签名与构建

| 来源 | 用途 | 许可 |
| --- | --- | --- |
| [bonyback1/sony-pmca-ricoh-mod](https://github.com/bonyback1/sony-pmca-ricoh-mod) | `tools/sign_apk.py` 的来源；本项目增加了 API 10 所需的显式 SHA-1 与 JAR 清单折行 | Apache-2.0；见 [LICENSES/Apache-2.0.txt](../LICENSES/Apache-2.0.txt) 与 [NOTICE](../NOTICE) |
| [Android 2.3.3 stub (Maven Central)](https://repo1.maven.org/maven2/com/google/android/android/2.3.3/android-2.3.3.jar) | API 10 编译门；只用于静态检查，不进入 APK | 上游许可 |
| [AOSP android-2.3.7_r1 public.xml](https://android.googlesource.com/platform/frameworks/base/+/android-2.3.7_r1/core/res/res/values/public.xml) | `tools/api10_public_ids.txt` 的公开资源 ID 清单 | Apache-2.0 |
| [Android SDK / Platform-Tools](https://developer.android.com/tools) | aapt2、d8、dexdump、apksigner、adb | Android SDK 条款 |

## 3. 字体

| 来源 | 用途 | 许可 |
| --- | --- | --- |
| [Noto Sans SC](https://fonts.google.com/noto/specimen/Noto+Sans+SC) | 简体中文界面子集 | SIL OFL 1.1；许可全文在 `app/assets/fonts/OFL-NotoSansSC.txt` |
| [Noto Sans TC](https://fonts.google.com/noto/specimen/Noto+Sans+TC) | 繁体中文界面子集 | SIL OFL 1.1；许可全文在 `app/assets/fonts/OFL-NotoSansTC.txt` |
| [SIL Open Font License](https://scripts.sil.org/OFL) | 字体许可说明 | SIL OFL 1.1 |

字体由 `tools/fetch_fonts.py` 从 Google Fonts 的 `text=` 接口生成，仅包含 ASCII 和界面用字；OFL 许可随字体一起打包。

## 4. 名称与商标

Sony、PlayMemories Camera Apps、NEX-6、FUJIFILM、Ricoh 及胶片/风格名称仅用于说明兼容性、目标机型和风格取向。所有商标归各自权利人所有。本项目未经 Sony、FUJIFILM、Ricoh 认证、赞助或背书。

本仓库不包含：

- Sony 固件或 Sony 基础应用代码；
- 富士官方 LUT 文件或由其拟合的参数；
- 理光/街头风格的矩阵或 Gamma 数组；
- 相机签名私钥或任何解锁用的设备数据。

[ukiki0718-netizen/sony-a5100-film-studio](https://github.com/ukiki0718-netizen/sony-a5100-film-studio) 是本项目的前身：15 个风格、强度分级与验证方法来自该项目的 a5100 版 Film Studio。其 LUT、矩阵和验证材料不在本仓库发布；如需查阅，请使用该仓库或本地 `legacy/a5100/` 存档。

## 5. 外部工具不是依赖

上表中的工具和文档只用于构建、验证或安装说明，不是随 APK 分发的运行时依赖。各工具适用其自身许可。

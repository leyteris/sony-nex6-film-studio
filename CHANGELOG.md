# 版本历史

## 0.2.19-alpha — 2026-11-10

- 新增 Neutral 回退：NEX-6 这一代创意风格表没有 `neutral`（标准/生动/肖像/风景/黄昏/夜景/红叶/黑白/棕褐色），而 Sony 对未知 `color-mode` 静默忽略，导致 CHROME/PRONEG/CNEG/ETERNA/BLEACH/GR-/LEICA-C 七个中性底风格会保留上一个风格的基调；现在固件公布的 `color-mode-values` 可读且不含 `neutral` 时改发 `standard` 并额外降饱和 2×强度（clamp 到 -3），日志记 `STYLE neutral fallback standard sat=…`；
- 风格表不可读（null，如模拟器）时行为不变：照发 `neutral` 并由回读决定；固件公布 `neutral` 时照旧；其他未列出的值仍照发；
- 回退判定与饱和计算为纯静态方法（`styleOrFallback` / `neutralFallbackSat`），14 项 host 单测全过（NEX-6 风格表、含 neutral 的表、null 表、空格 trim、饱和 clamp 与四档强度）；
- Manifest 升至 `0.2.19-alpha` / versionCode 21；`tools/build.py`、CI 产物名、README、安装文档与 `validation/` 同步；构建元数据新增一条 Neutral 回退 limitation；
- 构建与验证：`6450c467…db6d`、81,318 B、11 个 APK 条目、v1 签名；22 个 class、35 个 `android.*` 类型、0 缺失、dex 035（另 26 个 `java.*` 核心类型，均为 API 1 已有）；Android 37 模拟器（简体中文，640×480）复验 CHROME/CNEG 参数行 `AWB A+1`/`AWB A+2`、`CAP color-mode=null` 时回退不触发、`APPLY … cm=neutral` 回显正常、MENU 退出 `RESTORE ok`，`REPORT.TXT` 3,516 字节、82 行、0 个 NUL；
- 已知：`camera_tested=false`；实机需看 `CAP color-mode=…` 风格表与是否出现 `STYLE neutral fallback`，并确认七个中性底风格的成片基调。

## 0.2.18-alpha — 2026-11-10

- 修复白平衡写入 bug：带色温的风格此前把 `color-temperture-white-balance` 写成 K×100（`5600` → `560000`），远超 Sony 2500–9900 的合法范围，固件行为不可预测；用户实机反馈 `CHROME`/`GR-` 明显偏黄、`CNEG` 明显偏蓝，与此一致；
- 对齐 a5100 上游「滤镜不会锁定白平衡」：17 个风格一律写 `whitebalance=auto`，不再写色温；`CHROME`/`GR-` 改为 AWB + A-B `+1`，`CNEG`/`CROSS` 改为 AWB + A-B `+2`（琥珀为正，直接写入并回读，被固件丢弃则记 `DROPPED`）；
- 顶部参数行由 `5600K A+1` 变为 `AWB A+1`；低强度对 A-B 的线性缩放不变；删除 `wb`/`kelvin` 字段与色温写入路径，用户自己的色温参数仍随原参数捕获并在退出时恢复；界面文案不变，字体子集字节不变；
- Manifest 升至 `0.2.18-alpha` / versionCode 20；`tools/build.py`、CI 产物名、README、安装文档与 `validation/` 同步；
- 构建与验证：`42c80337…c44d`、81,094 B、11 个 APK 条目、v1 签名；22 个 class、35 个 `android.*` 类型、0 缺失、dex 035；Android 37 模拟器（简体中文，640×480）确认 `CHROME` 参数行 `NEUTRAL S-1 C+1 R+0 AWB A+1 DRO auto`，日志 `APPLY CHROME 30/50/70/100% neutral cm=neutral`、`APPLY CNEG 100% neutral cm=neutral`，全程无 `color-temperture-white-balance` 写入与 `SET-` 失败，MENU 退出 `RESTORE ok`，`REPORT.TXT` 3,080 字节、72 行、0 个 NUL；
- 已知：`camera_tested=false`；偏色修复效果等待用户实机复验，A-B 偏移是否被固件保留也需实机确认。

## 0.2.17-alpha — 2026-11-09

- 新增两个徕卡参考 Look：`LEICA-C`（徕卡 经典 / Leica Look Classic）与 `LEICA-N`（徕卡 自然 / Leica Look Natural），风格总数 15 → 17；
- 参数按上游 [Chai-Yu/sony-a7r2-film-studio](https://github.com/Chai-Yu/sony-a7r2-film-studio) 的机身回退方向落地：Natural 用 `standard` + `0/0/0` + AWB auto；Classic 用 `neutral` + `-1/-1/0` + AWB auto。NEX-6 没有矩阵/Gamma 通道，因此这只是参数级近似，未复制上游 LUT、矩阵或曲线；
- 中文名沿用上游的家族前缀规则：富士 10 个 + 理光 5 个 + 徕卡 2 个；补全卡片短名与英文名；
- 重新生成 SC/TC 字体子集，加入 `富士`、`徕卡`/`徠卡`、`自然` 等新界面用字；
- Manifest 升至 `0.2.17-alpha` / versionCode 19；`tools/build.py`、CI 产物名、README、安装文档与 `validation/` 同步；构建元数据新增 `look_families` 计数；
- 构建与本轮验证：`2e0a3be4…464d`、81,270 B、11 个 APK 条目、v1 签名；22 个 class、35 个 `android.*` 类型、0 缺失、dex 035；Android 37 模拟器（640×480）确认 `LEICA-C`/`LEICA-N` 的简体、繁体、英文名称与参数行，日志记录 `APPLY LEICA-C 100% neutral`、`APPLY LEICA-N 100% standard`、`APPLY LEICA-N 70% standard`，MENU 退出 `RESTORE ok`，`REPORT.TXT` 2,016 字节、43 行、0 个 NUL；
- 第二轮模拟器会话（2026-11-10）补验 `LEICA-C` 低强度：100% → 70% → 50% → 30% 四档芯片与 `APPLY LEICA-C <pct>% neutral cm=neutral` 日志一致；胶囊显示 `LEICA-C 经典 30%`、隐藏阶段无覆盖元素，DEL 三阶段循环与 MENU 退出 `RESTORE ok` 通过；该会话 `REPORT.TXT` 1,667 字节、44 行、0 个 NUL；
- 已知：`camera_tested=false`，两个新 Look 与其他 15 个一样等待 NEX-6 实机验证。

## 0.2.16-alpha — 2026-11-09

- 完整面板底部提示行右侧新增右对齐的橙色模式提示：简体 `P/A/S/M:风格`、繁体 `P/A/S/M:風格`、英文 `P/A/S/M: STYLE`，与左侧按键提示同一基线；胶囊和隐藏阶段不显示；
- 原因：用户在 NEX-6 实机上确认 0.2.15 的黑白风格在 iAuto / SCN 下仍出彩色，拨盘转到 P 后正常——两种自动模式由相机接管创意风格和照片效果；提示用于避免把拨盘位置误判为应用缺陷；
- 只改界面提示与版本号，取景、参数应用、黑白双路径和日志逻辑与 0.2.15 相同；
- 提示复用已有字形 `风`/`風`/`格`，SC/TC 字体子集无需重新生成，字体字节不变；
- Manifest 升至 `0.2.16-alpha` / versionCode 18；`tools/build.py`、CI 产物名与 `validation/` 同步；
- 模拟器验证（简体中文）：22 个 class、35 个 `android.*` 类型、0 缺失、dex 035、v1 签名；640×480 下右下角 `P/A/S/M:风格` 完整、无重叠、无豆腐块；DISP 日志、MENU 退出与导出回读通过（单次会话 `REPORT.TXT` 1,089 字节、25 行、0 个 NUL；两次会话连续存放 2,168 字节时 `EXPORT-VERIFY size=2168 head=2168 zero=0 text=2118 other=50`）；`camera_tested=false`。

## 0.2.15-alpha — 2026-11-09

- 重做黑白风格：ACROS/HCBW/MORI 现在同时写 `color-mode=mono` 和单色照片效果（ACROS 为 `richtone-mono`，HCBW/MORI 为 `rough-mono`），只要一路被固件保留，成片就应为灰阶；
- `Cam` 按固件公布的 `picture-effect-values` 解析别名（`richtone-mono` / `rich-tone-monochrome`、`rough-mono` / `high-contrast-mono` 等写法），两值都回读；若都不是单色，先单独重试 `color-mode`（轮换全部 `mono` 别名），仍失败再单独重试效果；
- 右上角诊断改为 `CM <color-mode> FX <picture-effect>`；新增 `CAP picture-effect=…`、`EFFECT`（别名解析）与 `MONO`（重试结论）日志，一次实机拍摄即可判断哪条路径被接受；
- 已知取舍：照片效果开启时 Sony 忽略创意风格及其滑块，黑白强度可能不再改变对比；为保证灰阶接受该代价，待实机确认；
- 映射参考 Recipe Lab 的 `STYLE_NAMES=mono`、`PE_KEYS=richtone-mono/rough-mono`；HCBW 选 `rough-mono` 是按高反差语义，与参考项目 `Chai-Yu/sony-a7r2-film-studio` 的 HCBW 映射不完全相同；
- 模拟器验证：22 个 class、35 个 `android.*` 类型、0 缺失、dex 035、v1 签名；ACROS/HCBW/MORI 分别显示 `CM mono FX richtone-mono` / `rough-mono`，DISP 日志、MENU 退出与导出回读通过（`REPORT.TXT` 2,027 字节、39 行、0 个 NUL；`EXPORT-VERIFY zero=0 text=1988`）；`camera_tested=false`。

## 0.2.14-alpha — 2026-11-09

- 新增屏内日志：按 DISP 在相机屏上显示内存中最近 600 行，上/下翻页，再按 DISP 或 BACK 关闭，MENU 仍退出；不依赖存储卡，回传诊断只需拍照；
- `Log8` 新增启动回读探针 `PROBE`（上一轮 `REPORT.TXT`/`FSLOG.TXT` 的 `size/head/zero/text/other`）与导出后立即回读的 `EXPORT-VERIFY`，并记录最终日志路径 `PATH`；
- 实机回传的两份 NEX-6 日志（2,853 B / 21,562 B）经相机 USB/MTP 与远程桌面复制后均为 100% NUL，文件大小正常且 `FSLOG` 持续增长；0.2.14 的回读探针用于区分“卡上就是 NUL”和“读取路径有问题”，`text=0` 时改用读卡器；
- 修复黑白风格显示为彩色：不再把固件未列出的 `color-mode` 值降级为 `standard`（Sony 对未知值是静默忽略），改为发送目标值并支持 `mono`/`bw` 等别名，写入后回读，不一致时单独重试一次；右上角显示实际值 `CM mono` / `CM standard want mono`；
- 字体子集加入新的日志提示用字“关闭翻页”/“關閉翻頁”，重新生成 SC/TC 子集；
- 模拟器验证：22 个 class、35 个 `android.*` 类型、0 缺失、dex 035、v1 签名；DISP 叠加层、翻页、关闭、MENU 退出、`REPORT.TXT` 回读（0 个 NUL）与 `EXPORT-VERIFY` 均通过；`camera_tested=false`。

## 0.2.13-alpha — 2026-11-09

- 修复卡上日志读回全 NUL 的问题：Sony FUSE 不允许同时读写同一文件，运行期间一直追加的 `FSLOG.TXT` 在应用存活时无法读取；
- `Log8` 新增最近 600 行的内存环形缓冲，退出（`onDestroy`）或崩溃时把缓冲一次性写入全新文件 `FSNEX6/REPORT.TXT`，`flush` + `getFD().sync()` 后立即关闭、不再打开；这种“写完即关”的新文件在 Sony FUSE 上可读（同一做法已由 LUT Camera 在 A6300 实机验证）；快照写入前先记 `EXPORT`，文件末尾可见 `END app` → `EXPORT <路径>`；
- `FSLOG.TXT` 保留为运行期间的追加日志/崩溃残留；诊断取回改用 `REPORT.TXT`；
- 新增 `EXPORT` 日志事件；不新增界面文案，字体子集不变；
- 静态验收 21 个 class、35 个 `android.*` 类型、0 缺失、dex 035、v1 签名；`camera_tested=false`。

## Unreleased — 仓库重组

- 目录改为 NEX-6 单一项目：`app/`、`docs/`、`tools/`、`validation/`；
- 新增中文/英文 README、安装、架构、开发、兼容性、验证与来源文档；
- README 与来源文档增加对前身项目 [ukiki0718-netizen/sony-a5100-film-studio](https://github.com/ukiki0718-netizen/sony-a5100-film-studio) 的致谢；
- README 增加三张 Android 37 模拟器界面截图（完整面板、胶囊阶段、隐藏阶段对焦框），并注明非实机画面；
- `validation/` 只保留当前版本元数据，历史 JSON 移到本地 `legacy/validation-history/`；
- 新增 GitHub Actions 工作流：push/PR 时执行 API 10 编译门并构建调试签名 APK artifact；
- `.gitignore` 补充编辑器、SD 卡元数据和本地配置规则；
- a5100 旧项目资料移到本地 `legacy/a5100/`，不进入新仓库；
- 应用源码与功能未变。`0.2.12-alpha` 重新构建为 `51889474…e349`（与原 `b7fe23f7…ace3` 源码相同，差异仅来自时间戳）；
- 重新执行 API 10 编译门、dex 类型审计和模拟器冒烟测试。

## 0.2.12-alpha — 2026-11-08

- S1 半按调用 `Camera.autoFocus`，在取景中央绘制四角对焦框：白=搜索、绿=合焦、红=失败，松开消失；
- 对焦回调经主线程 `Handler` 回到界面，并用 generation 计数忽略过期回调；
- 无 Sony 扫描码时把标准 `KEYCODE_FOCUS`（80）映射到 S1，便于模拟器测试；实机扫描码 516 优先；
- 静态验收 21 个 class、35 个 `android.*` 类型、dex 035、v1 签名；模拟器验证对焦框、三阶段、拍摄请求与 MENU 退出；
- `camera_tested=false`。

## 0.2.11-alpha — 2026-11-08

- 风格卡上下间距对称：卡高保持约 59 px，提示行上移，卡底到提示文字、强度行到卡片的空隙都约 11 px；
- 简/繁/英截图确认提示行完整，三阶段与退出链路复测。

## 0.2.10-alpha — 2026-11-08

- 风格卡更扁（约 73→59 px），底部面板总高不变；
- 提示区从 0.044h 增至 0.076h，卡底与提示文字拉开；
- 三语言截图确认标签、简称和色条完整。

## 0.2.9-alpha — 2026-11-08

- 左右键切换风格，上下键切换强度；拨轮/转盘也切换风格；
- hidden 阶段按 UP 显示强度浮层，按 LEFT/RIGHT 显示风格浮层；
- 提示行改为「左右:风格 上下:强度 …」。

## 0.2.8-alpha — 2026-11-08

- 强度整行居左：四个百分比块紧接标签，块间无重叠、无裁切；
- 提示行保持居左。

## 0.2.7-alpha — 2026-11-08

- 底部提示行从左侧 `pad` 起排，与强度标签左边缘对齐。

## 0.2.6-alpha — 2026-11-08

- 启动日志增加 `FONT fonts/... ok script=...` / `FONT-FAIL`，用于确认中文字体是否加载。

## 0.2.5-alpha — 2026-11-08

- 胶囊移到左下角，对齐 Recipe Lab `mini` 的 `bottom|left`；
- 简/繁/英 pill 与三阶段复测。

## 0.2.4-alpha — 2026-11-08

- hidden 阶段按左右键显示本地化强度浮层；按中央键显示「已拍摄」。

## 0.2.3-alpha — 2026-11-08

- 风格切换浮层本地化；hidden 阶段仍显示风格/强度提示。

## 0.2.2-alpha — 2026-11-08

- 左下角胶囊本地化（简体/繁体简称）；状态错误行本地化。

## 0.2.1-alpha — 2026-11-08

- 底部面板由 0.36h 收紧到 0.29h，强度行与风格卡之间增加间隔；
- 本地化风格名和卡片简称；`zh-HK` 归入繁体；
- 三阶段只改变绘制层，不吞按键。

## 0.2.0-alpha — 2026-11-08

- 三阶段 UI：完整面板 → 左下角胶囊 → 仅取景；DELETE/SK2 切换；
- 简体、繁体、英文界面；内置 Noto Sans SC/TC 子集；
- 日志目录改为 `/sdcard/FSNEX6/FSLOG.TXT`，满足 FUSE DOS 8.3；
- 修复 `KEYCODE_FORWARD_DEL` 的 API 11 引用，API 10 编译门通过。

## 0.1.0-alpha — 2026-11-08

- 首个独立 NEX-6 外壳（包名 `com.filmstudio.nex6`），不再给 a5100 APK 打补丁；
- Recipe Lab 式 Activity + SurfaceView + Canvas；Sony 接口全部反射；
- 15 个 Film Studio 风格映射为 NEX-6 运行时参数；4 档强度；
- 中央键/快门拍摄、MENU/BACK 恢复退出、`FSLOG.TXT` 日志、60 秒心跳；
- 模拟器冒烟测试通过，`camera_tested=false`。

---

更早的 `0.3.x-alpha-nex6` 属于“给 a5100 APK 打补丁”的旧路线，不属于本仓库；其源码和验证资料保存在本地 `legacy/a5100/`。a5100 版 Film Studio 的版本历史请见其独立仓库。

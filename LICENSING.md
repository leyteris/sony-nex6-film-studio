# 许可范围 / License scope

## 中文

本仓库原创的代码与文档采用未经改写的 **[PolyForm Noncommercial 1.0.0](LICENSE)**，仅适用于各贡献者有权许可的部分。版权归各贡献者就其各自创作的内容所有；“Film Studio NEX-6 contributors”是署名集合，不表示成立了公司或独立法人。

该许可不授予商业用途。个人爱好、研究及许可原文列出的机构例外和法定权利，以许可证原文为准。分发适用内容时请保留许可证和 [NOTICE](NOTICE)。这是非商用源码公开许可，不符合 [OSI 开源定义](https://opensource.org/osd)。

| 内容 | 许可 / 权利 |
| --- | --- |
| `app/` 中的应用源码、界面与参数表 | PolyForm Noncommercial 1.0.0，限贡献者有权许可的部分 |
| `tools/build.py`、`tools/fetch_fonts.py`、文档与验证记录 | PolyForm Noncommercial 1.0.0 |
| `tools/sign_apk.py` 及其修改 | 源自 bonyback1/sony-pmca-ricoh-mod，整个文件保持 [Apache-2.0](LICENSES/Apache-2.0.txt)；修改说明见文件头与 [NOTICE](NOTICE) |
| `app/assets/fonts/NotoSansSC-UI.ttf`、`NotoSansTC-UI.ttf` | Noto Sans 子集，SIL Open Font License 1.1；许可全文随字体放在 `app/assets/fonts/` |
| Sony 固件、Sony 基础应用、PlayMemories Camera Apps | 不在本仓库中；权利归 Sony |
| 富士官方 LUT、富士色彩参数 | 不在本仓库中；权利归 FUJIFILM 及相应权利人 |
| 理光矩阵/Gamma、理光风格数据 | 不在本仓库中；相关名称仅用于说明风格取向 |
| Recipe Lab、sony-lut-camera 的源码 | 不在本仓库中；GPL-3.0，仅作行为/接口参考，未复制 |

**APK 的来源**：`output/` 中的 APK 由本仓库 `app/` 源码、Android 公开 SDK、`tools/sign_apk.py` 和两份 OFL 字体子集构建；不包含 Sony 固件、Sony 基础应用、富士 LUT、理光矩阵或第三方 APK 内容。它仍然是实验软件，不附带任何兼容性、色彩准确性或特定用途保证。

`tools/sign_apk.py` 的上游 Apache-2.0 许可不能被本项目的非商用条款覆盖；第三方仍可依据 Apache-2.0 独立使用该文件及其上游来源。许可证原文本身不被重新许可。

Sony、PlayMemories、NEX-6、FUJIFILM、Ricoh 及产品/风格名称的商标权归各自权利人。本项目未经这些公司认证、赞助或背书。

如果认为本仓库包含未经许可的内容，或来源、版权、商标表述有误，请通过本仓库的 **Issues** 提供文件路径、涉及段落、原始来源和权利依据。不要公开个人身份证件、私人联系方式、未授权 APK/LUT 或签名密钥。维护者会评估并视情况更正、移除或暂停相关内容；这不构成保证，也不限制权利人依法采取其他措施。

软件按现状提供；在法律允许的范围内，不保证兼容性、色彩准确性、适销性、特定用途适用性或不侵权。非商用条款、署名、免责声明和 GitHub 发布均不构成第三方授权或保证免责。英文许可证是条款原文；本页是范围说明，不是法律意见。

## English

Original code and documentation in this repository are licensed under the unmodified **[PolyForm Noncommercial 1.0.0](LICENSE)**, only to the extent each contributor can grant rights. Copyright remains with the respective contributors. “Film Studio NEX-6 contributors” is a collective attribution, not a company or separate legal entity.

Commercial use is not granted. The license text controls the permitted purposes, including its listed organizational exceptions and statutory rights. Keep the license and [NOTICE](NOTICE) when distributing covered material. This is noncommercial source-available software, not [OSI-defined open source](https://opensource.org/osd).

| Material | License / rights |
| --- | --- |
| App sources, UI and parameter tables under `app/` | PolyForm Noncommercial 1.0.0, to the extent contributors can grant rights |
| `tools/build.py`, `tools/fetch_fonts.py`, documentation and validation records | PolyForm Noncommercial 1.0.0 |
| `tools/sign_apk.py` and our modifications | Adapted from bonyback1/sony-pmca-ricoh-mod; the whole file remains [Apache-2.0](LICENSES/Apache-2.0.txt). See the file header and [NOTICE](NOTICE) |
| `app/assets/fonts/NotoSansSC-UI.ttf`, `NotoSansTC-UI.ttf` | Noto Sans subsets under the SIL Open Font License 1.1; the full license ships beside the fonts |
| Sony firmware, Sony base apps, PlayMemories Camera Apps | Not included; rights remain with Sony |
| Official Fujifilm LUTs and color parameters | Not included; rights remain with FUJIFILM and respective holders |
| Ricoh matrix/gamma or style data | Not included; names only describe the intended look |
| Recipe Lab and sony-lut-camera source | Not included; GPL-3.0, used as behavioral/interface references only; no source copied |

**APK provenance**: the APK under `output/` is built from this repository's `app/` sources, the public Android SDK, `tools/sign_apk.py` and two OFL font subsets. It contains no Sony firmware or base-app code, no Fujifilm LUTs, no Ricoh matrix data and no third-party APK material. It is experimental software with no warranty of compatibility, color accuracy or fitness for a particular purpose.

The upstream Apache-2.0 license for `tools/sign_apk.py` cannot be overridden by this project's noncommercial terms; third parties may still use that file and its upstream source under Apache-2.0. The license texts themselves are not relicensed.

Sony, PlayMemories, NEX-6, FUJIFILM, Ricoh and product/style names are trademarks of their owners. This project is not affiliated with, certified by or endorsed by them.

If you believe content lacks permission, or an attribution, copyright or trademark statement is inaccurate, use this repository's **Issues** to identify the file, passage, original source and basis of your rights. Do not post identity documents, private contact details, unlicensed APK/LUT files or signing keys. Maintainers will assess the concern and may correct, remove or suspend the material; this is not a guarantee and does not limit a rights holder's remedies.

The software is provided as is, without warranties of compatibility, color accuracy, merchantability, fitness or non-infringement to the extent permitted by law. Noncommercial terms, attribution, disclaimers and publication on GitHub do not confer third-party permission or guarantee immunity. The English license text controls; this page explains scope and is not legal advice.

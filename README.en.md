# Film Studio NEX-6

[中文](README.md) · **English**

A standalone, unofficial film-look app for the **Sony NEX-6**. It expresses the fifteen Film Studio looks with runtime camera parameters the NEX-6 actually accepts, draws its own control overlay over the live view, and restores the camera's original parameters on exit.

> **Status: `0.2.12-alpha` (versionCode 14), not yet installed or run on a NEX-6.** The build, API-10 static checks and Android 37 emulator smoke tests are complete; `camera_tested=false`. Until on-camera evidence exists, this project makes no compatibility or usability claim.

## Scope

- **Target**: Sony NEX-6, firmware 1.03, Android 2.3.7 / API 10.
- **Standalone app**: package `com.filmstudio.nex6`; it can coexist with the older a5100 Film Studio.
- **Still images only**: there is no movie UI; PLAY, DISP and MOVIE are ignored.
- **Runtime parameters only**: it writes `Camera.Parameters` and restores them on exit or pause; no firmware changes, no camera settings storage, no factory reset.
- **Parameter-level approximation**: the NEX-6 has no matrix/gamma or 3D LUT channel, so the result will not be pixel-identical to the a5100 build.

## Features

- 15 looks (10 Fujifilm-reference, 5 Ricoh-reference), 4 strengths (30 / 50 / 70 / 100%).
- LEFT/RIGHT or the wheels switch looks; UP/DOWN change strength, with live-view feedback.
- Three-stage UI: full panel → bottom-left pill → live view only; DELETE or SK2 cycles.
- English, Simplified Chinese and Traditional Chinese UI following the system locale; bundled Noto Sans SC/TC subsets supply the CJK glyphs the firmware lacks.
- S1 half-press autofocus with an app-drawn focus frame (white searching, green locked, red failed).
- Center button or shutter captures a JPEG; the app checks `DCIM` for a new file and logs the result.
- The last look and strength are restored on the next launch.
- Card log at `/sdcard/FSNEX6/FSLOG.TXT`, with no IP address or serial number.

## Compatibility

| Model / firmware | Status |
| --- | --- |
| **NEX-6, firmware 1.03** | Development target; **not yet tested on hardware** |
| Other NEX-6 firmware | Not assessed; 1.03 results cannot be assumed |
| a5100 / ILCE-5100 | Not applicable; use the a5100 Film Studio instead |
| Other PMCA bodies | Not assessed; no support claim |

PMCA (PlayMemories Camera Apps) is the on-camera platform required to install this app. A successful install would not prove the features work; see the first-run checklist in [VALIDATION.md](docs/VALIDATION.md).

## Controls

| Key | Action |
| --- | --- |
| LEFT / RIGHT, wheels and dials | Previous / next look |
| UP / DOWN | Strength + / − |
| Center button, shutter | Capture |
| S1 half-press | Autofocus; release cancels |
| DELETE / SK2 | Cycle UI stage (full → pill → hidden) |
| MENU / SK1 | Restore parameters and exit |
| BACK | Restore parameters and exit |
| PLAY / DISP / MOVIE | Ignored |

## The 15 looks

| # | Tag | Name |
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

The per-look color mode, saturation, contrast, sharpness, white balance and DRO values are in [ARCHITECTURE.md](docs/ARCHITECTURE.md#5-15-个风格) (Chinese). Names describe the intended look only; they are not pixel-accurate matches to Fujifilm or Ricoh output.

## Installation

Prerequisites:

1. The NEX-6 app menu has been restored with the **sony-nex6-app-menu-unlock** procedure.
2. Wi-Fi ADB is enabled on the camera (Sony-PMCA-RE / OpenMemories: Tweak).
3. Android Platform-Tools are installed on the computer.

Download `FilmStudioNex6-0.2.12-alpha.apk` from the Releases page (once published), or use the local build at `output/FilmStudioNex6-0.2.12-alpha.apk`, then run:

```sh
adb connect CAMERA_IP:5555
adb -s CAMERA_IP:5555 install -r FilmStudioNex6-0.2.12-alpha.apk
```

`CAMERA_IP` is a placeholder; replace it with the address currently shown on the camera and keep the `:5555` port. Mask the real IP before sharing screenshots or logs.

SHA-256 of the current build:

```text
51889474607a71f8b2f0db1ad2aab74f924d6db7c50a72bfaa2a5df0d2f2e349
```

See the **[English installation guide](docs/INSTALL.en.md)** for the full procedure, first-run checks, log retrieval and troubleshooting.

> **Do not** use the camera's Settings → Initialize to troubleshoot. The CN2 preset closes the app entry again and the unlock has to be repeated. The unlock parameters apply only to NEX-6 1.03 / `CX76001_CN2`, not to other models or regions.

## Logs

The app writes diagnostics to the card:

```text
/sdcard/FSNEX6/FSLOG.TXT
```

Both names satisfy the Sony FUSE DOS 8.3 limit. The card-root `FSLOG.TXT` is used only if the directory cannot be created. The log records body/firmware, preview size, key scan codes, parameter apply/drop results, capture, card-save checks, focus results and exceptions; it does **not** record IP addresses, serial numbers or image content.

Retrieve it with:

```sh
adb -s CAMERA_IP:5555 pull /sdcard/FSNEX6/FSLOG.TXT
```

If the file is all NUL or incomplete, exit the app before pulling, or zip the file on the camera and transfer the archive.

## Building

Requirements: JDK 17, Android SDK build-tools 36.0.0, `platforms/android-37.0/android.jar`, OpenSSL and Python 3.

```sh
python3 tools/build.py
```

The APK is written to `output/FilmStudioNex6-0.2.12-alpha.apk` and a metadata file to `validation/FilmStudioNex6-0.2.12-alpha.json`. Signing uses the local `.private/signing.pem`, which is never committed or uploaded. If it is missing, the script signs with a throwaway debug key; that signature differs and cannot update an installed release in place.

To regenerate the Chinese font subsets (optional; needs network and `fonttools`):

```sh
python3 -m pip install fonttools
python3 tools/fetch_fonts.py
```

Full build, API-10 gate, emulator test and release notes are in [DEVELOPMENT.md](docs/DEVELOPMENT.md).

## Repository layout

```text
app/            Java sources, manifest, resources and bundled fonts
docs/           Installation, architecture, development, compatibility, validation, sources
tools/          Build, font generation and API-10 signer
validation/     Per-version build metadata and SHA-256 (camera_tested=false)
output/         Local build artifacts (git-ignored)
inputs/         Local toolchain and third-party inputs (git-ignored)
.private/       Signing key (git-ignored, never commit)
```

## Known limitations

- No hardware validation yet; preview, capture, card save, focus callbacks and exit restore are unconfirmed on a NEX-6.
- Parameter-level approximation, not a port of the a5100 matrix/gamma or LUT engine.
- Looks apply only while the app is open; the native camera does not retain them.
- Some bodies may drop Kelvin, fine-tune or DRO parameters; the app falls back to the core subset and logs `DROPPED`.
- The focus frame is an app-centered indicator, not the body's selected AF point.
- Still images only; no movie recording.
- Preview aspect and orientation may need adjustment after hardware testing.

## License and credits

This project originated from, and thanks, [ukiki0718-netizen/sony-a5100-film-studio](https://github.com/ukiki0718-netizen/sony-a5100-film-studio): the a5100 Film Studio defined the fifteen looks and the validation approach, and the NEX-6 shell is an independent rewrite built on that work.

Original code and documentation are under [PolyForm Noncommercial 1.0.0](LICENSE). `tools/sign_apk.py` is adapted from bonyback1/sony-pmca-ricoh-mod and remains Apache-2.0; the bundled Noto Sans subsets are SIL OFL 1.1. Recipe Lab and sony-lut-camera are GPL-3.0 and were used as behavioral/interface references only; no source was copied.

This repository contains no Sony firmware or base-app code, no official Fujifilm LUTs, no Ricoh matrix data and no signing private key. Sony, PlayMemories, FUJIFILM and Ricoh trademarks belong to their owners; this project is not affiliated with, certified by or endorsed by them.

See [LICENSING.md](LICENSING.md), [NOTICE](NOTICE) and [SOURCES.md](docs/SOURCES.md).

## Documentation

- [Installation guide (English)](docs/INSTALL.en.md) · [安装教程（中文）](docs/INSTALL.zh-CN.md)
- [Architecture](docs/ARCHITECTURE.md) (Chinese)
- [Development and build](docs/DEVELOPMENT.md) (Chinese)
- [Compatibility and limits](docs/COMPATIBILITY.md) (Chinese)
- [Validation records](docs/VALIDATION.md)
- [Sources](docs/SOURCES.md)
- [Changelog](CHANGELOG.md)

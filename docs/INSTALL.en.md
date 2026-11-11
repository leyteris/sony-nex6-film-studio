# Installation and use: Film Studio NEX-6

[Project home](../README.md) · [中文](INSTALL.zh-CN.md)

This guide targets the **Sony NEX-6, firmware 1.03, Android 2.3.7 / API 10**. The current `0.2.19-alpha` build **has not been validated on hardware**. The first-run checklist below is also the list of results worth recording. Use the DISP overlay first; the card files are a supplement.

> **Do not** use the camera's Settings → Initialize to troubleshoot. The CN2 preset closes the app entry again and the unlock has to be repeated. The unlock parameters apply only to NEX-6 1.03 / `CX76001_CN2`, not to other models or regions.

## 0. Before you start

You need:

- a NEX-6 whose app menu has been restored (see the **sony-nex6-app-menu-unlock** procedure; Windows only);
- Wi-Fi ADB enabled on the camera (Sony-PMCA-RE / OpenMemories: Tweak);
- [Android Platform-Tools / adb](https://developer.android.com/tools/releases/platform-tools) on the computer;
- sufficient battery, a backed-up card and a working USB connection;
- `FilmStudioNex6-0.2.19-alpha.apk`.

The installer adds an ordinary PMCA app. It does not write firmware, camera settings storage or the Backup attribute used by the unlock. Installing this APK is independent of the unlock procedure.

## 1. Download and verify

Download `FilmStudioNex6-0.2.19-alpha.apk` from Releases (built by CI on tag `0.2.19`), or use the local build at `output/FilmStudioNex6-0.2.19-alpha.apk`.

SHA-256 of the local build:

```text
6450c4676599a1ca51c4e492f2ee31a56d2f37158a70c9b715789b07d385db6d
```

> The release APK is CI-signed with a throwaway debug key, so its SHA-256 differs from the local build above. Verify it against the `SHA256SUMS.txt` attached to the release. The two signatures cannot update each other in place; use the local build when you need the checksum documented here.

macOS / Linux:

```sh
shasum -a 256 FilmStudioNex6-0.2.19-alpha.apk
```

Windows PowerShell:

```powershell
Get-FileHash -Algorithm SHA256 .\FilmStudioNex6-0.2.19-alpha.apk
```

This checks file integrity, not compatibility or legal permission.

## 2. Connect the camera

Find the current Wi-Fi ADB address under Tweak → Developer on the camera. `CAMERA_IP` is only a placeholder; replace it with your own address and keep the `:5555` port:

```sh
adb connect CAMERA_IP:5555
adb devices
```

`adb devices` should list `CAMERA_IP:5555	device`. If it reports `offline` or `unauthorized`, reconnect or toggle Wi-Fi ADB on the camera.

**Privacy**: mask the real IP before publishing screenshots, logs or command transcripts. The app log does not record IP addresses or serial numbers.

## 3. Install

```sh
adb -s CAMERA_IP:5555 install -r FilmStudioNex6-0.2.19-alpha.apk
```

A successful install prints `Success`. The package is `com.filmstudio.nex6`, so it can coexist with the older a5100 Film Studio; no uninstall is required.

If you see `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, a different-signature copy of `com.filmstudio.nex6` is already installed. Uninstall only that package and reinstall:

```sh
adb -s CAMERA_IP:5555 uninstall com.filmstudio.nex6
adb -s CAMERA_IP:5555 install FilmStudioNex6-0.2.19-alpha.apk
```

**Do not uninstall Sony system packages or any package you do not recognize.** If the only problem is a signature mismatch, rebuild with the same `.private/signing.pem`; that key is not distributed.

## 4. First launch and checks

Open **Film Studio NEX-6** from the camera's app list. If the list has not refreshed, reboot the camera and look again.

> **Set the mode dial to P / A / S / M first.** iAuto and SCN hand Creative Style and Picture Effect back to the camera, so B&W renders in color and other looks may be overridden. The orange `P/A/S/M: STYLE` hint at the right of the full panel's bottom line is there to keep this in view; it is shown regardless of the dial position.

Check and record, in order:

1. **Live view**: does the preview appear, does the overlay stay visible, or does the firmware return to the native UI?
2. **Looks**: LEFT/RIGHT or the wheels should switch looks with a visible change; UP/DOWN should change strength (30/50/70/100%); confirm the bottom-right `P/A/S/M: STYLE` hint is complete and does not overlap the left key hints;
3. **UI stages**: one DELETE or SK2 press should shrink the UI to the bottom-left pill; another should leave only the live view; a third should restore the full panel.
4. **Focus**: half-press S1; a centered focus frame should appear (white → green/red) and disappear on release.
5. **Capture**: the center button or shutter should save a JPEG. The log starts with `SHOOT requested` and within about 2.5–10 seconds writes `SAVED <name> <bytes>` or `SAVED no new JPEG`.
6. **Language**: with a Chinese system locale the UI should be Simplified or Traditional Chinese, and the log should contain `FONT ... ok script=zh-CN/zh-TW`.
7. **On-screen log**: DISP opens the log overlay; the header reads `LOG n from-to` and the header-right hint `DISP: close   UP/DOWN: scroll`. DISP or BACK closes it.
8. **Monochrome read-back**: with ACROS / HCBW / MORI selected, the top-right corner should read `CM mono FX richtone-mono` (ACROS) or `CM mono FX rough-mono` (HCBW/MORI). If either `CM` or `FX` is a mono token, the frame should be grayscale. If it reads `CM standard FX off`, the firmware rejected both paths; record that line with the look name.
9. **Restore and exit**: MENU or BACK should produce `EXIT menu` → `RESTORE ok` → `PAUSE released finishing=true` → `END app` → `EXPORT ...`; the native preview should return with its original color and parameters.

You do not need to change camera settings for the first run. Leave Focus Area at its default; the frame is an app-drawn centered indicator, not the body's selected AF point.

## 5. Log

**Preferred: the on-screen log.** From 0.2.14, DISP shows the last 600 buffered lines directly on the camera screen. It needs neither the card nor a computer:

- while the log is open, UP/DOWN page it and DISP or BACK closes it; MENU always exits;
- other keys are swallowed while it is open, but MENU still exits the app;
- the `LOG n from-to` header shows the total buffer size and the visible range;
- `PROBE ... size=… zero=… text=…` reports whether the previous session's `REPORT.TXT` / `FSLOG.TXT` read back as text; `text=0` means it read back as all NUL.

Two files are still written to the card as a card-reader supplement:

```text
/sdcard/FSNEX6/REPORT.TXT   snapshot written on exit/crash
/sdcard/FSNEX6/FSLOG.TXT    append journal kept while running (crash residue)
```

The card root is used only if the directory cannot be created. Sony FUSE rejects simultaneous read/write of the same file: the continuously appended `FSLOG.TXT` reads back as all-NUL while the app is alive; `REPORT.TXT` is a fresh file written once and closed on exit/crash. 0.2.14 reads it back immediately after close and records `EXPORT-VERIFY size=… zero=… text=…` in `FSLOG.TXT`, then records the same check as `PROBE` on the next start.

If `PROBE` reports `text=0`, either the file reads back as NUL on the card or the read path is at fault: **copy `FSNEX6/REPORT.TXT` with a card reader**, not by dragging over camera USB/MTP or relying on a temporary desktop copy. Exit the app normally before removing the card.

With adb available:

```sh
adb -s CAMERA_IP:5555 pull /sdcard/FSNEX6/REPORT.TXT
```

## 5.1 Events

Main events:

| Event | Meaning |
| --- | --- |
| `START` / `BODY` | App start; model, firmware, platform and Android version (no serial) |
| `OPEN` / `CAP` / `PREVIEW` | Camera open path, supported parameter lists, preview size and retries |
| `APPLY` / `DROPPED` / `STYLE` / `EFFECT` / `MONO` | Applied parameters, dropped keys, `cm=` (color-mode read-back), `fx=` (picture-effect read-back); `MONO` is the verdict after both paths failed and were retried |
| `PROBE` / `EXPORT-VERIFY` | Start-up read-back of the previous card log, and the immediate post-export read-back (`zero`/`text` byte counts) |
| `FOCUS` | `requested` / `locked` / `failed` / `FOCUS-FAIL` |
| `SHOOT` / `SAVED` | Capture request; whether a new JPEG appeared under `DCIM` |
| `UI` | `full` / `pill` / `hidden` |
| `KEY` / `KEYUP` | First press/release scan code and keyCode of each key |
| `RESTORE` / `EXIT` / `PAUSE` / `EXPORT` | Exit, parameter restore and snapshot export |
| `ALIVE` | 60-second heartbeat |
| `CRASH` | Uncaught exception |

The log contains no IP address, serial number or image content.

## 6. Update and uninstall

Update in place:

```sh
adb -s CAMERA_IP:5555 install -r FilmStudioNex6-<new-version>.apk
```

With the same signing key this updates the existing install. To uninstall:

```sh
adb -s CAMERA_IP:5555 uninstall com.filmstudio.nex6
```

Uninstalling removes the app and its preferences only; it does not delete photos under `DCIM` and does not change the unlock state.

## 7. Troubleshooting

| Symptom | Likely cause / action |
| --- | --- |
| Film Studio NEX-6 is not in the app list | App menu not unlocked, or list not refreshed; reboot the camera, then repeat the unlock if needed |
| Only the native UI appears / no overlay | Press DISP and inspect `START`, `OPEN`, `CRASH`, `PREVIEW-FAIL` |
| No log file | Press DISP first; the card files are under `/sdcard/FSNEX6/REPORT.TXT` and `FSLOG.TXT`; exit normally before copying |
| Black preview | Check `OPEN` / `PREVIEW` / `PREVIEW-FAIL`; restart the app and make sure no other app owns the camera |
| Switching looks has no effect | Check `APPLY`, `DROPPED`, `STYLE`, `EFFECT` and the top-right `CM ... FX ...`; the firmware may reject some keys and the app falls back to the core subset |
| A monochrome look still renders in color | **Check the mode dial first: it must be on P / A / S / M.** In iAuto / SCN the camera owns the look and B&W necessarily renders in color; that is firmware behavior. With the dial correct, check whether the corner reads `CM mono` or `FX <mono effect>`. If neither is a mono token (e.g. `CM standard FX off`), the firmware rejected both Creative Style and Picture Effect; record that line plus the `MONO` / `EFFECT` entries |
| Shutter does not save a JPEG | Check `SHOOT` and `SAVED`; `SAVED no new JPEG` means the camera did not file a picture within about 10 s |
| Focus frame stays red | Low light or low contrast; check the camera's Focus Area setting; the red path is not hardware-verified |
| Signature conflict on install | Rebuild with the same key, or uninstall only `com.filmstudio.nex6` and reinstall |
| Card log is all NUL | Use the DISP overlay; `PROBE` / `EXPORT-VERIFY` show the `zero`/`text` byte counts. If `text=0`, use a card reader instead of MTP |
| Color or effect read-back shows `?` | The firmware does not expose that read-back; use the target value in the `APPLY` line |

## 8. Safety and privacy

- No firmware writes, no camera settings storage, no factory reset or initialize.
- Back up the card and charge the battery before the first hardware test.
- Unlock parameters apply only to NEX-6 1.03 / `CX76001_CN2`; do not apply them to other models, firmware or regions.
- Do not uninstall Sony system packages or install APKs from unknown sources.
- This is experimental software. It does not come with a “cannot go wrong” guarantee, and no compatibility or usability claim is made before hardware confirmation.
- You can search a shared log for `IP` or `serial`, but the app does not write those fields; mask `CAMERA_IP` in command transcripts.

When reporting a hardware result, include the model, firmware, app version, tested items and `REPORT.TXT`. Only real NEX-6 evidence can change `camera_tested` to `true`.

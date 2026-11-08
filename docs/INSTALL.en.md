# Installation and use: Film Studio NEX-6

[Project home](../README.md) · [中文](INSTALL.zh-CN.md)

This guide targets the **Sony NEX-6, firmware 1.03, Android 2.3.7 / API 10**. The current `0.2.12-alpha` build **has not been validated on hardware**. The first-run checklist below is also the list of results worth recording in `/sdcard/FSNEX6/FSLOG.TXT`.

> **Do not** use the camera's Settings → Initialize to troubleshoot. The CN2 preset closes the app entry again and the unlock has to be repeated. The unlock parameters apply only to NEX-6 1.03 / `CX76001_CN2`, not to other models or regions.

## 0. Before you start

You need:

- a NEX-6 whose app menu has been restored (see the **sony-nex6-app-menu-unlock** procedure; Windows only);
- Wi-Fi ADB enabled on the camera (Sony-PMCA-RE / OpenMemories: Tweak);
- [Android Platform-Tools / adb](https://developer.android.com/tools/releases/platform-tools) on the computer;
- sufficient battery, a backed-up card and a working USB connection;
- `FilmStudioNex6-0.2.12-alpha.apk`.

The installer adds an ordinary PMCA app. It does not write firmware, camera settings storage or the Backup attribute used by the unlock. Installing this APK is independent of the unlock procedure.

## 1. Download and verify

Download `FilmStudioNex6-0.2.12-alpha.apk` from Releases, or use the local build at `output/FilmStudioNex6-0.2.12-alpha.apk`.

SHA-256 of the current build:

```text
51889474607a71f8b2f0db1ad2aab74f924d6db7c50a72bfaa2a5df0d2f2e349
```

macOS / Linux:

```sh
shasum -a 256 FilmStudioNex6-0.2.12-alpha.apk
```

Windows PowerShell:

```powershell
Get-FileHash -Algorithm SHA256 .\FilmStudioNex6-0.2.12-alpha.apk
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
adb -s CAMERA_IP:5555 install -r FilmStudioNex6-0.2.12-alpha.apk
```

A successful install prints `Success`. The package is `com.filmstudio.nex6`, so it can coexist with the older a5100 Film Studio; no uninstall is required.

If you see `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, a different-signature copy of `com.filmstudio.nex6` is already installed. Uninstall only that package and reinstall:

```sh
adb -s CAMERA_IP:5555 uninstall com.filmstudio.nex6
adb -s CAMERA_IP:5555 install FilmStudioNex6-0.2.12-alpha.apk
```

**Do not uninstall Sony system packages or any package you do not recognize.** If the only problem is a signature mismatch, rebuild with the same `.private/signing.pem`; that key is not distributed.

## 4. First launch and checks

Open **Film Studio NEX-6** from the camera's app list. If the list has not refreshed, reboot the camera and look again.

Check and record, in order:

1. **Live view**: does the preview appear, does the overlay stay visible, or does the firmware return to the native UI?
2. **Looks**: LEFT/RIGHT or the wheels should switch looks with a visible change; UP/DOWN should change strength (30/50/70/100%).
3. **UI stages**: one DELETE or SK2 press should shrink the UI to the bottom-left pill; another should leave only the live view; a third should restore the full panel.
4. **Focus**: half-press S1; a centered focus frame should appear (white → green/red) and disappear on release.
5. **Capture**: the center button or shutter should save a JPEG. The log starts with `SHOOT requested` and within about 2.5–10 seconds writes `SAVED <name> <bytes>` or `SAVED no new JPEG`.
6. **Language**: with a Chinese system locale the UI should be Simplified or Traditional Chinese, and the log should contain `FONT ... ok script=zh-CN/zh-TW`.
7. **Restore and exit**: MENU or BACK should produce `EXIT menu` → `RESTORE ok` → `PAUSE released finishing=true` → `END app`; the native preview should return with its original color and parameters.

You do not need to change camera settings for the first run. Leave Focus Area at its default; the frame is an app-drawn centered indicator, not the body's selected AF point.

## 5. Card log

```text
/sdcard/FSNEX6/FSLOG.TXT
```

The card-root `/sdcard/FSLOG.TXT` is used only if the directory cannot be created. The file writes its own path at the top.

Retrieve it with:

```sh
adb -s CAMERA_IP:5555 pull /sdcard/FSNEX6/FSLOG.TXT
```

Pull it after exiting the app rather than dragging the text file over MTP while it is being written. If the file is all NUL or has an unexpected size, zip it on the camera first, or use:

```sh
adb -s CAMERA_IP:5555 shell cat /sdcard/FSNEX6/FSLOG.TXT > FSLOG.TXT
```

Main events:

| Event | Meaning |
| --- | --- |
| `START` / `BODY` | App start; model, firmware, platform and Android version (no serial) |
| `OPEN` / `CAP` / `PREVIEW` | Camera open path, supported parameter lists, preview size and retries |
| `APPLY` / `DROPPED` / `STYLE` | Applied parameters, keys the firmware dropped, style fallback |
| `FOCUS` | `requested` / `locked` / `failed` / `FOCUS-FAIL` |
| `SHOOT` / `SAVED` | Capture request; whether a new JPEG appeared under `DCIM` |
| `UI` | `full` / `pill` / `hidden` |
| `KEY` / `KEYUP` | First press/release scan code and keyCode of each key |
| `RESTORE` / `EXIT` / `PAUSE` / `END` | Exit and parameter restore |
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
| Only the native UI appears / no overlay | Retrieve `FSLOG.TXT`; inspect `START`, `OPEN`, `CRASH`, `PREVIEW-FAIL` |
| No log file | Check `/sdcard/FSNEX6/FSLOG.TXT`, then `/sdcard/FSLOG.TXT`; exit the app before pulling |
| Black preview | Check `OPEN` / `PREVIEW` / `PREVIEW-FAIL`; restart the app and make sure no other app owns the camera |
| Switching looks has no effect | Check `APPLY`, `DROPPED`, `STYLE`; the firmware may reject some keys and the app falls back to the core subset |
| Shutter does not save a JPEG | Check `SHOOT` and `SAVED`; `SAVED no new JPEG` means the camera did not file a picture within about 10 s |
| Focus frame stays red | Low light or low contrast; check the camera's Focus Area setting; the red path is not hardware-verified |
| Signature conflict on install | Rebuild with the same key, or uninstall only `com.filmstudio.nex6` and reinstall |
| Log is all NUL | The transfer did not land; pull again after exiting, or zip the file |

## 8. Safety and privacy

- No firmware writes, no camera settings storage, no factory reset or initialize.
- Back up the card and charge the battery before the first hardware test.
- Unlock parameters apply only to NEX-6 1.03 / `CX76001_CN2`; do not apply them to other models, firmware or regions.
- Do not uninstall Sony system packages or install APKs from unknown sources.
- This is experimental software. It does not come with a “cannot go wrong” guarantee, and no compatibility or usability claim is made before hardware confirmation.
- You can search a shared log for `IP` or `serial`, but the app does not write those fields; mask `CAMERA_IP` in command transcripts.

When reporting a hardware result, include the model, firmware, app version, tested items and `FSLOG.TXT`. Only real NEX-6 evidence can change `camera_tested` to `true`.

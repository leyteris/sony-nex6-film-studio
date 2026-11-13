#!/usr/bin/env python3
# SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
"""Build, sign and verify the standalone NEX-6 app apk (app/).

The app is a plain API-10 Activity compiled against the public SDK; it uses
no third-party libraries and no native code. Signing reuses the local debug
key through tools/sign_apk.py (v1 JAR signature, SHA-1, no CMS attributes),
which is the format the first-generation NEX-6 firmware accepts.
"""
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / 'app'
BUILD = ROOT / 'build-local/app'
OUTPUT = ROOT / 'output/FilmStudioNex6-0.2.20-alpha.apk'
VALIDATION = ROOT / 'validation/FilmStudioNex6-0.2.20-alpha.json'


def sdk_root():
    for name in ('ANDROID_SDK_ROOT', 'ANDROID_HOME'):
        value = os.environ.get(name)
        if value:
            return Path(value)
    return Path.home() / 'Library/Android/sdk'


SDK = sdk_root()
BUILD_TOOLS = Path(os.environ.get('ANDROID_BUILD_TOOLS', SDK / 'build-tools/36.0.0'))
ANDROID_JAR = Path(os.environ.get('ANDROID_JAR', SDK / 'platforms/android-37.0/android.jar'))
KEY = ROOT / '.private/signing.pem'

sys.path.insert(0, str(ROOT / 'tools'))
from sign_apk import sign_apk  # noqa: E402


def find_jdk_tool(name):
    local = ROOT / 'inputs/amazon-corretto-17.jdk/Contents/Home/bin' / name
    if local.exists():
        return str(local)
    home = os.environ.get('JAVA_HOME')
    if home and (Path(home) / 'bin' / name).exists():
        return str(Path(home) / 'bin' / name)
    found = shutil.which(name)
    if not found:
        raise SystemExit('%s not found; set JAVA_HOME to a JDK 17 install' % name)
    return found


def jdk_env():
    env = dict(os.environ)
    javac = Path(find_jdk_tool('javac'))
    home = javac.parent.parent
    env['JAVA_HOME'] = str(home)
    env['PATH'] = str(home / 'bin') + os.pathsep + env.get('PATH', '')
    return env


def run(cmd, env=None):
    print('$', ' '.join(str(c) for c in cmd))
    subprocess.run([str(c) for c in cmd], check=True, env=env)


def sha256(path):
    h = hashlib.sha256()
    with open(path, 'rb') as f:
        for chunk in iter(lambda: f.read(1 << 20), b''):
            h.update(chunk)
    return h.hexdigest()


def main():
    for tool in ('aapt2', 'd8', 'dexdump', 'apksigner'):
        if not (BUILD_TOOLS / tool).exists():
            raise SystemExit('missing %s in %s' % (tool, BUILD_TOOLS))
    if not ANDROID_JAR.exists():
        raise SystemExit('missing android.jar: %s' % ANDROID_JAR)

    if BUILD.exists():
        shutil.rmtree(BUILD)
    classes = BUILD / 'classes'
    dex = BUILD / 'dex'
    classes.mkdir(parents=True)
    dex.mkdir(parents=True)

    unsigned = BUILD / 'unsigned.apk'
    res_zip = BUILD / 'res.zip'
    run([BUILD_TOOLS / 'aapt2', 'compile', '--dir', SRC / 'res', '-o', res_zip])
    # --no-compile-sdk-metadata keeps android:compileSdkVersion and the
    # platformBuildVersion attributes out of the manifest; neither ID exists in
    # the 2.3.7 framework and nothing in this app needs them.
    run([
        BUILD_TOOLS / 'aapt2', 'link',
        '-o', unsigned,
        '-I', ANDROID_JAR,
        '--manifest', SRC / 'AndroidManifest.xml',
        '--no-compile-sdk-metadata',
        '--min-sdk-version', '10',
        '--target-sdk-version', '10',
        '-A', SRC / 'assets',
        res_zip,
    ])

    sources = sorted(str(p) for p in (SRC / 'src').rglob('*.java'))
    run([
        find_jdk_tool('javac'),
        '-source', '8', '-target', '8',
        '-bootclasspath', ANDROID_JAR,
        '-encoding', 'UTF-8',
        '-d', classes,
    ] + sources)

    bytecode = sorted(str(p) for p in classes.rglob('*.class'))
    run([
        BUILD_TOOLS / 'd8', '--min-api', '10',
        '--lib', ANDROID_JAR,
        '--output', dex,
    ] + bytecode, env=jdk_env())

    classes_dex = dex / 'classes.dex'
    with zipfile.ZipFile(unsigned, 'a', compression=zipfile.ZIP_DEFLATED) as z:
        z.write(classes_dex, 'classes.dex')

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    if OUTPUT.exists():
        OUTPUT.unlink()
    local_key = KEY.exists()
    if local_key:
        sign_apk(str(unsigned), str(OUTPUT), str(KEY))
    else:
        print('WARNING: %s missing, signing with a throwaway debug key' % KEY)
        sign_apk(str(unsigned), str(OUTPUT), None)

    with zipfile.ZipFile(OUTPUT) as z:
        names = z.namelist()
        for required in ('AndroidManifest.xml', 'classes.dex',
                         'META-INF/MANIFEST.MF', 'META-INF/CERT.SF', 'META-INF/CERT.RSA'):
            if required not in names:
                raise SystemExit('output apk is missing %s' % required)
        magic = z.read('classes.dex')[:8]
        if magic[:4] != b'dex\n':
            raise SystemExit('classes.dex is not a dex file: %r' % magic)
        dex_version = magic[4:7].decode('ascii')

    badging = subprocess.run(
        [str(BUILD_TOOLS / 'aapt'), 'dump', 'badging', str(OUTPUT)],
        capture_output=True, text=True, check=True).stdout
    package = re.search(r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'", badging)
    sdk = re.search(r"sdkVersion:'([^']+)'", badging)
    label = re.search(r"application-label:'([^']+)'", badging)
    if not package or package.group(1) != 'com.filmstudio.nex6':
        raise SystemExit('unexpected package line:\n%s' % badging)

    verify = subprocess.run(
        [str(BUILD_TOOLS / 'apksigner'), 'verify', '--min-sdk-version', '10',
         '--print-certs', str(OUTPUT)],
        capture_output=True, text=True, env=jdk_env())
    if verify.returncode != 0:
        raise SystemExit('apksigner rejected the apk:\n%s\n%s' % (verify.stdout, verify.stderr))

    digest = sha256(OUTPUT)
    report = {
        'file': OUTPUT.name,
        'package': package.group(1),
        'version': package.group(3),
        'version_code': int(package.group(2)),
        'sha256': digest,
        'size_bytes': OUTPUT.stat().st_size,
        'min_sdk': int(sdk.group(1)) if sdk else None,
        'label': label.group(1) if label else None,
        'dex_version': dex_version,
        'architecture': 'dex only, no native code',
        'signature': 'v1 JAR (SHA-1, RSA, openssl smime -noattr -binary)',
        'signing_key': (
            'local key (.private/signing.pem, not distributed)'
            if local_key else
            'throwaway debug key generated for this build (not distributed)'
        ),
        'target': 'Sony NEX-6 (first-generation PMCA, firmware 1.03, Android 2.3.7 / API 10)',
        'camera_tested': False,
        'cameras_tested': [],
        'install_note': (
            'Standalone package; it installs beside older Film Studio builds. '
            'DISP opens the on-body log overlay, which does not depend on the card files '
            'reading back. /sdcard/FSNEX6/REPORT.TXT (fresh closed snapshot on exit/crash) and '
            'FSLOG.TXT (append journal) are still written for a card-reader copy.'
        ),
        'limitations': [
            'looks are live camera parameters (color mode, mono Picture Effect, saturation, contrast, sharpness, WB, DRO), not the 3D LUT engine',
            'the mode dial must stay on P/A/S/M: iAuto and SCN let the camera override Creative Style and Picture Effect, so the look does not land',
            'ACROS and HCBW set color-mode=mono plus a mono Picture Effect, which makes the camera ignore Creative Style adjustments; MORI uses color-mode=mono with contrast/sharpness sliders and falls back to rough-mono only if the body rejects mono',
            'bodies without a Neutral Creative Style (NEX-6 generation) fall back to Standard with an extra strength-scaled saturation cut for neutral-based looks; the STYLE log line records the fallback',
            'settings are applied only while the app is open and restored on exit',
            'center button or shutter saves the JPEG through the native camera; FSLOG reports whether a new DCIM file appeared',
            'UI starts at the full panel; DELETE or SK2 cycles full -> pill -> hidden with the live view always visible',
            'S1 draws an app-centered focus frame (white searching, green locked, red failed); it is not the camera AF point, which PMCA does not expose',
            'still images only; PLAY and MOVIE are swallowed; DISP shows the in-memory log overlay',
        ],
        'looks': 17,
        'look_families': {'fujifilm': 10, 'ricoh': 5, 'leica': 2},
        'strengths': [30, 50, 70, 100],
        'ui_modes': ['full', 'pill', 'hidden'],
        'ui_keys': ['delete (scan 595)', 'SK2 (scan 513)'],
        'languages': ['en', 'zh-CN', 'zh-TW'],
        'language_detection': 'Locale.getDefault(); zh-HK/zh-MO/zh-TW use the Traditional font, other zh the Simplified font',
        'localized_look_names': True,
        'fonts': [
            'assets/fonts/NotoSansSC-UI.ttf (subset, SIL OFL 1.1)',
            'assets/fonts/NotoSansTC-UI.ttf (subset, SIL OFL 1.1)',
        ],
        'log_path': 'DISP overlay (in-memory, last 600 lines, no card read required); /sdcard/FSNEX6/REPORT.TXT (fresh closed snapshot) + FSLOG.TXT (append journal; 8.3-safe directory, root FSLOG.TXT only as last-resort fallback)',
    }
    VALIDATION.parent.mkdir(parents=True, exist_ok=True)
    with open(VALIDATION, 'w', encoding='utf-8') as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
        f.write('\n')

    print()
    print('built    %s' % OUTPUT)
    print('package  %s  %s  (versionCode %s)' % (report['package'], report['version'], report['version_code']))
    print('sdk      min/target 10, dex %s, label %s' % (dex_version, report['label']))
    print('sha256   %s' % digest)
    print('size     %d bytes' % report['size_bytes'])
    print('verify   v1 signature accepted for min-sdk 10')


if __name__ == '__main__':
    main()

#!/usr/bin/env python3
# SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
"""Fetch the Noto Sans SC/TC UI subsets for the NEX-6 app.

The NEX-6 firmware font has no CJK glyphs, so the app bundles two small
Noto Sans subsets (SIL OFL 1.1). Google Fonts' text= endpoint returns a plain
TTF for an Android 2.3 user agent, which is the format Typeface.createFromAsset
reads on API 10. The character inventory below mirrors Lang.java; keep the two
in sync when UI strings change, then rerun this script.

Outputs (committed, a few tens of KB):
  app/assets/fonts/NotoSansSC-UI.ttf
  app/assets/fonts/NotoSansTC-UI.ttf
  app/assets/fonts/OFL-NotoSansSC.txt
  app/assets/fonts/OFL-NotoSansTC.txt

Network access is needed only when regenerating the fonts.
"""
from pathlib import Path
import re
import sys
import urllib.parse
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'app/assets/fonts'
UA = ('Mozilla/5.0 (Linux; U; Android 2.3.7; en-us; Nexus One Build/FRF91) '
      'AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1')

SC_TEXT = ('强度上下风格左右中央拍摄界面退出已快门失败相机未就绪打开实时取景参数被拒绝'
           '标准鲜艳经典正片负电影漂白黑理光高反差森山逆冲柔和关闭翻页富士徕卡自然')
TC_TEXT = ('強度上下風格左右中央拍攝介面退出已快門失敗相機未就緒開啟即時取景參數被拒絕離開'
           '標準鮮豔經典正片負電影漂白黑理光高反差森山逆沖柔和關閉翻頁富士徠卡自然')

FAMILIES = [
    ('Noto Sans SC', 'NotoSansSC-UI.ttf', 'OFL-NotoSansSC.txt', SC_TEXT),
    ('Noto Sans TC', 'NotoSansTC-UI.ttf', 'OFL-NotoSansTC.txt', TC_TEXT),
]

OFL_URL = {
    'Noto Sans SC': 'https://cdn.jsdelivr.net/gh/google/fonts@main/ofl/notosanssc/OFL.txt',
    'Noto Sans TC': 'https://cdn.jsdelivr.net/gh/google/fonts@main/ofl/notosanstc/OFL.txt',
}


def fetch(url):
    req = urllib.request.Request(url, headers={'User-Agent': UA})
    with urllib.request.urlopen(req, timeout=120) as r:
        return r.read()


def charset(extra):
    ascii_printable = ''.join(chr(c) for c in range(0x20, 0x7f))
    return ''.join(sorted(set(ascii_printable + extra)))


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for family, filename, license_name, extra in FAMILIES:
        text = charset(extra)
        query = urllib.parse.urlencode({'family': family + ':wght@700', 'text': text})
        css = fetch('https://fonts.googleapis.com/css2?' + query).decode('utf-8')
        urls = re.findall(r'url\((https://[^)]+)\)', css)
        if not urls:
            raise SystemExit('no font url for %s:\n%s' % (family, css))
        font = fetch(urls[0])
        if font[:4] != b'\x00\x01\x00\x00':
            raise SystemExit('%s is not TrueType: %r' % (family, font[:8]))
        (OUT / filename).write_bytes(font)

        try:
            from fontTools.ttLib import TTFont
        except ImportError:
            print('fontTools not installed, skipping coverage check')
        else:
            cmap = TTFont(str(OUT / filename)).getBestCmap()
            missing = [c for c in text if ord(c) not in cmap]
            if missing:
                raise SystemExit('%s is missing %r' % (family, ''.join(missing)))

        (OUT / license_name).write_bytes(fetch(OFL_URL[family]))
        print('%-14s %6d bytes  %d glyphs requested' % (filename, len(font), len(text)))


if __name__ == '__main__':
    sys.exit(main())

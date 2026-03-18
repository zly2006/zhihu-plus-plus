# Zhihu Feed Ad Filter (Chrome Extension)

This extension injects a fetch hook into `www.zhihu.com` and filters ad-like entries from:

- `https://www.zhihu.com/api/v3/feed/topstory/recommend`

## Ad rules (mapped from app `checkForAd`)

An item is removed when one of the following matches:

- `target.paid_info` (or `target.paidInfo`) is present
- item content contains ad links/markers:
  - `xg.zhihu.com`
  - `d.zhihu.com`
  - `data-edu-card-id`
  - `mp.weixin.qq.com`

Additional explicit ad markers are also removed when present:

- `feed_advert` item type
- `promotion_extra`, `ad_info`, `feed_specific`, `monitor_urls`

## Files

- `manifest.json`: MV3 extension metadata
- `content.js`: injects page-world script at `document_start`
- `injected.js`: fetch interception and payload filtering logic
- `test/filter.test.mjs`: lightweight Node test for core logic

## Load in Chrome

1. Open `chrome://extensions`
2. Enable **Developer mode**
3. Click **Load unpacked**
4. Select folder: `misc/chrome-zhihu-ad-filter`

## Run local tests

```bash
node misc/chrome-zhihu-ad-filter/test/filter.test.mjs
```

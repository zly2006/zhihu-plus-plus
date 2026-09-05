import { ArkUIViewController, Compose } from '@cpf-kmp-cmp/compose';
import { preferences } from '@kit.ArkData';
import { util } from '@kit.ArkTS';
import { http } from '@kit.NetworkKit';
import { hilog } from '@kit.PerformanceAnalysisKit';
import nativeApi from 'libentry.so';

const DOMAIN = 0x0000;
const DAILY_API = 'https://news-at.zhihu.com/api/4';
const GUEST_PREFERENCES = 'p2_guest_session';
const GUEST_COOKIE_KEY = 'cookies';
const REQUEST_TIMEOUT = 15000;
const MOBILE_USER_AGENT =
  'com.zhihu.android/Futureve/10.61.0 Mozilla/5.0 (Linux; Android 12; sdk_gphone64_arm64 ' +
    'Build/SE1A.220630.001.A1; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 ' +
    'Chrome/57.0.1000.10 Mobile Safari/537.36';
const MOBILE_X_APP_ZA =
  'OS=Android&Release=12&Model=sdk_gphone64_arm64&VersionName=10.61.0&VersionCode=26107&' +
    'Product=com.zhihu.android&Width=1440&Height=2952&Installer=%E7%81%B0%E5%BA%A6&' +
    'DeviceType=AndroidPhone&Brand=google';

interface DailyStoryPayload {
  id: number;
  hint: string;
}

interface DailyHomePayload {
  stories: Array<DailyStoryPayload>;
}

interface DailyDetailPayload {
  image: string;
  body: string;
}

interface ImagePayload {
  url: string;
  data: string;
}

async function requestText(url: string, headers: Record<string, string> = {}): Promise<string> {
  let request: http.HttpRequest | undefined = undefined;
  try {
    request = http.createHttp();
    const response = await request.request(url, {
      method: http.RequestMethod.GET,
      header: headers,
      expectDataType: http.HttpDataType.STRING,
      connectTimeout: REQUEST_TIMEOUT,
      readTimeout: REQUEST_TIMEOUT,
      maxLimit: 2 * 1024 * 1024,
      maxRedirects: 2,
      usingCache: false
    });
    if (Number(response.responseCode) < 200 || Number(response.responseCode) >= 300 ||
      typeof response.result !== 'string') {
      throw new Error(`HTTP ${response.responseCode}`);
    }
    return response.result;
  } finally {
    request?.destroy();
  }
}

async function requestImageBase64(url: string): Promise<string> {
  if (!/^https:\/\/[a-zA-Z0-9-]+\.zhimg\.com\//.test(url)) {
    throw new Error('图片地址不是知乎 HTTPS CDN');
  }
  let request: http.HttpRequest | undefined = undefined;
  try {
    request = http.createHttp();
    const response = await request.request(url, {
      method: http.RequestMethod.GET,
      expectDataType: http.HttpDataType.ARRAY_BUFFER,
      connectTimeout: REQUEST_TIMEOUT,
      readTimeout: REQUEST_TIMEOUT,
      maxLimit: 5 * 1024 * 1024,
      maxRedirects: 0,
      usingCache: false
    });
    if (Number(response.responseCode) !== 200 || !(response.result instanceof ArrayBuffer)) {
      throw new Error(`封面请求失败：HTTP ${response.responseCode}`);
    }
    return new util.Base64Helper().encodeToStringSync(new Uint8Array(response.result));
  } finally {
    request?.destroy();
  }
}

@Entry
@Component
struct Index {
  private controller: ArkUIViewController | undefined = undefined;

  @State errorMessage: string = 'Native module not ready';

  aboutToAppear() {
    if (nativeApi === undefined) {
      hilog.error(DOMAIN, 'Compose', 'nativeApi is undefined, cannot create controller');
      this.errorMessage = 'nativeApi is undefined';
      return;
    }
    try {
      this.controller = nativeApi.MainArkUIViewController();
      const state = this.controller ? 'created' : 'not_created';
      hilog.info(DOMAIN, 'Compose', 'controller init state: %{public}s', state);
      if (!this.controller) {
        this.errorMessage = 'Controller creation failed (returned null)';
        return;
      }
      if (!nativeApi.usesNativeNetwork()) {
        this.loadP2Data();
      }
      this.refreshGuestSession();
    } catch (e) {
      hilog.error(DOMAIN, 'Compose', 'Exception creating controller: %{public}s', JSON.stringify(e));
      this.errorMessage = 'Exception creating controller: ' + JSON.stringify(e);
    }
  }

  private async loadP2Data(): Promise<void> {
    try {
      const homeJson = await requestText(`${DAILY_API}/stories/latest`);
      nativeApi.applyHomeJson(homeJson);
      const home = JSON.parse(homeJson) as DailyHomePayload;
      if (home.stories.length === 0) {
        throw new Error('真实首页没有文章');
      }
      let selected = home.stories[0];
      let selectedMinutes = this.hintMinutes(selected.hint);
      home.stories.forEach((story: DailyStoryPayload): void => {
        const minutes = this.hintMinutes(story.hint);
        if (minutes > selectedMinutes) {
          selected = story;
          selectedMinutes = minutes;
        }
      });
      const detailJson = await requestText(`${DAILY_API}/news/${selected.id}`);
      nativeApi.applyDetailJson(detailJson);
      const detail = JSON.parse(detailJson) as DailyDetailPayload;
      if (detail.image.length > 0) {
        const image: ImagePayload = { url: detail.image, data: await requestImageBase64(detail.image) };
        nativeApi.applyImageBase64(JSON.stringify(image));
      }
      const images: Set<string> = new Set<string>();
      const imagePattern = /<img[^>]+src=["']([^"']+)["']/gi;
      let match = imagePattern.exec(detail.body);
      while (match !== null && images.size < 23) {
        images.add(match[1].replace(/&amp;/g, '&'));
        match = imagePattern.exec(detail.body);
      }
      for (const url of images) {
        try {
          const image: ImagePayload = { url: url, data: await requestImageBase64(url) };
          nativeApi.applyImageBase64(JSON.stringify(image));
        } catch (_error) {
          hilog.warn(DOMAIN, 'P2', 'An article image could not be loaded');
        }
      }
    } catch (error) {
      const message = error instanceof Error ? error.message : JSON.stringify(error);
      nativeApi.applyError(`P2 数据加载失败：${message}`);
      hilog.error(DOMAIN, 'P2', 'load failed: %{public}s', message);
    }
  }

  private hintMinutes(value: string): number {
    const match = /(\d+)\s*分钟阅读/.exec(value);
    return match === null ? 0 : Number(match[1]);
  }

  private async refreshGuestSession(): Promise<void> {
    let request: http.HttpRequest | undefined = undefined;
    try {
      const context = this.getUIContext().getHostContext();
      if (context === undefined) {
        throw new Error('无法取得存储上下文');
      }
      const store = await preferences.getPreferences(context, GUEST_PREFERENCES);
      const storedValue = await store.get(GUEST_COOKIE_KEY, '');
      const storedCookies = typeof storedValue === 'string' ? guestCookieHeader(storedValue) : '';
      request = http.createHttp();
      const headers: Record<string, string> = {
        'Accept': 'application/json, text/plain, */*',
        'User-Agent': MOBILE_USER_AGENT,
        'x-api-version': '3.1.8',
        'x-app-version': '10.61.0',
        'x-app-za': MOBILE_X_APP_ZA
      };
      if (storedCookies.length > 0) {
        headers['Cookie'] = storedCookies;
      }
      const response = await request.request('https://api.zhihu.com/topstory/recommend', {
        method: http.RequestMethod.GET,
        header: headers,
        expectDataType: http.HttpDataType.STRING,
        connectTimeout: REQUEST_TIMEOUT,
        readTimeout: REQUEST_TIMEOUT,
        maxLimit: 1024 * 1024,
        maxRedirects: 0,
        usingCache: false
      });
      if (Number(response.responseCode) !== 200) {
        throw new Error(`访客会话请求失败：HTTP ${response.responseCode}`);
      }
      const responseCookies = typeof response.cookies === 'string' ? guestCookieHeader(response.cookies) : '';
      if (responseCookies.length > 0) {
        await store.put(GUEST_COOKIE_KEY, responseCookies);
        await store.flush();
        const restored = storedCookies.length > 0 ? '冷启动已恢复；' : '';
        nativeApi.applySessionStatus(`${restored}访客 Cookie 已持久化（Preferences）`);
      } else if (storedCookies.length > 0) {
        nativeApi.applySessionStatus('已复用本地访客 Cookie（Preferences）');
      } else {
        nativeApi.applySessionStatus('访客接口可用，服务端本次未下发 Cookie');
      }
    } catch (error) {
      const message = error instanceof Error ? error.message : JSON.stringify(error);
      nativeApi.applySessionStatus(`访客会话降级：${message}`);
    } finally {
      request?.destroy();
    }
  }

  build() {
    Column() {
      if (this.controller) {
        Compose({
          controller: this.controller,
          libraryName: 'entry',
          onBackPressed: () => nativeApi.handleBack() || this.controller!.onBackPress()
        })
      } else {
        Text(this.errorMessage)
      }
    }
    .width('100%')
    .height('100%')
  }

  onPageHide() {
    this.controller?.onPageHide();
  }

  onPageShow() {
    hilog.info(DOMAIN, 'P1', 'Index.onPageShow');
    this.controller?.onPageShow();
  }

  // 注意：@Entry 组件的返回键回调名是 onBackPress（不是 onBackPressed）。
  onBackPress(): boolean {
    const consumed = nativeApi.handleBack() || (this.controller ? this.controller.onBackPress() : false);
    hilog.info(DOMAIN, 'P1', 'Index.onBackPress consumed=%{public}s', String(consumed));
    return consumed;
  }
}

// NetworkKit returns a Netscape cookie jar, not a Cookie request header.
// Persist/send only known guest cookies; never import login credentials here.
function guestCookieHeader(raw: string): string {
  if (raw.length > 8192) {
    return '';
  }
  const cookies: Map<string, string> = new Map<string, string>();
  const allowed = ['_xsrf', 'BEC', 'd_c0'];
  const entries = raw.includes('\t') ? raw.split('\n') : raw.split(';');
  entries.forEach((entry: string): void => {
    let name = '';
    let value = '';
    if (raw.includes('\t')) {
      const fields = entry.trim().split('\t');
      if (fields.length !== 7) { return; }
      const expires = Number(fields[4]);
      if (expires > 0 && expires * 1000 <= Date.now()) { return; }
      name = fields[5];
      value = fields[6];
    } else {
      const separator = entry.indexOf('=');
      if (separator < 1) { return; }
      name = entry.slice(0, separator).trim();
      value = entry.slice(separator + 1).trim();
    }
    if (allowed.includes(name) && value.length > 0 && value.length <= 4096 && !/[\x00-\x20;\x7f]/.test(value)) {
      cookies.set(name, value);
    }
  });
  const pairs: Array<string> = [];
  cookies.forEach((value: string, name: string): void => { pairs.push(`${name}=${value}`); });
  return pairs.join('; ');
}

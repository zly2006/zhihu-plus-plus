(() => {
  if (window.__zhihuFeedAdFilterInstalled__) {
    return;
  }

  Object.defineProperty(window, '__zhihuFeedAdFilterInstalled__', {
    value: true,
    configurable: false,
    enumerable: false,
    writable: false,
  });

  const API_HOST = 'www.zhihu.com';
  const API_PATH = '/api/v3/feed/topstory/recommend';
  const TOAST_CONTAINER_ID = 'zhihu-ad-filter-toast-container';
  const LINK_PATTERNS = [
    'xg.zhihu.com',
    'd.zhihu.com',
    'data-edu-card-id',
    'mp.weixin.qq.com',
  ];

  function normalizeUrl(input) {
    try {
      return new URL(input, window.location.href);
    } catch (_) {
      return null;
    }
  }

  function shouldIntercept(urlLike) {
    const parsed = normalizeUrl(urlLike);
    if (!parsed) {
      return String(urlLike || '').includes('https://www.zhihu.com/api/v3/feed/topstory/recommend');
    }

    return parsed.hostname === API_HOST && parsed.pathname === API_PATH;
  }

  function hasJsonContentType(response) {
    const contentType = response.headers.get('content-type') || '';
    return contentType.includes('application/json');
  }

  function toSearchableText(value) {
    if (typeof value === 'string') {
      return value;
    }

    if (Array.isArray(value) || (value && typeof value === 'object')) {
      try {
        return JSON.stringify(value);
      } catch (_) {
        return '';
      }
    }

    return '';
  }

  function hasAnyAdLink(text) {
    if (!text) {
      return false;
    }

    const normalized = text.toLowerCase();
    return LINK_PATTERNS.some((pattern) => normalized.includes(pattern));
  }

  function hasPaidInfo(target) {
    return !!target && (target.paid_info != null || target.paidInfo != null);
  }

  function hasExplicitAdMarkers(candidate) {
    if (!candidate || typeof candidate !== 'object') {
      return false;
    }

    const markers = [
      'promotion_extra',
      'promotionExtra',
      'ad_info',
      'adInfo',
      'feed_specific',
      'feedSpecific',
      'monitor_urls',
      'monitorUrls',
    ];

    return markers.some((key) => candidate[key] != null);
  }

  function collectCandidateTexts(item, target) {
    const values = [
      target?.content,
      target?.content_html,
      target?.contentHtml,
      target?.excerpt,
      item?.content,
      item?.content_html,
      item?.contentHtml,
    ];

    return values
      .map(toSearchableText)
      .filter((text) => text.length > 0);
  }

  function extractDebugTitle(item) {
    if (!item || typeof item !== 'object') {
      return 'unknown-title';
    }

    const target = item.target && typeof item.target === 'object' ? item.target : null;
    const titleCandidates = [
      target?.title,
      target?.question?.title,
      target?.question?.name,
      target?.excerpt_title,
      target?.excerptTitle,
      item?.title,
      item?.ext_info?.title,
      item?.action_text,
      item?.actionText,
    ];

    for (const candidate of titleCandidates) {
      if (typeof candidate === 'string' && candidate.trim().length > 0) {
        return candidate.trim();
      }
    }

    return 'unknown-title';
  }

  function ensureToastContainer() {
    if (typeof document === 'undefined') {
      return null;
    }

    const existing = document.getElementById(TOAST_CONTAINER_ID);
    if (existing) {
      return existing;
    }

    const root = document.body || document.documentElement;
    if (!root) {
      return null;
    }

    const container = document.createElement('div');
    container.id = TOAST_CONTAINER_ID;
    container.style.position = 'fixed';
    container.style.top = '60px';
    container.style.right = '16px';
    container.style.zIndex = '2147483647';
    container.style.display = 'flex';
    container.style.flexDirection = 'column';
    container.style.gap = '8px';
    container.style.pointerEvents = 'none';

    root.appendChild(container);
    return container;
  }

  function showFilteredToast(title) {
    const container = ensureToastContainer();
    if (!container) {
      return;
    }

    const toast = document.createElement('div');
    toast.style.display = 'flex';
    toast.style.alignItems = 'center';
    toast.style.gap = '8px';
    toast.style.maxWidth = '360px';
    toast.style.padding = '10px 12px';
    toast.style.border = '1px solid #e5e7eb';
    toast.style.borderRadius = '10px';
    toast.style.background = '#ffffff';
    toast.style.color = '#111827';
    toast.style.boxShadow = '0 8px 24px rgba(0, 0, 0, 0.12)';
    toast.style.fontSize = '13px';
    toast.style.lineHeight = '1.3';

    const checkIcon = document.createElement('span');
    checkIcon.textContent = '✓';
    checkIcon.style.color = '#16a34a';
    checkIcon.style.fontWeight = '700';
    checkIcon.style.flex = '0 0 auto';

    const message = document.createElement('span');
    message.textContent = `已过滤：${title}`;
    message.style.wordBreak = 'break-word';

    toast.appendChild(checkIcon);
    toast.appendChild(message);
    container.appendChild(toast);

    (window.setTimeout || setTimeout)(() => {
      toast.remove();
      if (container.childElementCount === 0) {
        container.remove();
      }
    }, 5000);
  }

  function checkForAd(item) {
    if (!item || typeof item !== 'object') {
      return false;
    }

    if (String(item.type || '').toLowerCase() === 'feed_advert') {
      return true;
    }

    const target = item.target && typeof item.target === 'object' ? item.target : null;

    if (hasExplicitAdMarkers(item) || hasExplicitAdMarkers(target)) {
      return true;
    }

    if (hasPaidInfo(target)) {
      return true;
    }

    const candidateTexts = collectCandidateTexts(item, target);
    return candidateTexts.some((text) => hasAnyAdLink(text));
  }

  function filterRecommendPayload(payload) {
    if (!payload || typeof payload !== 'object' || !Array.isArray(payload.data)) {
      return null;
    }

    const originalLength = payload.data.length;
    const removedItems = [];
    const filteredData = payload.data.filter((item) => {
      const blocked = checkForAd(item);
      if (blocked) {
        removedItems.push(item);
      }
      return !blocked;
    });
    const removedCount = originalLength - filteredData.length;

    if (removedCount <= 0) {
      return null;
    }

    removedItems.forEach((item) => {
      const title = extractDebugTitle(item);
      showFilteredToast(title);
    });

    const nextPayload = {
      ...payload,
      data: filteredData,
    };

    if (payload.paging && typeof payload.paging === 'object') {
      const nextPaging = { ...payload.paging };
      if (typeof nextPaging.totals === 'number') {
        nextPaging.totals = Math.max(0, nextPaging.totals - removedCount);
      }
      nextPayload.paging = nextPaging;
    }

    return nextPayload;
  }

  const originalFetch = window.fetch.bind(window);

  window.fetch = async function patchedFetch(input, init) {
    const response = await originalFetch(input, init);
    const requestUrl = typeof input === 'string' ? input : input?.url;

    if (!shouldIntercept(requestUrl) || !hasJsonContentType(response)) {
      return response;
    }

    try {
      const payload = await response.clone().json();
      const filteredPayload = filterRecommendPayload(payload);

      if (!filteredPayload) {
        return response;
      }

      const body = JSON.stringify(filteredPayload);
      const headers = new Headers(response.headers);
      headers.set('content-type', 'application/json; charset=utf-8');
      headers.delete('content-length');

      return new Response(body, {
        status: response.status,
        statusText: response.statusText,
        headers,
      });
    } catch (_) {
      return response;
    }
  };

  window.__zhihuFeedAdFilterCore__ = {
    checkForAd,
    filterRecommendPayload,
  };
})();

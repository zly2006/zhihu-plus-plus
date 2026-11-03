(() => {
    'use strict';

    const COLLECTION_PREFIX = 'com.github.zly2006.zhplus.openinbrowser';
    const statusEl = document.getElementById('status');
    const hintEl = document.getElementById('hint');
    const loginButton = document.getElementById('login');

    function show(status, hint) {
        statusEl.textContent = status;
        hintEl.textContent = hint || '';
        loginButton.hidden = true;
    }

    function showLogin() {
        show('请先在浏览器登录知乎', '必须登录与手机相同的知乎账号，登录后点击扩展图标即可直接打开最新内容。');
        loginButton.hidden = false;
    }

    async function request(path) {
        const response = await fetch(`https://www.zhihu.com${path}`, {
            credentials: 'include',
            signal: AbortSignal.timeout(10000),
        });
        if (!response.ok) {
            const error = new Error(`HTTP ${response.status}`);
            error.status = response.status;
            throw error;
        }
        return response.json();
    }

    async function findCollection() {
        let token = null;
        try {
            const me = await request('/api/v4/me');
            token = me.url_token || me.urlToken || null;
        } catch (_) {
            // 退回使用 self 别名。
        }
        const paths = token
            ? [`/api/v4/people/${token}/collections?limit=50`]
            : ['/api/v4/people/self/collections?limit=50'];
        let lastError = null;
        for (const path of paths) {
            try {
                const payload = await request(path);
                const collections = Array.isArray(payload.data) ? payload.data : [];
                const found = collections.find((collection) =>
                    (collection.description || '').startsWith(COLLECTION_PREFIX),
                );
                if (found) return found;
            } catch (error) {
                lastError = error;
            }
        }
        if (lastError) throw lastError;
        return null;
    }

    function contentHref(content) {
        if (!content) return null;
        if (content.type === 'answer') {
            if (content.question?.id) {
                return `https://www.zhihu.com/question/${content.question.id}/answer/${content.id}`;
            }
            return `https://www.zhihu.com/answer/${content.id}`;
        }
        if (content.type === 'article') {
            return `https://zhuanlan.zhihu.com/p/${content.id}`;
        }
        if (content.type === 'pin') {
            return `https://www.zhihu.com/pin/${content.id}`;
        }
        if (content.type === 'zvideo') {
            return `https://www.zhihu.com/zvideo/${content.id}`;
        }
        if (typeof content.url === 'string' && /^https:\/\/(www|zhuanlan)\.zhihu\.com\//.test(content.url)) {
            return content.url;
        }
        return null;
    }

    function contentTime(item) {
        const value = Number(item.created);
        if (Number.isFinite(value)) return value;
        const parsed = Date.parse(item.created);
        return Number.isFinite(parsed) ? parsed : 0;
    }

    async function openLatest() {
        try {
            const collection = await findCollection();
            if (!collection) {
                show('没有找到收藏夹', '请确认浏览器登录的是与手机相同的知乎账号，并先在 Zhihu++ 中点击「在电脑打开」。');
                return;
            }
            const payload = await request(`/api/v4/collections/${collection.id}/items?offset=0&limit=20`);
            const items = Array.isArray(payload.data) ? payload.data : [];
            const newest = items
                .filter((item) => item && item.content)
                .sort((left, right) => contentTime(right) - contentTime(left))[0];
            if (!newest) {
                show('收藏夹是空的', '先在 Zhihu++ 中点击「在电脑打开」。');
                return;
            }
            const href = contentHref(newest.content);
            if (!href) {
                show('无法打开这条内容');
                return;
            }
            chrome.tabs.create({ url: href }, () => window.close());
        } catch (error) {
            if (error.status === 401 || error.status === 403) {
                showLogin();
            } else if (error.name === 'TimeoutError' || error.name === 'AbortError') {
                show('长时间没有反应', '请检查浏览器是否登录了与手机相同的知乎账号，然后重试。');
            } else {
                show('读取失败', '请检查浏览器是否登录了与手机相同的知乎账号，然后重试。');
            }
        }
    }

    loginButton.addEventListener('click', () => {
        chrome.tabs.create({ url: 'https://www.zhihu.com/signin' }, () => window.close());
    });

    openLatest();
})();

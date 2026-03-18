import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const injectedPath = path.resolve(__dirname, '../injected.js');
const injectedCode = fs.readFileSync(injectedPath, 'utf8');

class FakeElement {
  constructor(tagName) {
    this.tagName = String(tagName || 'div').toUpperCase();
    this.children = [];
    this.parentNode = null;
    this.style = {};
    this.id = '';
    this.textContent = '';
  }

  appendChild(child) {
    child.parentNode = this;
    this.children.push(child);
    return child;
  }

  removeChild(child) {
    const index = this.children.indexOf(child);
    if (index >= 0) {
      this.children.splice(index, 1);
      child.parentNode = null;
    }
    return child;
  }

  remove() {
    if (this.parentNode) {
      this.parentNode.removeChild(this);
    }
  }

  get childElementCount() {
    return this.children.length;
  }
}

function findById(node, id) {
  if (!node) {
    return null;
  }
  if (node.id === id) {
    return node;
  }
  for (const child of node.children) {
    const found = findById(child, id);
    if (found) {
      return found;
    }
  }
  return null;
}

const fakeDocument = {
  body: new FakeElement('body'),
  documentElement: new FakeElement('html'),
  createElement: (tagName) => new FakeElement(tagName),
  getElementById(id) {
    return findById(this.body, id) || findById(this.documentElement, id);
  },
};

const sandbox = {
  URL,
  Headers,
  Response,
  document: fakeDocument,
  setTimeout: () => 1,
  console: {
    log: (...args) => console.log(...args),
    warn: (...args) => console.warn(...args),
    error: (...args) => console.error(...args),
  },
  window: {
    location: { href: 'https://www.zhihu.com/' },
    setTimeout: () => 1,
    fetch: async () =>
      new Response(JSON.stringify({ data: [] }), {
        headers: { 'content-type': 'application/json' },
      }),
  },
};

sandbox.window.window = sandbox.window;
vm.createContext(sandbox);
vm.runInContext(injectedCode, sandbox, { filename: injectedPath });

const core = sandbox.window.__zhihuFeedAdFilterCore__;
assert.ok(core, 'core API should exist on window');

const { checkForAd, filterRecommendPayload } = core;

assert.equal(
  checkForAd({ type: 'feed', target: { type: 'answer', paid_info: { id: 1 }, content: 'normal' } }),
  true,
  'paid_info should be treated as ad'
);

assert.equal(
  checkForAd({ type: 'feed', target: { type: 'article', content: '<a href="https://xg.zhihu.com">ad</a>' } }),
  true,
  'xg.zhihu.com should be treated as ad link'
);

assert.equal(
  checkForAd({ type: 'feed', target: { type: 'pin', content_html: '<a href="https://mp.weixin.qq.com/s/abc">wx</a>' } }),
  true,
  'mp.weixin.qq.com should be treated as ad link'
);

assert.equal(
  checkForAd({ type: 'feed_advert', action_text: 'promoted' }),
  true,
  'feed_advert should be treated as ad'
);

assert.equal(
  checkForAd({ type: 'feed', target: { type: 'answer', content: '<p>normal content</p>' } }),
  false,
  'normal entry should not be filtered'
);

const payload = {
  data: [
    { type: 'feed', target: { type: 'answer', content: 'clean' } },
    {
      type: 'feed',
      target: {
        type: 'answer',
        paid_info: { sku: 'vip' },
        content: 'paid',
        question: { title: '付费回答标题' },
      },
    },
    {
      type: 'feed',
      target: {
        type: 'article',
        title: '学堂文章标题',
        content: '<a href="https://d.zhihu.com">school</a>',
      },
    },
  ],
  paging: {
    totals: 20,
  },
};

const filteredPayload = filterRecommendPayload(payload);
assert.ok(filteredPayload, 'payload with ads should be rewritten');
assert.equal(filteredPayload.data.length, 1, 'only one clean entry should remain');
assert.equal(filteredPayload.paging.totals, 18, 'paging.totals should be reduced by removed count');

const toastContainer = fakeDocument.getElementById('zhihu-ad-filter-toast-container');
assert.ok(toastContainer, 'toast container should be created');
assert.equal(toastContainer.children.length, 2, 'each filtered item should render one toast');

const firstToast = toastContainer.children[0];
const secondToast = toastContainer.children[1];
assert.equal(firstToast.style.background, '#ffffff', 'toast background should be white');
assert.equal(secondToast.style.background, '#ffffff', 'toast background should be white');

const firstIcon = firstToast.children[0];
const firstText = firstToast.children[1];
const secondIcon = secondToast.children[0];
const secondText = secondToast.children[1];

assert.equal(firstIcon.textContent, '✓', 'toast icon should be checkmark');
assert.equal(secondIcon.textContent, '✓', 'toast icon should be checkmark');
assert.equal(firstIcon.style.color, '#16a34a', 'toast checkmark should be green');
assert.equal(secondIcon.style.color, '#16a34a', 'toast checkmark should be green');
assert.ok(firstText.textContent.includes('付费回答标题'), 'first toast should include filtered answer title');
assert.ok(secondText.textContent.includes('学堂文章标题'), 'second toast should include filtered article title');

assert.equal(
  filterRecommendPayload({ data: [{ type: 'feed', target: { type: 'answer', content: 'clean' } }] }),
  null,
  'payload without ads should not be rewritten'
);

console.log('zhihu ad filter tests passed');

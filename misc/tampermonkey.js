// ==UserScript==
// @name         New Userscript
// @namespace    http://tampermonkey.net/
// @version      2024-09-30
// @description  try to take over the world!
// @author       You
// @run-at       document-start
// @match        https://www.zhihu.com/question/**
// @icon         https://www.google.com/s2/favicons?sz=64&domain=zhihu.com
// @grant        none
// ==/UserScript==

(function() {
    'use strict';
    var origDescriptor = Object.getOwnPropertyDescriptor(Document.prototype, 'cookie');
    Object.defineProperty(document, 'cookie', {
        get() {
            return origDescriptor.get.call(this);
        },
        set(value) {
            if (value.indexOf("__zse_ck")!=-1) {
                console.log('!!!add', value)
                debugger;
            }
            return origDescriptor.set.call(this, value);
        },
        enumerable: true,
        configurable: true
    });
    // Your code here...
})();

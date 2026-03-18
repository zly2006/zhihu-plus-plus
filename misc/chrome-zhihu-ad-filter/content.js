(() => {
  const script = document.createElement('script');
  script.src = chrome.runtime.getURL('injected.js');
  script.type = 'text/javascript';
  script.async = false;

  (document.head || document.documentElement).appendChild(script);
  script.remove();
})();

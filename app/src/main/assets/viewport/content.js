// Zoom "de verdade" para o relogio: em vez de encolher a pagina com CSS
// (o que quebra barras fixas, rolagem e toques), troca o <meta viewport>.
// A pagina passa a ser montada numa largura maior (ex.: 650 px) e o Gecko
// reduz tudo para caber na tela, exatamente como o zoom do navegador no PC.
(() => {
  const KEY = "__wear_vp";
  let want = null;

  try {
    const saved = sessionStorage.getItem(KEY);
    if (saved) want = JSON.parse(saved);
  } catch (e) {}

  function contentFor(w) {
    // Largura do GeckoView em CSS px (o app passa em pixels fisicos).
    const dpr = window.devicePixelRatio || 1;
    const deviceCss = w.viewPx / dpr;
    const width = Math.round(w.viewPx / w.zoom);
    const scale = Math.round((deviceCss / width) * 10000) / 10000;
    return "width=" + width +
      ", initial-scale=" + scale +
      ", minimum-scale=" + scale +
      ", maximum-scale=" + scale +
      ", user-scalable=no";
  }

  function enforce() {
    if (!want) return;
    const content = contentFor(want);
    const metas = document.querySelectorAll('meta[name="viewport" i]');
    if (metas.length === 0) {
      const parent = document.head || document.documentElement;
      if (!parent) return;
      const m = document.createElement("meta");
      m.setAttribute("name", "viewport");
      m.setAttribute("content", content);
      parent.appendChild(m);
      return;
    }
    for (const m of metas) {
      if (m.getAttribute("content") !== content) m.setAttribute("content", content);
    }
  }

  // Enquanto a pagina carrega, observa o documento todo (a <head> e o meta
  // da pagina ainda vao aparecer). Depois, so a <head>.
  let obs = new MutationObserver(enforce);
  obs.observe(document, { childList: true, subtree: true, attributes: true, attributeFilter: ["content", "name"] });
  document.addEventListener("DOMContentLoaded", () => {
    enforce();
    if (document.head) {
      obs.disconnect();
      obs = new MutationObserver(enforce);
      obs.observe(document.head, { childList: true, subtree: true, attributes: true, attributeFilter: ["content", "name"] });
    }
  });
  enforce();

  try {
    const port = browser.runtime.connectNative("browser");
    port.onMessage.addListener((msg) => {
      if (!msg || !msg.viewPx || !msg.zoom) return;
      want = { viewPx: msg.viewPx, zoom: msg.zoom };
      try { sessionStorage.setItem(KEY, JSON.stringify(want)); } catch (e) {}
      enforce();
    });
  } catch (e) {}
})();

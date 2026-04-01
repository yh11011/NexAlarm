/**
 * 使用 Playwright 截取各 AI 服務官網 Logo
 * 儲存至 app/src/main/res/drawable-nodpi/
 */
const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');

const OUTPUT_DIR = path.join(__dirname, '../app/src/main/res/drawable-nodpi');
fs.mkdirSync(OUTPUT_DIR, { recursive: true });

const AI_SERVICES = [
  {
    id: 'claude',
    name: 'Claude (Anthropic)',
    url: 'https://www.anthropic.com',
    logoSelector: 'header img, nav img, [class*="logo"] img, [class*="Logo"] img, svg[class*="logo"], header svg',
    fallbackSelector: 'header a img, nav a img',
  },
  {
    id: 'chatgpt',
    name: 'ChatGPT (OpenAI)',
    url: 'https://openai.com',
    logoSelector: 'header img, [class*="logo"] img, nav img',
    fallbackSelector: 'header a img',
  },
  {
    id: 'gemini',
    name: 'Gemini (Google)',
    url: 'https://gemini.google.com',
    logoSelector: '[class*="logo"] img, header img, [aria-label*="Gemini"] img',
    fallbackSelector: 'img[alt*="Gemini"], img[alt*="Google"]',
  },
  {
    id: 'copilot',
    name: 'Copilot (Microsoft)',
    url: 'https://copilot.microsoft.com',
    logoSelector: 'header img, [class*="logo"] img, [class*="Logo"] img',
    fallbackSelector: 'header a img, img[alt*="Copilot"]',
  },
  {
    id: 'grok',
    name: 'Grok (xAI)',
    url: 'https://x.ai',
    logoSelector: 'header img, [class*="logo"] img, nav img',
    fallbackSelector: 'img[alt*="xAI"], img[alt*="Grok"]',
  },
  {
    id: 'cursor',
    name: 'Cursor',
    url: 'https://www.cursor.com',
    logoSelector: 'header img, [class*="logo"] img, nav img',
    fallbackSelector: 'img[alt*="Cursor"]',
  },
  {
    id: 'perplexity',
    name: 'Perplexity',
    url: 'https://www.perplexity.ai',
    logoSelector: 'header img, [class*="logo"] img, nav img',
    fallbackSelector: 'img[alt*="Perplexity"]',
  },
  {
    id: 'deepseek',
    name: 'DeepSeek',
    url: 'https://www.deepseek.com',
    logoSelector: 'header img, [class*="logo"] img, .header img',
    fallbackSelector: 'img[alt*="DeepSeek"], img[alt*="deepseek"]',
  },
  {
    id: 'kimi',
    name: 'Kimi (Moonshot)',
    url: 'https://kimi.moonshot.cn',
    logoSelector: 'header img, [class*="logo"] img, nav img',
    fallbackSelector: 'img[alt*="Kimi"]',
  },
  {
    id: 'doubao',
    name: '豆包 (ByteDance)',
    url: 'https://www.doubao.com',
    logoSelector: 'header img, [class*="logo"] img, .header img',
    fallbackSelector: 'img[alt*="豆包"], img[alt*="Doubao"]',
  },
  {
    id: 'qwen',
    name: '通義千問 (Alibaba)',
    url: 'https://tongyi.aliyun.com',
    logoSelector: 'header img, [class*="logo"] img',
    fallbackSelector: 'img[alt*="通義"], img[alt*="Tongyi"]',
  },
  {
    id: 'wenxin',
    name: '文心一言 (Baidu)',
    url: 'https://yiyan.baidu.com',
    logoSelector: 'header img, [class*="logo"] img',
    fallbackSelector: 'img[alt*="文心"], img[alt*="ERNIE"]',
  },
  {
    id: 'chatglm',
    name: '智譜清言 (Zhipu)',
    url: 'https://chatglm.cn',
    logoSelector: 'header img, [class*="logo"] img',
    fallbackSelector: 'img[alt*="智譜"], img[alt*="ChatGLM"]',
  },
];

async function captureLogoFromFavicon(page, serviceId) {
  const outputPath = path.join(OUTPUT_DIR, `ai_${serviceId}.png`);
  // Take a screenshot of the favicon area (top-left 100x100)
  await page.screenshot({
    path: outputPath,
    clip: { x: 0, y: 0, width: 100, height: 100 },
  });
  return outputPath;
}

async function captureLogoElement(page, selectors, serviceId) {
  const outputPath = path.join(OUTPUT_DIR, `ai_${serviceId}.png`);

  for (const selector of selectors) {
    try {
      const elements = await page.$$(selector);
      for (const el of elements) {
        const box = await el.boundingBox();
        if (box && box.width >= 20 && box.width <= 300 && box.height >= 20 && box.height <= 200) {
          // Crop with some padding
          const padding = 8;
          const clip = {
            x: Math.max(0, box.x - padding),
            y: Math.max(0, box.y - padding),
            width: box.width + padding * 2,
            height: box.height + padding * 2,
          };
          await page.screenshot({ path: outputPath, clip });
          console.log(`  ✓ Logo captured via selector "${selector}" (${Math.round(box.width)}x${Math.round(box.height)})`);
          return outputPath;
        }
      }
    } catch (e) {
      // selector failed, try next
    }
  }
  return null;
}

async function main() {
  const browser = await chromium.launch({ headless: true });
  const results = [];

  for (const service of AI_SERVICES) {
    console.log(`\n[${service.id}] ${service.name}`);
    console.log(`  URL: ${service.url}`);

    const context = await browser.newContext({
      viewport: { width: 1280, height: 800 },
      userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    });
    const page = await context.newPage();

    try {
      await page.goto(service.url, { waitUntil: 'domcontentloaded', timeout: 20000 });
      await page.waitForTimeout(2000);

      const selectors = [service.logoSelector, service.fallbackSelector].filter(Boolean);
      const captured = await captureLogoElement(page, selectors, service.id);

      if (captured) {
        const size = fs.statSync(captured).size;
        console.log(`  ✓ Saved: ${path.basename(captured)} (${size} bytes)`);
        results.push({ id: service.id, status: 'ok', file: captured });
      } else {
        console.log(`  ✗ Logo element not found, taking full-page header screenshot`);
        // Fallback: screenshot top 200px of page
        const outputPath = path.join(OUTPUT_DIR, `ai_${service.id}.png`);
        await page.screenshot({ path: outputPath, clip: { x: 0, y: 0, width: 200, height: 100 } });
        results.push({ id: service.id, status: 'fallback', file: outputPath });
      }
    } catch (err) {
      console.log(`  ✗ Error: ${err.message}`);
      results.push({ id: service.id, status: 'error', error: err.message });
    } finally {
      await context.close();
    }
  }

  await browser.close();

  console.log('\n=== Summary ===');
  for (const r of results) {
    const icon = r.status === 'ok' ? '✓' : r.status === 'fallback' ? '⚠' : '✗';
    console.log(`${icon} ${r.id}: ${r.status}`);
  }

  // Write results manifest
  fs.writeFileSync(
    path.join(OUTPUT_DIR, '_logo_manifest.json'),
    JSON.stringify(results, null, 2)
  );
}

main().catch(console.error);

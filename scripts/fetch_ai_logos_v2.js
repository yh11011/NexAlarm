/**
 * 使用 Playwright 精確截取各 AI 服務官網 Logo（v2）
 * 策略：
 * 1. 載入頁面後列出所有 img 元素，找最符合 logo 的
 * 2. 找 favicon 連結並下載
 * 3. 截取整個 header 區域
 */
const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');
const https = require('https');
const http = require('http');

const OUTPUT_DIR = path.join(__dirname, '../app/src/main/res/drawable-nodpi');
fs.mkdirSync(OUTPUT_DIR, { recursive: true });

function downloadFile(url, dest) {
  return new Promise((resolve, reject) => {
    const proto = url.startsWith('https') ? https : http;
    const file = fs.createWriteStream(dest);
    proto.get(url, { headers: { 'User-Agent': 'Mozilla/5.0' } }, (res) => {
      if (res.statusCode === 301 || res.statusCode === 302) {
        file.close();
        return downloadFile(res.headers.location, dest).then(resolve).catch(reject);
      }
      res.pipe(file);
      file.on('finish', () => { file.close(); resolve(dest); });
    }).on('error', (err) => { fs.unlink(dest, () => {}); reject(err); });
  });
}

const AI_SERVICES = [
  { id: 'claude',      name: 'Claude',       url: 'https://www.anthropic.com',         faviconHint: '/images/favicon.png' },
  { id: 'chatgpt',     name: 'ChatGPT',      url: 'https://openai.com',                faviconHint: null },
  { id: 'gemini',      name: 'Gemini',       url: 'https://gemini.google.com',         faviconHint: null },
  { id: 'copilot',     name: 'Copilot',      url: 'https://copilot.microsoft.com',     faviconHint: null },
  { id: 'grok',        name: 'Grok',         url: 'https://x.ai',                      faviconHint: null },
  { id: 'cursor',      name: 'Cursor',       url: 'https://www.cursor.com',            faviconHint: null },
  { id: 'perplexity',  name: 'Perplexity',   url: 'https://www.perplexity.ai',         faviconHint: null },
  { id: 'deepseek',    name: 'DeepSeek',     url: 'https://www.deepseek.com',          faviconHint: null },
  { id: 'kimi',        name: 'Kimi',         url: 'https://kimi.moonshot.cn',          faviconHint: null },
  { id: 'doubao',      name: '豆包',          url: 'https://www.doubao.com',            faviconHint: null },
  { id: 'qwen',        name: '通義千問',      url: 'https://tongyi.aliyun.com',         faviconHint: null },
  { id: 'wenxin',      name: '文心一言',      url: 'https://yiyan.baidu.com',           faviconHint: null },
  { id: 'chatglm',     name: '智譜清言',      url: 'https://chatglm.cn',               faviconHint: null },
];

async function getFaviconUrl(page, baseUrl) {
  return await page.evaluate(() => {
    const selectors = [
      'link[rel="icon"][sizes="192x192"]',
      'link[rel="apple-touch-icon"]',
      'link[rel="icon"][type="image/png"]',
      'link[rel="shortcut icon"][type="image/png"]',
      'link[rel="icon"]',
      'link[rel="shortcut icon"]',
    ];
    for (const sel of selectors) {
      const el = document.querySelector(sel);
      if (el && el.href) return el.href;
    }
    return null;
  });
}

async function findBestLogoImg(page) {
  return await page.evaluate(() => {
    const imgs = Array.from(document.querySelectorAll('header img, nav img, [class*="logo" i] img, [id*="logo" i] img'));
    for (const img of imgs) {
      const rect = img.getBoundingClientRect();
      const src = img.src || img.getAttribute('src') || '';
      if (rect.width > 20 && rect.width < 400 && rect.height > 15 && rect.height < 200 && src) {
        return { src: img.src, rect: { x: rect.x, y: rect.y, w: rect.width, h: rect.height } };
      }
    }
    // Try SVG logos
    const svgs = Array.from(document.querySelectorAll('header svg, nav svg, [class*="logo" i] svg'));
    for (const svg of svgs) {
      const rect = svg.getBoundingClientRect();
      if (rect.width > 20 && rect.width < 400 && rect.height > 15) {
        return { src: null, rect: { x: rect.x, y: rect.y, w: rect.width, h: rect.height } };
      }
    }
    return null;
  });
}

async function main() {
  const browser = await chromium.launch({ headless: true });
  const results = [];

  for (const service of AI_SERVICES) {
    console.log(`\n[${service.id}] ${service.name} - ${service.url}`);

    const context = await browser.newContext({
      viewport: { width: 1280, height: 900 },
      userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/120 Safari/537.36',
    });
    const page = await context.newPage();

    try {
      await page.goto(service.url, { waitUntil: 'domcontentloaded', timeout: 25000 });
      await page.waitForTimeout(2500);

      const outputPath = path.join(OUTPUT_DIR, `ai_${service.id}.png`);
      let method = null;

      // 1. Try to find best logo img element
      const logoInfo = await findBestLogoImg(page);
      if (logoInfo && logoInfo.rect && logoInfo.rect.w > 20) {
        const pad = 4;
        const clip = {
          x: Math.max(0, logoInfo.rect.x - pad),
          y: Math.max(0, logoInfo.rect.y - pad),
          width: Math.min(logoInfo.rect.w + pad * 2, 1280),
          height: Math.min(logoInfo.rect.h + pad * 2, 200),
        };
        if (logoInfo.src) {
          // Download image directly if possible
          try {
            await downloadFile(logoInfo.src, outputPath);
            const size = fs.statSync(outputPath).size;
            if (size > 100) {
              console.log(`  ✓ Downloaded img src: ${logoInfo.src.substring(0, 60)}... (${size}b)`);
              method = 'img-download';
            }
          } catch (_) {}
        }
        if (!method) {
          await page.screenshot({ path: outputPath, clip });
          console.log(`  ✓ Screenshot logo element (${Math.round(logoInfo.rect.w)}×${Math.round(logoInfo.rect.h)})`);
          method = 'element-screenshot';
        }
      }

      // 2. Try favicon
      if (!method) {
        const faviconUrl = await getFaviconUrl(page, service.url);
        if (faviconUrl) {
          console.log(`  → Favicon: ${faviconUrl}`);
          try {
            await downloadFile(faviconUrl, outputPath);
            const size = fs.statSync(outputPath).size;
            if (size > 100) {
              console.log(`  ✓ Downloaded favicon (${size}b)`);
              method = 'favicon';
            }
          } catch (e) {
            console.log(`  ✗ Favicon download failed: ${e.message}`);
          }
        }
      }

      // 3. Screenshot the header area (top 120px)
      if (!method) {
        await page.screenshot({ path: outputPath, clip: { x: 0, y: 0, width: 300, height: 80 } });
        console.log(`  ⚠ Fallback: header area screenshot`);
        method = 'header-fallback';
      }

      const size = fs.existsSync(outputPath) ? fs.statSync(outputPath).size : 0;
      results.push({ id: service.id, name: service.name, status: method, file: outputPath, size });
    } catch (err) {
      console.log(`  ✗ Error: ${err.message.split('\n')[0]}`);
      results.push({ id: service.id, name: service.name, status: 'error', error: err.message.split('\n')[0] });
    } finally {
      await context.close();
    }
  }

  await browser.close();

  console.log('\n=== Results ===');
  for (const r of results) {
    const icon = r.status === 'img-download' || r.status === 'favicon' || r.status === 'element-screenshot' ? '✓' : r.status === 'error' ? '✗' : '⚠';
    console.log(`${icon} ai_${r.id}.png  ${r.status}  ${r.size ? r.size + 'b' : r.error || ''}`);
  }

  fs.writeFileSync(
    path.join(OUTPUT_DIR, '_logo_manifest.json'),
    JSON.stringify(results, null, 2)
  );
  console.log(`\nManifest saved. Files in: ${OUTPUT_DIR}`);
}

main().catch(console.error);

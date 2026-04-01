/**
 * Playwright 測試：驗證 AI 模型 Logo 正確載入
 * 測試 AI 模型選擇介面的所有官方圖示
 */
const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');

const HTML_FILE = path.resolve(__dirname, 'ai_logo_test.html');
const SCREENSHOTS_DIR = path.resolve(__dirname, 'test_screenshots');
fs.mkdirSync(SCREENSHOTS_DIR, { recursive: true });

const EXPECTED_MODELS = [
  { id: 'claude',      name: 'Claude' },
  { id: 'chatgpt',     name: 'ChatGPT' },
  { id: 'gemini',      name: 'Gemini' },
  { id: 'copilot',     name: 'Copilot' },
  { id: 'grok',        name: 'Grok' },
  { id: 'cursor',      name: 'Cursor' },
  { id: 'perplexity',  name: 'Perplexity' },
  { id: 'deepseek',    name: 'DeepSeek' },
  { id: 'kimi',        name: 'Kimi' },
  { id: 'doubao',      name: '豆包' },
  { id: 'qwen',        name: '通義千問' },
  { id: 'wenxin',      name: '文心一言' },
  { id: 'chatglm',     name: '智譜清言' },
];

async function runTests() {
  console.log('=== NexAlarm AI Logo Playwright Test ===\n');

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 500, height: 700 } });
  const page = await context.newPage();

  const passed = [];
  const failed = [];

  // ── Test 1: Page loads ────────────────────────────────────────
  console.log('Test 1: Page loads correctly');
  await page.goto(`file://${HTML_FILE}`);
  await page.waitForLoadState('domcontentloaded');
  const title = await page.title();
  if (title === 'NexAlarm AI Model Logo Test') {
    console.log('  ✓ Page title correct');
    passed.push('page-title');
  } else {
    console.log(`  ✗ Unexpected title: ${title}`);
    failed.push('page-title');
  }

  // ── Test 2: All model cards rendered ─────────────────────────
  console.log('\nTest 2: All 13 model cards rendered');
  const cardCount = await page.locator('.card').count();
  if (cardCount === EXPECTED_MODELS.length) {
    console.log(`  ✓ ${cardCount} cards found`);
    passed.push('card-count');
  } else {
    console.log(`  ✗ Expected ${EXPECTED_MODELS.length}, got ${cardCount}`);
    failed.push('card-count');
  }

  // ── Test 3: Wait for images to load ──────────────────────────
  console.log('\nTest 3: Logos load successfully');
  // Wait up to 10s for all images to load
  await page.waitForFunction(() => {
    const imgs = document.querySelectorAll('.card img');
    return imgs.length > 0 && Array.from(imgs).every(img => img.complete);
  }, { timeout: 10000 }).catch(() => {});

  let loadedCount = 0;
  let failedLogos = [];
  for (const model of EXPECTED_MODELS) {
    const img = page.locator(`.card[data-id="${model.id}"] img`);
    const exists = await img.count() > 0;
    if (!exists) {
      failedLogos.push(model.id);
      continue;
    }
    const isLoaded = await img.evaluate(el => el.complete && el.naturalWidth > 0);
    if (isLoaded) {
      loadedCount++;
      console.log(`  ✓ ${model.id} (${model.name})`);
    } else {
      failedLogos.push(model.id);
      console.log(`  ✗ ${model.id} (${model.name}) - failed to load`);
    }
  }
  if (failedLogos.length === 0) {
    passed.push('logos-loaded');
  } else {
    failed.push(`logos-failed:${failedLogos.join(',')}`);
  }

  // ── Test 4: Summary text shows all loaded ────────────────────
  console.log('\nTest 4: Summary shows correct count');
  await page.waitForTimeout(500);
  const summaryText = await page.locator('#summary').textContent();
  console.log(`  Summary: "${summaryText}"`);
  if (summaryText.includes(`${EXPECTED_MODELS.length}/${EXPECTED_MODELS.length}`) ||
      summaryText.includes('全部正常')) {
    console.log('  ✓ All logos reported as loaded');
    passed.push('summary-ok');
  } else {
    console.log('  ⚠ Summary may indicate failures');
    failed.push('summary-incomplete');
  }

  // ── Test 5: Selection interaction works ──────────────────────
  console.log('\nTest 5: Model selection interaction');
  await page.locator('.card[data-id="claude"]').click();
  const isSelected = await page.locator('.card[data-id="claude"]').evaluate(
    el => el.classList.contains('selected')
  );
  if (isSelected) {
    console.log('  ✓ Claude card selected on click');
    passed.push('selection');
  } else {
    console.log('  ✗ Card selection not working');
    failed.push('selection');
  }

  // Only one card selected at a time
  await page.locator('.card[data-id="gemini"]').click();
  const claudeSelected = await page.locator('.card[data-id="claude"]').evaluate(
    el => el.classList.contains('selected')
  );
  const geminiSelected = await page.locator('.card[data-id="gemini"]').evaluate(
    el => el.classList.contains('selected')
  );
  if (!claudeSelected && geminiSelected) {
    console.log('  ✓ Single selection enforced');
    passed.push('single-selection');
  } else {
    console.log('  ✗ Multiple selections found or wrong selection');
    failed.push('single-selection');
  }

  // ── Test 6: Screenshot ────────────────────────────────────────
  console.log('\nTest 6: Screenshot the AI model picker');
  const screenshotPath = path.join(SCREENSHOTS_DIR, 'ai_model_picker.png');
  await page.screenshot({ path: screenshotPath, fullPage: false });
  const screenshotSize = fs.statSync(screenshotPath).size;
  if (screenshotSize > 1000) {
    console.log(`  ✓ Screenshot saved: ${screenshotPath} (${screenshotSize.toLocaleString()} bytes)`);
    passed.push('screenshot');
  } else {
    console.log('  ✗ Screenshot too small or failed');
    failed.push('screenshot');
  }

  // ── Results ───────────────────────────────────────────────────
  await browser.close();

  console.log('\n═══════════════════════════════════');
  console.log('Results:');
  console.log(`  PASSED: ${passed.length} / ${passed.length + failed.length}`);
  if (failed.length > 0) {
    console.log(`  FAILED: ${failed.join(', ')}`);
  }
  console.log('═══════════════════════════════════');
  console.log(`\nScreenshot: ${screenshotPath}`);

  return { passed, failed, loadedCount };
}

runTests()
  .then(({ passed, failed }) => {
    process.exit(failed.length > 0 ? 1 : 0);
  })
  .catch(err => {
    console.error('Test runner error:', err);
    process.exit(1);
  });

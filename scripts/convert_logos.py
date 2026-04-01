"""
將 ICO/SVG 格式的 logo 轉換為 PNG，並統一輸出為 192x192 的方形圖
"""
import os
import sys
from pathlib import Path
from PIL import Image
import io

OUTPUT_DIR = Path(__file__).parent.parent / 'app/src/main/res/drawable-nodpi'
TARGET_SIZE = 192  # px

def convert_ico_to_png(src_path: Path, dst_path: Path):
    """Convert ICO file to PNG, picking largest icon"""
    img = Image.open(src_path)
    # ICO may have multiple sizes; pick largest
    if hasattr(img, 'n_frames') or hasattr(img, 'ico'):
        try:
            sizes = img.ico.sizes()
            largest = max(sizes, key=lambda s: s[0] * s[1])
            img.size = largest
            img = img.ico.getimage(largest)
        except Exception:
            pass
    img = img.convert('RGBA')
    img.save(dst_path, 'PNG')
    print(f'  ICO→PNG: {src_path.name} → {dst_path.name} ({img.size})')

def resize_to_square(path: Path, target: int = TARGET_SIZE):
    """Resize image to target×target with transparent padding to preserve aspect ratio"""
    img = Image.open(path).convert('RGBA')
    w, h = img.size
    scale = min(target / w, target / h)
    new_w, new_h = int(w * scale), int(h * scale)
    img = img.resize((new_w, new_h), Image.LANCZOS)

    canvas = Image.new('RGBA', (target, target), (0, 0, 0, 0))
    offset_x = (target - new_w) // 2
    offset_y = (target - new_h) // 2
    canvas.paste(img, (offset_x, offset_y), img)
    canvas.save(path, 'PNG')
    print(f'  Resized: {path.name} → {target}×{target}')

def main():
    ico_files = ['ai_claude.png', 'ai_copilot.png', 'ai_deepseek.png', 'ai_kimi.png', 'ai_perplexity.png']
    svg_file = 'ai_qwen.png'  # actually SVG

    # Convert ICO files
    for fname in ico_files:
        src = OUTPUT_DIR / fname
        if src.exists():
            try:
                convert_ico_to_png(src, src)
            except Exception as e:
                print(f'  ✗ Failed {fname}: {e}')

    # Convert SVG (qwen) - use cairosvg if available, else skip
    svg_path = OUTPUT_DIR / svg_file
    if svg_path.exists():
        try:
            import cairosvg
            png_data = cairosvg.svg2png(url=str(svg_path), output_width=192, output_height=192)
            with open(svg_path, 'wb') as f:
                f.write(png_data)
            print(f'  SVG→PNG: {svg_file} converted')
        except ImportError:
            print(f'  ⚠ cairosvg not available, trying rsvg-convert...')
            try:
                import subprocess
                tmp = OUTPUT_DIR / '_qwen_tmp.svg'
                svg_path.rename(tmp)
                result = subprocess.run(
                    ['rsvg-convert', '-w', '192', '-h', '192', str(tmp), '-o', str(svg_path)],
                    capture_output=True
                )
                if result.returncode != 0:
                    tmp.rename(svg_path)  # restore
                    print(f'  ✗ rsvg-convert failed, keeping SVG')
                else:
                    tmp.unlink()
                    print(f'  SVG→PNG via rsvg-convert: {svg_file}')
            except Exception as e2:
                print(f'  ✗ SVG conversion failed: {e2}')

    # Resize all to 192x192
    print('\nResizing all logos to 192×192...')
    for f in sorted(OUTPUT_DIR.glob('ai_*.png')):
        if f.name.startswith('_'):
            continue
        try:
            # Check if it's actually PNG now
            with open(f, 'rb') as fh:
                magic = fh.read(4)
            if magic[:4] == b'\x89PNG':
                resize_to_square(f, TARGET_SIZE)
            else:
                print(f'  ✗ Still not PNG: {f.name} (magic: {magic.hex()})')
        except Exception as e:
            print(f'  ✗ Resize failed {f.name}: {e}')

    print('\nDone!')
    for f in sorted(OUTPUT_DIR.glob('ai_*.png')):
        if not f.name.startswith('_'):
            size = f.stat().st_size
            print(f'  {f.name}: {size:,} bytes')

if __name__ == '__main__':
    main()

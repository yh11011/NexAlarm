import os
import sys
from PIL import Image

if len(sys.argv) < 2:
    raise SystemExit('Usage: python gen_icons.py <source-image> [project-root]')

src = sys.argv[1]
base = sys.argv[2] if len(sys.argv) >= 3 else os.path.dirname(os.path.abspath(__file__))

img = Image.open(src).convert('RGBA')

# Make square
w, h = img.size
size = max(w, h)
square = Image.new('RGBA', (size, size), (255, 255, 255, 255))
square.paste(img, ((size - w) // 2, (size - h) // 2), img)

print('Source: %dx%d' % (w, h))

# Android mipmap PNGs
mipmap_sizes = {
    'mipmap-mdpi': 48,
    'mipmap-hdpi': 72,
    'mipmap-xhdpi': 96,
    'mipmap-xxhdpi': 144,
    'mipmap-xxxhdpi': 192,
}
for folder, px in mipmap_sizes.items():
    d = os.path.join(base, 'app', 'src', 'main', 'res', folder)
    os.makedirs(d, exist_ok=True)
    icon = square.resize((px, px), Image.LANCZOS)
    rgb = Image.new('RGB', (px, px), (255, 255, 255))
    rgb.paste(icon, mask=icon.split()[3])
    rgb.save(os.path.join(d, 'ic_launcher.png'))
    rgb.save(os.path.join(d, 'ic_launcher_round.png'))
    print('  %s/ic_launcher.png (%dx%d)' % (folder, px, px))

# Adaptive icon foreground (432x432, content in safe zone 288x288)
canvas = 432
safe = 288
pad = (canvas - safe) // 2
fg_img = Image.new('RGBA', (canvas, canvas), (0, 0, 0, 0))
content = square.resize((safe, safe), Image.LANCZOS)
fg_img.paste(content, (pad, pad), content)
drawable_dir = os.path.join(base, 'app', 'src', 'main', 'res', 'drawable')
fg_img.save(os.path.join(drawable_dir, 'ic_launcher_bitmap.png'))
print('  drawable/ic_launcher_bitmap.png (%dx%d)' % (canvas, canvas))

# Website icons
website = os.path.join(base, 'website')

# logo.png (240x240)
logo = square.resize((240, 240), Image.LANCZOS)
logo_rgb = Image.new('RGB', (240, 240), (255, 255, 255))
logo_rgb.paste(logo, mask=logo.split()[3])
logo_rgb.save(os.path.join(website, 'logo.png'))
print('  website/logo.png (240x240)')

# logo-full.png (1024x1024)
full = square.resize((1024, 1024), Image.LANCZOS)
full_rgb = Image.new('RGB', (1024, 1024), (255, 255, 255))
full_rgb.paste(full, mask=full.split()[3])
full_rgb.save(os.path.join(website, 'logo-full.png'))
print('  website/logo-full.png (1024x1024)')

# favicon-32.png
fav32 = square.resize((32, 32), Image.LANCZOS)
fav32_rgb = Image.new('RGB', (32, 32), (255, 255, 255))
fav32_rgb.paste(fav32, mask=fav32.split()[3])
fav32_rgb.save(os.path.join(website, 'favicon-32.png'))
print('  website/favicon-32.png (32x32)')

# apple-touch-icon.png (180x180)
apple = square.resize((180, 180), Image.LANCZOS)
apple_rgb = Image.new('RGB', (180, 180), (255, 255, 255))
apple_rgb.paste(apple, mask=apple.split()[3])
apple_rgb.save(os.path.join(website, 'apple-touch-icon.png'))
print('  website/apple-touch-icon.png (180x180)')

# favicon.ico
fav32_rgb.save(os.path.join(website, 'favicon.ico'), format='ICO', sizes=[(16, 16), (32, 32)])
print('  website/favicon.ico')

print('Done!')

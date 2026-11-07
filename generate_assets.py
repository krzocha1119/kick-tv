#!/usr/bin/env python3
"""Generate graphical assets for Kick TV Android app."""
import os
from PIL import Image, ImageDraw, ImageFont

RES_DIR = "/home/kszunio/kick-tv/app/src/main/res"
DRAWABLE_DIR = os.path.join(RES_DIR, "drawable")
MIPMAP_DIR = os.path.join(RES_DIR, "mipmap-xhdpi")

os.makedirs(DRAWABLE_DIR, exist_ok=True)
os.makedirs(MIPMAP_DIR, exist_ok=True)

# 1. TV Banner (320x180 px - standard Android TV leanback banner)
def create_tv_banner():
    width, height = 320, 180
    im = Image.new("RGBA", (width, height), (15, 18, 20, 255))
    draw = ImageDraw.Draw(im)
    
    # Neon green accent stripe at top
    draw.rectangle([0, 0, width, 4], fill=(83, 252, 24, 255))
    
    # Draw "KICK" badge
    kx, ky = 40, 60
    draw.rounded_rectangle([kx, ky, kx + 130, ky + 60], radius=8, fill=(83, 252, 24, 255))
    
    try:
        font_large = ImageFont.truetype("/usr/share/fonts/dejavu-sans-fonts/DejaVuSans-Bold.ttf", 36)
        font_tv = ImageFont.truetype("/usr/share/fonts/dejavu-sans-fonts/DejaVuSans-Bold.ttf", 32)
        font_sub = ImageFont.truetype("/usr/share/fonts/dejavu-sans-fonts/DejaVuSans.ttf", 14)
    except Exception:
        font_large = ImageFont.load_default()
        font_tv = ImageFont.load_default()
        font_sub = ImageFont.load_default()
        
    draw.text((kx + 14, ky + 8), "KICK", fill=(0, 0, 0, 255), font=font_large)
    
    # Draw "TV" in bold white next to badge
    draw.text((kx + 145, ky + 10), "TV", fill=(255, 255, 255, 255), font=font_tv)
    
    # Subtitle
    draw.text((kx + 14, ky + 72), "LIVE STREAMING FOR ANDROID TV", fill=(160, 170, 180, 255), font=font_sub)
    
    banner_path = os.path.join(DRAWABLE_DIR, "tv_banner.png")
    im.save(banner_path, "PNG")
    print(f"Generated {banner_path}")

# 2. App Icon (512x512)
def create_app_icon():
    width, height = 512, 512
    im = Image.new("RGBA", (width, height), (15, 18, 20, 255))
    draw = ImageDraw.Draw(im)
    
    draw.rounded_rectangle([16, 16, width - 16, height - 16], radius=80, fill=(20, 24, 28, 255), outline=(83, 252, 24, 255), width=12)
    
    try:
        font_k = ImageFont.truetype("/usr/share/fonts/dejavu-sans-fonts/DejaVuSans-Bold.ttf", 160)
        font_tv = ImageFont.truetype("/usr/share/fonts/dejavu-sans-fonts/DejaVuSans-Bold.ttf", 80)
    except Exception:
        font_k = ImageFont.load_default()
        font_tv = ImageFont.load_default()
        
    draw.text((120, 100), "K", fill=(83, 252, 24, 255), font=font_k)
    draw.text((270, 220), "TV", fill=(255, 255, 255, 255), font=font_tv)
    
    icon_path = os.path.join(MIPMAP_DIR, "ic_launcher.png")
    im.save(icon_path, "PNG")
    im.save(os.path.join(DRAWABLE_DIR, "ic_launcher.png"), "PNG")
    print(f"Generated {icon_path}")

# 3. Virtual Mouse Cursor Pointer (48x48)
def create_cursor():
    width, height = 48, 48
    im = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)
    
    shadow_polygon = [
        (4, 4), (4, 38), (14, 28), (22, 44), (28, 41), (20, 25), (32, 25)
    ]
    draw.polygon(shadow_polygon, fill=(0, 0, 0, 200))
    
    pointer_polygon = [
        (2, 2), (2, 34), (11, 25), (18, 40), (23, 38), (16, 22), (27, 22)
    ]
    draw.polygon(pointer_polygon, fill=(255, 255, 255, 255), outline=(83, 252, 24, 255))
    
    cursor_path = os.path.join(DRAWABLE_DIR, "cursor_pointer.png")
    im.save(cursor_path, "PNG")
    print(f"Generated {cursor_path}")

if __name__ == "__main__":
    create_tv_banner()
    create_app_icon()
    create_cursor()

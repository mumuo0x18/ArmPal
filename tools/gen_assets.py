#!/usr/bin/env python3
"""ArmPal 提交素材草稿生成器(品牌几何构图,终稿可替换)。

产出(全部 JPG,符合 VibeLab 规格):
- assets_out/icon_1024.jpg            图标 1024×1024 ≤4MB
- assets_out/poster_2560x1440.jpg     海报 16:9 ≤5MB
- assets_out/poster_1024.jpg          海报 1:1 ≤5MB
- assets_out/detail_2560x1440.jpg     详情页图 16:9 ≥1 张
"""
from PIL import Image, ImageDraw, ImageFilter, ImageFont
import os

OUT = os.path.join(os.path.dirname(__file__), "assets_out")
os.makedirs(OUT, exist_ok=True)

BG = (20, 23, 29)
BG2 = (35, 48, 70)
GREEN = (74, 222, 128)
AMBER = (250, 204, 21)
BLUE = (96, 165, 250)
WHITE = (240, 244, 250)
GRAY = (150, 160, 180)

FONT_PATH = "/System/Library/Fonts/STHeiti Medium.ttc"


def font(size):
    return ImageFont.truetype(FONT_PATH, size)


def vgrad(w, h, top, bottom):
    img = Image.new("RGB", (w, h))
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / max(h - 1, 1)
        c = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
        d.line([(0, y), (w, y)], fill=c)
    return img


def rounded(d, box, r, fill=None, outline=None, width=1):
    d.rounded_rectangle(box, radius=r, fill=fill, outline=outline, width=width)


def arm_icon(size):
    """图标:深底 + 抬臂圆弧轨迹 + 臂形 + 目标刻度。"""
    img = vgrad(size, size, BG, BG2)
    d = ImageDraw.Draw(img)
    s = size / 1024
    # 圆弧轨迹(抬臂 0°→90°)
    d.arc([420*s, 180*s, 900*s, 660*s], start=180, end=270, fill=GREEN, width=int(26*s))
    d.arc([420*s, 180*s, 900*s, 660*s], start=150, end=180, fill=AMBER, width=int(14*s))
    # 目标刻度线
    d.line([(900*s, 360*s), (900*s, 480*s)], fill=WHITE, width=int(10*s))
    # 臂(肩点 → 前平举):肩圆 + 两段圆角矩形近似
    shoulder = (480*s, 640*s)
    d.ellipse([shoulder[0]-46*s, shoulder[1]-46*s, shoulder[0]+46*s, shoulder[1]+46*s], fill=BLUE)
    d.rounded_rectangle([500*s, 560*s, 830*s, 640*s], radius=40*s, fill=BLUE)
    d.rounded_rectangle([810*s, 580*s, 940*s, 655*s], radius=34*s, fill=(96, 165, 250))
    # 手部关键点(26 关节意象)
    for i, r in enumerate((30, 22, 16)):
        d.ellipse([905*s - r*s, 560*s - r*s, 905*s + r*s, 560*s + r*s],
                  outline=GREEN, width=int(6*s))
    t = font(int(150*s))
    d.text((120*s, 760*s), "臂伴", font=t, fill=WHITE)
    t2 = font(int(56*s))
    d.text((126*s, 930*s), "ArmPal · 空间康复陪伴", font=t2, fill=GRAY)
    img.convert("RGB").save(os.path.join(OUT, "icon_1024.jpg"), quality=92)


def windows_row(d, x, y, scale=1.0):
    """三窗示意:主窗 + 左右窗。"""
    def win(w, h, cx, color):
        rounded(d, [cx - w/2*scale, y - h/2*scale, cx + w/2*scale, y + h/2*scale],
                24*scale, fill=(28, 32, 42), outline=color, width=int(3*scale))
    win(560, 420, x - 980*scale, BLUE)
    win(760, 520, x, GREEN)
    win(560, 420, x + 980*scale, AMBER)
    # 主窗内容:大数字
    d.text((x - 60*scale, y - 120*scale), "12", font=font(int(120*scale)), fill=GREEN)
    d.text((x - 70*scale, y + 30*scale), "/ 24 次", font=font(int(44*scale)), fill=GRAY)
    d.text((x - 150*scale, y + 120*scale), "再抬高一点", font=font(int(52*scale)), fill=AMBER)


def poster_169(path, w=2560, h=1440):
    img = vgrad(w, h, BG, BG2)
    d = ImageDraw.Draw(img)
    d.text((140, 120), "臂伴 ArmPal", font=font(120), fill=WHITE)
    d.text((145, 260), "给爸妈的空间康复陪伴", font=font(72), fill=GREEN)
    d.text((145, 370), "真人比例示范 · 手势实时纠正 · 子女远程安心", font=font(48), fill=GRAY)
    windows_row(d, w/2 + 60, 900)
    for i, (txt, c) in enumerate((("三窗训练", GREEN), ("26 关节纠正", BLUE), ("打卡 streak", AMBER))):
        cx = 260 + i * 380
        rounded(d, [cx, h - 170, cx + 330, h - 80], 30, outline=c, width=3)
        d.text((cx + 40, h - 155), txt, font=font(44), fill=WHITE)
    img.convert("RGB").save(path, quality=92)


def detail():
    w, h = 2560, 1440
    img = vgrad(w, h, BG, BG2)
    d = ImageDraw.Draw(img)
    d.text((140, 100), "三窗协同:患者视角 + 家属安心", font=font(88), fill=WHITE)
    d.text((148, 220), "训练主窗 · 动作纠正侧窗 · 今日报告窗", font=font(52), fill=GRAY)
    windows_row(d, w/2, 760)
    items = [
        ("26 关节手部追踪", "抬举角/轨迹/对称性 实时判定", GREEN),
        ("训练位锚点", "到点到位,连续打卡习惯回路", BLUE),
        ("报告落在本机", "一键生成家属卡片分享", AMBER),
    ]
    for i, (t1, t2, c) in enumerate(items):
        x = 220 + i * 800
        rounded(d, [x, h - 330, x + 720, h - 100], 32, fill=(24, 28, 38), outline=c, width=3)
        d.text((x + 40, h - 300), t1, font=font(52), fill=c)
        d.text((x + 40, h - 210), t2, font=font(40), fill=WHITE)
    img.convert("RGB").save(os.path.join(OUT, "detail_2560x1440.jpg"), quality=92)


arm_icon(1024)
poster_169(os.path.join(OUT, "poster_2560x1440.jpg"))
poster_169(os.path.join(OUT, "poster_1024.jpg"), 1024, 1024)
detail()
for f in sorted(os.listdir(OUT)):
    p = os.path.join(OUT, f)
    print(f, os.path.getsize(p) // 1024, "KB")

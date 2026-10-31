import random, sys
from PIL import Image, ImageDraw, ImageFont, ImageFilter
random.seed(7)
W, H = 1500, 1300
img = Image.new("RGB", (W, H), (236, 228, 214))           # cardboard-ish background
d = ImageDraw.Draw(img)
R = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"; B = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
f, fb, fh = ImageFont.truetype(R, 40), ImageFont.truetype(B, 40), ImageFont.truetype(B, 44)
x0, y0, x1 = 120, 120, 1380
d.rectangle([x0, y0, x1, 1180], outline=(20, 20, 20), width=4)
d.text((x0 + 20, y0 + 18), "Nährwertangaben / Nutrition information", font=fh, fill=(15, 15, 15))
cols = [x0 + 20, 820, 1080]
d.text((cols[1], y0 + 100), "pro 100 g", font=fb, fill=(15, 15, 15)); d.text((cols[2], y0 + 100), "pro 40 g", font=fb, fill=(15, 15, 15))
rows = [("Brennwert", "1590 kJ", "636 kJ"), ("", "376 kcal", "150 kcal"), ("Fett", "6,9 g", "2,8 g"),
        ("davon gesättigte Fettsäuren", "1,2 g", "0,5 g"), ("Kohlenhydrate", "60 g", "24 g"),
        ("davon Zucker", "1,1 g", "0,4 g"), ("Ballaststoffe", "9,7 g", "3,9 g"), ("Eiweiß", "13 g", "5,2 g"), ("Salz", "0,02 g", "<0,01 g")]
y = y0 + 170
for lab, a, b in rows:
    d.line([x0, y - 12, x1, y - 12], fill=(60, 60, 60), width=2)
    d.text((cols[0], y), lab, font=f, fill=(15, 15, 15)); d.text((cols[1], y), a, font=f, fill=(15, 15, 15)); d.text((cols[2], y), b, font=f, fill=(15, 15, 15))
    y += 95
img = img.rotate(2.5, resample=Image.BICUBIC, expand=True, fillcolor=(200, 190, 175))
img = img.filter(ImageFilter.GaussianBlur(1.1))
px = img.load()
for _ in range(60000):
    x, yy = random.randrange(img.width), random.randrange(img.height); c = random.randint(-40, 40)
    r, g, bb = px[x, yy]; px[x, yy] = (max(0, min(255, r + c)), max(0, min(255, g + c)), max(0, min(255, bb + c)))
img.save(sys.argv[1], quality=82)

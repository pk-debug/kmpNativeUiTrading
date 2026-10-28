"""Draw our original Northstar mark; no competitor branding or artwork is used."""
from pathlib import Path
from PIL import Image, ImageDraw
import json

root = Path(__file__).resolve().parents[1]

def icon(size, shape=None):
    scale = 4
    image = Image.new('RGB', (size*scale, size*scale), '#152B2A')
    d = ImageDraw.Draw(image)
    def points(values): return [(int(x*size*scale), int(y*size*scale)) for x,y in values]
    d.polygon(points([(0.5,0.16),(0.59,0.41),(0.84,0.5),(0.59,0.59),(0.5,0.84),(0.41,0.59),(0.16,0.5),(0.41,0.41)]), fill='#71E2B3')
    d.ellipse((int(.46*size*scale),int(.46*size*scale),int(.54*size*scale),int(.54*size*scale)),fill='#FFE0A3')
    if shape:
        image = image.convert('RGBA')
        mask = Image.new('L', image.size, 0)
        outline = ImageDraw.Draw(mask)
        bounds = tuple(int(v*size*scale) for v in (.05,.05,.95,.95))
        if shape == 'round': outline.ellipse(bounds, fill=255)
        else: outline.rounded_rectangle(bounds, radius=int(.2*size*scale), fill=255)
        image.putalpha(mask)
    return image.resize((size,size),Image.Resampling.LANCZOS)

for density,size in [('mdpi',48),('hdpi',72),('xhdpi',96),('xxhdpi',144),('xxxhdpi',192)]:
    for name in ['ic_launcher.png','ic_launcher_round.png']:
        icon(size, "round" if "round" in name else "square").save(root/f'androidApp/src/main/res/mipmap-{density}/{name}')
path=root/'iosApp/iosApp/Assets.xcassets/AppIcon.appiconset'
icon(1024).save(path/'AppIcon.png')
(path/'Contents.json').write_text(json.dumps({'images':[{'filename':'AppIcon.png','idiom':'universal','platform':'ios','size':'1024x1024'}],'info':{'author':'xcode','version':1}},indent=2)+'\n')

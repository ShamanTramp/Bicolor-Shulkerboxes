#!/usr/bin/env python3
"""Derive the dye mask from the original 1.21.11 client texture (requires Pillow)."""
import argparse
from io import BytesIO
from pathlib import Path
from zipfile import ZipFile
from PIL import Image

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('minecraft_client_jar', type=Path)
args = parser.parse_args()
with ZipFile(args.minecraft_client_jar) as archive:
    original = Image.open(BytesIO(archive.read('assets/minecraft/textures/item/shulker_shell.png'))).convert('RGBA')
mask = original.copy()
mask.putdata([(max(r, g, b),) * 3 + (a,) for r, g, b, a in original.getdata()])
assert mask.getchannel('A').tobytes() == original.getchannel('A').tobytes()
target = Path(__file__).resolve().parents[1] / 'src/client/resources/assets/bicolor_shulkerboxes/textures/item/shulker_shell.png'
mask.save(target)
print('Preserved vanilla pixels, alpha and shading:', target)

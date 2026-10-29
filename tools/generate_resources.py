#!/usr/bin/env python3
"""Regenerate deterministic recipe and client model JSON. No Minecraft install required."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
COLORS = {
    'white': (0xf9fffe, 'blanc'), 'orange': (0xf9801d, 'orange'), 'magenta': (0xc74ebd, 'magenta'),
    'light_blue': (0x3ab3da, 'bleu clair'), 'yellow': (0xfed83d, 'jaune'), 'lime': (0x80c71f, 'vert clair'),
    'pink': (0xf38baa, 'rose'), 'gray': (0x474f52, 'gris'), 'light_gray': (0x9d9d97, 'gris clair'),
    'cyan': (0x169c9c, 'cyan'), 'purple': (0x8932b8, 'violet'), 'blue': (0x3c44aa, 'bleu'),
    'brown': (0x835432, 'marron'), 'green': (0x5e7c16, 'vert'), 'red': (0xb02e26, 'rouge'), 'black': (0x1d1d21, 'noir')
}
ID = 'bicolor_shulkerboxes'

def write(relative, data):
    target = ROOT / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n')

for color, (rgb, french) in COLORS.items():
    name = {'translate': ID + '.shell', 'fallback': 'Shulker shell / Carapace: %s', 'with': [
        {'translate': ID + '.color.' + color, 'fallback': color.replace('_', ' ') + ' / ' + french}]}
    write(f'src/main/resources/data/{ID}/recipe/{color}_shell.json', {
        'type': 'minecraft:crafting_shapeless', 'group': ID + ':shell_dye', 'category': 'misc',
        'ingredients': ['minecraft:shulker_shell', f'minecraft:{color}_dye'],
        'result': {'id': 'minecraft:shulker_shell', 'components': {
            'minecraft:custom_data': {ID: {'version': 1, 'kind': 'shell', 'color': color}}, 'minecraft:item_name': name}}})
    write(f'src/main/resources/data/minecraft/recipe/{color}_shulker_box.json', {
        'type': 'minecraft:crafting_transmute', 'category': 'misc', 'group': ID + ':box_recolor',
        'input': '#minecraft:shulker_boxes', 'material': f'minecraft:{color}_dye',
        'result': {'id': f'minecraft:{color}_shulker_box'}})
    write(f'src/client/resources/assets/{ID}/items/shell_{color}.json', {
        'model': {'type': 'minecraft:model', 'model': ID + ':item/shulker_shell',
                  'tints': [{'type': 'minecraft:constant', 'value': rgb}]}})
write(f'src/client/resources/assets/{ID}/models/item/shulker_shell.json', {
    'parent': 'minecraft:item/generated', 'textures': {'layer0': ID + ':item/shulker_shell'}})
write('src/main/resources/data/minecraft/recipe/shulker_box.json', {
    'type': 'minecraft:crafting_shaped', 'category': 'misc', 'group': ID + ':box_assembly',
    'pattern': ['S', 'C', 'S'], 'key': {'S': 'minecraft:shulker_shell', 'C': 'minecraft:chest'},
    'result': {'id': 'minecraft:shulker_box'}})
write(f'src/main/resources/data/{ID}/advancement/recipes/shells.json', {
    'criteria': {'has_shell': {'trigger': 'minecraft:inventory_changed',
                             'conditions': {'items': [{'items': 'minecraft:shulker_shell'}]}}},
    'rewards': {'recipes': [f'{ID}:{c}_shell' for c in COLORS]}})
for lang in ('en_us', 'fr_fr'):
    fr = lang == 'fr_fr'
    translations = {ID + '.shell': 'Carapace de Shulker : %s' if fr else 'Shulker Shell: %s',
                    ID + '.colors': 'Couvercle : %s | Base : %s' if fr else 'Lid: %s | Base: %s',
                    ID + '.color.natural': 'naturelle' if fr else 'natural'}
    translations.update({ID + '.color.' + c: french if fr else c.replace('_', ' ') for c, (_, french) in COLORS.items()})
    write(f'src/client/resources/assets/{ID}/lang/{lang}.json', translations)

#!/usr/bin/env python3
"""Verify vanilla client compatibility and persistent legacy colors on a real production server."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import queue
import shutil
import subprocess
import threading
import time
from project_config import ROOT, MINECRAFT, LOADER, FABRIC_API, JAR_NAME
from vanilla_client import download, prepare


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jar', type=Path, default=ROOT / 'build/libs' / JAR_NAME)
    parser.add_argument('--workdir', type=Path, default=ROOT / 'build' / f'vanilla-smoke-{MINECRAFT}')
    parser.add_argument('--java', default=str(Path(os.environ['JAVA_HOME']) / 'bin/java') if 'JAVA_HOME' in os.environ else 'java')
    parser.add_argument('--port', type=int, default=25576)
    args = parser.parse_args()
    prepare()
    work = args.workdir.resolve(); work.mkdir(parents=True, exist_ok=True)
    download(f'https://meta.fabricmc.net/v2/versions/loader/{MINECRAFT}/{LOADER}/1.1.2/server/jar', work / 'fabric-server-launch.jar')
    download(f'https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/{FABRIC_API}/fabric-api-{FABRIC_API}.jar', work / 'mods/fabric-api.jar')
    shutil.copyfile(args.jar, work / 'mods/bicolor-shulkerboxes.jar')
    metadata = json.loads((ROOT / 'build/vanilla-runtime' / MINECRAFT / 'version.json').read_text())
    artifact = metadata['downloads']['server']
    download(artifact['url'], work / 'server.jar', artifact['sha1'])
    (work / 'eula.txt').write_text('eula=true\n')
    props = {
        'server-ip': '127.0.0.1', 'server-port': str(args.port), 'online-mode': 'false',
        'white-list': 'false', 'level-name': 'smoke-world', 'level-type': 'minecraft:flat', 'spawn-protection': '0',
        'view-distance': '2', 'simulation-distance': '2', 'max-players': '4', 'gamemode': 'creative',
        'generator-settings': json.dumps({'layers': [{'block': 'minecraft:bedrock', 'height': 1}, {'block': 'minecraft:dirt', 'height': 2}, {'block': 'minecraft:grass_block', 'height': 1}], 'biome': 'minecraft:plains'}, separators=(',', ':'))
    }
    (work / 'server.properties').write_text(''.join(f'{key}={value}\n' for key, value in props.items()))
    phases = []
    for phase in ('create', 'restart'):
        if phase == 'create':
            shutil.rmtree(work / 'smoke-world', ignore_errors=True)
        with (work / f'{phase}-server.log').open('w') as log, (work / f'{phase}-vanilla.log').open('w') as client_log:
            server = subprocess.Popen([args.java, '-Xmx1G', '-jar', 'fabric-server-launch.jar', 'nogui'], cwd=work, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1)
            lines = queue.Queue()
            def consume():
                for line in server.stdout:
                    log.write(line); log.flush(); lines.put(line)
            reader = threading.Thread(target=consume, daemon=True); reader.start()
            client = None
            def send(command):
                server.stdin.write(command + '\n'); server.stdin.flush()
            def await_line(marker, timeout=120):
                deadline = time.monotonic() + timeout
                while time.monotonic() < deadline:
                    if server.poll() is not None: raise RuntimeError(f'Server exited: {log.name}')
                    try: line = lines.get(timeout=1)
                    except queue.Empty: continue
                    if marker in line: return line
                raise TimeoutError(f'Missing {marker!r}: see {log.name}')
            try:
                await_line('Done (', 180)
                send('forceload add 0 0')
                if phase == 'create':
                    send('setblock 2 -60 0 minecraft:blue_shulker_box')
                    # Exact version-1 envelope used by the original 1.21.11 mod.
                    send('data merge block 2 -60 0 {bicolor_shulkerboxes:{bicolor_shulkerboxes:{version:1,kind:"box",lid:"red",base:"blue"}},Items:[{Slot:0b,id:"minecraft:diamond",count:37}]}')
                send('execute if data block 2 -60 0 bicolor_shulkerboxes.bicolor_shulkerboxes{version:1,lid:"red",base:"blue"} run say BICOLOR_COLORS_OK')
                await_line('BICOLOR_COLORS_OK')
                send('execute if data block 2 -60 0 {Items:[{Slot:0b,id:"minecraft:diamond",count:37}]} run say BICOLOR_CONTENTS_OK')
                await_line('BICOLOR_CONTENTS_OK')
                client = subprocess.Popen(['python3', str(ROOT / 'tools/vanilla_client.py'), '--port', str(args.port), '--username', 'VanillaProbe'], stdout=client_log, stderr=subprocess.STDOUT)
                await_line('VanillaProbe joined the game', 180)
                send('tp VanillaProbe 2 -60 5 180 12')
                send('give VanillaProbe minecraft:blue_shulker_box[minecraft:custom_data={bicolor_shulkerboxes:{version:1,kind:"box",lid:"red",base:"blue"}}]')
                send('give VanillaProbe minecraft:shulker_shell[minecraft:custom_data={bicolor_shulkerboxes:{version:1,kind:"shell",color:"red"}}]')
                end = time.monotonic() + 10
                while time.monotonic() < end:
                    if client.poll() is not None: raise RuntimeError(f'Vanilla client exited: {client_log.name}')
                    try: line = lines.get(timeout=.5)
                    except queue.Empty: continue
                    if 'VanillaProbe lost connection' in line: raise RuntimeError(line)
                phases.append({'phase': phase, 'passed': True})
                print(f'PASS {phase}: official vanilla client connected; version-1 colors and 37 diamonds retained', flush=True)
            finally:
                if client is not None and client.poll() is None:
                    client.terminate()
                    try: client.wait(timeout=15)
                    except subprocess.TimeoutExpired: client.kill(); client.wait(timeout=5)
                if server.poll() is None:
                    send('stop')
                    try: server.wait(timeout=45)
                    except subprocess.TimeoutExpired: server.terminate(); server.wait(timeout=15)
                reader.join(timeout=5)
    report = {'minecraft': MINECRAFT, 'jar_sha256': hashlib.sha256(args.jar.read_bytes()).hexdigest(), 'client': 'Official Mojang Minecraft client; no Fabric, no mods', 'phases': phases}
    (work / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
    print('Production vanilla compatibility and restart smoke: PASS', flush=True)


if __name__ == '__main__':
    main()

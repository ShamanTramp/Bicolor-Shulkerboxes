#!/usr/bin/env python3
"""Launch an isolated production server twice and exercise it through a mod-free protocol client."""
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
import urllib.request
import uuid

ROOT = Path(__file__).resolve().parents[1]


def download(url, path, sha1=None):
    path.parent.mkdir(parents=True, exist_ok=True)
    if not path.exists():
        with urllib.request.urlopen(url, timeout=60) as response:
            path.write_bytes(response.read())
    if sha1 and hashlib.sha1(path.read_bytes()).hexdigest() != sha1:
        raise RuntimeError(f'Checksum mismatch: {path}')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jar', type=Path, default=ROOT / 'build/libs/bicolor-shulkerboxes-0.1.0.jar')
    parser.add_argument('--workdir', type=Path, default=ROOT / 'build/vanilla-smoke')
    parser.add_argument('--java', default=str(Path(os.environ['JAVA_HOME']) / 'bin/java') if 'JAVA_HOME' in os.environ else 'java')
    parser.add_argument('--phase', choices=['all', 'create', 'restart'], default='all')
    parser.add_argument('--port', type=int, default=25576)
    args = parser.parse_args()
    work = args.workdir.resolve(); work.mkdir(parents=True, exist_ok=True)
    download('https://meta.fabricmc.net/v2/versions/loader/1.21.11/0.18.4/1.1.2/server/jar', work / 'fabric-server-launch.jar')
    download('https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.141.6+1.21.11/fabric-api-0.141.6+1.21.11.jar', work / 'mods/fabric-api.jar')
    shutil.copyfile(args.jar, work / 'mods/bicolor-shulkerboxes.jar')
    # Offline mode is restricted to loopback and exists only inside this disposable test server.
    (work / 'eula.txt').write_text('eula=true\n')
    props = {
        'server-ip': '127.0.0.1', 'server-port': str(args.port), 'online-mode': 'false',
        'level-name': 'smoke-world', 'level-type': 'minecraft:flat', 'spawn-protection': '0',
        'view-distance': '2', 'simulation-distance': '2', 'max-players': '4', 'gamemode': 'survival',
        'generator-settings': json.dumps({'layers': [{'block': 'minecraft:bedrock', 'height': 1}, {'block': 'minecraft:dirt', 'height': 2}, {'block': 'minecraft:grass_block', 'height': 1}], 'biome': 'minecraft:plains'}, separators=(',', ':'))
    }
    (work / 'server.properties').write_text(''.join(f'{key}={value}\n' for key, value in props.items()))
    identifier = uuid.UUID(bytes=hashlib.md5(b'OfflinePlayer:VanillaProbe').digest(), version=3)
    (work / 'ops.json').write_text(json.dumps([{'uuid': str(identifier), 'name': 'VanillaProbe', 'level': 4, 'bypassesPlayerLimit': True}]))
    # Resolve the exact vanilla server through Mojang's release manifest.
    if not (work / 'server.jar').exists():
        with urllib.request.urlopen('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json') as response:
            manifest = json.load(response)
        url = next(v['url'] for v in manifest['versions'] if v['id'] == '1.21.11')
        with urllib.request.urlopen(url) as response:
            artifact = json.load(response)['downloads']['server']
        download(artifact['url'], work / 'server.jar', artifact['sha1'])
    phases = []
    for phase in (('create', 'restart') if args.phase == 'all' else (args.phase,)):
        if phase == 'create':
            shutil.rmtree(work / 'smoke-world', ignore_errors=True)
        with (work / f'{phase}-server.log').open('w') as log:
            process = subprocess.Popen([args.java, '-Xmx1G', '-jar', 'fabric-server-launch.jar', 'nogui'], cwd=work, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1)
            lines = queue.Queue()
            def consume():
                for line in process.stdout:
                    log.write(line); log.flush(); lines.put(line)
            thread = threading.Thread(target=consume, daemon=True); thread.start()
            try:
                deadline = time.monotonic() + 180
                while True:
                    if process.poll() is not None: raise RuntimeError(f'Server exited: see {log.name}')
                    if time.monotonic() > deadline: raise TimeoutError(f'Server startup timed out: {log.name}')
                    try: line = lines.get(timeout=1)
                    except queue.Empty: continue
                    if 'Done (' in line: break
                env = dict(os.environ, BICOLOR_TEST_PORT=str(args.port))
                result = subprocess.run(['node', str(ROOT / 'tests/vanilla/probe.cjs'), phase], env=env, capture_output=True, text=True, timeout=150)
                print(result.stdout, end='', flush=True)
                (work / f'{phase}-probe.log').write_text(result.stdout + result.stderr)
                if result.returncode: raise RuntimeError(result.stderr)
                phases.append({'phase': phase, 'passed': True})
            finally:
                if process.poll() is None:
                    process.stdin.write('stop\n'); process.stdin.flush()
                    try: process.wait(timeout=45)
                    except subprocess.TimeoutExpired: process.terminate(); process.wait(timeout=15)
                thread.join(timeout=5)
    report = {'jar_sha256': hashlib.sha256(args.jar.read_bytes()).hexdigest(), 'client': 'Mineflayer 4.37.1; vanilla protocol 1.21.11; no Fabric or mod', 'phases': phases}
    (work / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
    print('Production vanilla protocol smoke: PASS', flush=True)


if __name__ == '__main__':
    main()

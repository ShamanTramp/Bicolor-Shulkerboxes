#!/usr/bin/env python3
"""Launch Mojang's official client without Fabric or any mod, in an isolated test directory."""
import argparse
from concurrent.futures import ThreadPoolExecutor
import hashlib
import json
import os
from pathlib import Path
import platform
import re
import shutil
import urllib.request
from project_config import ROOT, MINECRAFT

HEADERS = {'User-Agent': 'ShamanTramp/Bicolor-Shulkerboxes verification (https://github.com/ShamanTramp/Bicolor-Shulkerboxes)'}


def fetch(url):
    with urllib.request.urlopen(urllib.request.Request(url, headers=HEADERS), timeout=60) as response:
        return response.read()


def download(url, path, sha1=None):
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.exists() and (sha1 is None or hashlib.sha1(path.read_bytes()).hexdigest() == sha1):
        return
    data = fetch(url)
    if sha1 and hashlib.sha1(data).hexdigest() != sha1:
        raise RuntimeError(f'Checksum mismatch: {path}')
    path.write_bytes(data)


def allowed(rules):
    if not rules:
        return True
    result = False
    for rule in rules:
        os_rule = rule.get('os', {})
        name = {'Linux': 'linux', 'Darwin': 'osx', 'Windows': 'windows'}.get(platform.system())
        arch = platform.machine()
        matches = (not os_rule.get('name') or os_rule['name'] == name)
        matches &= (not os_rule.get('arch') or re.fullmatch(os_rule['arch'], arch) is not None)
        if matches:
            result = rule['action'] == 'allow'
    return result


def prepare():
    runtime = ROOT / 'build/vanilla-runtime' / MINECRAFT
    runtime.mkdir(parents=True, exist_ok=True)
    version_file = runtime / 'version.json'
    if not version_file.exists():
        manifest = json.loads(fetch('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'))
        url = next(v['url'] for v in manifest['versions'] if v['id'] == MINECRAFT and v['type'] == 'release')
        version_file.write_bytes(fetch(url))
    version = json.loads(version_file.read_text())
    gradle_home = Path(os.environ.get('GRADLE_USER_HOME', Path.home() / '.gradle'))
    client = runtime / 'client.jar'
    artifact = version['downloads']['client']
    loom_client = gradle_home / 'caches/fabric-loom' / MINECRAFT / 'minecraft-client.jar'
    if not client.exists() and loom_client.exists() and hashlib.sha1(loom_client.read_bytes()).hexdigest() == artifact['sha1']:
        shutil.copyfile(loom_client, client)
    download(artifact['url'], client, artifact['sha1'])
    classpath = [client]
    libraries = [lib for lib in version['libraries'] if allowed(lib.get('rules', [])) and 'artifact' in lib.get('downloads', {})]

    def get_library(lib):
        artifact = lib['downloads']['artifact']
        target = runtime / 'libraries' / artifact['path']
        coords = lib['name'].split(':')
        cache = gradle_home / 'caches/modules-2/files-2.1' / coords[0] / coords[1] / coords[2]
        if not target.exists():
            for cached in cache.glob('*/' + target.name):
                if hashlib.sha1(cached.read_bytes()).hexdigest() == artifact['sha1']:
                    target.parent.mkdir(parents=True, exist_ok=True); shutil.copyfile(cached, target); break
        download(artifact['url'], target, artifact['sha1'])
        return target

    with ThreadPoolExecutor(max_workers=6) as pool:
        classpath.extend(pool.map(get_library, libraries))
    assets = gradle_home / 'caches/fabric-loom/assets'
    assets.mkdir(parents=True, exist_ok=True)
    asset_index = version['assetIndex']
    index_file = assets / 'indexes' / (asset_index['id'] + '.json')
    download(asset_index['url'], index_file, asset_index['sha1'])
    entries = json.loads(index_file.read_text())['objects'].values()

    def get_asset(entry):
        digest = entry['hash']
        download(f'https://resources.download.minecraft.net/{digest[:2]}/{digest}', assets / 'objects' / digest[:2] / digest, digest)
    with ThreadPoolExecutor(max_workers=6) as pool:
        list(pool.map(get_asset, entries))
    result = {'version': MINECRAFT, 'classpath': [str(p) for p in classpath], 'assets': str(assets), 'asset_index': asset_index['id']}
    (runtime / 'runtime.json').write_text(json.dumps(result, indent=2) + '\n')
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--prepare-only', action='store_true')
    parser.add_argument('--port', type=int, default=25578)
    parser.add_argument('--username', default='VanillaGuest')
    args = parser.parse_args()
    runtime = prepare()
    if args.prepare_only:
        print('Official vanilla runtime prepared for Minecraft ' + MINECRAFT, flush=True)
        return
    game = ROOT / 'build/run/officialVanillaClient' / args.username
    game.mkdir(parents=True, exist_ok=True)
    # These settings apply only to the disposable test client, never the user's installation.
    (game / 'options.txt').write_text('onboardAccessibility:true\nskipMultiplayerWarning:true\npauseOnLostFocus:false\nrenderDistance:2\nsimulationDistance:2\nmaxFps:30\nsoundCategory_master:0.0\nguiScale:2\n')
    natives = game / 'natives'; natives.mkdir(exist_ok=True)
    java = str(Path(os.environ['JAVA_HOME']) / 'bin/java') if 'JAVA_HOME' in os.environ else 'java'
    command = [java, '-Xmx1G', '--enable-native-access=ALL-UNNAMED', '--add-exports', 'java.base/jdk.internal.misc=ALL-UNNAMED',
               '-Djava.library.path=' + str(natives / 'java'), '-Djna.tmpdir=' + str(natives / 'jna'),
               '-Dorg.lwjgl.system.SharedLibraryExtractPath=' + str(natives / 'lwjgl'), '-Dio.netty.native.workdir=' + str(natives / 'netty'),
               '-cp', os.pathsep.join(runtime['classpath']), 'net.minecraft.client.main.Main',
               '--version', MINECRAFT, '--gameDir', str(game), '--assetsDir', runtime['assets'], '--assetIndex', runtime['asset_index'],
               '--username', args.username, '--accessToken', '0', '--offlineDeveloperMode', '--graphicsBackend', 'opengl',
               '--quickPlayMultiplayer', '127.0.0.1:' + str(args.port), '--width', '854', '--height', '480']
    env = dict(os.environ, SDL_VIDEODRIVER='x11')
    print('Launching the official Minecraft client without Fabric or mods.', flush=True)
    # Replace this process so the harness can terminate the actual client without leaving an orphan.
    os.execvpe(java, command, env)


if __name__ == '__main__':
    main()

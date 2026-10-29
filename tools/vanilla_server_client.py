#!/usr/bin/env python3
"""Run the universal mod client against an actual server.jar without Fabric or mods."""
import os
from pathlib import Path
import queue
import shutil
import subprocess
import threading
import time

ROOT = Path(__file__).resolve().parents[1]
work = ROOT / 'build/vanilla-server-client'
work.mkdir(parents=True, exist_ok=True)
server_jar = ROOT / 'build/vanilla-smoke/server.jar'
if not server_jar.exists():
    raise SystemExit('Run tools/vanilla_smoke.py first to obtain the official server.jar')
shutil.copyfile(server_jar, work / 'server.jar')
shutil.rmtree(work / 'world', ignore_errors=True)
(work / 'eula.txt').write_text('eula=true\n')
props = (ROOT / 'build/vanilla-smoke/server.properties').read_text()
props = props.replace('server-port=25576', 'server-port=25577').replace('level-name=smoke-world', 'level-name=world')
props = '\n'.join(line for line in props.splitlines() if not line.startswith(('initial-enabled-packs=', 'initial-disabled-packs='))) + '\ninitial-enabled-packs=vanilla\n'
(work / 'server.properties').write_text(props)
java = str(Path(os.environ['JAVA_HOME']) / 'bin/java') if 'JAVA_HOME' in os.environ else 'java'
with (work / 'server.log').open('w') as log:
    server = subprocess.Popen([java, '-Xmx1G', '-jar', 'server.jar', 'nogui'], cwd=work, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1)
    events = queue.Queue()
    def consume():
        for line in server.stdout:
            log.write(line); log.flush(); events.put(line)
            if 'logged in with entity id' in line:
                server.stdin.write('tp @a 2 -60 5 180 12\n'); server.stdin.flush()
    thread = threading.Thread(target=consume, daemon=True); thread.start()
    try:
        deadline = time.monotonic() + 180
        while True:
            if server.poll() is not None: raise RuntimeError('Vanilla server exited')
            if time.monotonic() > deadline: raise TimeoutError('Vanilla server startup timed out')
            try: line = events.get(timeout=1)
            except queue.Empty: continue
            if 'Done (' in line: break
        server.stdin.write('forceload add 0 0\nsetworldspawn 2 -60 5\nsetblock 2 -60 0 minecraft:red_shulker_box\ntime set 6000\n'); server.stdin.flush()
        subprocess.run([str(ROOT / 'gradlew'), 'runProductionVanillaClientGameTest', '--no-daemon'], cwd=ROOT, check=True, timeout=360)
        print('PASS universal mod client connects to a real vanilla server without Fabric or mods')
    finally:
        if server.poll() is None:
            server.stdin.write('stop\n'); server.stdin.flush()
            try: server.wait(timeout=45)
            except subprocess.TimeoutExpired: server.terminate(); server.wait(timeout=15)
        thread.join(timeout=5)

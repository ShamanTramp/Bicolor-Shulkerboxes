"""Shared version settings for the isolated Minecraft verification scripts."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SETTINGS = dict(line.split('=', 1) for line in (ROOT / 'gradle.properties').read_text().splitlines() if line and not line.startswith('#') and '=' in line)
MINECRAFT = SETTINGS['minecraft_version']
LOADER = SETTINGS['loader_version']
FABRIC_API = SETTINGS['fabric_version']
MOD_VERSION = SETTINGS['mod_version']
JAR_NAME = f"{SETTINGS['archives_base_name']}-{MOD_VERSION}.jar"

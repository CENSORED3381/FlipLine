#!/usr/bin/env python3
"""Verificações estáticas portáteis para o pacote Flipline Fase 2."""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
KOTLIN = ROOT / "app/src/main/java/com/huzeifa/flipline"
STRINGS = ROOT / "app/src/main/res/values/strings.xml"
MANIFEST = ROOT / "app/src/main/AndroidManifest.xml"
BUILD = ROOT / "app/build.gradle.kts"

errors = []
def check(condition: bool, message: str) -> None:
    (print("PASS:", message) if condition else errors.append(message))

# XML válido e nomes de strings únicos.
try:
    root = ET.parse(STRINGS).getroot()
    names = [item.attrib.get("name") for item in root.findall("string")]
    check(len(names) == len(set(names)), "strings.xml não tem nomes duplicados")
    strings = set(names)
except Exception as exc:
    strings = set()
    check(False, f"strings.xml é XML válido ({exc})")

try:
    ET.parse(MANIFEST)
    check(True, "AndroidManifest.xml é XML válido")
except Exception as exc:
    check(False, f"AndroidManifest.xml é XML válido ({exc})")

# Recursos usados em stringResource(...) devem existir.
used = set()
for path in (ROOT / "app/src/main/java").rglob("*.kt"):
    text = path.read_text(encoding="utf-8")
    used.update(re.findall(r"R\.string\.([A-Za-z0-9_]+)", text))
missing = sorted(used - strings)
check(not missing, "todas as referências R.string existem" if not missing else f"strings ausentes: {missing}")

# A fase 2 deve conter os componentes essenciais.
source_files = {
    "filtro adaptativo": KOTLIN / "draw/FiltroTraco.kt",
    "preferências persistentes": KOTLIN / "draw/PreferenciasDesenho.kt",
    "vista com multitoque": KOTLIN / "draw/VistaDesenho.kt",
    "ecrã de configurações": KOTLIN / "ui/ConfiguracoesScreen.kt",
}
for label, path in source_files.items():
    check(path.exists() and path.stat().st_size > 500, f"ficheiro presente: {label}")

vista = (KOTLIN / "draw/VistaDesenho.kt").read_text(encoding="utf-8")
prefs = (KOTLIN / "draw/PreferenciasDesenho.kt").read_text(encoding="utf-8")
config = (KOTLIN / "ui/ConfiguracoesScreen.kt").read_text(encoding="utf-8")
for label, condition in [
    ("atalho dois dedos para desfazer", "2 -> m.desfazer()" in vista),
    ("atalho três dedos para refazer", "3 -> m.refazer()" in vista),
    ("rejeição de contacto grande", "contactoParecePalma" in vista),
    ("cursor deslocado desenhado", "desenharCursor" in vista),
    ("matriz invertida verificada", "if (!m.matrizVista.invert(inversa)) return" in vista),
    ("preferências persistidas em SharedPreferences", "getSharedPreferences" in prefs and ".apply()" in prefs),
    ("controlo para repor padrões", "reporPadroes" in config),
]:
    check(condition, label)

build = BUILD.read_text(encoding="utf-8")
check('versionCode = 4' in build and 'versionName = "0.3.0"' in build, "versão configurada para Fase 2 (0.3.0)")

all_sources = "\n".join(p.read_text(encoding="utf-8") for p in (ROOT / "app/src/main/java").rglob("*.kt"))
check("<<<<<<<" not in all_sources and ">>>>>>>" not in all_sources, "sem marcadores de conflito Git")

if errors:
    print("\nFALHAS:")
    for error in errors:
        print("-", error)
    sys.exit(1)
print("\nTodas as verificações estáticas passaram. Isto não substitui a compilação Gradle nem testes num dispositivo Android.")

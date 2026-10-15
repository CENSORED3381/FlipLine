#!/usr/bin/env python3
"""Verificações estáticas portáteis para a Fase 3 do FlipLine."""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/com/huzeifa/flipline"
STRINGS = ROOT / "app/src/main/res/values/strings.xml"
MANIFEST = ROOT / "app/src/main/AndroidManifest.xml"
BUILD = ROOT / "app/build.gradle.kts"
errors = []

def check(condition: bool, message: str) -> None:
    if condition:
        print("PASS:", message)
    else:
        errors.append(message)

try:
    root = ET.parse(STRINGS).getroot()
    nomes = [item.attrib.get("name") for item in root.findall("string")]
    check(len(nomes) == len(set(nomes)), "strings.xml não tem nomes duplicados")
    strings = set(nomes)
except Exception as exc:
    strings = set()
    check(False, f"strings.xml é XML válido ({exc})")

try:
    ET.parse(MANIFEST)
    check(True, "AndroidManifest.xml é XML válido")
except Exception as exc:
    check(False, f"AndroidManifest.xml é XML válido ({exc})")

fontes = list((ROOT / "app/src/main/java").rglob("*.kt"))
textos = {p: p.read_text(encoding="utf-8") for p in fontes}
usadas = set()
for texto in textos.values():
    usadas.update(re.findall(r"R\.string\.([A-Za-z0-9_]+)", texto))
ausentes = sorted(usadas - strings)
check(not ausentes, "todas as referências R.string existem" if not ausentes else f"strings ausentes: {ausentes}")

arquivos = {
    "motor de desenho em camadas": JAVA / "draw/MotorDesenho.kt",
    "vista de desenho e ferramentas da Fase 3": JAVA / "draw/VistaDesenho.kt",
    "renderizador de traço e simetria": JAVA / "draw/RenderizadorTraco.kt",
    "serialização de projetos e exportação PNG": JAVA / "draw/ProjetosFlipline.kt",
    "ecrã com ferramentas e gestor de camadas": JAVA / "ui/TelaDesenho.kt",
    "seletor de ficheiro e integração de projetos": JAVA / "MainActivity.kt",
}
for label, path in arquivos.items():
    # MainActivity está um nível acima de JAVA, todos os outros caminhos são absolutos a partir de JAVA.
    caminho = path if path.is_absolute() else path
    check(caminho.exists() and caminho.stat().st_size > 600, f"ficheiro presente: {label}")

motor = (JAVA / "draw/MotorDesenho.kt").read_text(encoding="utf-8")
vista = (JAVA / "draw/VistaDesenho.kt").read_text(encoding="utf-8")
render = (JAVA / "draw/RenderizadorTraco.kt").read_text(encoding="utf-8")
repo = (JAVA / "draw/ProjetosFlipline.kt").read_text(encoding="utf-8")
tela = (JAVA / "ui/TelaDesenho.kt").read_text(encoding="utf-8")
main = (ROOT / "app/src/main/java/com/huzeifa/flipline/MainActivity.kt").read_text(encoding="utf-8")
checks = [
    ("camadas possuem bitmap próprio", "class CamadaDesenho" in motor and "val bitmap: Bitmap" in motor),
    ("visibilidade, opacidade e modos de mesclagem", "definirVisibilidade" in motor and "definirOpacidade" in motor and "definirModoMesclagem" in motor),
    ("reordenação e remoção de camadas", "moverCamada" in motor and "removerCamada" in motor),
    ("limite de camadas evita crescimento ilimitado", "camadas.size >= 12" in motor),
    ("desfazer/refazer global por comandos", "historico.removeAt" in motor and "pilhaRefazer" in motor),
    ("preenchimento por inundação com visitados", "fun preencher(" in motor and "BooleanArray(total)" in motor),
    ("seleção suporta movimento e histórico", "moverConteudoSelecionado" in motor and "depoisPng" in motor),
    ("bald e conta-gotas conectados à área de toque", "FerramentaDesenho.BALDE" in vista and "FerramentaDesenho.CONTA_GOTAS" in vista),
    ("linha, retângulo e elipse com segmentos geométricos lineares", all(t in vista for t in ("FerramentaDesenho.LINHA", "FerramentaDesenho.RETANGULO", "FerramentaDesenho.ELIPSE")) and "segmentosLineares = true" in vista and "if (traco.segmentosLineares)" in render),
    ("balde não atravessa pixels de contorno parcialmente transparentes", "if (alphaA == 0 || alphaB == 0) return alphaA == 0 && alphaB == 0" in motor),
    ("composição em cache para evitar redesenho inútil", "composicaoSuja" in motor and "forcar: Boolean = false" in motor),
    ("assistência de forma por pausa", "detectarFormaAssistida" in vista and "ATRASO_FORMA_MS" in vista),
    ("grade, réguas, encaixe e simetria", "gradeAtiva" in vista and "reguasAtivas" in vista and "encaixeAtivo" in vista and "simetriaAtiva" in vista and "traco.simetriaVertical" in render),
    ("projeto ZIP com metadados, PNG por camada e mesclagem persistida", 'ZipOutputStream' in repo and 'document.json' in repo and 'Bitmap.CompressFormat.PNG' in repo and 'put("blend"' in repo and 'ModoMesclagem.valueOf' in repo),
    ("base PNG é reutilizada ao abrir projeto, sem recompressão no thread de interface", "bitmapBasePng = bytes" in repo and "dado.bitmapBasePng != null" in motor),
    ("validação de ZIP e limite de entradas", "lerLimitado" in repo and "MAX_BYTES_ENTRADA" in repo and "contains(\"..\")" in repo and "entradas.containsKey(nome)" in repo),
    ("exportação PNG preserva transparência", "fun exportarPng" in repo and "Bitmap.CompressFormat.PNG" in repo),
    ("seletores SAF para abrir, guardar e exportar", "CreateDocument" in main and "OpenDocument" in main),
    ("interface apresenta ferramentas e camadas", all(s in tela for s in ("GestorCamadas", "R.string.exportar_png", "FerramentaDesenho.SELECAO", "MiniToggle"))),
]
for label, condition in checks:
    check(condition, label)

build = BUILD.read_text(encoding="utf-8")
check('versionCode = 5' in build and 'versionName = "0.4.0"' in build, "versão configurada para Fase 3 (0.4.0)")
all_sources = "\n".join(textos.values())
check("<<<<<<<" not in all_sources and ">>>>>>>" not in all_sources, "sem marcadores de conflito Git")

if errors:
    print("\nFALHAS:")
    for error in errors:
        print("-", error)
    sys.exit(1)
print("\nTodas as verificações estáticas da Fase 3 passaram. Isto não substitui a compilação Gradle nem os testes no dispositivo Android.")

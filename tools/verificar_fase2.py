#!/usr/bin/env python3
"""Compatibilidade com o comando antigo: executa as verificações atuais da Fase 3."""
from pathlib import Path
import runpy
runpy.run_path(str(Path(__file__).with_name("verificar_fase3.py")), run_name="__main__")

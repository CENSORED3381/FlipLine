# FlipLine — Fase 3 implementada

Versão planeada: `0.4.0` (versionCode 5).

## Incluído

- Motor de desenho em camadas raster: nova camada, seleção da camada ativa, visibilidade, opacidade, reordenação, remoção e modos de mesclagem Normal/Multiplicar/Tela.
- Ferramentas de desenho: pincel, borracha, balde de tinta, conta-gotas, linha, retângulo, elipse e seleção retangular com movimentação do conteúdo da camada ativa.
- Assistência de forma ao manter o fim de um traço; deteção conservadora de linha/forma fechada. Ferramentas geométricas desenham segmentos retos para preservar cantos de retângulos.
- Guias: grade visual, réguas, encaixe nas formas/seleção e simetria vertical.
- Histórico para traços, preenchimentos e operações de movimento da seleção.
- Projetos `.flipline` (ZIP com `document.json` e PNGs de camada), abertura pelo seletor SAF e exportação PNG transparente.
- Limites defensivos de 12 camadas e documento fixo de 1200 × 1600 para controlar memória e evitar ficheiros de dimensões inesperadas. A composição raster fica em cache quando não há mudanças para reduzir trabalho de renderização; ao abrir projetos, o PNG original de cada camada é reutilizado como base do histórico para evitar recompressão no thread de interface.
- Verificador estático `tools/verificar_fase3.py`; o nome antigo `tools/verificar_fase2.py` continua a funcionar como compatibilidade.

## Como validar

1. No terminal do Ubuntu/Termux, executar `python3 tools/verificar_fase3.py`.
2. Enviar as alterações com `git add -A`, `git commit` e `git push`.
3. Esperar `Actions → Compilar APK` terminar com `Success`.
4. Instalar o artefacto `Flipline-debug-apk` e testar no dispositivo.
5. Confirmar o round-trip de projeto: guardar como `.flipline`, fechar/abrir novamente e verificar cores, visibilidade, opacidade, ordem e modos de mesclagem das camadas.
6. Exportar um PNG e confirmar a transparência fora da área pintada.

## Limites conhecidos a testar

- O balde preenche apenas a região contígua da camada ativa; o contorno que fecha a região deve estar na mesma camada. Pixels totalmente transparentes são tratados como uma classe própria para reduzir fugas pelos contornos antialias.
- A seleção move apenas pixels da camada ativa.
- Os gestos de forma assistida são heurísticos e precisam de validação com traços reais.
- A pressão da caneta, o texto, o balde com referência a todas as camadas e os formatos GIF/MP4 continuam fora desta fase.
- A fase só deve ser considerada aprovada depois da compilação no GitHub Actions e de testes no telemóvel. O verificador estático não substitui estes testes.

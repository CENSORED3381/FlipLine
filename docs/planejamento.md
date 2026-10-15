# Flipline — Planejamento do Projeto

App Android de desenho e animação, com foco em desenhar bem usando o dedo.

---

## 1. Visão geral

- **Nome do app:** Flipline (nome em inglês, interface toda em português)
- **Plataforma:** Android (APK)
- **Objetivo:** app de desenho avançado, com camadas, pincéis, exportação e animação quadro a quadro
- **Maior desafio:** desenhar com precisão usando o dedo
- **Splash:** tela de abertura com o texto **CRIADO POR HUZEIFA**

---

## 2. Ambiente e fluxo de trabalho

- Desenvolvimento no **Ubuntu rodando no Termux**
- Edição do código no terminal (nano, vim ou neovim)
- Fluxo: editar → `git commit` → `git push` → **GitHub Actions** compila o APK
- O APK é baixado como artefato do workflow e instalado no celular

---

## 3. Stack

**Kotlin nativo + Jetpack Compose**

| Parte | Tecnologia |
|---|---|
| Linguagem | Kotlin |
| Interface (menus, toggles, linha do tempo, FPS) | Jetpack Compose |
| Canvas de desenho | `View`/`SurfaceView` própria, com um `Bitmap` por camada |
| Build | Gradle no GitHub Actions (`assembleDebug` e `assembleRelease`) |
| Splash | `androidx.core:core-splashscreen` + tela rápida em Compose com o texto |

**Por que Kotlin nativo**
- Acesso a todos os pontos intermediários do toque
- Previsão de movimento para baixa latência
- Rejeição de palma e controle fino de memória dos bitmaps
- Exportação de MP4 com `MediaCodec`
- Gestos simultâneos previsíveis, sem camada de plugins

**Custo aceito:** código mais verboso e builds mais lentos. Como cada teste exige push e espera do Actions, o código será mantido em módulos pequenos e o workflow terá cache.

---

## 4. Recursos para desenhar com o dedo

Todos serão implementados e **cada um poderá ser ativado ou desativado nas configurações do app**.

1. **Estabilização do traço:** suaviza a linha em tempo real (filtro One Euro ou média móvel), com controle de intensidade
2. **Modo cursor deslocado** *(favorito)*: o ponto de desenho aparece afastado do dedo, que não tampa o que está sendo desenhado
3. **Zoom fácil:** pinça com dois dedos para zoom, pan e rotação, enquanto um dedo desenha
4. **Assistência de forma:** segurar no fim do traço endireita a linha ou vira círculo/elipse perfeitos
5. **Guias:** réguas, simetria, grade e encaixe (snap)
6. **Gestos de atalho:** toque com dois dedos desfaz, três dedos refaz
7. **Rejeição de palma:** ignora toques grandes ou simultâneos indesejados
8. **Baixa latência:** uso de todos os pontos do toque e previsão de movimento
9. **Pressão simulada:** espessura varia pela velocidade do traço
10. **Traço editável:** ajustar pontos depois de desenhar

---

## 5. Funcionalidades do app

### Desenho
- Pincéis (espessura, opacidade, cor), borracha
- Balde de tinta, conta-gotas, formas, seleção, texto
- Desfazer e refazer ilimitado
- Camadas: criar, ocultar, reordenar, opacidade, modos de mesclagem

### Animação
- Quadro a quadro com linha do tempo
- **FPS ajustável**
- Papel cebola (onion skin)
- Copiar, colar e duplicar quadros
- Cada quadro com suas próprias camadas

### Projetos e exportação
- Salvar e abrir projetos
- Exportar PNG (com transparência)
- Exportar GIF, MP4 e sequência de PNGs

---

## 6. Como funciona por dentro

- Cada camada é um bitmap próprio, composto em ordem na hora de exibir
- O traço é guardado como lista de pontos suavizados com curvas (Bézier ou Catmull-Rom)
- O desfazer usa comandos de traço ou salva apenas a região alterada
- Meta de desempenho: 60 fps ao desenhar
- Na animação, a memória é o ponto crítico (muitos bitmaps): precisa de compressão e carregamento sob demanda

---

## 7. Fases de desenvolvimento

Cada fase termina com um APK testável no celular.

### Fase 0 — Base do projeto
- Projeto Kotlin + Compose, ID do pacote, ícone
- Workflow do GitHub Actions gerando o APK
- Splash "CRIADO POR HUZEIFA" e tela inicial em português

### Fase 1 — Desenho básico
- Canvas com pincel, borracha, cor e espessura
- Desfazer e refazer
- Zoom, pan e rotação com dois dedos

### Fase 2 — Ajudas para o dedo
- Estabilização do traço
- Cursor deslocado
- Tela de configurações com liga/desliga de cada opção
- Gestos de atalho e rejeição de palma

### Fase 3 — Camadas e ferramentas
- [x] Camadas (ocultar, reordenar, opacidade e modos Normal/Multiplicar/Tela)
- [x] Balde, conta-gotas, linha, retângulo, elipse e seleção com movimentação
- [x] Assistência de forma, grade, réguas, encaixe e simetria
- [x] Guardar/abrir projeto `.flipline` e exportar PNG transparente
- [ ] Validar compilação Gradle e comportamento das ferramentas num dispositivo Android

### Fase 4 — Animação
- Linha do tempo com quadros
- FPS ajustável
- Papel cebola
- Copiar, colar e duplicar quadros

### Fase 5 — Exportação e polimento
- GIF, MP4 e sequência de PNGs
- Baixa latência (previsão de movimento) e pressão simulada
- Otimização de memória e desempenho
- Traço editável, se ainda fizer sentido

**Por que esta ordem:** cursor deslocado e estabilização vêm cedo porque são o maior desafio. A animação só entra depois que camadas e salvamento estão sólidos.

---

## 8. Splash "CRIADO POR HUZEIFA"

- Fundo escuro limpo, ícone do app no centro e o texto **CRIADO POR HUZEIFA** embaixo, em letras espaçadas
- Duração de cerca de 1,5 a 2 segundos, com animação suave de entrada do texto
- A splash nativa do Android 12+ mostra só o ícone, então o texto fica numa segunda tela rápida em Compose, para funcionar em todas as versões

---

## 9. Pendências a definir

- [x] ID do pacote: `com.huzeifa.flipline`
- [x] Tema visual inicial: escuro com destaque laranja
- [x] Exportação por etapas: PNG na Fase 3; GIF/MP4 e sequência de PNGs na Fase 5
- [x] Ordem de fases adotada; as fases de animação e polimento continuam futuras


---

## Estado registado da Fase 3 (0.4.0)

A implementação adiciona um motor raster com até 12 camadas, histórico de ações por camada, preenchimento contíguo, seleção móvel e modos de mesclagem Normal/Multiplicar/Tela. Os projetos são arquivos ZIP com metadados JSON e um PNG por camada; a leitura verifica dimensões, número/tamanho das entradas e referências antes de aceitar o documento. A tela está fixa em 1200 × 1600 nesta versão para controlar a utilização de memória.

**Validação obrigatória antes de considerar a fase concluída:** executar a workflow `Compilar APK`, instalar o artefacto no Android e testar pincel/borracha, balde dentro de contorno na mesma camada, ordem/visibilidade/opacidade/mesclagem, seleção/movimentação com undo/redo, formas e simetria, guardar/abrir `.flipline` e exportar PNG com transparência. A execução do verificador estático não é prova de compilação nem de ausência de bugs.

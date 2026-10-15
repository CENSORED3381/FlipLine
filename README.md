# Flipline

Aplicação Android de desenho raster e animação planejada, com foco em desenhar com precisão usando o dedo.

**Criado por Huzeifa.**

## Estado do projeto

- **Fase 0:** estrutura Android, splash e tela inicial.
- **Fase 1:** desenho básico, pincel, borracha, cores, espessura, desfazer/refazer e navegação com dois dedos.
- **Fase 2:** estabilização adaptativa, cursor deslocado, preferências persistentes, atalhos de dois/três dedos e rejeição conservadora de contactos grandes.
- **Fase 3:** camadas com visibilidade, ordem, opacidade e modos Normal/Multiplicar/Tela; balde, conta-gotas, linha, retângulo, elipse, seleção e movimentação; assistência de forma; grade, réguas, encaixe e simetria; abrir/guardar projetos `.flipline` e exportar PNG transparente.

A interface é construída com Kotlin/Jetpack Compose e a área de desenho usa uma `View`/`Canvas` nativa. Cada camada mantém um bitmap separado. Os projetos guardam metadados e um PNG por camada dentro de um ZIP com extensão `.flipline`; o PNG exportado mantém transparência.

## Ferramentas e gestos

- **Pincel/Borracha:** um dedo desenha na camada ativa.
- **Balde:** preenche uma região contígua da camada ativa com a cor atual.
- **Conta-gotas:** recolhe a cor visível na composição sobre o papel branco.
- **Linha/Retângulo/Elipse:** arrasta para definir a forma.
- **Seleção:** arrasta para criar um retângulo; arrasta dentro dele para mover o conteúdo da camada ativa.
- **Dois dedos em movimento:** amplia, move e roda a folha.
- **Dois dedos em toque rápido:** desfaz; **três dedos em toque rápido:** refaz.
- **Simetria:** espelha verticalmente o traço ou a forma.

O balde trabalha na camada ativa. Para preencher o interior de um contorno, o contorno deve estar nessa mesma camada. A seleção também afeta apenas a camada ativa. O encaixe usa passos de 50 unidades nas formas e na seleção; a grade visual usa linhas de 100 unidades.

## Projetos e exportação

Use os botões **Guardar**, **Abrir** e **Exportar PNG** na tela de desenho. O Android abre o seletor de ficheiros para escolher onde guardar ou qual projeto abrir. A exportação PNG contém a composição visível das camadas e preserva a transparência onde não há desenho.

Na Fase 3, o documento tem tela fixa de **1200 × 1600** e um limite de **12 camadas** para controlar a memória em telemóveis. Os projetos com outra dimensão ou mais camadas são recusados nesta versão, em vez de serem carregados parcialmente.

## Compilar o APK

O GitHub Actions compila o APK de debug ao fazer `git push` para `main`/`master`, ou manualmente em **Actions → Compilar APK → Run workflow**. A workflow executa verificações estáticas e depois `gradle assembleDebug`.

Quando a execução termina com sucesso, abre-a e descarrega o artefacto `Flipline-debug-apk`.

## Requisitos de desenvolvimento

- JDK 17
- Gradle 8.9
- Android SDK compatível com `compileSdk = 34`

## Estrutura principal

- `app/src/main/java/com/huzeifa/flipline/draw/`: motor de camadas, renderização, preenchimento e serialização de projetos.
- `app/src/main/java/com/huzeifa/flipline/ui/`: ecrãs Compose, ferramentas e gestor de camadas.
- `app/src/main/res/values/strings.xml`: interface em português.
- `.github/workflows/build.yml`: compilação automática do APK.
- `tools/verificar_fase3.py`: verificações estáticas da Fase 3. O antigo comando `verificar_fase2.py` encaminha para o mesmo verificador.
- `docs/planejamento.md`: plano geral do FlipLine.
- `docs/FASE_3_IMPLEMENTADA.md`: resumo e roteiro de validação da Fase 3.

## Limites de validação

As verificações estáticas validam XML, referências de strings, presença dos componentes e marcadores de conflito. Não substituem a compilação Gradle do GitHub Actions nem os testes reais no Android. A rejeição de palma depende dos dados de toque que o dispositivo fornece.

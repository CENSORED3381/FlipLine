# Flipline

Aplicação Android de desenho e animação, com foco em desenhar com precisão usando o dedo.

**Criado por Huzeifa.**

## Estado do projeto

- **Fase 0:** estrutura, splash e tela inicial.
- **Fase 1:** desenho básico com pincel, borracha, paleta, espessura, desfazer/refazer e navegação com dois dedos.
- **Fase 2:** estabilização One Euro ajustável, cursor deslocado, configurações persistentes, atalhos de dois/três dedos e rejeição conservadora de contactos grandes.

A Fase 2 mantém a implementação de desenho em `View`/`Canvas` nativa, separada da interface Compose. Os recursos podem ser ativados ou desativados em **Configurações**; os valores são guardados no armazenamento privado da aplicação.

## Gestos

- **Um dedo:** desenha.
- **Dois dedos em movimento:** amplia, move e roda a folha (se a navegação estiver ativa).
- **Dois dedos em toque rápido, sem arrastar:** desfaz.
- **Três dedos em toque rápido, sem arrastar:** refaz.

Os atalhos de toque reconhecem pequenos movimentos naturais do dedo; se houver movimento de navegação, o gesto não é interpretado como desfazer/refazer.

## Compilar o APK

O GitHub Actions compila o APK de debug ao fazer `git push` para `main`/`master`, ou manualmente em **Actions → Compilar APK → Run workflow**.

Depois, abra a execução concluída e baixe o artefacto `Flipline-debug-apk`.

## Requisitos de desenvolvimento

- JDK 17
- Gradle 8.9
- Android SDK compatível com `compileSdk = 34`

## Estrutura principal

- `app/src/main/java/com/huzeifa/flipline/draw/`: motor, traços, filtro adaptativo, preferências e deteção de gestos.
- `app/src/main/java/com/huzeifa/flipline/ui/`: ecrãs Compose, incluindo as configurações da Fase 2.
- `app/src/main/res/values/strings.xml`: textos em português.
- `.github/workflows/build.yml`: compilação automática e publicação do APK como artefacto.
- `docs/planejamento.md`: plano geral do Flipline.
- `docs/FASE_2_IMPLEMENTADA.md`: funcionalidades, limites e passos de validação da fase.

## Nota sobre rejeição de palma

O Android não fornece informação fiável sobre o tamanho do contacto em todos os ecrãs. Quando os dados estão ausentes, a aplicação não rejeita o toque automaticamente, para evitar bloquear o desenho legítimo. O limite é deliberadamente conservador.

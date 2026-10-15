# Flipline

App Android de desenho e animação, com foco em desenhar bem usando o dedo.

Criado por Huzeifa.

## Status

- Fase 0: base do projeto, splash e tela inicial (concluída)
- Próxima: Fase 1, desenho básico

O planejamento completo está em `docs/planejamento.md`.

## Como compilar

Cada `git push` para `main` (ou `master`) roda o GitHub Actions e gera o APK.
Para baixar: aba **Actions** do repositório, abra a execução mais recente e
baixe o artefato **Flipline-debug-apk**.

## Estrutura

- `app/src/main/java/com/huzeifa/flipline/`: código Kotlin
- `app/src/main/res/`: textos, cores, temas e ícone
- `.github/workflows/build.yml`: workflow que compila o APK

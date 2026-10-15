# Flipline — Fase 2: ajudas para o dedo

## Implementado nesta versão

1. **Estabilização adaptativa do traço:** filtro One Euro bidimensional com intensidade configurável.
2. **Cursor deslocado:** o ponto de desenho aparece acima e à direita do dedo; a distância vertical pode ser ajustada.
3. **Configurações persistentes:** as preferências são guardadas no armazenamento privado da aplicação e mantidas entre sessões.
4. **Atalhos multitoque:** toque rápido com dois dedos para desfazer e com três dedos para refazer. O movimento acima do limite cancela a interpretação como toque.
5. **Rejeição conservadora de palma:** rejeita contactos com medidas de toque muito grandes quando o dispositivo fornece esses dados.
6. **Controlos individuais:** estabilização, cursor deslocado, atalhos, rejeição de palma e navegação com dois dedos têm interruptores próprios.
7. **Reposição de padrões:** permite recuperar os valores iniciais das preferências.

## Comportamento de desenho

- O filtro atua nas amostras do toque, incluindo os pontos históricos fornecidos pelo Android.
- O cursor e o traço usam a mesma posição filtrada, para que a indicação visual corresponda ao local onde a linha é efetivamente desenhada.
- Antes de um segundo dedo iniciar um gesto, o traço em curso é cancelado e o bitmap é reconstruído se necessário; isto evita deixar uma marca parcial ao tentar navegar.
- Os limites de zoom e os cálculos de matriz continuam protegidos contra escala nula e matriz não invertível.
- A deteção de palma é intencionalmente conservadora: contactos sem dimensões válidas não são rejeitados.

## Validação efetuada no pacote

- Verificação de integridade do ZIP de entrada.
- Verificação estática de referências às preferências e dos recursos de texto usados pelo código.
- Parsing XML de `AndroidManifest.xml` e dos recursos de strings.
- Verificação de chaves de preferências, entradas de configuração e versão do projeto.
- Pesquisa de marcadores de conflito Git e de código placeholder nas funcionalidades da Fase 2.

## Validação pendente no dispositivo/CI

A compilação Android e os testes táteis reais dependem do Android SDK e de um ecrã físico/emulador. O pacote não deve ser considerado testado em dispositivo até o GitHub Actions concluir a compilação e os gestos serem verificados no telemóvel. Em particular, a rejeição de palma depende da qualidade dos dados de contacto reportados pelo fabricante.

## Roteiro de teste manual

1. Abrir **Configurações**, desligar e ligar a estabilização e confirmar que o estado persiste após fechar e reabrir a aplicação.
2. Desenhar uma linha lenta com estabilização em 0%, 55% e 100%; confirmar que o efeito aumenta gradualmente.
3. Ativar o cursor deslocado; desenhar e verificar que a linha acompanha o cursor mostrado acima e à direita do dedo.
4. Fazer alguns traços, tocar rapidamente com dois dedos e confirmar o desfazer; tocar rapidamente com três dedos e confirmar o refazer.
5. Arrastar dois dedos para confirmar zoom/movimento/rotação e que esse gesto não dispara um atalho.
6. Desativar cada opção e confirmar que o comportamento correspondente deixa de ocorrer.
7. Testar a rejeição de palma com o aparelho disponível, verificando também que dedos normais continuam a desenhar.

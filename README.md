# Lista Nômade

Aplicativo Android nativo para organizar compras por categorias, com foco em uso rápido, funcionamento offline, baixo consumo e dados persistidos localmente.

**Versão atual:** 1.0.4+5  
**applicationId:** `com.listanomade.app`

## Funcionalidades

- Catálogo inicial com `Bicicleta`, `Camping`, `Eletrônicos`, `Pesca`, `Alimentação`, `Ferramentas`, `Viagem` e `Outros`.
- Itens já discutidos são pré-cadastrados com preços de referência editáveis, reduzindo o preenchimento manual.
- Categorias independentes com itens, total, orçamento opcional e resumo de compras.
- Totais no topo separados em total geral, valor pendente e valor já comprado.
- Itens com nome, preço unitário, quantidade, total automático e estado comprado/não comprado.
- Digitação monetária automática em padrão brasileiro: os números digitados são convertidos para `R$ 0,00` sem inserir vírgulas manualmente.
- Busca instantânea de itens e filtros para todos, pendentes ou comprados.
- Categorias recolhíveis, mantendo o estado após fechar o aplicativo.
- Ordenação por ordem personalizada, nome, maior/menor valor ou pendentes primeiro.
- Reordenação manual de categorias e itens.
- Edição, duplicação e exclusão com `Desfazer` para itens.
- Fluxo `Salvar e adicionar outro` para preencher listas grandes com poucos toques.
- Backup e restauração local em JSON pelo seletor nativo de arquivos do Android.
- Tema claro/escuro persistente.
- Tela de informações com versão, 3 últimas alterações e doação via cópia da chave Pix.

## Catálogo inicial

O catálogo padrão foi montado a partir das listas de compra já definidas para o projeto e evita recadastrar manualmente itens recorrentes. Os valores são referências editáveis:

- `Bicicleta`: pezinho/descanso (R$ 19,00), suporte impermeável (R$ 23,00), sapatas GTS (R$ 13,99), kit 2 câmaras (R$ 24,99), farol (R$ 31,49) e cola + 6 remendos (R$ 16,00).
- `Camping`: lona 4 × 3 m (R$ 38,90), fogareiro (R$ 27,99), saco de dormir (R$ 48,70), 2 cartuchos de gás (R$ 14,00 cada) e faca de camping (R$ 10,00).
- `Eletrônicos`: power bank Geonav 10.000 mAh 20 W (R$ 137,68), tela Redmi Note 11 Pro+ 5G (R$ 95,27) e cabo USB-C 2 m (R$ 16,76).
- `Pesca`: linha (R$ 10,00), 10 anzóis (R$ 0,30 cada) e 6 chumbadas (R$ 0,50 cada).
- `Outros`: máquina de barba (R$ 19,99).
- `Alimentação`, `Ferramentas` e `Viagem` são criadas prontas para receber itens.

Itens anteriormente descartados ou substituídos não são recriados. A atualização para 1.0.4 também não repõe continuamente itens apagados: a inclusão automática ocorre apenas na criação do banco ou na migração para o schema 3, evitando que uma exclusão intencional volte a aparecer em toda abertura do aplicativo.

## Persistência

O app utiliza `SQLiteOpenHelper` para listas e `SharedPreferences` para configurações. Valores monetários são armazenados em centavos (`Long`) para evitar erros de ponto flutuante.

A versão 1.0.4 utiliza schema SQLite 3. A migração incremental preserva categorias e itens já cadastrados e acrescenta, uma única vez, apenas categorias e itens padrão que ainda não existam. Os preços iniciais são referências locais e permanecem editáveis.

O backup exportado contém categorias, itens, preços, quantidades, status de compra, orçamento, ordem e configurações principais. A restauração substitui os dados locais atuais pelo conteúdo do arquivo escolhido.

## Interface

- Áreas seguras respeitam barras de status e navegação do Android.
- Cartões e formulários compactos para aproveitar telas pequenas.
- Categorias podem ser recolhidas sem perder o resumo principal.
- Busca e filtros ficam disponíveis diretamente na tela inicial.
- Estados `Pendente` e `Comprado` usam tratamento visual distinto.
- Cadastro de valor usa teclado numérico e máscara monetária automática.

## Base técnica

- Android nativo / Kotlin / XML tradicional.
- Java/JVM 17.
- Kotlin 2.0.21.
- Android Gradle Plugin 8.7.3.
- Gradle 8.10.2 no CI.
- compileSdk 35, targetSdk 35, minSdk 26 (Android 8.0+).
- AppCompat 1.7.1 e RecyclerView 1.4.0; sem Compose, Room ou bibliotecas visuais pesadas.

## Estrutura

- `model/`: modelos das categorias e itens.
- `data/`: SQLite, repositório e backup/restauração.
- `ui/main/`: tela principal, filtros e adaptadores.
- `ui/item/`: inclusão e edição de itens.
- `ui/settings/`: informações, tema, backup e doação.
- `util/`: dinheiro, máscara monetária, preferências, tema e áreas seguras.
- `scripts/`: validações de versão e arquitetura.

Nenhum arquivo Kotlin pode ultrapassar 500 linhas. `scripts/verify_versions.py` valida essa regra e o sincronismo de versão.

## Build local

Requisitos: JDK 17, Android SDK 35 e Gradle 8.10.2.

```bash
python3 scripts/verify_versions.py
gradle :app:assembleDebug
```

Para Release assinado, defina `ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` e `ANDROID_KEY_PASSWORD`.

## GitHub Actions / Works

O workflow `.github/workflows/release.yml`:

1. prepara Java 17 e Android SDK 35;
2. fixa Gradle 8.10.2;
3. valida os Secrets da chave permanente;
4. valida versão e limite de linhas;
5. compila e verifica o APK Release assinado;
6. cria `Lista-Nomade-v<VERSAO>.apk`;
7. exige que `dist/` contenha exatamente esse APK;
8. publica diretamente o APK na GitHub Release.

O workflow **não gera `source.zip` personalizado**, **não usa `git archive`** e **não usa `actions/upload-artifact`**. Os links automáticos `Source code (zip)` e `Source code (tar.gz)` exibidos pelo próprio GitHub na página da Release não são arquivos gerados pelo Works/workflow.

Veja `SIGNING.md` para configurar os Secrets de assinatura permanente.

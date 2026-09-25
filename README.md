# Lista Nômade

Aplicativo Android nativo para listas de compras por categorias, focado em uso rápido, armazenamento local e baixo consumo.

**Versão atual:** 1.0.2+3  
**applicationId:** `com.listanomade.app`

## Funcionalidades

- Categoria inicial `Bicicleta` criada automaticamente no primeiro uso.
- Categorias independentes com nome, itens, total da categoria e contadores de pendentes/comprados.
- Total geral e total pendente no topo.
- Itens com nome, preço unitário, quantidade, total calculado, edição, exclusão e marcação como comprado.
- Inclusão rápida de itens, com categoria pré-selecionada quando iniciada pelo cartão da lista.
- Tema claro/escuro persistente.
- Tela de informações com versão, 3 últimas alterações e botão de doação que copia a chave Pix.
- Persistência local por `SQLiteOpenHelper`; preferências por `SharedPreferences`.
- Valores monetários armazenados em centavos (`Long`), sem erros de ponto flutuante na persistência.

## Interface

A versão 1.0.2 reorganiza a interface para uso diário em telas pequenas e recentes:

- conteúdo respeita automaticamente as áreas da barra de status e da navegação do Android;
- botão inferior `Adicionar item` permanece totalmente visível acima da navegação do sistema;
- cabeçalho e cartões usam menos espaço vertical;
- formulário de item combina preço e quantidade na mesma linha e identifica visualmente a seleção de categoria;
- itens usam um único menu de ações para editar/excluir, reduzindo poluição visual;
- estados `Pendente` e `Comprado` possuem cores distintas;
- diálogo de categoria segue a mesma linguagem visual do restante do aplicativo;
- área de doação foi reduzida para não competir com as configurações principais;
- contraste de textos secundários e estados desabilitados foi reforçado.

## Base técnica

- Android nativo / Kotlin / XML tradicional.
- Java/JVM 17.
- Kotlin 2.0.21.
- Android Gradle Plugin 8.7.3.
- Gradle 8.10.2 no CI.
- compileSdk 35, targetSdk 35, minSdk 26 (Android 8.0+).
- AppCompat 1.7.1 e RecyclerView 1.4.0; sem Compose, Room ou bibliotecas visuais pesadas.

## Estrutura

- `model/`: modelos imutáveis.
- `data/`: SQLite e repositório.
- `ui/main/`: tela principal e adaptadores.
- `ui/item/`: inclusão/edição.
- `ui/settings/`: informações, tema e doação.
- `util/`: dinheiro, preferências, tema e tratamento das áreas seguras do sistema.
- `scripts/`: validação de versão e arquitetura.

Nenhum arquivo Kotlin deve ultrapassar 500 linhas. `scripts/verify_versions.py` valida essa regra e o sincronismo de versão.

## Build local

Requisitos: JDK 17, Android SDK 35 e Gradle 8.10.2.

```bash
python3 scripts/verify_versions.py
gradle :app:assembleDebug
```

Para Release assinado, defina `ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` e `ANDROID_KEY_PASSWORD` antes do build.

## GitHub Actions

O workflow `.github/workflows/release.yml`:

1. prepara Java 17 e Android SDK 35;
2. fixa Gradle 8.10.2;
3. valida os Secrets da chave permanente;
4. valida versões e limite de linhas;
5. compila Release assinado e verifica a assinatura com `apksigner`;
6. cria `Lista-Nomade-v<VERSAO>.apk`;
7. valida que `dist/` contém somente esse APK;
8. publica diretamente o APK na GitHub Release.

O workflow **não usa `actions/upload-artifact`** e **não gera mais `Lista-Nomade-v<VERSAO>-source.zip`**. GitHub Releases pode continuar exibindo os arquivos automáticos `Source code (zip)` e `Source code (tar.gz)` gerados pelo próprio GitHub; eles não são artifacts criados pelo workflow do projeto.

Veja `SIGNING.md` para configurar os Secrets.

# Lista Nômade

Aplicativo Android nativo para organizar compras por categorias, com foco em uso rápido, funcionamento offline, baixo consumo e dados persistidos localmente.

**Versão atual:** 1.0.5+6  
**applicationId:** `com.listanomade.app`

## Funcionalidades

- Catálogo inicial com `Bicicleta`, `Camping`, `Eletrônicos`, `Pesca`, `Alimentação`, `Ferramentas`, `Viagem` e `Outros`.
- Itens de referência pré-cadastrados com preços editáveis para reduzir o preenchimento manual.
- Categorias independentes com orçamento opcional, totais, busca, filtros, recolhimento e ordenação.
- Orçamento geral para todas as compras, com saldo restante ou indicação de valor acima do limite.
- Estados `Pendente`, `Comprado` e `Já tenho`; itens `Já tenho` permanecem na lista sem entrar no valor que falta gastar.
- Prioridades `Essencial`, `Importante` e `Opcional`, inclusive com ordenação por prioridade.
- Preço previsto e preço realmente pago no mesmo item. Quando o preço pago é informado, os totais de compras concluídas usam o valor real e a lista mostra economia ou excesso.
- Loja/origem do preço e link opcional do produto; o link pode ser aberto diretamente pelo menu do item.
- Quantidade, cálculo automático de totais e máscara monetária brasileira durante a digitação.
- Totais no topo separados em total geral, pendente e comprado.
- Busca por nome do item ou loja e filtros `Todos`, `Pendentes`, `Comprados` e `Já tenho`.
- Ordenação personalizada, por prioridade, nome, maior/menor valor ou pendentes primeiro.
- Edição, duplicação, reordenação e exclusão com `Desfazer`.
- Fluxo `Salvar e adicionar outro` para preenchimento sequencial.
- Backup e restauração local em JSON pelo seletor nativo de arquivos do Android.
- Tema claro/escuro persistente, informações da versão e doação via cópia da chave Pix.

## Regras dos totais

- `Pendente`: usa o preço previsto dos itens ainda não resolvidos.
- `Comprado`: usa o preço pago quando informado; se ficar em branco, usa o preço previsto.
- `Já tenho`: não entra no valor pendente, comprado, total efetivo ou orçamento.
- `Total geral`: soma o custo efetivo das compras — valores reais dos comprados mais valores previstos dos pendentes.
- Orçamentos de categoria e orçamento geral usam a mesma regra do total efetivo.

## Catálogo inicial

Os valores abaixo são referências locais e permanecem editáveis:

- `Bicicleta`: pezinho/descanso (R$ 19,00), suporte impermeável (R$ 23,00), sapatas GTS (R$ 13,99), kit 2 câmaras (R$ 24,99), farol (R$ 31,49) e cola + 6 remendos (R$ 16,00).
- `Camping`: lona 4 × 3 m (R$ 38,90), fogareiro (R$ 27,99), saco de dormir (R$ 48,70), 2 cartuchos de gás (R$ 14,00 cada) e faca de camping (R$ 10,00).
- `Eletrônicos`: power bank Geonav 10.000 mAh 20 W (R$ 137,68), tela Redmi Note 11 Pro+ 5G (R$ 95,27) e cabo USB-C 2 m (R$ 16,76).
- `Pesca`: linha (R$ 10,00), 10 anzóis (R$ 0,30 cada) e 6 chumbadas (R$ 0,50 cada).
- `Outros`: máquina de barba (R$ 19,99).
- `Alimentação`, `Ferramentas` e `Viagem` são criadas prontas para receber itens.

Itens excluídos pelo usuário não são recriados em toda abertura; o catálogo é semeado somente na criação/migração correspondente.

## Persistência

O app utiliza `SQLiteOpenHelper` para categorias e itens e `SharedPreferences` para configurações. Valores monetários são armazenados em centavos (`Long`) para evitar erros de ponto flutuante.

A versão 1.0.5 utiliza **schema SQLite 4**. A migração adiciona `Já tenho`, prioridade, preço pago, loja e link sem apagar os registros anteriores. Itens existentes recebem `Importante` como prioridade inicial e mantêm seu estado de compra.

O backup exportado usa schema 3 e inclui categorias, itens, preços previstos e pagos, quantidades, estados, prioridades, loja/link, orçamentos, ordem e configurações principais, incluindo o orçamento geral.

## Interface

- Áreas seguras respeitam barras de status e navegação do Android.
- Cartões e formulários compactos para aproveitar telas pequenas.
- Preço previsto, quantidade, categoria, situação e prioridade ficam acessíveis no cadastro/edição.
- O campo `Preço pago` aparece quando o item está como `Comprado`.
- Itens exibem situação, prioridade, loja e diferenças entre previsto e pago quando aplicável.
- O orçamento geral é configurável diretamente no cartão de totais.

## Base técnica

- Android nativo / Kotlin / XML tradicional.
- Java/JVM 17.
- Kotlin 2.0.21.
- Android Gradle Plugin 8.7.3.
- Gradle 8.10.2 no CI.
- compileSdk 35, targetSdk 35, minSdk 26 (Android 8.0+).
- AppCompat 1.7.1 e RecyclerView 1.4.0; sem Compose, Room ou bibliotecas visuais pesadas.

## Estrutura

- `model/`: modelos de categorias e itens.
- `data/`: SQLite, catálogo inicial, repositório e backup/restauração.
- `ui/main/`: tela principal, filtros, totais e adaptadores.
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

O workflow `.github/workflows/release.yml` prepara Java 17 e Android SDK 35, valida assinatura e versão, compila o Release assinado, cria `Lista-Nomade-v<VERSAO>.apk` e publica diretamente esse APK na GitHub Release.

O workflow **não gera `source.zip` personalizado**, **não usa `git archive`** e **não usa `actions/upload-artifact`**. Os links automáticos `Source code (zip)` e `Source code (tar.gz)` exibidos pelo GitHub pertencem à própria plataforma e não são arquivos produzidos pelo Works.

Veja `SIGNING.md` para configurar a chave de assinatura permanente.

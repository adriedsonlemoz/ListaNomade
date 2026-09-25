# Changelog

## [1.0.6] - 2026-09-25

### Adicionado
- Compra parcial por item, com quantidade já comprada, cálculo do valor efetivamente gasto e saldo restante das unidades pendentes.
- Histórico de preços com data para alterações de preço previsto e preço pago.
- Meta de compra opcional por item e ordenação pela data mais próxima.
- Arquivamento de categorias com restauração posterior, sem perda dos itens.
- Painel de progresso com percentual baseado nas quantidades, contagem de comprados, parciais, `Já tenho` e pendentes, além de valores gasto e restante.
- Compartilhamento de uma categoria ou da lista completa em texto pelo compartilhamento nativo do Android.
- Modelos reutilizáveis: qualquer categoria pode ser salva como modelo e recriada depois como uma nova lista.
- Importação em massa por texto usando `Nome | preço | quantidade`, com preço e quantidade opcionais.
- Filtro por loja/origem e novo `Modo viagem`, que exibe somente itens Essenciais ainda pendentes.
- Categoria `Energia`, com `Bateria externa / Power bank Geonav 10.000 mAh 20 W` por R$ 137,68 e `Cabo USB-C PD 2 m` por R$ 16,76; instalações existentes movem esses itens de Eletrônicos sem duplicá-los.
- Catálogo de camping ampliado com `Lanterna Voxo T9 recarregável` por R$ 29,00 e `Barraca Ontrek Iglu 4 pessoas` por R$ 86,45, já marcada como `Já tenho` para não entrar no gasto pendente.

### Melhorado
- Totais agora consideram corretamente compras parciais: unidades adquiridas usam o preço pago quando informado e unidades restantes continuam usando o preço previsto.
- Backup local atualizado para schema 4, incluindo categorias arquivadas, compra parcial, histórico de preços, metas, modelos, filtro por loja e modo viagem.
- Resumo de categoria passa a informar itens parcialmente comprados.
- Catálogo padrão separa itens de energia dos demais eletrônicos e mantém preços de referência editáveis.

### Persistência, build e documentação
- SQLite migrado incrementalmente do schema 4 para o schema 5, preservando categorias, itens, estados e preços existentes.
- Versão sincronizada como `1.0.6+7` em `gradle.properties`, `github-manager.json`, `app_identity.json`, README, CHANGELOG e tela de informações.
- Workflow continua preparando/publicando somente o APK da Release; não gera `source.zip` personalizado nem usa `actions/upload-artifact`.

## [1.0.5] - 2026-09-25

### Adicionado
- Estado `Já tenho` para itens que já estão em posse do usuário e não devem entrar no valor pendente nem no orçamento de compra.
- Prioridades `Essencial`, `Importante` e `Opcional`, com exibição nos itens e nova ordenação por prioridade.
- Orçamento geral para todas as categorias, exibindo saldo restante ou valor acima do limite diretamente na tela principal.
- Preço previsto e preço pago no mesmo item; compras concluídas passam a usar o valor realmente pago nos totais e mostram economia ou excesso quando houver diferença.
- Campos opcionais de loja/origem e link do produto, com ação para abrir o anúncio diretamente pelo menu do item.

### Melhorado
- Filtros agora incluem `Já tenho`; itens resolvidos podem voltar a pendentes pelo checkbox ou menu.
- Busca também considera o nome da loja, e a duplicação de item limpa estados de compra para criar uma nova pendência.
- Backup local atualizado para schema 3, incluindo estado `Já tenho`, prioridade, preço pago, loja, link e orçamento geral.
- Totais por categoria e total geral desconsideram itens `Já tenho`; comprados usam preço pago quando informado e pendentes continuam usando preço previsto.

### Persistência, build e documentação
- SQLite migrado incrementalmente do schema 3 para o schema 4 sem apagar categorias ou itens existentes.
- Versão sincronizada como `1.0.5+6` em `gradle.properties`, `github-manager.json`, `app_identity.json`, README, CHANGELOG e tela de informações.
- Workflow permanece configurado para preparar/publicar somente o APK da Release, sem `source.zip` personalizado.

## [1.0.4] - 2026-09-25

### Adicionado
- Catálogo inicial com as categorias `Bicicleta`, `Camping`, `Eletrônicos`, `Pesca`, `Alimentação`, `Ferramentas`, `Viagem` e `Outros`.
- Itens de compra já definidos anteriormente foram pré-cadastrados com preços de referência editáveis, incluindo bicicleta, camping, energia/eletrônicos e pesca.
- Migração do banco para schema 3, capaz de adicionar somente categorias e itens padrão ausentes em instalações já existentes.

### Melhorado
- Instalações novas já abrem com uma lista prática para uso imediato, reduzindo o cadastro manual.
- A migração reconhece nomes alternativos de itens comuns para reduzir duplicações, inclusive `Câmara de Ar`, suporte de celular, farol, lona, power bank e itens de pesca.
- Itens descartados ou substituídos nas listas anteriores não fazem parte do catálogo inicial.

### Build e documentação
- Versão sincronizada como `1.0.4+5` em `gradle.properties`, `github-manager.json`, `app_identity.json`, README, CHANGELOG e tela de informações.
- Workflow continua preparando/publicando somente o APK da Release; não gera `source.zip` personalizado.

## [1.0.3] - 2026-09-25

### Adicionado
- Orçamento opcional por categoria, com indicação do valor planejado, saldo restante ou valor acima do limite.
- Total geral separado em `Total`, `Pendente` e `Comprado` para acompanhar o que ainda falta adquirir.
- Busca de itens e filtros `Todos`, `Pendentes` e `Comprados`.
- Categorias recolhíveis com estado persistido entre aberturas do aplicativo.
- Ordenação de itens por ordem personalizada, nome, maior valor, menor valor e pendentes primeiro.
- Reordenação manual de categorias e itens por ações `Mover para cima` e `Mover para baixo`.
- Ação `Salvar e adicionar outro` para cadastro sequencial de itens.
- Duplicação de itens e exclusão com opção `Desfazer` durante alguns segundos.
- Backup e restauração local em JSON usando o seletor de arquivos do Android, sem exigir permissões de armazenamento.

### Melhorado
- Digitação de preço unitário e orçamento com máscara monetária brasileira automática: os dígitos são convertidos diretamente para `R$ 0,00`, sem necessidade de inserir ponto ou vírgula manualmente.
- Resumo das categorias agora mostra quantidade de itens, comprados e valor pendente mesmo quando a categoria está recolhida.
- Persistência atualizada para armazenar orçamento, ordem dos itens e estado recolhido.
- Migração SQLite incremental da versão anterior, preservando categorias e itens existentes.

### Build e documentação
- Versão sincronizada como `1.0.3+4` em `gradle.properties`, `github-manager.json`, `app_identity.json`, README e aplicativo.
- Workflow continua preparando/publicando somente `Lista-Nomade-v<VERSAO>.apk`; não gera `source.zip` personalizado nem usa `actions/upload-artifact`.
- Validação de versão e limite máximo de 500 linhas por arquivo Kotlin mantida antes do build.

## [1.0.2] - 2026-09-25

### Corrigido
- Aplicação das áreas seguras do Android nas telas principais, impedindo que o botão `Adicionar item` e outros controles fiquem atrás da barra de navegação ou próximos demais da barra de status.
- Cabeçalho principal reorganizado, com melhor espaçamento entre Informações, título do aplicativo e ação `+ Categoria`.
- Cartão de total geral e cartões de categoria compactados para aproveitar melhor a altura da tela.
- Contraste de textos secundários, estados e botões desabilitados aprimorado nos temas claro e escuro.

### Melhorado
- Tela de novo/editar item compactada: preço e quantidade agora ficam lado a lado, total ocupa menos espaço e a categoria possui indicador visual de seleção.
- Itens da lista agora concentram `Editar` e `Excluir` em um único menu, liberando espaço para nomes e valores maiores.
- Estados `Pendente` e `Comprado` possuem cores semanticamente diferentes e itens comprados mantêm indicação visual própria.
- Diálogo de adicionar/renomear categoria substituído por diálogo próprio, arredondado e consistente com o restante do aplicativo.
- Área de Doação foi reduzida e integrada a um cartão de apoio ao projeto, sem dominar visualmente a tela de configurações.

### Build e distribuição
- Workflow agora prepara e publica somente `Lista-Nomade-v<VERSAO>.apk`.
- Removida a geração do `Lista-Nomade-v<VERSAO>-source.zip` personalizado pelo GitHub Actions/Works.
- Adicionada validação para falhar caso a pasta `dist/` contenha qualquer arquivo além do APK da versão.

## [1.0.1] - 2026-09-25

### Corrigido
- Assinatura do GitHub Actions alinhada ao padrão de Secrets importado pelo GitHub Manager: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` e `KEY_PASSWORD`.
- Workflow mantém compatibilidade com os nomes antigos prefixados por `ANDROID_`, evitando quebra em repositórios já configurados.
- Validação de assinatura agora informa claramente os nomes aceitos antes de iniciar a compilação.

## [1.0.0] - 2026-09-25

### Adicionado
- Listas por categorias com `Bicicleta` criada automaticamente, total geral e totais por categoria.
- Cadastro de itens com preço unitário, quantidade, total automático, edição, exclusão e status comprado/não comprado.
- Persistência SQLite, tema claro/escuro, informações da versão, doação via cópia de Pix e integração com GitHub Manager/GitHub Release.

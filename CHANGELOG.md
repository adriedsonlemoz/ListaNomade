# Changelog

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

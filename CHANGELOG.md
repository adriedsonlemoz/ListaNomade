# Changelog

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

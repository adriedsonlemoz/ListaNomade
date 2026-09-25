# Assinatura permanente

O repositório não deve conter a chave privada. Gere uma única keystore e preserve-a permanentemente em local seguro.

## Secrets reconhecidos pelo GitHub Manager

Importe estes quatro Secrets usando o arquivo `.txt` fornecido separadamente:

- `KEYSTORE_BASE64`: conteúdo da keystore codificado em Base64.
- `KEYSTORE_PASSWORD`: senha da keystore.
- `KEY_ALIAS`: alias da chave.
- `KEY_PASSWORD`: senha da chave.

O workflow também aceita, por compatibilidade, os nomes antigos `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` e `ANDROID_KEY_PASSWORD`. Os nomes sem o prefixo `ANDROID_` são o padrão recomendado para o GitHub Manager.

Exemplo para gerar uma keystore uma única vez:

```bash
keytool -genkeypair -v -keystore lista-nomade-release.jks -alias lista-nomade -keyalg RSA -keysize 4096 -validity 10000
```

Exemplo para gerar o Base64 no Linux:

```bash
base64 -w 0 lista-nomade-release.jks > lista-nomade-release.base64.txt
```

Nunca troque a keystore entre versões, pois a assinatura identifica as atualizações do mesmo aplicativo Android.

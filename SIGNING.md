# Assinatura permanente

O repositório não deve conter a chave privada. Gere uma única keystore e preserve-a permanentemente em local seguro.

Configure estes GitHub Secrets:

- `ANDROID_KEYSTORE_BASE64`: conteúdo da keystore codificado em Base64.
- `ANDROID_KEYSTORE_PASSWORD`: senha da keystore.
- `ANDROID_KEY_ALIAS`: alias da chave.
- `ANDROID_KEY_PASSWORD`: senha da chave.

Exemplo para gerar a keystore uma única vez:

```bash
keytool -genkeypair -v -keystore lista-nomade-release.jks -alias lista-nomade -keyalg RSA -keysize 4096 -validity 10000
```

Exemplo para gerar o Base64 no Linux:

```bash
base64 -w 0 lista-nomade-release.jks > lista-nomade-release.base64.txt
```

Nunca troque a keystore entre versões, pois a assinatura identifica as atualizações do mesmo aplicativo Android.

plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
}

tasks.register("verifyVersionSync") {
    group = "verification"
    description = "Valida versionName/versionCode em todos os arquivos de identidade."
    doLast {
        val props = java.util.Properties().apply {
            rootProject.file("gradle.properties").inputStream().use(::load)
        }
        val versionName = props.getProperty("APP_VERSION_NAME")
        val versionCode = props.getProperty("APP_VERSION_CODE")
        val required = mapOf(
            "github-manager.json" to listOf("\"version\": \"$versionName\"", "\"versionName\": \"$versionName\"", "\"versionCode\": $versionCode"),
            "app_identity.json" to listOf("\"versionName\": \"$versionName\"", "\"versionCode\": $versionCode"),
            "README.md" to listOf("$versionName+$versionCode"),
            "CHANGELOG.md" to listOf("[$versionName]")
        )
        required.forEach { (path, tokens) ->
            val text = rootProject.file(path).readText()
            tokens.forEach { token ->
                check(text.contains(token)) { "Versao fora de sincronia em $path: esperado $token" }
            }
        }
        println("Versoes sincronizadas: $versionName ($versionCode)")
    }
}

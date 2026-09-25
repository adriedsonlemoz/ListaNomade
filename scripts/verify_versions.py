#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
props = {}
for line in (ROOT / "gradle.properties").read_text(encoding="utf-8").splitlines():
    if "=" in line and not line.lstrip().startswith("#"):
        key, value = line.split("=", 1)
        props[key.strip()] = value.strip()

version_name = props["APP_VERSION_NAME"]
version_code = int(props["APP_VERSION_CODE"])


def require(condition, message):
    if not condition:
        raise SystemExit(f"ERRO: {message}")


gm = json.loads((ROOT / "github-manager.json").read_text(encoding="utf-8"))
ai = json.loads((ROOT / "app_identity.json").read_text(encoding="utf-8"))
require(gm["version"] == version_name, "github-manager.json version")
require(gm["android"]["versionName"] == version_name, "github-manager.json versionName")
require(gm["android"]["versionCode"] == version_code, "github-manager.json versionCode")
require(ai["versionName"] == version_name, "app_identity.json versionName")
require(ai["versionCode"] == version_code, "app_identity.json versionCode")
require(f"{version_name}+{version_code}" in (ROOT / "README.md").read_text(encoding="utf-8"), "README.md")
require(f"[{version_name}]" in (ROOT / "CHANGELOG.md").read_text(encoding="utf-8"), "CHANGELOG.md")

workflow = (ROOT / ".github/workflows/release.yml").read_text(encoding="utf-8")
workflow_lower = workflow.lower()
require("source.zip" not in workflow_lower, "workflow não deve gerar source.zip personalizado")
require("git archive" not in workflow_lower, "workflow não deve empacotar o código-fonte")
require("actions/upload-artifact" not in workflow_lower, "workflow não deve usar upload-artifact")
require("Lista-Nomade-v${VERSION}.apk" in workflow, "workflow deve preparar o APK versionado")

for path in ROOT.rglob("*.kt"):
    lines = path.read_text(encoding="utf-8").count("\n") + 1
    require(lines <= 500, f"{path.relative_to(ROOT)} possui {lines} linhas (máximo 500)")

print(
    f"OK: versão {version_name} ({version_code}) sincronizada, "
    "workflow somente APK e arquivos Kotlin <= 500 linhas."
)

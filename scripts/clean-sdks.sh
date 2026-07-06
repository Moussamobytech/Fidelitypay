#!/bin/bash

#######################################################
# clean-sdks.sh
# Supprime tous les SDKs générés dans sdk/
# Le dossier openapi/ (spec JSON) est conservé.
#######################################################

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SDK_DIR="$ROOT_DIR/sdk"

echo "========================================="
echo "🧹 Nettoyage des SDKs générés..."
echo "========================================="

if [ ! -d "$SDK_DIR" ]; then
    echo "ℹ️  Dossier sdk/ introuvable, rien à nettoyer."
    exit 0
fi

for dir in "$SDK_DIR"/*/; do
    if [ -d "$dir" ]; then
        echo "  ➜ Suppression : $dir"
        rm -rf "$dir"
    fi
done

echo ""
echo "✅ Nettoyage terminé. Le dossier openapi/ est conservé."

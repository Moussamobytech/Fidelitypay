#!/bin/bash

############################################
# Configuration
############################################

PORT=8060
API_DOC="http://localhost:${PORT}/v3/api-docs/merchant-sdk"

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

OPENAPI_DIR="$ROOT_DIR/openapi"
SDK_DIR="$ROOT_DIR/sdk"

mkdir -p "$OPENAPI_DIR"
mkdir -p "$SDK_DIR"

echo "========================================="
echo "Export de OpenAPI..."
echo "========================================="

if ! curl --fail --silent "$API_DOC" -o "$OPENAPI_DIR/openapi.json"; then
    echo "❌ Erreur: impossible de récupérer OpenAPI"
    exit 1
fi

echo "✅ OpenAPI exporté avec succès."
echo

############################################
# SDK à générer
############################################

declare -A generators=(
    ["flutter"]="dart"
    ["java"]="java"
    ["javascript"]="javascript"
    ["typescript"]="typescript-axios"
    ["python"]="python"
    ["csharp"]="csharp"
    ["kotlin"]="kotlin"
    ["php"]="php"
)

declare -A status

############################################
# Génération
############################################

for sdk in "${!generators[@]}"
do
    echo "========================================="
    echo "🚀 Génération du SDK : $sdk"
    echo "========================================="

    rm -rf "$SDK_DIR/$sdk"

    if ! docker run --rm \
        --network host \
        -v "$ROOT_DIR:/local" \
        openapitools/openapi-generator-cli generate \
        -i /local/openapi/openapi.json \
        -g "${generators[$sdk]}" \
        -o "/local/sdk/$sdk" \
        --additional-properties npmName=fidelitypay-sdk,npmVersion=1.0.0,packageName=fidelitypay_sdk,projectName=fidelitypay-sdk; then

        echo "❌ Génération OpenAPI échouée : $sdk"
        status[$sdk]="FAILED"
        continue
    fi

    echo "📦 Compilation..."

    case $sdk in

        flutter)
            if command -v flutter >/dev/null 2>&1; then
                (
                    cd "$SDK_DIR/flutter" || exit 1
                    flutter pub get >/dev/null 2>&1
                    dart analyze >/dev/null 2>&1
                ) && status[$sdk]="OK" || status[$sdk]="FAILED"
            else
                status[$sdk]="SKIPPED (flutter missing)"
            fi
        ;;

        java)
            if command -v mvn >/dev/null 2>&1; then
                (
                    cd "$SDK_DIR/java" || exit 1
                    mvn -q clean package \
                        -Dmaven.javadoc.skip=true \
                        -DskipTests
                ) && status[$sdk]="OK" || status[$sdk]="FAILED"
            else
                status[$sdk]="SKIPPED (maven missing)"
            fi
        ;;

        javascript)
            if command -v npm >/dev/null 2>&1; then
                (
                    cd "$SDK_DIR/javascript" || exit 1
                    npm install >/dev/null 2>&1
                ) && status[$sdk]="OK" || status[$sdk]="FAILED"
            else
                status[$sdk]="SKIPPED (npm missing)"
            fi
        ;;

        typescript)
            if command -v npm >/dev/null 2>&1; then
                (
                    cd "$SDK_DIR/typescript" || exit 1
                    npm install >/dev/null 2>&1

                    if grep -q "\"build\"" package.json; then
                        npm run build >/dev/null 2>&1
                    fi
                ) && status[$sdk]="OK" || status[$sdk]="FAILED"
            else
                status[$sdk]="SKIPPED (npm missing)"
            fi
        ;;

        python)
            if command -v python3 >/dev/null 2>&1; then
                (
                    cd "$SDK_DIR/python" || exit 1
                    python3 -m compileall . >/dev/null 2>&1
                ) && status[$sdk]="OK" || status[$sdk]="FAILED"
            else
                status[$sdk]="SKIPPED (python missing)"
            fi
        ;;

        php|kotlin|csharp)
            status[$sdk]="GENERATED (no build step)"
        ;;
    esac

    echo "SDK $sdk : ${status[$sdk]}"
    echo
done

############################################
# RAPPORT FINAL
############################################

echo
echo "========================================="
echo "📊 RAPPORT FINAL"
echo "========================================="

for sdk in flutter java javascript typescript python csharp kotlin php
do
    printf "%-15s %s\n" "$sdk" "${status[$sdk]}"
done

echo
echo "🎉 Génération terminée."
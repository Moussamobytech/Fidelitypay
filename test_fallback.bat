@echo off
echo ========================================
echo TEST FALLBACK FIDELITYPAY
echo ========================================

REM Configuration des variables d'environnement pour le test local
REM PayDunya avec clés invalides pour forcer l'échec
set PAYDUNYA_API_BASE_URL=https://app.paydunya.com/api/v1
set PAYDUNYA_API_MASTER_KEY=invalid_master_key_test
set PAYDUNYA_API_PRIVATE_KEY=invalid_private_key_test
set PAYDUNYA_API_TOKEN=invalid_token_test

REM Kkiapay avec clés valides pour le fallback
set KKIAPAY_API_BASE_URL=https://api.kkiapay.me
set KKIAPAY_API_PUBLIC_KEY=f93862db9d083a3f670cc7c03b8a86b4ae37ee45
set KKIAPAY_API_PRIVATE_KEY=pk_9f6c5c776ed7765f29f1d5aaa0dbeeb30f2ba1f7472e538292b35f9d60b75ee5
set KKIAPAY_API_SECRET_KEY=sk_1db7af97df6a8f456bbf9aaae38e09882c29b578f73fc361f9179f0f8ebe54c
set KKIAPAY_CALLBACK_URL=http://localhost:8060/api/payments/callback/kkiapay

echo Variables d'environnement configurees:
echo PAYDUNYA_API_MASTER_KEY=%PAYDUNYA_API_MASTER_KEY%
echo KKIAPAY_API_PUBLIC_KEY=%KKIAPAY_API_PUBLIC_KEY%
echo.

echo Lancement de l'application...
echo (PayDunya va echouer avec les cles invalides, Kkiapay devrait prendre le relais automatiquement)
echo.

cd "C:\Users\Mondiale Informatiqu\Desktop\Fidelitypay (1)\Fidelitypay"
mvn spring-boot:run
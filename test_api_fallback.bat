@echo off
echo ========================================
echo TEST API FALLBACK FIDELITYPAY
echo ========================================

REM Démarrer l'application en arrière-plan
start "FidelityPay Test" cmd /c "cd C:\Users\Mondiale Informatiqu\Desktop\Fidelitypay (1)\Fidelitypay && test_fallback.bat"

echo Attente du démarrage de l'application...
timeout /t 10 /nobreak > nul

echo Test du fallback automatique...
echo.

REM Test avec un paiement Wave (devrait échouer sur PayDunya et réussir sur Kkiapay)
curl -X POST http://localhost:8060/api/payments/initiate ^
  -H "Content-Type: application/json" ^
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiaWF0IjoxNzEyNzQ4MDAwLCJleHAiOjE3MTI3NTE2MDB9.test" ^
  -d "{ ^
    \"amount\": 100.0, ^
    \"country\": \"SN\", ^
    \"operator\": \"WAVE\", ^
    \"phone\": \"221770000000\", ^
    \"firstname\": \"Test\", ^
    \"lastname\": \"User\", ^
    \"email\": \"test@example.com\" ^
  }"

echo.
echo Test terminé. Vérifiez les logs de l'application pour voir si le fallback a fonctionné.
pause
@echo off
echo Testing Fidelitypay API...
echo.

echo 1. Testing GET /api/payment-options...
curl -X GET "http://localhost:8060/api/payment-options?country=SN"
echo.
echo.

echo 2. Testing POST /api/payments/initiate...
curl -X POST "http://localhost:8060/api/payments/initiate" ^
     -H "Content-Type: application/json" ^
     -d "{\"amount\": 1000.0, \"country\": \"SN\", \"operator\": \"SamirPay\"}"
echo.
echo.

echo 3. Testing GET /api/payments/status/{paymentId} ...
echo (Note: Copy the 'paymentId' from the previous response to test a real ID)
echo Testing with a placeholder ID to check endpoint reachability:
curl -X GET "http://localhost:8060/api/payments/status/test-id-123"
echo.
echo.

echo Test complete.
pause

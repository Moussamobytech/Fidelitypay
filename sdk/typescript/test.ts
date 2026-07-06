import { Configuration, MerchantPayInApi, MerchantPaymentRequest } from './dist';

// Define the SDK configuration with the API key
const config = new Configuration({
    basePath: 'http://localhost:8060', // Address of the backend
    apiKey: 'your_secret_api_key',     // The API key header
});

// Instantiate the SDK's Merchant API client
const merchantApi = new MerchantPayInApi(config);

async function testSDK() {
    console.log("🚀 Testing FidelityPay Merchant SDK...");
    
    const request: MerchantPaymentRequest = {
        amount: 1000,
        currency: 'XOF',
        country: 'CI',
        operator: 'ORANGE',
        customer: {
            firstname: 'John',
            lastname: 'Doe',
            phone: '+2250102030405',
            email: 'john.doe@example.com'
        },
        returnUrl: 'https://myshop.com/success',
        cancelUrl: 'https://myshop.com/cancel'
    };

    try {
        console.log("Calling POST /api/v1/payments/initiate...");
        // Call the SDK method
        // Note: SDK signature typically takes (requestBody, additionalHeaders)
        // Check the generated api.ts for exact signatures if this fails.
        const response = await merchantApi.initiate('your_secret_api_key', 'unique-order-12345', request);
        
        console.log("✅ SDK Call Successful!");
        console.log("Payment ID:", response.data.paymentId);
        console.log("Status:", response.data.status);
        console.log("Payment URL:", response.data.paymentUrl);
        
    } catch (error: any) {
        if (error.response) {
            console.log("❌ API responded with an error:");
            console.log("Status Code:", error.response.status);
            console.log("Response Body:", error.response.data);
        } else {
            console.log("❌ SDK Request failed:", error.message);
        }
    }
}

testSDK();

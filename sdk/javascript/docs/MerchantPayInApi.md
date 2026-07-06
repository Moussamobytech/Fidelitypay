# FidelitypaySdk.MerchantPayInApi

All URIs are relative to *http://localhost:8060*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getPayment**](MerchantPayInApi.md#getPayment) | **GET** /api/v1/payments/{paymentId} | Read a merchant pay-in
[**initiate**](MerchantPayInApi.md#initiate) | **POST** /api/v1/payments/initiate | Initiate a merchant pay-in
[**submitOtp**](MerchantPayInApi.md#submitOtp) | **POST** /api/v1/payments/{paymentId}/actions/otp | Submit a required OTP action



## getPayment

> MerchantPaymentResponse getPayment(xAPIKey, paymentId)

Read a merchant pay-in

### Example

```javascript
import FidelitypaySdk from 'fidelitypay-sdk';
let defaultClient = FidelitypaySdk.ApiClient.instance;
// Configure API key authorization: apiKeyAuth
let apiKeyAuth = defaultClient.authentications['apiKeyAuth'];
apiKeyAuth.apiKey = 'YOUR API KEY';
// Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
//apiKeyAuth.apiKeyPrefix = 'Token';

let apiInstance = new FidelitypaySdk.MerchantPayInApi();
let xAPIKey = "xAPIKey_example"; // String | 
let paymentId = "paymentId_example"; // String | 
apiInstance.getPayment(xAPIKey, paymentId, (error, data, response) => {
  if (error) {
    console.error(error);
  } else {
    console.log('API called successfully. Returned data: ' + data);
  }
});
```

### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **xAPIKey** | **String**|  | 
 **paymentId** | **String**|  | 

### Return type

[**MerchantPaymentResponse**](MerchantPaymentResponse.md)

### Authorization

[apiKeyAuth](../README.md#apiKeyAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*


## initiate

> MerchantPaymentResponse initiate(xAPIKey, idempotencyKey, merchantPaymentRequest)

Initiate a merchant pay-in

Public API endpoint for merchant servers and SDKs. Uses X-API-Key and Idempotency-Key headers.

### Example

```javascript
import FidelitypaySdk from 'fidelitypay-sdk';
let defaultClient = FidelitypaySdk.ApiClient.instance;
// Configure API key authorization: apiKeyAuth
let apiKeyAuth = defaultClient.authentications['apiKeyAuth'];
apiKeyAuth.apiKey = 'YOUR API KEY';
// Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
//apiKeyAuth.apiKeyPrefix = 'Token';

let apiInstance = new FidelitypaySdk.MerchantPayInApi();
let xAPIKey = "xAPIKey_example"; // String | 
let idempotencyKey = "idempotencyKey_example"; // String | 
let merchantPaymentRequest = new FidelitypaySdk.MerchantPaymentRequest(); // MerchantPaymentRequest | 
apiInstance.initiate(xAPIKey, idempotencyKey, merchantPaymentRequest, (error, data, response) => {
  if (error) {
    console.error(error);
  } else {
    console.log('API called successfully. Returned data: ' + data);
  }
});
```

### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **xAPIKey** | **String**|  | 
 **idempotencyKey** | **String**|  | 
 **merchantPaymentRequest** | [**MerchantPaymentRequest**](MerchantPaymentRequest.md)|  | 

### Return type

[**MerchantPaymentResponse**](MerchantPaymentResponse.md)

### Authorization

[apiKeyAuth](../README.md#apiKeyAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: */*


## submitOtp

> MerchantPaymentResponse submitOtp(xAPIKey, paymentId, otpActionRequest)

Submit a required OTP action

### Example

```javascript
import FidelitypaySdk from 'fidelitypay-sdk';
let defaultClient = FidelitypaySdk.ApiClient.instance;
// Configure API key authorization: apiKeyAuth
let apiKeyAuth = defaultClient.authentications['apiKeyAuth'];
apiKeyAuth.apiKey = 'YOUR API KEY';
// Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
//apiKeyAuth.apiKeyPrefix = 'Token';

let apiInstance = new FidelitypaySdk.MerchantPayInApi();
let xAPIKey = "xAPIKey_example"; // String | 
let paymentId = "paymentId_example"; // String | 
let otpActionRequest = new FidelitypaySdk.OtpActionRequest(); // OtpActionRequest | 
apiInstance.submitOtp(xAPIKey, paymentId, otpActionRequest, (error, data, response) => {
  if (error) {
    console.error(error);
  } else {
    console.log('API called successfully. Returned data: ' + data);
  }
});
```

### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **xAPIKey** | **String**|  | 
 **paymentId** | **String**|  | 
 **otpActionRequest** | [**OtpActionRequest**](OtpActionRequest.md)|  | 

### Return type

[**MerchantPaymentResponse**](MerchantPaymentResponse.md)

### Authorization

[apiKeyAuth](../README.md#apiKeyAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: */*


# openapi.api.MerchantPayInApi

## Load the API package
```dart
import 'package:openapi/api.dart';
```

All URIs are relative to *http://localhost:8060*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getPayment**](MerchantPayInApi.md#getpayment) | **GET** /api/v1/payments/{paymentId} | Read a merchant pay-in
[**initiate**](MerchantPayInApi.md#initiate) | **POST** /api/v1/payments/initiate | Initiate a merchant pay-in
[**submitOtp**](MerchantPayInApi.md#submitotp) | **POST** /api/v1/payments/{paymentId}/actions/otp | Submit a required OTP action


# **getPayment**
> MerchantPaymentResponse getPayment(xAPIKey, paymentId)

Read a merchant pay-in

### Example
```dart
import 'package:openapi/api.dart';
// TODO Configure API key authorization: apiKeyAuth
//defaultApiClient.getAuthentication<ApiKeyAuth>('apiKeyAuth').apiKey = 'YOUR_API_KEY';
// uncomment below to setup prefix (e.g. Bearer) for API key, if needed
//defaultApiClient.getAuthentication<ApiKeyAuth>('apiKeyAuth').apiKeyPrefix = 'Bearer';

final api_instance = MerchantPayInApi();
final xAPIKey = xAPIKey_example; // String | 
final paymentId = paymentId_example; // String | 

try {
    final result = api_instance.getPayment(xAPIKey, paymentId);
    print(result);
} catch (e) {
    print('Exception when calling MerchantPayInApi->getPayment: $e\n');
}
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

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **initiate**
> MerchantPaymentResponse initiate(xAPIKey, idempotencyKey, merchantPaymentRequest)

Initiate a merchant pay-in

Public API endpoint for merchant servers and SDKs. Uses X-API-Key and Idempotency-Key headers.

### Example
```dart
import 'package:openapi/api.dart';
// TODO Configure API key authorization: apiKeyAuth
//defaultApiClient.getAuthentication<ApiKeyAuth>('apiKeyAuth').apiKey = 'YOUR_API_KEY';
// uncomment below to setup prefix (e.g. Bearer) for API key, if needed
//defaultApiClient.getAuthentication<ApiKeyAuth>('apiKeyAuth').apiKeyPrefix = 'Bearer';

final api_instance = MerchantPayInApi();
final xAPIKey = xAPIKey_example; // String | 
final idempotencyKey = idempotencyKey_example; // String | 
final merchantPaymentRequest = MerchantPaymentRequest(); // MerchantPaymentRequest | 

try {
    final result = api_instance.initiate(xAPIKey, idempotencyKey, merchantPaymentRequest);
    print(result);
} catch (e) {
    print('Exception when calling MerchantPayInApi->initiate: $e\n');
}
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

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **submitOtp**
> MerchantPaymentResponse submitOtp(xAPIKey, paymentId, otpActionRequest)

Submit a required OTP action

### Example
```dart
import 'package:openapi/api.dart';
// TODO Configure API key authorization: apiKeyAuth
//defaultApiClient.getAuthentication<ApiKeyAuth>('apiKeyAuth').apiKey = 'YOUR_API_KEY';
// uncomment below to setup prefix (e.g. Bearer) for API key, if needed
//defaultApiClient.getAuthentication<ApiKeyAuth>('apiKeyAuth').apiKeyPrefix = 'Bearer';

final api_instance = MerchantPayInApi();
final xAPIKey = xAPIKey_example; // String | 
final paymentId = paymentId_example; // String | 
final otpActionRequest = OtpActionRequest(); // OtpActionRequest | 

try {
    final result = api_instance.submitOtp(xAPIKey, paymentId, otpActionRequest);
    print(result);
} catch (e) {
    print('Exception when calling MerchantPayInApi->submitOtp: $e\n');
}
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

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)


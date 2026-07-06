# MerchantPayInApi

All URIs are relative to *http://localhost:8060*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**getPayment**](MerchantPayInApi.md#getPayment) | **GET** /api/v1/payments/{paymentId} | Read a merchant pay-in |
| [**initiate**](MerchantPayInApi.md#initiate) | **POST** /api/v1/payments/initiate | Initiate a merchant pay-in |
| [**submitOtp**](MerchantPayInApi.md#submitOtp) | **POST** /api/v1/payments/{paymentId}/actions/otp | Submit a required OTP action |


<a id="getPayment"></a>
# **getPayment**
> MerchantPaymentResponse getPayment(xAPIKey, paymentId)

Read a merchant pay-in

### Example
```kotlin
// Import classes:
//import fidelitypay_sdk.infrastructure.*
//import fidelitypay_sdk.models.*

val apiInstance = MerchantPayInApi()
val xAPIKey : kotlin.String = xAPIKey_example // kotlin.String | 
val paymentId : kotlin.String = paymentId_example // kotlin.String | 
try {
    val result : MerchantPaymentResponse = apiInstance.getPayment(xAPIKey, paymentId)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MerchantPayInApi#getPayment")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MerchantPayInApi#getPayment")
    e.printStackTrace()
}
```

### Parameters
| **xAPIKey** | **kotlin.String**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **paymentId** | **kotlin.String**|  | |

### Return type

[**MerchantPaymentResponse**](MerchantPaymentResponse.md)

### Authorization


Configure apiKeyAuth:
    ApiClient.apiKey["X-API-Key"] = ""
    ApiClient.apiKeyPrefix["X-API-Key"] = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

<a id="initiate"></a>
# **initiate**
> MerchantPaymentResponse initiate(xAPIKey, idempotencyKey, merchantPaymentRequest)

Initiate a merchant pay-in

Public API endpoint for merchant servers and SDKs. Uses X-API-Key and Idempotency-Key headers.

### Example
```kotlin
// Import classes:
//import fidelitypay_sdk.infrastructure.*
//import fidelitypay_sdk.models.*

val apiInstance = MerchantPayInApi()
val xAPIKey : kotlin.String = xAPIKey_example // kotlin.String | 
val idempotencyKey : kotlin.String = idempotencyKey_example // kotlin.String | 
val merchantPaymentRequest : MerchantPaymentRequest =  // MerchantPaymentRequest | 
try {
    val result : MerchantPaymentResponse = apiInstance.initiate(xAPIKey, idempotencyKey, merchantPaymentRequest)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MerchantPayInApi#initiate")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MerchantPayInApi#initiate")
    e.printStackTrace()
}
```

### Parameters
| **xAPIKey** | **kotlin.String**|  | |
| **idempotencyKey** | **kotlin.String**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **merchantPaymentRequest** | [**MerchantPaymentRequest**](MerchantPaymentRequest.md)|  | |

### Return type

[**MerchantPaymentResponse**](MerchantPaymentResponse.md)

### Authorization


Configure apiKeyAuth:
    ApiClient.apiKey["X-API-Key"] = ""
    ApiClient.apiKeyPrefix["X-API-Key"] = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: Not defined

<a id="submitOtp"></a>
# **submitOtp**
> MerchantPaymentResponse submitOtp(xAPIKey, paymentId, otpActionRequest)

Submit a required OTP action

### Example
```kotlin
// Import classes:
//import fidelitypay_sdk.infrastructure.*
//import fidelitypay_sdk.models.*

val apiInstance = MerchantPayInApi()
val xAPIKey : kotlin.String = xAPIKey_example // kotlin.String | 
val paymentId : kotlin.String = paymentId_example // kotlin.String | 
val otpActionRequest : OtpActionRequest =  // OtpActionRequest | 
try {
    val result : MerchantPaymentResponse = apiInstance.submitOtp(xAPIKey, paymentId, otpActionRequest)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MerchantPayInApi#submitOtp")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MerchantPayInApi#submitOtp")
    e.printStackTrace()
}
```

### Parameters
| **xAPIKey** | **kotlin.String**|  | |
| **paymentId** | **kotlin.String**|  | |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **otpActionRequest** | [**OtpActionRequest**](OtpActionRequest.md)|  | |

### Return type

[**MerchantPaymentResponse**](MerchantPaymentResponse.md)

### Authorization


Configure apiKeyAuth:
    ApiClient.apiKey["X-API-Key"] = ""
    ApiClient.apiKeyPrefix["X-API-Key"] = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: Not defined


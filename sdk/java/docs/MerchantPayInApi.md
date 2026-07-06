# MerchantPayInApi

All URIs are relative to *http://localhost:8060*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getPayment**](MerchantPayInApi.md#getPayment) | **GET** /api/v1/payments/{paymentId} | Read a merchant pay-in |
| [**initiate**](MerchantPayInApi.md#initiate) | **POST** /api/v1/payments/initiate | Initiate a merchant pay-in |
| [**submitOtp**](MerchantPayInApi.md#submitOtp) | **POST** /api/v1/payments/{paymentId}/actions/otp | Submit a required OTP action |


<a id="getPayment"></a>
# **getPayment**
> MerchantPaymentResponse getPayment(xAPIKey, paymentId)

Read a merchant pay-in

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.auth.*;
import org.openapitools.client.models.*;
import org.openapitools.client.api.MerchantPayInApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8060");
    
    // Configure API key authorization: apiKeyAuth
    ApiKeyAuth apiKeyAuth = (ApiKeyAuth) defaultClient.getAuthentication("apiKeyAuth");
    apiKeyAuth.setApiKey("YOUR API KEY");
    // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
    //apiKeyAuth.setApiKeyPrefix("Token");

    MerchantPayInApi apiInstance = new MerchantPayInApi(defaultClient);
    String xAPIKey = "xAPIKey_example"; // String | 
    String paymentId = "paymentId_example"; // String | 
    try {
      MerchantPaymentResponse result = apiInstance.getPayment(xAPIKey, paymentId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling MerchantPayInApi#getPayment");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters

| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **xAPIKey** | **String**|  | |
| **paymentId** | **String**|  | |

### Return type

[**MerchantPaymentResponse**](MerchantPaymentResponse.md)

### Authorization

[apiKeyAuth](../README.md#apiKeyAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

<a id="initiate"></a>
# **initiate**
> MerchantPaymentResponse initiate(xAPIKey, idempotencyKey, merchantPaymentRequest)

Initiate a merchant pay-in

Public API endpoint for merchant servers and SDKs. Uses X-API-Key and Idempotency-Key headers.

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.auth.*;
import org.openapitools.client.models.*;
import org.openapitools.client.api.MerchantPayInApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8060");
    
    // Configure API key authorization: apiKeyAuth
    ApiKeyAuth apiKeyAuth = (ApiKeyAuth) defaultClient.getAuthentication("apiKeyAuth");
    apiKeyAuth.setApiKey("YOUR API KEY");
    // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
    //apiKeyAuth.setApiKeyPrefix("Token");

    MerchantPayInApi apiInstance = new MerchantPayInApi(defaultClient);
    String xAPIKey = "xAPIKey_example"; // String | 
    String idempotencyKey = "idempotencyKey_example"; // String | 
    MerchantPaymentRequest merchantPaymentRequest = new MerchantPaymentRequest(); // MerchantPaymentRequest | 
    try {
      MerchantPaymentResponse result = apiInstance.initiate(xAPIKey, idempotencyKey, merchantPaymentRequest);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling MerchantPayInApi#initiate");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters

| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **xAPIKey** | **String**|  | |
| **idempotencyKey** | **String**|  | |
| **merchantPaymentRequest** | [**MerchantPaymentRequest**](MerchantPaymentRequest.md)|  | |

### Return type

[**MerchantPaymentResponse**](MerchantPaymentResponse.md)

### Authorization

[apiKeyAuth](../README.md#apiKeyAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

<a id="submitOtp"></a>
# **submitOtp**
> MerchantPaymentResponse submitOtp(xAPIKey, paymentId, otpActionRequest)

Submit a required OTP action

### Example
```java
// Import classes:
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.auth.*;
import org.openapitools.client.models.*;
import org.openapitools.client.api.MerchantPayInApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://localhost:8060");
    
    // Configure API key authorization: apiKeyAuth
    ApiKeyAuth apiKeyAuth = (ApiKeyAuth) defaultClient.getAuthentication("apiKeyAuth");
    apiKeyAuth.setApiKey("YOUR API KEY");
    // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
    //apiKeyAuth.setApiKeyPrefix("Token");

    MerchantPayInApi apiInstance = new MerchantPayInApi(defaultClient);
    String xAPIKey = "xAPIKey_example"; // String | 
    String paymentId = "paymentId_example"; // String | 
    OtpActionRequest otpActionRequest = new OtpActionRequest(); // OtpActionRequest | 
    try {
      MerchantPaymentResponse result = apiInstance.submitOtp(xAPIKey, paymentId, otpActionRequest);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling MerchantPayInApi#submitOtp");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters

| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **xAPIKey** | **String**|  | |
| **paymentId** | **String**|  | |
| **otpActionRequest** | [**OtpActionRequest**](OtpActionRequest.md)|  | |

### Return type

[**MerchantPaymentResponse**](MerchantPaymentResponse.md)

### Authorization

[apiKeyAuth](../README.md#apiKeyAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


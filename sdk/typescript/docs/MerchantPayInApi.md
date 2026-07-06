# MerchantPayInApi

All URIs are relative to *http://localhost:8060*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**getPayment**](#getpayment) | **GET** /api/v1/payments/{paymentId} | Read a merchant pay-in|
|[**initiate**](#initiate) | **POST** /api/v1/payments/initiate | Initiate a merchant pay-in|
|[**submitOtp**](#submitotp) | **POST** /api/v1/payments/{paymentId}/actions/otp | Submit a required OTP action|

# **getPayment**
> MerchantPaymentResponse getPayment()


### Example

```typescript
import {
    MerchantPayInApi,
    Configuration
} from 'fidelitypay-sdk';

const configuration = new Configuration();
const apiInstance = new MerchantPayInApi(configuration);

let xAPIKey: string; // (default to undefined)
let paymentId: string; // (default to undefined)

const { status, data } = await apiInstance.getPayment(
    xAPIKey,
    paymentId
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **xAPIKey** | [**string**] |  | defaults to undefined|
| **paymentId** | [**string**] |  | defaults to undefined|


### Return type

**MerchantPaymentResponse**

### Authorization

[apiKeyAuth](../README.md#apiKeyAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **initiate**
> MerchantPaymentResponse initiate(merchantPaymentRequest)

Public API endpoint for merchant servers and SDKs. Uses X-API-Key and Idempotency-Key headers.

### Example

```typescript
import {
    MerchantPayInApi,
    Configuration,
    MerchantPaymentRequest
} from 'fidelitypay-sdk';

const configuration = new Configuration();
const apiInstance = new MerchantPayInApi(configuration);

let xAPIKey: string; // (default to undefined)
let idempotencyKey: string; // (default to undefined)
let merchantPaymentRequest: MerchantPaymentRequest; //

const { status, data } = await apiInstance.initiate(
    xAPIKey,
    idempotencyKey,
    merchantPaymentRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **merchantPaymentRequest** | **MerchantPaymentRequest**|  | |
| **xAPIKey** | [**string**] |  | defaults to undefined|
| **idempotencyKey** | [**string**] |  | defaults to undefined|


### Return type

**MerchantPaymentResponse**

### Authorization

[apiKeyAuth](../README.md#apiKeyAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **submitOtp**
> MerchantPaymentResponse submitOtp(otpActionRequest)


### Example

```typescript
import {
    MerchantPayInApi,
    Configuration,
    OtpActionRequest
} from 'fidelitypay-sdk';

const configuration = new Configuration();
const apiInstance = new MerchantPayInApi(configuration);

let xAPIKey: string; // (default to undefined)
let paymentId: string; // (default to undefined)
let otpActionRequest: OtpActionRequest; //

const { status, data } = await apiInstance.submitOtp(
    xAPIKey,
    paymentId,
    otpActionRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **otpActionRequest** | **OtpActionRequest**|  | |
| **xAPIKey** | [**string**] |  | defaults to undefined|
| **paymentId** | [**string**] |  | defaults to undefined|


### Return type

**MerchantPaymentResponse**

### Authorization

[apiKeyAuth](../README.md#apiKeyAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)


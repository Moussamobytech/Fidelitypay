# fidelitypay_sdk.Api.MerchantPayInApi

All URIs are relative to *http://localhost:8060*

| Method | HTTP request | Description |
|--------|--------------|-------------|
| [**GetPayment**](MerchantPayInApi.md#getpayment) | **GET** /api/v1/payments/{paymentId} | Read a merchant pay-in |
| [**Initiate**](MerchantPayInApi.md#initiate) | **POST** /api/v1/payments/initiate | Initiate a merchant pay-in |
| [**SubmitOtp**](MerchantPayInApi.md#submitotp) | **POST** /api/v1/payments/{paymentId}/actions/otp | Submit a required OTP action |

<a id="getpayment"></a>
# **GetPayment**
> MerchantPaymentResponse GetPayment (string xAPIKey, string paymentId)

Read a merchant pay-in


### Parameters

| Name | Type | Description | Notes |
|------|------|-------------|-------|
| **xAPIKey** | **string** |  |  |
| **paymentId** | **string** |  |  |

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

[[Back to top]](#) [[Back to API list]](../../README.md#documentation-for-api-endpoints) [[Back to Model list]](../../README.md#documentation-for-models) [[Back to README]](../../README.md)

<a id="initiate"></a>
# **Initiate**
> MerchantPaymentResponse Initiate (string xAPIKey, string idempotencyKey, MerchantPaymentRequest merchantPaymentRequest)

Initiate a merchant pay-in

Public API endpoint for merchant servers and SDKs. Uses X-API-Key and Idempotency-Key headers.


### Parameters

| Name | Type | Description | Notes |
|------|------|-------------|-------|
| **xAPIKey** | **string** |  |  |
| **idempotencyKey** | **string** |  |  |
| **merchantPaymentRequest** | [**MerchantPaymentRequest**](MerchantPaymentRequest.md) |  |  |

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

[[Back to top]](#) [[Back to API list]](../../README.md#documentation-for-api-endpoints) [[Back to Model list]](../../README.md#documentation-for-models) [[Back to README]](../../README.md)

<a id="submitotp"></a>
# **SubmitOtp**
> MerchantPaymentResponse SubmitOtp (string xAPIKey, string paymentId, OtpActionRequest otpActionRequest)

Submit a required OTP action


### Parameters

| Name | Type | Description | Notes |
|------|------|-------------|-------|
| **xAPIKey** | **string** |  |  |
| **paymentId** | **string** |  |  |
| **otpActionRequest** | [**OtpActionRequest**](OtpActionRequest.md) |  |  |

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

[[Back to top]](#) [[Back to API list]](../../README.md#documentation-for-api-endpoints) [[Back to Model list]](../../README.md#documentation-for-models) [[Back to README]](../../README.md)


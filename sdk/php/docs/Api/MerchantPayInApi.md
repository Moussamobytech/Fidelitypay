# OpenAPI\Client\MerchantPayInApi



All URIs are relative to http://localhost:8060, except if the operation defines another base path.

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**getPayment()**](MerchantPayInApi.md#getPayment) | **GET** /api/v1/payments/{paymentId} | Read a merchant pay-in |
| [**initiate()**](MerchantPayInApi.md#initiate) | **POST** /api/v1/payments/initiate | Initiate a merchant pay-in |
| [**submitOtp()**](MerchantPayInApi.md#submitOtp) | **POST** /api/v1/payments/{paymentId}/actions/otp | Submit a required OTP action |


## `getPayment()`

```php
getPayment($x_api_key, $payment_id): \OpenAPI\Client\Model\MerchantPaymentResponse
```

Read a merchant pay-in

### Example

```php
<?php
require_once(__DIR__ . '/vendor/autoload.php');


// Configure API key authorization: apiKeyAuth
$config = OpenAPI\Client\Configuration::getDefaultConfiguration()->setApiKey('X-API-Key', 'YOUR_API_KEY');
// Uncomment below to setup prefix (e.g. Bearer) for API key, if needed
// $config = OpenAPI\Client\Configuration::getDefaultConfiguration()->setApiKeyPrefix('X-API-Key', 'Bearer');


$apiInstance = new OpenAPI\Client\Api\MerchantPayInApi(
    // If you want use custom http client, pass your client which implements `GuzzleHttp\ClientInterface`.
    // This is optional, `GuzzleHttp\Client` will be used as default.
    new GuzzleHttp\Client(),
    $config
);
$x_api_key = 'x_api_key_example'; // string
$payment_id = 'payment_id_example'; // string

try {
    $result = $apiInstance->getPayment($x_api_key, $payment_id);
    print_r($result);
} catch (Exception $e) {
    echo 'Exception when calling MerchantPayInApi->getPayment: ', $e->getMessage(), PHP_EOL;
}
```

### Parameters

| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **x_api_key** | **string**|  | |
| **payment_id** | **string**|  | |

### Return type

[**\OpenAPI\Client\Model\MerchantPaymentResponse**](../Model/MerchantPaymentResponse.md)

### Authorization

[apiKeyAuth](../../README.md#apiKeyAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `*/*`

[[Back to top]](#) [[Back to API list]](../../README.md#endpoints)
[[Back to Model list]](../../README.md#models)
[[Back to README]](../../README.md)

## `initiate()`

```php
initiate($x_api_key, $idempotency_key, $merchant_payment_request): \OpenAPI\Client\Model\MerchantPaymentResponse
```

Initiate a merchant pay-in

Public API endpoint for merchant servers and SDKs. Uses X-API-Key and Idempotency-Key headers.

### Example

```php
<?php
require_once(__DIR__ . '/vendor/autoload.php');


// Configure API key authorization: apiKeyAuth
$config = OpenAPI\Client\Configuration::getDefaultConfiguration()->setApiKey('X-API-Key', 'YOUR_API_KEY');
// Uncomment below to setup prefix (e.g. Bearer) for API key, if needed
// $config = OpenAPI\Client\Configuration::getDefaultConfiguration()->setApiKeyPrefix('X-API-Key', 'Bearer');


$apiInstance = new OpenAPI\Client\Api\MerchantPayInApi(
    // If you want use custom http client, pass your client which implements `GuzzleHttp\ClientInterface`.
    // This is optional, `GuzzleHttp\Client` will be used as default.
    new GuzzleHttp\Client(),
    $config
);
$x_api_key = 'x_api_key_example'; // string
$idempotency_key = 'idempotency_key_example'; // string
$merchant_payment_request = new \OpenAPI\Client\Model\MerchantPaymentRequest(); // \OpenAPI\Client\Model\MerchantPaymentRequest

try {
    $result = $apiInstance->initiate($x_api_key, $idempotency_key, $merchant_payment_request);
    print_r($result);
} catch (Exception $e) {
    echo 'Exception when calling MerchantPayInApi->initiate: ', $e->getMessage(), PHP_EOL;
}
```

### Parameters

| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **x_api_key** | **string**|  | |
| **idempotency_key** | **string**|  | |
| **merchant_payment_request** | [**\OpenAPI\Client\Model\MerchantPaymentRequest**](../Model/MerchantPaymentRequest.md)|  | |

### Return type

[**\OpenAPI\Client\Model\MerchantPaymentResponse**](../Model/MerchantPaymentResponse.md)

### Authorization

[apiKeyAuth](../../README.md#apiKeyAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `*/*`

[[Back to top]](#) [[Back to API list]](../../README.md#endpoints)
[[Back to Model list]](../../README.md#models)
[[Back to README]](../../README.md)

## `submitOtp()`

```php
submitOtp($x_api_key, $payment_id, $otp_action_request): \OpenAPI\Client\Model\MerchantPaymentResponse
```

Submit a required OTP action

### Example

```php
<?php
require_once(__DIR__ . '/vendor/autoload.php');


// Configure API key authorization: apiKeyAuth
$config = OpenAPI\Client\Configuration::getDefaultConfiguration()->setApiKey('X-API-Key', 'YOUR_API_KEY');
// Uncomment below to setup prefix (e.g. Bearer) for API key, if needed
// $config = OpenAPI\Client\Configuration::getDefaultConfiguration()->setApiKeyPrefix('X-API-Key', 'Bearer');


$apiInstance = new OpenAPI\Client\Api\MerchantPayInApi(
    // If you want use custom http client, pass your client which implements `GuzzleHttp\ClientInterface`.
    // This is optional, `GuzzleHttp\Client` will be used as default.
    new GuzzleHttp\Client(),
    $config
);
$x_api_key = 'x_api_key_example'; // string
$payment_id = 'payment_id_example'; // string
$otp_action_request = new \OpenAPI\Client\Model\OtpActionRequest(); // \OpenAPI\Client\Model\OtpActionRequest

try {
    $result = $apiInstance->submitOtp($x_api_key, $payment_id, $otp_action_request);
    print_r($result);
} catch (Exception $e) {
    echo 'Exception when calling MerchantPayInApi->submitOtp: ', $e->getMessage(), PHP_EOL;
}
```

### Parameters

| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **x_api_key** | **string**|  | |
| **payment_id** | **string**|  | |
| **otp_action_request** | [**\OpenAPI\Client\Model\OtpActionRequest**](../Model/OtpActionRequest.md)|  | |

### Return type

[**\OpenAPI\Client\Model\MerchantPaymentResponse**](../Model/MerchantPaymentResponse.md)

### Authorization

[apiKeyAuth](../../README.md#apiKeyAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `*/*`

[[Back to top]](#) [[Back to API list]](../../README.md#endpoints)
[[Back to Model list]](../../README.md#models)
[[Back to README]](../../README.md)

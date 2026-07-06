# fidelitypay_sdk.MerchantPayInApi

All URIs are relative to *http://localhost:8060*

Method | HTTP request | Description
------------- | ------------- | -------------
[**get_payment**](MerchantPayInApi.md#get_payment) | **GET** /api/v1/payments/{paymentId} | Read a merchant pay-in
[**initiate**](MerchantPayInApi.md#initiate) | **POST** /api/v1/payments/initiate | Initiate a merchant pay-in
[**submit_otp**](MerchantPayInApi.md#submit_otp) | **POST** /api/v1/payments/{paymentId}/actions/otp | Submit a required OTP action


# **get_payment**
> MerchantPaymentResponse get_payment(x_api_key, payment_id)

Read a merchant pay-in

### Example

* Api Key Authentication (apiKeyAuth):

```python
import fidelitypay_sdk
from fidelitypay_sdk.models.merchant_payment_response import MerchantPaymentResponse
from fidelitypay_sdk.rest import ApiException
from pprint import pprint

# Defining the host is optional and defaults to http://localhost:8060
# See configuration.py for a list of all supported configuration parameters.
configuration = fidelitypay_sdk.Configuration(
    host = "http://localhost:8060"
)

# The client must configure the authentication and authorization parameters
# in accordance with the API server security policy.
# Examples for each auth method are provided below, use the example that
# satisfies your auth use case.

# Configure API key authorization: apiKeyAuth
configuration.api_key['apiKeyAuth'] = os.environ["API_KEY"]

# Uncomment below to setup prefix (e.g. Bearer) for API key, if needed
# configuration.api_key_prefix['apiKeyAuth'] = 'Bearer'

# Enter a context with an instance of the API client
with fidelitypay_sdk.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = fidelitypay_sdk.MerchantPayInApi(api_client)
    x_api_key = 'x_api_key_example' # str | 
    payment_id = 'payment_id_example' # str | 

    try:
        # Read a merchant pay-in
        api_response = api_instance.get_payment(x_api_key, payment_id)
        print("The response of MerchantPayInApi->get_payment:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MerchantPayInApi->get_payment: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **x_api_key** | **str**|  | 
 **payment_id** | **str**|  | 

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
**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **initiate**
> MerchantPaymentResponse initiate(x_api_key, idempotency_key, merchant_payment_request)

Initiate a merchant pay-in

Public API endpoint for merchant servers and SDKs. Uses X-API-Key and Idempotency-Key headers.

### Example

* Api Key Authentication (apiKeyAuth):

```python
import fidelitypay_sdk
from fidelitypay_sdk.models.merchant_payment_request import MerchantPaymentRequest
from fidelitypay_sdk.models.merchant_payment_response import MerchantPaymentResponse
from fidelitypay_sdk.rest import ApiException
from pprint import pprint

# Defining the host is optional and defaults to http://localhost:8060
# See configuration.py for a list of all supported configuration parameters.
configuration = fidelitypay_sdk.Configuration(
    host = "http://localhost:8060"
)

# The client must configure the authentication and authorization parameters
# in accordance with the API server security policy.
# Examples for each auth method are provided below, use the example that
# satisfies your auth use case.

# Configure API key authorization: apiKeyAuth
configuration.api_key['apiKeyAuth'] = os.environ["API_KEY"]

# Uncomment below to setup prefix (e.g. Bearer) for API key, if needed
# configuration.api_key_prefix['apiKeyAuth'] = 'Bearer'

# Enter a context with an instance of the API client
with fidelitypay_sdk.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = fidelitypay_sdk.MerchantPayInApi(api_client)
    x_api_key = 'x_api_key_example' # str | 
    idempotency_key = 'idempotency_key_example' # str | 
    merchant_payment_request = fidelitypay_sdk.MerchantPaymentRequest() # MerchantPaymentRequest | 

    try:
        # Initiate a merchant pay-in
        api_response = api_instance.initiate(x_api_key, idempotency_key, merchant_payment_request)
        print("The response of MerchantPayInApi->initiate:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MerchantPayInApi->initiate: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **x_api_key** | **str**|  | 
 **idempotency_key** | **str**|  | 
 **merchant_payment_request** | [**MerchantPaymentRequest**](MerchantPaymentRequest.md)|  | 

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
**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **submit_otp**
> MerchantPaymentResponse submit_otp(x_api_key, payment_id, otp_action_request)

Submit a required OTP action

### Example

* Api Key Authentication (apiKeyAuth):

```python
import fidelitypay_sdk
from fidelitypay_sdk.models.merchant_payment_response import MerchantPaymentResponse
from fidelitypay_sdk.models.otp_action_request import OtpActionRequest
from fidelitypay_sdk.rest import ApiException
from pprint import pprint

# Defining the host is optional and defaults to http://localhost:8060
# See configuration.py for a list of all supported configuration parameters.
configuration = fidelitypay_sdk.Configuration(
    host = "http://localhost:8060"
)

# The client must configure the authentication and authorization parameters
# in accordance with the API server security policy.
# Examples for each auth method are provided below, use the example that
# satisfies your auth use case.

# Configure API key authorization: apiKeyAuth
configuration.api_key['apiKeyAuth'] = os.environ["API_KEY"]

# Uncomment below to setup prefix (e.g. Bearer) for API key, if needed
# configuration.api_key_prefix['apiKeyAuth'] = 'Bearer'

# Enter a context with an instance of the API client
with fidelitypay_sdk.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = fidelitypay_sdk.MerchantPayInApi(api_client)
    x_api_key = 'x_api_key_example' # str | 
    payment_id = 'payment_id_example' # str | 
    otp_action_request = fidelitypay_sdk.OtpActionRequest() # OtpActionRequest | 

    try:
        # Submit a required OTP action
        api_response = api_instance.submit_otp(x_api_key, payment_id, otp_action_request)
        print("The response of MerchantPayInApi->submit_otp:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MerchantPayInApi->submit_otp: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **x_api_key** | **str**|  | 
 **payment_id** | **str**|  | 
 **otp_action_request** | [**OtpActionRequest**](OtpActionRequest.md)|  | 

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
**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)


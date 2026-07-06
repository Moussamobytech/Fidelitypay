# MerchantPaymentRequest


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**amount** | **int** |  | [optional] 
**currency** | **str** |  | 
**country** | **str** |  | 
**operator** | **str** |  | 
**customer** | [**Customer**](Customer.md) |  | 
**return_url** | **str** |  | [optional] 
**cancel_url** | **str** |  | [optional] 

## Example

```python
from fidelitypay_sdk.models.merchant_payment_request import MerchantPaymentRequest

# TODO update the JSON string below
json = "{}"
# create an instance of MerchantPaymentRequest from a JSON string
merchant_payment_request_instance = MerchantPaymentRequest.from_json(json)
# print the JSON string representation of the object
print(MerchantPaymentRequest.to_json())

# convert the object into a dict
merchant_payment_request_dict = merchant_payment_request_instance.to_dict()
# create an instance of MerchantPaymentRequest from a dict
merchant_payment_request_from_dict = MerchantPaymentRequest.from_dict(merchant_payment_request_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)



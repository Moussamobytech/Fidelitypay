# MerchantPaymentResponse


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**payment_id** | **str** |  | [optional] 
**status** | **str** |  | [optional] 
**payment_url** | **str** |  | [optional] 
**provider** | **str** |  | [optional] 
**flow_type** | **str** |  | [optional] 
**operator** | **str** |  | [optional] 
**country** | **str** |  | [optional] 
**amount** | **float** |  | [optional] 
**currency** | **str** |  | [optional] 
**next_action** | [**NextAction**](NextAction.md) |  | [optional] 
**failure_reason** | **str** |  | [optional] 
**error_type** | **str** |  | [optional] 
**failure_stage** | **str** |  | [optional] 

## Example

```python
from fidelitypay_sdk.models.merchant_payment_response import MerchantPaymentResponse

# TODO update the JSON string below
json = "{}"
# create an instance of MerchantPaymentResponse from a JSON string
merchant_payment_response_instance = MerchantPaymentResponse.from_json(json)
# print the JSON string representation of the object
print(MerchantPaymentResponse.to_json())

# convert the object into a dict
merchant_payment_response_dict = merchant_payment_response_instance.to_dict()
# create an instance of MerchantPaymentResponse from a dict
merchant_payment_response_from_dict = MerchantPaymentResponse.from_dict(merchant_payment_response_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)



# OtpActionRequest


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**otp** | **str** |  | 

## Example

```python
from fidelitypay_sdk.models.otp_action_request import OtpActionRequest

# TODO update the JSON string below
json = "{}"
# create an instance of OtpActionRequest from a JSON string
otp_action_request_instance = OtpActionRequest.from_json(json)
# print the JSON string representation of the object
print(OtpActionRequest.to_json())

# convert the object into a dict
otp_action_request_dict = otp_action_request_instance.to_dict()
# create an instance of OtpActionRequest from a dict
otp_action_request_from_dict = OtpActionRequest.from_dict(otp_action_request_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)



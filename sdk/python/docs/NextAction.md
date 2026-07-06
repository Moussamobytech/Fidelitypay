# NextAction


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**type** | **str** |  | [optional] 
**provider** | **str** |  | [optional] 
**message** | **str** |  | [optional] 

## Example

```python
from fidelitypay_sdk.models.next_action import NextAction

# TODO update the JSON string below
json = "{}"
# create an instance of NextAction from a JSON string
next_action_instance = NextAction.from_json(json)
# print the JSON string representation of the object
print(NextAction.to_json())

# convert the object into a dict
next_action_dict = next_action_instance.to_dict()
# create an instance of NextAction from a dict
next_action_from_dict = NextAction.from_dict(next_action_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)



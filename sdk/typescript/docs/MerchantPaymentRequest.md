# MerchantPaymentRequest


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**amount** | **number** |  | [optional] [default to undefined]
**currency** | **string** |  | [default to undefined]
**country** | **string** |  | [default to undefined]
**operator** | **string** |  | [default to undefined]
**customer** | [**Customer**](Customer.md) |  | [default to undefined]
**returnUrl** | **string** |  | [optional] [default to undefined]
**cancelUrl** | **string** |  | [optional] [default to undefined]

## Example

```typescript
import { MerchantPaymentRequest } from 'fidelitypay-sdk';

const instance: MerchantPaymentRequest = {
    amount,
    currency,
    country,
    operator,
    customer,
    returnUrl,
    cancelUrl,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)

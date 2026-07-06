# MerchantPaymentResponse


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**paymentId** | **string** |  | [optional] [default to undefined]
**status** | **string** |  | [optional] [default to undefined]
**paymentUrl** | **string** |  | [optional] [default to undefined]
**provider** | **string** |  | [optional] [default to undefined]
**flowType** | **string** |  | [optional] [default to undefined]
**operator** | **string** |  | [optional] [default to undefined]
**country** | **string** |  | [optional] [default to undefined]
**amount** | **number** |  | [optional] [default to undefined]
**currency** | **string** |  | [optional] [default to undefined]
**nextAction** | [**NextAction**](NextAction.md) |  | [optional] [default to undefined]
**failureReason** | **string** |  | [optional] [default to undefined]
**errorType** | **string** |  | [optional] [default to undefined]
**failureStage** | **string** |  | [optional] [default to undefined]

## Example

```typescript
import { MerchantPaymentResponse } from 'fidelitypay-sdk';

const instance: MerchantPaymentResponse = {
    paymentId,
    status,
    paymentUrl,
    provider,
    flowType,
    operator,
    country,
    amount,
    currency,
    nextAction,
    failureReason,
    errorType,
    failureStage,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)

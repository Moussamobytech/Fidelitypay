
# MerchantPaymentResponse

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **paymentId** | **kotlin.String** |  |  [optional] |
| **status** | [**inline**](#Status) |  |  [optional] |
| **paymentUrl** | **kotlin.String** |  |  [optional] |
| **provider** | **kotlin.String** |  |  [optional] |
| **flowType** | **kotlin.String** |  |  [optional] |
| **&#x60;operator&#x60;** | **kotlin.String** |  |  [optional] |
| **country** | **kotlin.String** |  |  [optional] |
| **amount** | [**java.math.BigDecimal**](java.math.BigDecimal.md) |  |  [optional] |
| **currency** | **kotlin.String** |  |  [optional] |
| **nextAction** | [**NextAction**](NextAction.md) |  |  [optional] |
| **failureReason** | **kotlin.String** |  |  [optional] |
| **errorType** | [**inline**](#ErrorType) |  |  [optional] |
| **failureStage** | [**inline**](#FailureStage) |  |  [optional] |


<a id="Status"></a>
## Enum: status
| Name | Value |
| ---- | ----- |
| status | PENDING, REQUIRES_ACTION, PENDING_RECONCILIATION, SUCCESS, FAILED, CANCELLED |


<a id="ErrorType"></a>
## Enum: errorType
| Name | Value |
| ---- | ----- |
| errorType | NETWORK, TIMEOUT, AUTHENTICATION, PROVIDER_DOWN, BAD_REQUEST, INTERNAL_ERROR, UNKNOWN |


<a id="FailureStage"></a>
## Enum: failureStage
| Name | Value |
| ---- | ----- |
| failureStage | VALIDATION, ROUTING, PROVIDER_INIT, PROVIDER_ACTION, PROVIDER_CALLBACK, RECONCILIATION, INTERNAL, UNKNOWN |




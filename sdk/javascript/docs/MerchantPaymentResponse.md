# FidelitypaySdk.MerchantPaymentResponse

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**paymentId** | **String** |  | [optional] 
**status** | **String** |  | [optional] 
**paymentUrl** | **String** |  | [optional] 
**provider** | **String** |  | [optional] 
**flowType** | **String** |  | [optional] 
**operator** | **String** |  | [optional] 
**country** | **String** |  | [optional] 
**amount** | **Number** |  | [optional] 
**currency** | **String** |  | [optional] 
**nextAction** | [**NextAction**](NextAction.md) |  | [optional] 
**failureReason** | **String** |  | [optional] 
**errorType** | **String** |  | [optional] 
**failureStage** | **String** |  | [optional] 



## Enum: StatusEnum


* `PENDING` (value: `"PENDING"`)

* `REQUIRES_ACTION` (value: `"REQUIRES_ACTION"`)

* `PENDING_RECONCILIATION` (value: `"PENDING_RECONCILIATION"`)

* `SUCCESS` (value: `"SUCCESS"`)

* `FAILED` (value: `"FAILED"`)

* `CANCELLED` (value: `"CANCELLED"`)





## Enum: ErrorTypeEnum


* `NETWORK` (value: `"NETWORK"`)

* `TIMEOUT` (value: `"TIMEOUT"`)

* `AUTHENTICATION` (value: `"AUTHENTICATION"`)

* `PROVIDER_DOWN` (value: `"PROVIDER_DOWN"`)

* `BAD_REQUEST` (value: `"BAD_REQUEST"`)

* `INTERNAL_ERROR` (value: `"INTERNAL_ERROR"`)

* `UNKNOWN` (value: `"UNKNOWN"`)





## Enum: FailureStageEnum


* `VALIDATION` (value: `"VALIDATION"`)

* `ROUTING` (value: `"ROUTING"`)

* `PROVIDER_INIT` (value: `"PROVIDER_INIT"`)

* `PROVIDER_ACTION` (value: `"PROVIDER_ACTION"`)

* `PROVIDER_CALLBACK` (value: `"PROVIDER_CALLBACK"`)

* `RECONCILIATION` (value: `"RECONCILIATION"`)

* `INTERNAL` (value: `"INTERNAL"`)

* `UNKNOWN` (value: `"UNKNOWN"`)





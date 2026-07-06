

# MerchantPaymentResponse


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**paymentId** | **String** |  |  [optional] |
|**status** | [**StatusEnum**](#StatusEnum) |  |  [optional] |
|**paymentUrl** | **String** |  |  [optional] |
|**provider** | **String** |  |  [optional] |
|**flowType** | **String** |  |  [optional] |
|**operator** | **String** |  |  [optional] |
|**country** | **String** |  |  [optional] |
|**amount** | **BigDecimal** |  |  [optional] |
|**currency** | **String** |  |  [optional] |
|**nextAction** | [**NextAction**](NextAction.md) |  |  [optional] |
|**failureReason** | **String** |  |  [optional] |
|**errorType** | [**ErrorTypeEnum**](#ErrorTypeEnum) |  |  [optional] |
|**failureStage** | [**FailureStageEnum**](#FailureStageEnum) |  |  [optional] |



## Enum: StatusEnum

| Name | Value |
|---- | -----|
| PENDING | &quot;PENDING&quot; |
| REQUIRES_ACTION | &quot;REQUIRES_ACTION&quot; |
| PENDING_RECONCILIATION | &quot;PENDING_RECONCILIATION&quot; |
| SUCCESS | &quot;SUCCESS&quot; |
| FAILED | &quot;FAILED&quot; |
| CANCELLED | &quot;CANCELLED&quot; |



## Enum: ErrorTypeEnum

| Name | Value |
|---- | -----|
| NETWORK | &quot;NETWORK&quot; |
| TIMEOUT | &quot;TIMEOUT&quot; |
| AUTHENTICATION | &quot;AUTHENTICATION&quot; |
| PROVIDER_DOWN | &quot;PROVIDER_DOWN&quot; |
| BAD_REQUEST | &quot;BAD_REQUEST&quot; |
| INTERNAL_ERROR | &quot;INTERNAL_ERROR&quot; |
| UNKNOWN | &quot;UNKNOWN&quot; |



## Enum: FailureStageEnum

| Name | Value |
|---- | -----|
| VALIDATION | &quot;VALIDATION&quot; |
| ROUTING | &quot;ROUTING&quot; |
| PROVIDER_INIT | &quot;PROVIDER_INIT&quot; |
| PROVIDER_ACTION | &quot;PROVIDER_ACTION&quot; |
| PROVIDER_CALLBACK | &quot;PROVIDER_CALLBACK&quot; |
| RECONCILIATION | &quot;RECONCILIATION&quot; |
| INTERNAL | &quot;INTERNAL&quot; |
| UNKNOWN | &quot;UNKNOWN&quot; |




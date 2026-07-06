## fidelitypay-sdk@1.0.0

This generator creates TypeScript/JavaScript client that utilizes [axios](https://github.com/axios/axios). The generated Node module can be used in the following environments:

Environment
* Node.js
* Webpack
* Browserify

Language level
* ES5 - you must have a Promises/A+ library installed
* ES6

Module system
* CommonJS
* ES6 module system

It can be used in both TypeScript and JavaScript. In TypeScript, the definition will be automatically resolved via `package.json`. ([Reference](https://www.typescriptlang.org/docs/handbook/declaration-files/consumption.html))

### Building

To build and compile the typescript sources to javascript use:
```
npm install
npm run build
```

### Publishing

First build the package then run `npm publish`

### Consuming

navigate to the folder of your consuming project and run one of the following commands.

_published:_

```
npm install fidelitypay-sdk@1.0.0 --save
```

_unPublished (not recommended):_

```
npm install PATH_TO_GENERATED_PACKAGE --save
```

### Documentation for API Endpoints

All URIs are relative to *http://localhost:8060*

Class | Method | HTTP request | Description
------------ | ------------- | ------------- | -------------
*MerchantPayInApi* | [**getPayment**](docs/MerchantPayInApi.md#getpayment) | **GET** /api/v1/payments/{paymentId} | Read a merchant pay-in
*MerchantPayInApi* | [**initiate**](docs/MerchantPayInApi.md#initiate) | **POST** /api/v1/payments/initiate | Initiate a merchant pay-in
*MerchantPayInApi* | [**submitOtp**](docs/MerchantPayInApi.md#submitotp) | **POST** /api/v1/payments/{paymentId}/actions/otp | Submit a required OTP action


### Documentation For Models

 - [Customer](docs/Customer.md)
 - [MerchantPaymentRequest](docs/MerchantPaymentRequest.md)
 - [MerchantPaymentResponse](docs/MerchantPaymentResponse.md)
 - [NextAction](docs/NextAction.md)
 - [OtpActionRequest](docs/OtpActionRequest.md)


<a id="documentation-for-authorization"></a>
## Documentation For Authorization


Authentication schemes defined for the API:
<a id="bearerAuth"></a>
### bearerAuth

- **Type**: Bearer authentication (JWT)

<a id="apiKeyAuth"></a>
### apiKeyAuth

- **Type**: API key
- **API key parameter name**: X-API-Key
- **Location**: HTTP header


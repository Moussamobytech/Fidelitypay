//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;


class MerchantPayInApi {
  MerchantPayInApi([ApiClient? apiClient]) : apiClient = apiClient ?? defaultApiClient;

  final ApiClient apiClient;

  /// Read a merchant pay-in
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] xAPIKey (required):
  ///
  /// * [String] paymentId (required):
  Future<Response> getPaymentWithHttpInfo(String xAPIKey, String paymentId, { Future<void>? abortTrigger, }) async {
    // ignore: prefer_const_declarations
    final path = r'/api/v1/payments/{paymentId}'
      .replaceAll('{paymentId}', paymentId);

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    headerParams[r'X-API-Key'] = parameterToString(xAPIKey);

    const contentTypes = <String>[];


    return apiClient.invokeAPI(
      path,
      'GET',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
      abortTrigger: abortTrigger,
    );
  }

  /// Read a merchant pay-in
  ///
  /// Parameters:
  ///
  /// * [String] xAPIKey (required):
  ///
  /// * [String] paymentId (required):
  Future<MerchantPaymentResponse?> getPayment(String xAPIKey, String paymentId, { Future<void>? abortTrigger, }) async {
    final response = await getPaymentWithHttpInfo(xAPIKey, paymentId, abortTrigger: abortTrigger,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'MerchantPaymentResponse',) as MerchantPaymentResponse;
    
    }
    return null;
  }

  /// Initiate a merchant pay-in
  ///
  /// Public API endpoint for merchant servers and SDKs. Uses X-API-Key and Idempotency-Key headers.
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] xAPIKey (required):
  ///
  /// * [String] idempotencyKey (required):
  ///
  /// * [MerchantPaymentRequest] merchantPaymentRequest (required):
  Future<Response> initiateWithHttpInfo(String xAPIKey, String idempotencyKey, MerchantPaymentRequest merchantPaymentRequest, { Future<void>? abortTrigger, }) async {
    // ignore: prefer_const_declarations
    final path = r'/api/v1/payments/initiate';

    // ignore: prefer_final_locals
    Object? postBody = merchantPaymentRequest;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    headerParams[r'X-API-Key'] = parameterToString(xAPIKey);
    headerParams[r'Idempotency-Key'] = parameterToString(idempotencyKey);

    const contentTypes = <String>['application/json'];


    return apiClient.invokeAPI(
      path,
      'POST',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
      abortTrigger: abortTrigger,
    );
  }

  /// Initiate a merchant pay-in
  ///
  /// Public API endpoint for merchant servers and SDKs. Uses X-API-Key and Idempotency-Key headers.
  ///
  /// Parameters:
  ///
  /// * [String] xAPIKey (required):
  ///
  /// * [String] idempotencyKey (required):
  ///
  /// * [MerchantPaymentRequest] merchantPaymentRequest (required):
  Future<MerchantPaymentResponse?> initiate(String xAPIKey, String idempotencyKey, MerchantPaymentRequest merchantPaymentRequest, { Future<void>? abortTrigger, }) async {
    final response = await initiateWithHttpInfo(xAPIKey, idempotencyKey, merchantPaymentRequest, abortTrigger: abortTrigger,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'MerchantPaymentResponse',) as MerchantPaymentResponse;
    
    }
    return null;
  }

  /// Submit a required OTP action
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] xAPIKey (required):
  ///
  /// * [String] paymentId (required):
  ///
  /// * [OtpActionRequest] otpActionRequest (required):
  Future<Response> submitOtpWithHttpInfo(String xAPIKey, String paymentId, OtpActionRequest otpActionRequest, { Future<void>? abortTrigger, }) async {
    // ignore: prefer_const_declarations
    final path = r'/api/v1/payments/{paymentId}/actions/otp'
      .replaceAll('{paymentId}', paymentId);

    // ignore: prefer_final_locals
    Object? postBody = otpActionRequest;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    headerParams[r'X-API-Key'] = parameterToString(xAPIKey);

    const contentTypes = <String>['application/json'];


    return apiClient.invokeAPI(
      path,
      'POST',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
      abortTrigger: abortTrigger,
    );
  }

  /// Submit a required OTP action
  ///
  /// Parameters:
  ///
  /// * [String] xAPIKey (required):
  ///
  /// * [String] paymentId (required):
  ///
  /// * [OtpActionRequest] otpActionRequest (required):
  Future<MerchantPaymentResponse?> submitOtp(String xAPIKey, String paymentId, OtpActionRequest otpActionRequest, { Future<void>? abortTrigger, }) async {
    final response = await submitOtpWithHttpInfo(xAPIKey, paymentId, otpActionRequest, abortTrigger: abortTrigger,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'MerchantPaymentResponse',) as MerchantPaymentResponse;
    
    }
    return null;
  }
}

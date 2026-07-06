//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class MerchantPaymentRequest {
  /// Returns a new [MerchantPaymentRequest] instance.
  MerchantPaymentRequest({
    this.amount,
    required this.currency,
    required this.country,
    required this.operator_,
    required this.customer,
    this.returnUrl,
    this.cancelUrl,
  });

  /// Minimum value: 1
  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? amount;

  String currency;

  String country;

  String operator_;

  Customer customer;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? returnUrl;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? cancelUrl;

  @override
  bool operator ==(Object other) => identical(this, other) || other is MerchantPaymentRequest &&
    other.amount == amount &&
    other.currency == currency &&
    other.country == country &&
    other.operator_ == operator_ &&
    other.customer == customer &&
    other.returnUrl == returnUrl &&
    other.cancelUrl == cancelUrl;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (amount == null ? 0 : amount!.hashCode) +
    (currency.hashCode) +
    (country.hashCode) +
    (operator_.hashCode) +
    (customer.hashCode) +
    (returnUrl == null ? 0 : returnUrl!.hashCode) +
    (cancelUrl == null ? 0 : cancelUrl!.hashCode);

  @override
  String toString() => 'MerchantPaymentRequest[amount=$amount, currency=$currency, country=$country, operator_=$operator_, customer=$customer, returnUrl=$returnUrl, cancelUrl=$cancelUrl]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.amount != null) {
      json[r'amount'] = this.amount;
    } else {
      json[r'amount'] = null;
    }
      json[r'currency'] = this.currency;
      json[r'country'] = this.country;
      json[r'operator'] = this.operator_;
      json[r'customer'] = this.customer;
    if (this.returnUrl != null) {
      json[r'returnUrl'] = this.returnUrl;
    } else {
      json[r'returnUrl'] = null;
    }
    if (this.cancelUrl != null) {
      json[r'cancelUrl'] = this.cancelUrl;
    } else {
      json[r'cancelUrl'] = null;
    }
    return json;
  }

  /// Returns a new [MerchantPaymentRequest] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static MerchantPaymentRequest? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        assert(json.containsKey(r'currency'), 'Required key "MerchantPaymentRequest[currency]" is missing from JSON.');
        assert(json[r'currency'] != null, 'Required key "MerchantPaymentRequest[currency]" has a null value in JSON.');
        assert(json.containsKey(r'country'), 'Required key "MerchantPaymentRequest[country]" is missing from JSON.');
        assert(json[r'country'] != null, 'Required key "MerchantPaymentRequest[country]" has a null value in JSON.');
        assert(json.containsKey(r'operator'), 'Required key "MerchantPaymentRequest[operator]" is missing from JSON.');
        assert(json[r'operator'] != null, 'Required key "MerchantPaymentRequest[operator]" has a null value in JSON.');
        assert(json.containsKey(r'customer'), 'Required key "MerchantPaymentRequest[customer]" is missing from JSON.');
        assert(json[r'customer'] != null, 'Required key "MerchantPaymentRequest[customer]" has a null value in JSON.');
        return true;
      }());

      return MerchantPaymentRequest(
        amount: mapValueOfType<int>(json, r'amount'),
        currency: mapValueOfType<String>(json, r'currency')!,
        country: mapValueOfType<String>(json, r'country')!,
        operator_: mapValueOfType<String>(json, r'operator')!,
        customer: Customer.fromJson(json[r'customer'])!,
        returnUrl: mapValueOfType<String>(json, r'returnUrl'),
        cancelUrl: mapValueOfType<String>(json, r'cancelUrl'),
      );
    }
    return null;
  }

  static List<MerchantPaymentRequest> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <MerchantPaymentRequest>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = MerchantPaymentRequest.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, MerchantPaymentRequest> mapFromJson(dynamic json) {
    final map = <String, MerchantPaymentRequest>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = MerchantPaymentRequest.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of MerchantPaymentRequest-objects as value to a dart map
  static Map<String, List<MerchantPaymentRequest>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<MerchantPaymentRequest>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = MerchantPaymentRequest.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
    'currency',
    'country',
    'operator',
    'customer',
  };
}


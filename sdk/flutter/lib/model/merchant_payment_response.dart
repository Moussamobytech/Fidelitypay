//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class MerchantPaymentResponse {
  /// Returns a new [MerchantPaymentResponse] instance.
  MerchantPaymentResponse({
    this.paymentId,
    this.status,
    this.paymentUrl,
    this.provider,
    this.flowType,
    this.operator_,
    this.country,
    this.amount,
    this.currency,
    this.nextAction,
    this.failureReason,
    this.errorType,
    this.failureStage,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? paymentId;

  MerchantPaymentResponseStatusEnum? status;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? paymentUrl;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? provider;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? flowType;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? operator_;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? country;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  num? amount;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? currency;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  NextAction? nextAction;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? failureReason;

  MerchantPaymentResponseErrorTypeEnum? errorType;

  MerchantPaymentResponseFailureStageEnum? failureStage;

  @override
  bool operator ==(Object other) => identical(this, other) || other is MerchantPaymentResponse &&
    other.paymentId == paymentId &&
    other.status == status &&
    other.paymentUrl == paymentUrl &&
    other.provider == provider &&
    other.flowType == flowType &&
    other.operator_ == operator_ &&
    other.country == country &&
    other.amount == amount &&
    other.currency == currency &&
    other.nextAction == nextAction &&
    other.failureReason == failureReason &&
    other.errorType == errorType &&
    other.failureStage == failureStage;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (paymentId == null ? 0 : paymentId!.hashCode) +
    (status == null ? 0 : status!.hashCode) +
    (paymentUrl == null ? 0 : paymentUrl!.hashCode) +
    (provider == null ? 0 : provider!.hashCode) +
    (flowType == null ? 0 : flowType!.hashCode) +
    (operator_ == null ? 0 : operator_!.hashCode) +
    (country == null ? 0 : country!.hashCode) +
    (amount == null ? 0 : amount!.hashCode) +
    (currency == null ? 0 : currency!.hashCode) +
    (nextAction == null ? 0 : nextAction!.hashCode) +
    (failureReason == null ? 0 : failureReason!.hashCode) +
    (errorType == null ? 0 : errorType!.hashCode) +
    (failureStage == null ? 0 : failureStage!.hashCode);

  @override
  String toString() => 'MerchantPaymentResponse[paymentId=$paymentId, status=$status, paymentUrl=$paymentUrl, provider=$provider, flowType=$flowType, operator_=$operator_, country=$country, amount=$amount, currency=$currency, nextAction=$nextAction, failureReason=$failureReason, errorType=$errorType, failureStage=$failureStage]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.paymentId != null) {
      json[r'paymentId'] = this.paymentId;
    } else {
      json[r'paymentId'] = null;
    }
    if (this.status != null) {
      json[r'status'] = this.status;
    } else {
      json[r'status'] = null;
    }
    if (this.paymentUrl != null) {
      json[r'paymentUrl'] = this.paymentUrl;
    } else {
      json[r'paymentUrl'] = null;
    }
    if (this.provider != null) {
      json[r'provider'] = this.provider;
    } else {
      json[r'provider'] = null;
    }
    if (this.flowType != null) {
      json[r'flowType'] = this.flowType;
    } else {
      json[r'flowType'] = null;
    }
    if (this.operator_ != null) {
      json[r'operator'] = this.operator_;
    } else {
      json[r'operator'] = null;
    }
    if (this.country != null) {
      json[r'country'] = this.country;
    } else {
      json[r'country'] = null;
    }
    if (this.amount != null) {
      json[r'amount'] = this.amount;
    } else {
      json[r'amount'] = null;
    }
    if (this.currency != null) {
      json[r'currency'] = this.currency;
    } else {
      json[r'currency'] = null;
    }
    if (this.nextAction != null) {
      json[r'nextAction'] = this.nextAction;
    } else {
      json[r'nextAction'] = null;
    }
    if (this.failureReason != null) {
      json[r'failureReason'] = this.failureReason;
    } else {
      json[r'failureReason'] = null;
    }
    if (this.errorType != null) {
      json[r'errorType'] = this.errorType;
    } else {
      json[r'errorType'] = null;
    }
    if (this.failureStage != null) {
      json[r'failureStage'] = this.failureStage;
    } else {
      json[r'failureStage'] = null;
    }
    return json;
  }

  /// Returns a new [MerchantPaymentResponse] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static MerchantPaymentResponse? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        return true;
      }());

      return MerchantPaymentResponse(
        paymentId: mapValueOfType<String>(json, r'paymentId'),
        status: MerchantPaymentResponseStatusEnum.fromJson(json[r'status']),
        paymentUrl: mapValueOfType<String>(json, r'paymentUrl'),
        provider: mapValueOfType<String>(json, r'provider'),
        flowType: mapValueOfType<String>(json, r'flowType'),
        operator_: mapValueOfType<String>(json, r'operator'),
        country: mapValueOfType<String>(json, r'country'),
        amount: num.parse('${json[r'amount']}'),
        currency: mapValueOfType<String>(json, r'currency'),
        nextAction: NextAction.fromJson(json[r'nextAction']),
        failureReason: mapValueOfType<String>(json, r'failureReason'),
        errorType: MerchantPaymentResponseErrorTypeEnum.fromJson(json[r'errorType']),
        failureStage: MerchantPaymentResponseFailureStageEnum.fromJson(json[r'failureStage']),
      );
    }
    return null;
  }

  static List<MerchantPaymentResponse> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <MerchantPaymentResponse>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = MerchantPaymentResponse.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, MerchantPaymentResponse> mapFromJson(dynamic json) {
    final map = <String, MerchantPaymentResponse>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = MerchantPaymentResponse.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of MerchantPaymentResponse-objects as value to a dart map
  static Map<String, List<MerchantPaymentResponse>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<MerchantPaymentResponse>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = MerchantPaymentResponse.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}


class MerchantPaymentResponseStatusEnum {
  /// Instantiate a new enum with the provided [value].
  const MerchantPaymentResponseStatusEnum._(this.value);

  /// The underlying value of this enum member.
  final String value;

  @override
  String toString() => value;

  String toJson() => value;

  static const PENDING = MerchantPaymentResponseStatusEnum._(r'PENDING');
  static const REQUIRES_ACTION = MerchantPaymentResponseStatusEnum._(r'REQUIRES_ACTION');
  static const PENDING_RECONCILIATION = MerchantPaymentResponseStatusEnum._(r'PENDING_RECONCILIATION');
  static const SUCCESS = MerchantPaymentResponseStatusEnum._(r'SUCCESS');
  static const FAILED = MerchantPaymentResponseStatusEnum._(r'FAILED');
  static const CANCELLED = MerchantPaymentResponseStatusEnum._(r'CANCELLED');

  /// List of all possible values in this [enum][MerchantPaymentResponseStatusEnum].
  static const values = <MerchantPaymentResponseStatusEnum>[
    PENDING,
    REQUIRES_ACTION,
    PENDING_RECONCILIATION,
    SUCCESS,
    FAILED,
    CANCELLED,
  ];

  static MerchantPaymentResponseStatusEnum? fromJson(dynamic value) => MerchantPaymentResponseStatusEnumTypeTransformer().decode(value);

  static List<MerchantPaymentResponseStatusEnum> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <MerchantPaymentResponseStatusEnum>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = MerchantPaymentResponseStatusEnum.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }
}

/// Transformation class that can [encode] an instance of [MerchantPaymentResponseStatusEnum] to String,
/// and [decode] dynamic data back to [MerchantPaymentResponseStatusEnum].
class MerchantPaymentResponseStatusEnumTypeTransformer {
  factory MerchantPaymentResponseStatusEnumTypeTransformer() => _instance ??= const MerchantPaymentResponseStatusEnumTypeTransformer._();

  const MerchantPaymentResponseStatusEnumTypeTransformer._();

  String encode(MerchantPaymentResponseStatusEnum data) => data.value;

  /// Decodes a [dynamic value][data] to a MerchantPaymentResponseStatusEnum.
  ///
  /// If [allowNull] is true and the [dynamic value][data] cannot be decoded successfully,
  /// then null is returned. However, if [allowNull] is false and the [dynamic value][data]
  /// cannot be decoded successfully, then an [UnimplementedError] is thrown.
  ///
  /// The [allowNull] is very handy when an API changes and a new enum value is added or removed,
  /// and users are still using an old app with the old code.
  MerchantPaymentResponseStatusEnum? decode(dynamic data, {bool allowNull = true}) {
    if (data != null) {
      switch (data) {
        case r'PENDING': return MerchantPaymentResponseStatusEnum.PENDING;
        case r'REQUIRES_ACTION': return MerchantPaymentResponseStatusEnum.REQUIRES_ACTION;
        case r'PENDING_RECONCILIATION': return MerchantPaymentResponseStatusEnum.PENDING_RECONCILIATION;
        case r'SUCCESS': return MerchantPaymentResponseStatusEnum.SUCCESS;
        case r'FAILED': return MerchantPaymentResponseStatusEnum.FAILED;
        case r'CANCELLED': return MerchantPaymentResponseStatusEnum.CANCELLED;
        default:
          if (!allowNull) {
            throw ArgumentError('Unknown enum value to decode: $data');
          }
      }
    }
    return null;
  }

  /// Singleton [MerchantPaymentResponseStatusEnumTypeTransformer] instance.
  static MerchantPaymentResponseStatusEnumTypeTransformer? _instance;
}



class MerchantPaymentResponseErrorTypeEnum {
  /// Instantiate a new enum with the provided [value].
  const MerchantPaymentResponseErrorTypeEnum._(this.value);

  /// The underlying value of this enum member.
  final String value;

  @override
  String toString() => value;

  String toJson() => value;

  static const NETWORK = MerchantPaymentResponseErrorTypeEnum._(r'NETWORK');
  static const TIMEOUT = MerchantPaymentResponseErrorTypeEnum._(r'TIMEOUT');
  static const AUTHENTICATION = MerchantPaymentResponseErrorTypeEnum._(r'AUTHENTICATION');
  static const PROVIDER_DOWN = MerchantPaymentResponseErrorTypeEnum._(r'PROVIDER_DOWN');
  static const BAD_REQUEST = MerchantPaymentResponseErrorTypeEnum._(r'BAD_REQUEST');
  static const INTERNAL_ERROR = MerchantPaymentResponseErrorTypeEnum._(r'INTERNAL_ERROR');
  static const UNKNOWN = MerchantPaymentResponseErrorTypeEnum._(r'UNKNOWN');

  /// List of all possible values in this [enum][MerchantPaymentResponseErrorTypeEnum].
  static const values = <MerchantPaymentResponseErrorTypeEnum>[
    NETWORK,
    TIMEOUT,
    AUTHENTICATION,
    PROVIDER_DOWN,
    BAD_REQUEST,
    INTERNAL_ERROR,
    UNKNOWN,
  ];

  static MerchantPaymentResponseErrorTypeEnum? fromJson(dynamic value) => MerchantPaymentResponseErrorTypeEnumTypeTransformer().decode(value);

  static List<MerchantPaymentResponseErrorTypeEnum> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <MerchantPaymentResponseErrorTypeEnum>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = MerchantPaymentResponseErrorTypeEnum.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }
}

/// Transformation class that can [encode] an instance of [MerchantPaymentResponseErrorTypeEnum] to String,
/// and [decode] dynamic data back to [MerchantPaymentResponseErrorTypeEnum].
class MerchantPaymentResponseErrorTypeEnumTypeTransformer {
  factory MerchantPaymentResponseErrorTypeEnumTypeTransformer() => _instance ??= const MerchantPaymentResponseErrorTypeEnumTypeTransformer._();

  const MerchantPaymentResponseErrorTypeEnumTypeTransformer._();

  String encode(MerchantPaymentResponseErrorTypeEnum data) => data.value;

  /// Decodes a [dynamic value][data] to a MerchantPaymentResponseErrorTypeEnum.
  ///
  /// If [allowNull] is true and the [dynamic value][data] cannot be decoded successfully,
  /// then null is returned. However, if [allowNull] is false and the [dynamic value][data]
  /// cannot be decoded successfully, then an [UnimplementedError] is thrown.
  ///
  /// The [allowNull] is very handy when an API changes and a new enum value is added or removed,
  /// and users are still using an old app with the old code.
  MerchantPaymentResponseErrorTypeEnum? decode(dynamic data, {bool allowNull = true}) {
    if (data != null) {
      switch (data) {
        case r'NETWORK': return MerchantPaymentResponseErrorTypeEnum.NETWORK;
        case r'TIMEOUT': return MerchantPaymentResponseErrorTypeEnum.TIMEOUT;
        case r'AUTHENTICATION': return MerchantPaymentResponseErrorTypeEnum.AUTHENTICATION;
        case r'PROVIDER_DOWN': return MerchantPaymentResponseErrorTypeEnum.PROVIDER_DOWN;
        case r'BAD_REQUEST': return MerchantPaymentResponseErrorTypeEnum.BAD_REQUEST;
        case r'INTERNAL_ERROR': return MerchantPaymentResponseErrorTypeEnum.INTERNAL_ERROR;
        case r'UNKNOWN': return MerchantPaymentResponseErrorTypeEnum.UNKNOWN;
        default:
          if (!allowNull) {
            throw ArgumentError('Unknown enum value to decode: $data');
          }
      }
    }
    return null;
  }

  /// Singleton [MerchantPaymentResponseErrorTypeEnumTypeTransformer] instance.
  static MerchantPaymentResponseErrorTypeEnumTypeTransformer? _instance;
}



class MerchantPaymentResponseFailureStageEnum {
  /// Instantiate a new enum with the provided [value].
  const MerchantPaymentResponseFailureStageEnum._(this.value);

  /// The underlying value of this enum member.
  final String value;

  @override
  String toString() => value;

  String toJson() => value;

  static const VALIDATION = MerchantPaymentResponseFailureStageEnum._(r'VALIDATION');
  static const ROUTING = MerchantPaymentResponseFailureStageEnum._(r'ROUTING');
  static const PROVIDER_INIT = MerchantPaymentResponseFailureStageEnum._(r'PROVIDER_INIT');
  static const PROVIDER_ACTION = MerchantPaymentResponseFailureStageEnum._(r'PROVIDER_ACTION');
  static const PROVIDER_CALLBACK = MerchantPaymentResponseFailureStageEnum._(r'PROVIDER_CALLBACK');
  static const RECONCILIATION = MerchantPaymentResponseFailureStageEnum._(r'RECONCILIATION');
  static const INTERNAL = MerchantPaymentResponseFailureStageEnum._(r'INTERNAL');
  static const UNKNOWN = MerchantPaymentResponseFailureStageEnum._(r'UNKNOWN');

  /// List of all possible values in this [enum][MerchantPaymentResponseFailureStageEnum].
  static const values = <MerchantPaymentResponseFailureStageEnum>[
    VALIDATION,
    ROUTING,
    PROVIDER_INIT,
    PROVIDER_ACTION,
    PROVIDER_CALLBACK,
    RECONCILIATION,
    INTERNAL,
    UNKNOWN,
  ];

  static MerchantPaymentResponseFailureStageEnum? fromJson(dynamic value) => MerchantPaymentResponseFailureStageEnumTypeTransformer().decode(value);

  static List<MerchantPaymentResponseFailureStageEnum> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <MerchantPaymentResponseFailureStageEnum>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = MerchantPaymentResponseFailureStageEnum.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }
}

/// Transformation class that can [encode] an instance of [MerchantPaymentResponseFailureStageEnum] to String,
/// and [decode] dynamic data back to [MerchantPaymentResponseFailureStageEnum].
class MerchantPaymentResponseFailureStageEnumTypeTransformer {
  factory MerchantPaymentResponseFailureStageEnumTypeTransformer() => _instance ??= const MerchantPaymentResponseFailureStageEnumTypeTransformer._();

  const MerchantPaymentResponseFailureStageEnumTypeTransformer._();

  String encode(MerchantPaymentResponseFailureStageEnum data) => data.value;

  /// Decodes a [dynamic value][data] to a MerchantPaymentResponseFailureStageEnum.
  ///
  /// If [allowNull] is true and the [dynamic value][data] cannot be decoded successfully,
  /// then null is returned. However, if [allowNull] is false and the [dynamic value][data]
  /// cannot be decoded successfully, then an [UnimplementedError] is thrown.
  ///
  /// The [allowNull] is very handy when an API changes and a new enum value is added or removed,
  /// and users are still using an old app with the old code.
  MerchantPaymentResponseFailureStageEnum? decode(dynamic data, {bool allowNull = true}) {
    if (data != null) {
      switch (data) {
        case r'VALIDATION': return MerchantPaymentResponseFailureStageEnum.VALIDATION;
        case r'ROUTING': return MerchantPaymentResponseFailureStageEnum.ROUTING;
        case r'PROVIDER_INIT': return MerchantPaymentResponseFailureStageEnum.PROVIDER_INIT;
        case r'PROVIDER_ACTION': return MerchantPaymentResponseFailureStageEnum.PROVIDER_ACTION;
        case r'PROVIDER_CALLBACK': return MerchantPaymentResponseFailureStageEnum.PROVIDER_CALLBACK;
        case r'RECONCILIATION': return MerchantPaymentResponseFailureStageEnum.RECONCILIATION;
        case r'INTERNAL': return MerchantPaymentResponseFailureStageEnum.INTERNAL;
        case r'UNKNOWN': return MerchantPaymentResponseFailureStageEnum.UNKNOWN;
        default:
          if (!allowNull) {
            throw ArgumentError('Unknown enum value to decode: $data');
          }
      }
    }
    return null;
  }

  /// Singleton [MerchantPaymentResponseFailureStageEnumTypeTransformer] instance.
  static MerchantPaymentResponseFailureStageEnumTypeTransformer? _instance;
}



package com.Api.Fidelitypay.enums;

/**
 * Status of API requests for logging and metrics
 */
public enum ApiRequestStatus {
    SUCCESS,
    ERROR,
    TIMEOUT,
    UNAUTHORIZED,
    RATE_LIMITED,
    VALIDATION_ERROR
}

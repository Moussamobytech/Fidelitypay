package com.Api.Fidelitypay.config;

import com.Api.Fidelitypay.enums.ApiRequestStatus;
import com.Api.Fidelitypay.service.ApiKeyService;
import com.Api.Fidelitypay.service.DeveloperMetricsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor to log API requests for metrics and monitoring
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiRequestInterceptor implements HandlerInterceptor {

    private final DeveloperMetricsService metricsService;
    private final ApiKeyService apiKeyService;

    private static final String START_TIME_ATTR = "startTime";
    private static final String USER_ID_ATTR = "userId";
    private static final String API_KEY_ID_ATTR = "apiKeyId";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull Object handler) {
        // Record start time for latency calculation
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());

        String userId = authenticatedUserId();
        if (userId != null) {
            request.setAttribute(USER_ID_ATTR, userId);
        }

        String rawApiKey = request.getHeader("X-API-Key");
        if (rawApiKey != null) {
            apiKeyService.authenticateApiKey(rawApiKey)
                    .ifPresent(apiKey -> {
                        request.setAttribute(API_KEY_ID_ATTR, apiKey.getId());
                        request.setAttribute(USER_ID_ATTR, apiKey.getUserId());
                    });
        }

        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull Object handler, @Nullable Exception ex) {
        try {
            // Skip logging for non-API endpoints
            String path = request.getRequestURI();
            if (!path.startsWith("/api/")) {
                return;
            }

            // Get user ID
            String userId = (String) request.getAttribute(USER_ID_ATTR);

            // Only log if we have a user ID
            if (userId == null) {
                return;
            }

            // Calculate latency
            Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
            long latencyMs = startTime != null ? System.currentTimeMillis() - startTime : 0;

            // Determine status
            int statusCode = response.getStatus();
            ApiRequestStatus status = determineStatus(statusCode, ex);

            // Get request details
            String method = request.getMethod();
            String endpoint = path;
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            String apiKeyId = (String) request.getAttribute(API_KEY_ID_ATTR);

            // Get error message if present
            String errorMessage = ex != null ? ex.getMessage() : null;

            // Log the request
            metricsService.logApiRequest(
                    userId,
                    apiKeyId != null ? apiKeyId : "unknown",
                    method,
                    endpoint,
                    statusCode,
                    status,
                    ipAddress,
                    userAgent,
                    latencyMs,
                    errorMessage);

            // Update API key last used timestamp if applicable
            // MerchantApiAuthService records API-key usage after successful authentication.

        } catch (Exception e) {
            // Don't let logging errors affect the request
            log.error("Error logging API request: {}", e.getMessage());
        }
    }

    /**
     * Determine request status based on HTTP status code and exception
     */
    private ApiRequestStatus determineStatus(int statusCode, Exception ex) {
        if (ex != null) {
            return ApiRequestStatus.ERROR;
        }

        if (statusCode >= 200 && statusCode < 300) {
            return ApiRequestStatus.SUCCESS;
        } else if (statusCode == 401 || statusCode == 403) {
            return ApiRequestStatus.UNAUTHORIZED;
        } else if (statusCode == 429) {
            return ApiRequestStatus.RATE_LIMITED;
        } else if (statusCode >= 400 && statusCode < 500) {
            return ApiRequestStatus.VALIDATION_ERROR;
        } else {
            return ApiRequestStatus.ERROR;
        }
    }

    /**
     * Get the real client IP address (considering proxies)
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs, get the first one
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    private String authenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.Api.Fidelitypay.model.User user) {
            return user.getId();
        }
        return null;
    }
}

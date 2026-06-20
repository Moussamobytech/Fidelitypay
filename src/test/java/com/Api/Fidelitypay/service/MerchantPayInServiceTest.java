package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.controller.dto.MerchantApiPrincipal;
import com.Api.Fidelitypay.controller.dto.MerchantPaymentRequest;
import com.Api.Fidelitypay.controller.dto.MerchantPaymentResponse;
import com.Api.Fidelitypay.enums.ErrorType;
import com.Api.Fidelitypay.enums.PaymentFlowType;
import com.Api.Fidelitypay.enums.PaymentStatus;
import com.Api.Fidelitypay.integration.KkiapayClient;
import com.Api.Fidelitypay.integration.PayDunyaClient;
import com.Api.Fidelitypay.integration.PaymentResult;
import com.Api.Fidelitypay.model.ApiKey;
import com.Api.Fidelitypay.model.PaymentProvider;
import com.Api.Fidelitypay.model.PaymentProviderRoute;
import com.Api.Fidelitypay.model.Payment;
import com.Api.Fidelitypay.model.User;
import com.Api.Fidelitypay.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MerchantPayInServiceTest {

    private PaymentRepository paymentRepository;
    private com.Api.Fidelitypay.repository.LogEntryRepository logEntryRepository;
    private PaymentRouteService routeService;
    private KkiapayClient kkiapayClient;
    private PayDunyaClient payDunyaClient;
    private WebhookService webhookService;
    private MerchantProviderAccountService providerAccountService;
    private MerchantPayInService service;
    private MerchantApiPrincipal principal;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        logEntryRepository = mock(com.Api.Fidelitypay.repository.LogEntryRepository.class);
        routeService = mock(PaymentRouteService.class);
        kkiapayClient = mock(KkiapayClient.class);
        payDunyaClient = mock(PayDunyaClient.class);
        webhookService = mock(WebhookService.class);
        providerAccountService = mock(MerchantProviderAccountService.class);
        service = new MerchantPayInService(paymentRepository, logEntryRepository, routeService, kkiapayClient, payDunyaClient, webhookService,
                providerAccountService, new PaymentStateTransitionService());
        ReflectionTestUtils.setField(service, "allowGlobalCredentialsFallback", true);

        User user = User.builder()
                .id("user-1")
                .email("merchant@example.com")
                .password("x")
                .fullName("Merchant")
                .role(User.Role.DEVELOPER)
                .isActive(true)
                .build();
        ApiKey apiKey = ApiKey.builder()
                .id("key-1")
                .userId("user-1")
                .publicKey("pk_test")
                .environment("sandbox")
                .isActive(true)
                .build();
        principal = MerchantApiPrincipal.builder()
                .apiKey(apiKey)
                .user(user)
                .environment("sandbox")
                .build();

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void initiate_returnsExistingPaymentForDuplicateIdempotencyKey() {
        Payment existing = existingPayment();
        when(paymentRepository.findByApiKeyIdAndIdempotencyKey("key-1", "idem-1")).thenReturn(Optional.of(existing));

        MerchantPaymentResponse response = service.initiate(principal, request("SN", "WAVE"), "idem-1");

        assertEquals(existing.getPaymentId(), response.getPaymentId());
        assertEquals(PaymentStatus.PENDING, response.getStatus());
        verifyNoInteractions(routeService, kkiapayClient, payDunyaClient, webhookService);
    }

    @Test
    void initiate_rejectsUnsupportedCapabilityWithoutProviderCall() {
        when(paymentRepository.findByApiKeyIdAndIdempotencyKey("key-1", "idem-2")).thenReturn(Optional.empty());
        when(routeService.findAvailablePayIn("BJ", "OM", "SANDBOX", "user-1")).thenReturn(List.of());
        when(routeService.hasAnyPayInRoute("BJ", "OM")).thenReturn(false);

        MerchantPaymentResponse response = service.initiate(principal, request("BJ", "orange"), "idem-2");

        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertEquals("UNSUPPORTED_PAYIN_CAPABILITY", response.getFailureReason());
        verifyNoInteractions(kkiapayClient, payDunyaClient);
        verify(webhookService).sendWebhook(any(Payment.class));
    }

    @Test
    void initiate_doesNotFallbackWhenProviderTransactionMayExist() {
        when(paymentRepository.findByApiKeyIdAndIdempotencyKey("key-1", "idem-3")).thenReturn(Optional.empty());
        when(routeService.findAvailablePayIn("SN", "WAVE", "SANDBOX", "user-1")).thenReturn(List.of(
                route("KKIAPAY", "SN", "WAVE", PaymentFlowType.WAVE_REDIRECT, 10),
                route("PAYDUNYA", "SN", "WAVE", PaymentFlowType.HOSTED_CHECKOUT, 20)));

        PaymentResult unknown = new PaymentResult(false);
        unknown.setErrorType(ErrorType.TIMEOUT);
        unknown.setProviderId("kkiapay-unknown");
        unknown.setProviderTransactionCreated(true);
        when(kkiapayClient.initiatePayIn(any())).thenReturn(unknown);

        MerchantPaymentResponse response = service.initiate(principal, request("SN", "WAVE"), "idem-3");

        assertEquals(PaymentStatus.PENDING_RECONCILIATION, response.getStatus());
        assertEquals("KKIAPAY", response.getProvider());
        assertEquals("PROVIDER_RESULT_UNKNOWN", response.getFailureReason());
        verify(payDunyaClient, never()).initiatePayIn(any());
        verify(webhookService, never()).sendWebhook(any());
    }

    @Test
    void initiate_fallbacksOnlyForSafeTechnicalFailureBeforeProviderTransaction() {
        when(paymentRepository.findByApiKeyIdAndIdempotencyKey("key-1", "idem-4")).thenReturn(Optional.empty());
        when(routeService.findAvailablePayIn("SN", "WAVE", "SANDBOX", "user-1")).thenReturn(List.of(
                route("KKIAPAY", "SN", "WAVE", PaymentFlowType.WAVE_REDIRECT, 10),
                route("PAYDUNYA", "SN", "WAVE", PaymentFlowType.HOSTED_CHECKOUT, 20)));

        PaymentResult timeoutBeforeCreate = new PaymentResult(false);
        timeoutBeforeCreate.setErrorType(ErrorType.TIMEOUT);
        when(kkiapayClient.initiatePayIn(any())).thenReturn(timeoutBeforeCreate);

        PaymentResult paydunyaSuccess = new PaymentResult(true);
        paydunyaSuccess.setProviderId("paydunya-token");
        paydunyaSuccess.setPaymentUrl("https://paydunya/checkout");
        when(payDunyaClient.initiatePayIn(any())).thenReturn(paydunyaSuccess);

        MerchantPaymentResponse response = service.initiate(principal, request("SN", "WAVE"), "idem-4");

        assertEquals(PaymentStatus.PENDING, response.getStatus());
        assertEquals("PAYDUNYA", response.getProvider());
        assertEquals("https://paydunya/checkout", response.getPaymentUrl());

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        Payment saved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertEquals(true, saved.isUsedFallback());
        assertEquals("PRIMARY_PROVIDER_FAILED", saved.getFallbackReason());
    }

    @Test
    void initiate_fallbacksWhenPrimaryProviderCredentialsAreRejected() {
        when(paymentRepository.findByApiKeyIdAndIdempotencyKey("key-1", "idem-auth"))
                .thenReturn(Optional.empty());
        when(routeService.findAvailablePayIn("CI", "MOOV", "SANDBOX", "user-1")).thenReturn(List.of(
                route("KKIAPAY", "CI", "MOOV", PaymentFlowType.MOBILE_MONEY_REQUEST, 10),
                route("PAYDUNYA", "CI", "MOOV", PaymentFlowType.HOSTED_CHECKOUT, 20)));

        PaymentResult authenticationFailure = new PaymentResult(false);
        authenticationFailure.setErrorType(ErrorType.AUTHENTICATION);
        when(kkiapayClient.initiatePayIn(any())).thenReturn(authenticationFailure);

        PaymentResult paydunyaSuccess = new PaymentResult(true);
        paydunyaSuccess.setProviderId("paydunya-token");
        paydunyaSuccess.setPaymentUrl("https://paydunya/checkout");
        when(payDunyaClient.initiatePayIn(any())).thenReturn(paydunyaSuccess);

        MerchantPaymentResponse response = service.initiate(principal, request("CI", "MOOV"), "idem-auth");

        assertEquals(PaymentStatus.PENDING, response.getStatus());
        assertEquals("PAYDUNYA", response.getProvider());
        assertEquals("https://paydunya/checkout", response.getPaymentUrl());
        verify(payDunyaClient).initiatePayIn(any());
    }

    @Test
    void initiate_doesNotFallbackForBadRequest() {
        when(paymentRepository.findByApiKeyIdAndIdempotencyKey("key-1", "idem-5")).thenReturn(Optional.empty());
        when(routeService.findAvailablePayIn("SN", "WAVE", "SANDBOX", "user-1")).thenReturn(List.of(
                route("KKIAPAY", "SN", "WAVE", PaymentFlowType.WAVE_REDIRECT, 10),
                route("PAYDUNYA", "SN", "WAVE", PaymentFlowType.HOSTED_CHECKOUT, 20)));

        PaymentResult badRequest = new PaymentResult(false);
        badRequest.setErrorType(ErrorType.BAD_REQUEST);
        when(kkiapayClient.initiatePayIn(any())).thenReturn(badRequest);

        MerchantPaymentResponse response = service.initiate(principal, request("SN", "WAVE"), "idem-5");

        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertEquals("BAD_REQUEST", response.getFailureReason());
        verify(payDunyaClient, never()).initiatePayIn(any());
        verify(webhookService).sendWebhook(any(Payment.class));
    }

    @Test
    void initiate_returnsRequiresActionForOrangeCiOtp() {
        when(paymentRepository.findByApiKeyIdAndIdempotencyKey("key-1", "idem-6")).thenReturn(Optional.empty());
        when(routeService.findAvailablePayIn("CI", "OM", "SANDBOX", "user-1")).thenReturn(List.of(
                route("KKIAPAY", "CI", "OM", PaymentFlowType.ORANGE_CI_OTP, 10)));

        PaymentResult requiresOtp = new PaymentResult(true);
        requiresOtp.setProviderId("kkiapay-otp");
        requiresOtp.setRequiresAction(true);
        requiresOtp.setNextActionType("SUBMIT_OTP");
        when(kkiapayClient.initiatePayIn(any())).thenReturn(requiresOtp);

        MerchantPaymentResponse response = service.initiate(principal, request("CI", "ORANGE"), "idem-6");

        assertEquals(PaymentStatus.REQUIRES_ACTION, response.getStatus());
        assertNotNull(response.getNextAction());
        assertEquals("SUBMIT_OTP", response.getNextAction().getType());
        assertNull(response.getFailureReason());
        verify(webhookService).sendWebhook(any(Payment.class));
    }

    @Test
    void initiate_allowsCardPaymentWithoutPhone() {
        MerchantPaymentRequest cardRequest = request("INT", "VISA");
        cardRequest.getCustomer().setPhone(null);
        when(paymentRepository.findByApiKeyIdAndIdempotencyKey("key-1", "idem-card")).thenReturn(Optional.empty());
        when(routeService.findAvailablePayIn("INT", "VISA", "SANDBOX", "user-1")).thenReturn(List.of(
                route("PAYDUNYA", "INT", "VISA", PaymentFlowType.HOSTED_CHECKOUT, 10)));

        PaymentResult success = new PaymentResult(true);
        success.setProviderId("card-checkout");
        success.setPaymentUrl("https://paydunya/checkout/card");
        when(payDunyaClient.initiatePayIn(any())).thenReturn(success);

        MerchantPaymentResponse response = service.initiate(principal, cardRequest, "idem-card");

        assertEquals(PaymentStatus.PENDING, response.getStatus());
        assertEquals("HOSTED_CHECKOUT", response.getFlowType());
    }

    @Test
    void initiate_rejectsMobileMoneyWithoutPhone() {
        MerchantPaymentRequest mobileRequest = request("SN", "WAVE");
        mobileRequest.getCustomer().setPhone(" ");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.initiate(principal, mobileRequest, "idem-mobile"));

        assertEquals("customer.phone is required for mobile-money payments", error.getMessage());
        verifyNoInteractions(routeService, kkiapayClient, payDunyaClient);
    }

    private MerchantPaymentRequest request(String country, String operator) {
        MerchantPaymentRequest request = new MerchantPaymentRequest();
        request.setAmount(5_000);
        request.setCountry(country);
        request.setOperator(operator);
        MerchantPaymentRequest.Customer customer = new MerchantPaymentRequest.Customer();
        customer.setPhone("221776006060");
        customer.setFirstname("Awa");
        customer.setLastname("Diop");
        request.setCustomer(customer);
        request.setReturnUrl("https://merchant.example.com/success");
        request.setCancelUrl("https://merchant.example.com/cancel");
        return request;
    }

    private PaymentProviderRoute route(String provider, String country, String operator, PaymentFlowType flowType, int priority) {
        PaymentProviderRoute route = new PaymentProviderRoute();
        PaymentProvider paymentProvider = new PaymentProvider();
        paymentProvider.setCode(provider);
        route.setProvider(paymentProvider);
        route.setCountry(country);
        route.setOperator(operator);
        route.setFlowType(flowType);
        route.setProviderChannel(provider.toLowerCase() + "-" + operator.toLowerCase());
        route.setLiveEnabled(true);
        route.setSandboxEnabled(true);
        route.setPriority(priority);
        route.setEnabled(true);
        return route;
    }

    private Payment existingPayment() {
        Payment payment = new Payment();
        payment.setPaymentId("fp_existing");
        payment.setApiKeyId("key-1");
        payment.setAmount(BigDecimal.valueOf(5000));
        payment.setCountry("SN");
        payment.setOperator("WAVE");
        payment.setCurrency("XOF");
        payment.setStatus(PaymentStatus.PENDING);
        return payment;
    }
}

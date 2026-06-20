package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.controller.dto.CreateWebhookRequest;
import com.Api.Fidelitypay.controller.dto.WebhookResponse;
import com.Api.Fidelitypay.model.Webhook;
import com.Api.Fidelitypay.repository.WebhookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeveloperWebhookServiceTest {

    private WebhookRepository repository;
    private DeveloperWebhookService service;

    @BeforeEach
    void setUp() {
        repository = mock(WebhookRepository.class);
        service = new DeveloperWebhookService(repository);
    }

    @Test
    void createWebhookRejectsLocalAndPrivateDestinations() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createWebhook("user-1", request("https://localhost/webhook")));
        assertThrows(IllegalArgumentException.class,
                () -> service.createWebhook("user-1", request("https://192.168.1.20/webhook")));
        assertThrows(IllegalArgumentException.class,
                () -> service.createWebhook("user-1", request("http://merchant.example/webhook")));

        verify(repository, never()).save(any());
    }

    @Test
    void createWebhookAcceptsPublicHttpsDestination() {
        CreateWebhookRequest request = request("https://merchant.example/webhooks/fidelitypay");
        when(repository.existsByUserIdAndUrlAndEvent("user-1", request.getUrl(), request.getEvent())).thenReturn(false);
        when(repository.save(any(Webhook.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WebhookResponse response = service.createWebhook("user-1", request);

        assertEquals(request.getUrl(), response.getUrl());
        assertEquals("payment.success", response.getEvent());
    }

    private CreateWebhookRequest request(String url) {
        return CreateWebhookRequest.builder()
                .url(url)
                .event("payment.success")
                .description("Payment updates")
                .build();
    }
}

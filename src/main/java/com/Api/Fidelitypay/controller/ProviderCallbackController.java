package com.Api.Fidelitypay.controller;

import com.Api.Fidelitypay.integration.kkiapay.dto.KkiapayCallbackDTO;
import com.Api.Fidelitypay.service.ProviderCallbackService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
public class ProviderCallbackController {

    private final ProviderCallbackService callbackService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/paydunya/checkout/ipn", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> paydunyaCheckoutIpn(@RequestBody MultiValueMap<String, String> form) {
        try {
            String token = firstValue(form, "data[invoice][token]", "data[token]", "token");
            String status = firstValue(form, "data[status]", "status");
            String hash = firstValue(form, "data[hash]", "hash");
            String result = callbackService.acceptPayDunyaCheckoutIpn(token, status, hash, rawForm(form));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok("ignored");
        }
    }

    @PostMapping(value = "/kkiapay/checkout/ipn", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> kkiapayCheckoutIpn(@RequestBody JsonNode body) {
        try {
            KkiapayCallbackDTO callback = objectMapper.treeToValue(body, KkiapayCallbackDTO.class);
            callback.setTransactionId(firstText(body, "transactionId", "transaction_id", "transaction_id_str"));
            if (callback.getTransactionId() == null && body.has("data")) {
                callback.setTransactionId(firstText(body.get("data"), "transactionId", "transaction_id", "id"));
            }
            if (!body.has("isPaymentSucces") && !body.has("paymentSucces") && !body.has("paymentSuccess")) {
                callback.setPaymentSucces(successFrom(body));
            }
            String result = callbackService.acceptKkiapayCheckoutIpn(callback, body.toString());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok("ignored");
        }
    }

    private String firstValue(MultiValueMap<String, String> form, String... keys) {
        for (String key : keys) {
            String value = form.getFirst(key);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String rawForm(MultiValueMap<String, String> form) {
        Map<String, Object> raw = new LinkedHashMap<>();
        form.forEach((key, values) -> raw.put(key, values.size() == 1 ? values.get(0) : values));
        try {
            return objectMapper.writeValueAsString(raw);
        } catch (Exception e) {
            return raw.toString();
        }
    }

    private String firstText(JsonNode node, String... fields) {
        if (node == null) return null;
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull() && !value.asText().isBlank()) {
                return value.asText().trim();
            }
        }
        return null;
    }

    private boolean successFrom(JsonNode body) {
        String event = firstText(body, "event", "status");
        if (event == null && body.has("data")) {
            event = firstText(body.get("data"), "event", "status");
        }
        return event != null && (event.equalsIgnoreCase("success")
                || event.equalsIgnoreCase("succeeded")
                || event.equalsIgnoreCase("completed")
                || event.equalsIgnoreCase("transaction.success"));
    }
}

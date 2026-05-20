package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.controller.dto.MerchantProviderAccountRequest;
import com.Api.Fidelitypay.model.Agregateur;
import com.Api.Fidelitypay.model.PaymentProvider;
import com.Api.Fidelitypay.repository.AgregateurRepository;
import com.Api.Fidelitypay.repository.PaymentProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class LegacyAggregatorMigrationRunner implements ApplicationRunner {

    private final AgregateurRepository agregateurRepository;
    private final PaymentProviderRepository providerRepository;
    private final MerchantProviderAccountService accountService;

    @Override
    public void run(ApplicationArguments args) {
        agregateurRepository.findAll().stream()
                .filter(agregateur -> agregateur.getOwnerUserId() != null && !agregateur.getOwnerUserId().isBlank())
                .forEach(this::migrateMerchantAggregator);
    }

    private void migrateMerchantAggregator(Agregateur agregateur) {
        String providerCode = agregateur.getNomA() == null ? "" : agregateur.getNomA().trim().toUpperCase();
        PaymentProvider provider = providerRepository.findByCode(providerCode).orElse(null);
        if (provider == null) {
            log.warn("Skipping legacy merchant aggregator migration for unsupported provider={}", providerCode);
            return;
        }
        MerchantProviderAccountRequest request = new MerchantProviderAccountRequest();
        request.setProviderId(provider.getId());
        request.setEnvironment("LIVE");
        request.setEnabled(agregateur.isEnabled());
        request.setCredentials(credentialsFor(providerCode, agregateur));
        accountService.upsertAccount(agregateur.getOwnerUserId(), request);
    }

    private Map<String, String> credentialsFor(String providerCode, Agregateur agregateur) {
        Map<String, String> credentials = new LinkedHashMap<>();
        if ("PAYDUNYA".equals(providerCode)) {
            credentials.put("masterKey", safe(agregateur.getCleApblic()));
            credentials.put("privateKey", safe(agregateur.getCleApr()));
            credentials.put("token", safe(agregateur.getCleAtoken()));
        } else {
            credentials.put("publicKey", safe(agregateur.getCleApblic()));
            credentials.put("privateKey", safe(agregateur.getCleApr()));
            credentials.put("token", safe(agregateur.getCleAtoken()));
        }
        return credentials;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}

package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.controller.dto.MerchantProviderAccountRequest;
import com.Api.Fidelitypay.controller.dto.MerchantProviderAccountResponse;
import com.Api.Fidelitypay.integration.ProviderCredentials;
import com.Api.Fidelitypay.model.MerchantProviderAccount;
import com.Api.Fidelitypay.model.PaymentProvider;
import com.Api.Fidelitypay.repository.MerchantProviderAccountRepository;
import com.Api.Fidelitypay.repository.PaymentProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MerchantProviderAccountService {

    private final MerchantProviderAccountRepository accountRepository;
    private final PaymentProviderRepository providerRepository;
    private final CredentialCryptoService credentialCryptoService;

    @Transactional(readOnly = true)
    public List<MerchantProviderAccountResponse> listAccounts(String merchantUserId) {
        return accountRepository.findByMerchantUserIdWithProvider(merchantUserId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MerchantProviderAccountResponse upsertAccount(String merchantUserId, MerchantProviderAccountRequest request) {
        PaymentProvider provider = providerRepository.findById(request.getProviderId())
                .orElseThrow(() -> new IllegalArgumentException("Payment provider not found"));
        String environment = normalizeEnvironment(request.getEnvironment());
        MerchantProviderAccount account = accountRepository
                .findByMerchantUserIdAndProviderIdAndEnvironment(merchantUserId, provider.getId(), environment)
                .orElseGet(MerchantProviderAccount::new);
        account.setMerchantUserId(merchantUserId);
        account.setProvider(provider);
        account.setEnvironment(environment);
        Map<String, String> mergedCredentials = mergeCredentials(account, request.getCredentials());
        if (!mergedCredentials.isEmpty()) {
            account.setCredentialsEncrypted(credentialCryptoService.encrypt(mergedCredentials));
        }
        account.setEnabled(request.isEnabled());
        return toResponse(accountRepository.save(account));
    }

    @Transactional
    public MerchantProviderAccountResponse setEnabled(String merchantUserId, Long id, boolean enabled) {
        MerchantProviderAccount account = getOwnedAccount(merchantUserId, id);
        account.setEnabled(enabled);
        return toResponse(accountRepository.save(account));
    }

    @Transactional
    public void deleteAccount(String merchantUserId, Long id) {
        MerchantProviderAccount account = getOwnedAccount(merchantUserId, id);
        accountRepository.delete(account);
    }

    @Transactional(readOnly = true)
    public MerchantProviderAccount getEnabledAccount(String merchantUserId, Long providerId, String environment) {
        return accountRepository.findEnabledAccount(merchantUserId, providerId, normalizeEnvironment(environment))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public MerchantProviderAccount getAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Merchant provider account not found"));
    }

    @Transactional(readOnly = true)
    public ProviderCredentials decrypt(MerchantProviderAccount account) {
        Map<String, String> values = new HashMap<>(
                credentialCryptoService.decrypt(account.getCredentialsEncrypted()).values());
        values.put("_environment", account.getEnvironment());
        return new ProviderCredentials(values);
    }

    private Map<String, String> mergeCredentials(MerchantProviderAccount account, Map<String, String> submittedCredentials) {
        Map<String, String> merged = new HashMap<>();
        if (account.getCredentialsEncrypted() != null && !account.getCredentialsEncrypted().isBlank()) {
            merged.putAll(credentialCryptoService.decrypt(account.getCredentialsEncrypted()).values());
        }
        if (submittedCredentials == null || submittedCredentials.isEmpty()) {
            return merged;
        }
        submittedCredentials.forEach((key, value) -> {
            if (key != null && value != null && !value.isBlank()) {
                merged.put(key, value.trim());
            }
        });
        return merged;
    }

    private MerchantProviderAccount getOwnedAccount(String merchantUserId, Long id) {
        MerchantProviderAccount account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Merchant provider account not found"));
        if (!merchantUserId.equals(account.getMerchantUserId())) {
            throw new SecurityException("Provider account does not belong to this merchant");
        }
        return account;
    }

    private MerchantProviderAccountResponse toResponse(MerchantProviderAccount account) {
        Map<String, String> hints = credentialCryptoService.masked(account.getCredentialsEncrypted());
        return MerchantProviderAccountResponse.builder()
                .id(account.getId())
                .providerId(account.getProvider().getId())
                .providerCode(account.getProvider().getCode())
                .providerDisplayName(account.getProvider().getDisplayName())
                .environment(account.getEnvironment())
                .enabled(account.isEnabled())
                .credentialHints(hints)
                .build();
    }

    private String normalizeEnvironment(String environment) {
        return environment == null || environment.isBlank() ? "LIVE" : environment.trim().toUpperCase();
    }
}

package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.enums.PaymentProviderStatus;
import com.Api.Fidelitypay.model.Agregateur;
import com.Api.Fidelitypay.repository.AgregateurRepository;
import com.Api.Fidelitypay.repository.PaymentProviderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AgregateurService {

    @Autowired
    private AgregateurRepository agregateurRepository;

    @Autowired
    private PaymentProviderRepository paymentProviderRepository;

    public List<Agregateur> getAllAgregateurs() {
        return agregateurRepository.findAll();
    }

    public List<Agregateur> getMerchantAgregateurs(String userId) {
        return agregateurRepository.findByOwnerUserId(userId);
    }

    public Optional<Agregateur> getAgregateurById(Long id) {
        return agregateurRepository.findById(id);
    }

    public Agregateur createAgregateur(Agregateur agregateur) {
        if (agregateur.getCountryConfigs() != null) {
            agregateur.getCountryConfigs().forEach(config -> config.setAgregateur(agregateur));
        }
        return agregateurRepository.save(agregateur);
    }

    public Agregateur createMerchantAgregateur(String userId, Agregateur agregateur) {
        agregateur.setId(null);
        agregateur.setOwnerUserId(userId);
        agregateur.setEnabled(true);
        normalizeMerchantAggregator(agregateur);
        return createAgregateur(agregateur);
    }

    public Agregateur updateAgregateur(Long id, Agregateur agregateurDetails) {
        return agregateurRepository.findById(id).map(agregateur -> {
            agregateur.setNomA(agregateurDetails.getNomA());
            agregateur.setCleApblic(agregateurDetails.getCleApblic());
            agregateur.setCleApr(agregateurDetails.getCleApr());
            agregateur.setCleAtoken(agregateurDetails.getCleAtoken());
            agregateur.setCleAmaster(agregateurDetails.getCleAmaster());
            agregateur.setBaseUrl(agregateurDetails.getBaseUrl());
            agregateur.setNompays(agregateurDetails.getNompays());
            agregateur.setNomOperateur(agregateurDetails.getNomOperateur());
            agregateur.setEnabled(agregateurDetails.isEnabled());

            agregateur.getCountryConfigs().clear();
            if (agregateurDetails.getCountryConfigs() != null) {
                agregateurDetails.getCountryConfigs().forEach(config -> {
                    config.setAgregateur(agregateur);
                    agregateur.getCountryConfigs().add(config);
                });
            }

            return agregateurRepository.save(agregateur);
        }).orElseThrow(() -> new RuntimeException("Agregateur not found with id " + id));
    }

    public Agregateur updateMerchantAgregateur(String userId, Long id, Agregateur agregateurDetails) {
        Agregateur agregateur = getOwnedAgregateur(userId, id);
        agregateur.setNomA(agregateurDetails.getNomA());
        agregateur.setCleApblic(agregateurDetails.getCleApblic());
        agregateur.setCleApr(agregateurDetails.getCleApr());
        agregateur.setCleAtoken(agregateurDetails.getCleAtoken());
        agregateur.setNompays(agregateurDetails.getNompays());
        agregateur.setNomOperateur(agregateurDetails.getNomOperateur());
        normalizeMerchantAggregator(agregateur);
        return agregateurRepository.save(agregateur);
    }

    @Transactional
    public Agregateur setAgregateurEnabled(Long id, boolean enabled) {
        Agregateur agregateur = agregateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agregateur not found with id " + id));
        agregateur.setEnabled(enabled);
        return agregateurRepository.save(agregateur);
    }

    @Transactional
    public Agregateur setMerchantAgregateurEnabled(String userId, Long id, boolean enabled) {
        Agregateur agregateur = getOwnedAgregateur(userId, id);
        agregateur.setEnabled(enabled);
        return agregateurRepository.save(agregateur);
    }

    public Set<String> getDisabledProviderNamesForMerchant(String userId) {
        if (userId == null || userId.isBlank()) {
            return Set.of();
        }
        return agregateurRepository.findByOwnerUserIdAndEnabledFalse(userId).stream()
                .map(Agregateur::getNomA)
                .filter(name -> name != null && !name.isBlank())
                .map(name -> name.trim().toUpperCase())
                .collect(Collectors.toSet());
    }

    public void deleteAgregateur(Long id) {
        agregateurRepository.deleteById(id);
    }

    public void deleteMerchantAgregateur(String userId, Long id) {
        getOwnedAgregateur(userId, id);
        agregateurRepository.deleteById(id);
    }

    private void normalizeMerchantAggregator(Agregateur agregateur) {
        String provider = agregateur.getNomA() == null ? "" : agregateur.getNomA().trim().toUpperCase();
        Set<String> supportedProviders = paymentProviderRepository.findByStatusOrderByDisplayNameAsc(PaymentProviderStatus.ACTIVE)
                .stream()
                .map(paymentProvider -> paymentProvider.getCode() == null ? null : paymentProvider.getCode().trim().toUpperCase())
                .filter(providerName -> providerName != null && !providerName.isBlank())
                .collect(Collectors.toSet());
        if (supportedProviders.isEmpty()) {
            supportedProviders = Set.of("KKIAPAY", "PAYDUNYA");
        }
        if (!supportedProviders.contains(provider)) {
            throw new IllegalArgumentException("Unsupported payment aggregator: " + agregateur.getNomA());
        }
        agregateur.setNomA(provider);
        agregateur.setNompays(null);
        agregateur.setNomOperateur(null);
        if (agregateur.getCountryConfigs() == null) {
            agregateur.setCountryConfigs(new ArrayList<>());
        }
        agregateur.getCountryConfigs().clear();
    }

    private Agregateur getOwnedAgregateur(String userId, Long id) {
        Agregateur agregateur = agregateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agregateur not found with id " + id));
        if (agregateur.getOwnerUserId() == null || !agregateur.getOwnerUserId().equals(userId)) {
            throw new SecurityException("Aggregator does not belong to this merchant");
        }
        return agregateur;
    }
}

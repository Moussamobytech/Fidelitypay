package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.enums.PaymentProviderStatus;
import com.Api.Fidelitypay.model.Agregateur;
import com.Api.Fidelitypay.model.CountryConfig;
import com.Api.Fidelitypay.model.Route;
import com.Api.Fidelitypay.repository.AgregateurRepository;
<<<<<<< HEAD
import com.Api.Fidelitypay.repository.PaymentProviderRepository;
=======
import com.Api.Fidelitypay.repository.RouteRepository;
import lombok.extern.slf4j.Slf4j;
>>>>>>> 6451fc7ea20468a53eca0812ef46cd8840cb6a75
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AgregateurService {

    @Autowired
    private AgregateurRepository agregateurRepository;

    @Autowired
<<<<<<< HEAD
    private PaymentProviderRepository paymentProviderRepository;
=======
    private RouteRepository routeRepository;
>>>>>>> 6451fc7ea20468a53eca0812ef46cd8840cb6a75

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
            for (CountryConfig config : agregateur.getCountryConfigs()) {
                config.setAgregateur(agregateur);
            }
        }
        Agregateur saved = agregateurRepository.save(agregateur);
        syncRoutes(saved);
        return saved;
    }

    public Agregateur createMerchantAgregateur(String userId, Agregateur agregateur) {
        agregateur.setId(null);
        agregateur.setOwnerUserId(userId);
        agregateur.setEnabled(true);
        normalizeMerchantAggregator(agregateur);
        return createAgregateur(agregateur);
    }

    public Agregateur updateAgregateur(Long id, Agregateur agregateurDetails) {
<<<<<<< HEAD
        return agregateurRepository.findById(id).map(agregateur -> {
            agregateur.setNomA(agregateurDetails.getNomA());
            agregateur.setCleApblic(agregateurDetails.getCleApblic());
            agregateur.setCleApr(agregateurDetails.getCleApr());
            agregateur.setCleAtoken(agregateurDetails.getCleAtoken());
            agregateur.setNompays(agregateurDetails.getNompays());
            agregateur.setNomOperateur(agregateurDetails.getNomOperateur());
            agregateur.setEnabled(agregateurDetails.isEnabled());
            
            // Sync country configs
            agregateur.getCountryConfigs().clear();
            if (agregateurDetails.getCountryConfigs() != null) {
                agregateurDetails.getCountryConfigs().forEach(config -> {
                    config.setAgregateur(agregateur);
                    agregateur.getCountryConfigs().add(config);
                });
=======
        Agregateur agregateur = agregateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agregateur not found with id " + id));

        agregateur.setNomA(agregateurDetails.getNomA());
        agregateur.setCleApblic(agregateurDetails.getCleApblic());
        agregateur.setCleApr(agregateurDetails.getCleApr());
        agregateur.setCleAtoken(agregateurDetails.getCleAtoken());
        agregateur.setCleAmaster(agregateurDetails.getCleAmaster());
        agregateur.setBaseUrl(agregateurDetails.getBaseUrl());

        // Sync country configs
        agregateur.getCountryConfigs().clear();
        if (agregateurDetails.getCountryConfigs() != null) {
            for (CountryConfig config : agregateurDetails.getCountryConfigs()) {
                config.setAgregateur(agregateur);
                agregateur.getCountryConfigs().add(config);
>>>>>>> 6451fc7ea20468a53eca0812ef46cd8840cb6a75
            }
        }

        Agregateur saved = agregateurRepository.save(agregateur);
        syncRoutes(saved);
        return saved;
    }

    private void syncRoutes(Agregateur aggregator) {
        if (aggregator.getCountryConfigs() == null) return;

        String provider = aggregator.getNomA().toUpperCase();
        
        // 1. Get all current routes for this provider
        List<Route> existingRoutes = routeRepository.findByProvider(provider);
        
        // 2. Build the list of routes that SHOULD exist
        List<String> targetRouteNames = new ArrayList<>();
        
        for (CountryConfig config : aggregator.getCountryConfigs()) {
            String countryName = config.getCountryName();
            String countryCode = mapCountryNameToCode(countryName);
            
            String opsString = config.getOperators();
            if (opsString == null) continue;

            String[] operators = opsString.split(",");
            for (String op : operators) {
                String operator = op.trim().toUpperCase();
                if (operator.isEmpty()) continue;

                String routeName = provider + "_" + operator + "_" + countryCode;
                targetRouteNames.add(routeName);
                
                if (!routeRepository.existsByName(routeName)) {
                    Route route = new Route();
                    route.setName(routeName);
                    route.setProvider(provider);
                    route.setOperator(operator);
                    route.setCountry(countryCode);
                    route.setAvailability(true);
                    routeRepository.save(route);
                    log.info("Created new route: {}", routeName);
                }
            }
        }

        // 3. Remove routes that are no longer in the configuration
        for (Route existingRoute : existingRoutes) {
            if (!targetRouteNames.contains(existingRoute.getName())) {
                log.info("Removing obsolete route: {}", existingRoute.getName());
                routeRepository.delete(existingRoute);
            }
        }
    }

    private String mapCountryNameToCode(String name) {
        if (name == null) return "XX";
        String n = name.toUpperCase().trim();
        if (n.contains("BÉNIN") || n.contains("BENIN")) return "BJ";
        if (n.contains("SÉNÉGAL") || n.contains("SENEGAL")) return "SN";
        if (n.contains("CÔTE D'IVOIRE") || n.contains("COTE D'IVOIRE") || n.contains("IVORY COAST")) return "CI";
        if (n.contains("TOGO")) return "TG";
        if (n.contains("MALI")) return "ML";
        if (n.contains("BURKINA")) return "BF";
        if (n.contains("GUINÉE") || n.contains("GUINEE")) return "GN";
        if (n.contains("CAMEROUN")) return "CM";
        return n.substring(0, Math.min(n.length(), 2));
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
<<<<<<< HEAD

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
=======
}
>>>>>>> 6451fc7ea20468a53eca0812ef46cd8840cb6a75

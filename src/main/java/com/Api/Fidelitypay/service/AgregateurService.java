package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.model.Agregateur;
import com.Api.Fidelitypay.model.CountryConfig;
import com.Api.Fidelitypay.model.Route;
import com.Api.Fidelitypay.repository.AgregateurRepository;
import com.Api.Fidelitypay.repository.RouteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class AgregateurService {

    @Autowired
    private AgregateurRepository agregateurRepository;

    @Autowired
    private RouteRepository routeRepository;

    public List<Agregateur> getAllAgregateurs() {
        return agregateurRepository.findAll();
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

    public Agregateur updateAgregateur(Long id, Agregateur agregateurDetails) {
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

    public void deleteAgregateur(Long id) {
        agregateurRepository.deleteById(id);
    }
}
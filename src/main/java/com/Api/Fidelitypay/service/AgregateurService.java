package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.model.Agregateur;
import com.Api.Fidelitypay.repository.AgregateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AgregateurService {

    @Autowired
    private AgregateurRepository agregateurRepository;

    @Autowired
    private com.Api.Fidelitypay.repository.RouteRepository routeRepository;

    public List<Agregateur> getAllAgregateurs() {
        return agregateurRepository.findAll();
    }

    public Optional<Agregateur> getAgregateurById(Long id) {
        return agregateurRepository.findById(id);
    }

    public Agregateur createAgregateur(Agregateur agregateur) {
        if (agregateur.getCountryConfigs() != null) {
            agregateur.getCountryConfigs().forEach(config -> config.setAgregateur(agregateur));
        }
        Agregateur saved = agregateurRepository.save(agregateur);
        syncRoutes(saved);
        return saved;
    }

    public Agregateur updateAgregateur(Long id, Agregateur agregateurDetails) {
        return agregateurRepository.findById(id).map(agregateur -> {
            agregateur.setNomA(agregateurDetails.getNomA());
            agregateur.setCleApblic(agregateurDetails.getCleApblic());
            agregateur.setCleApr(agregateurDetails.getCleApr());
            agregateur.setCleAtoken(agregateurDetails.getCleAtoken());
            agregateur.setCleAmaster(agregateurDetails.getCleAmaster());
            agregateur.setBaseUrl(agregateurDetails.getBaseUrl());
            
            // Sync country configs
            agregateur.getCountryConfigs().clear();
            if (agregateurDetails.getCountryConfigs() != null) {
                agregateurDetails.getCountryConfigs().forEach(config -> {
                    config.setAgregateur(agregateur);
                    agregateur.getCountryConfigs().add(config);
                });
            }
            
            Agregateur saved = agregateurRepository.save(agregateur);
            syncRoutes(saved);
            return saved;
        }).orElseThrow(() -> new RuntimeException("Agregateur not found with id " + id));
    }

    private void syncRoutes(Agregateur aggregator) {
        if (aggregator.getCountryConfigs() == null) return;

        String provider = aggregator.getNomA().toUpperCase();
        
        for (com.Api.Fidelitypay.model.CountryConfig config : aggregator.getCountryConfigs()) {
            String countryName = config.getCountryName();
            String countryCode = mapCountryNameToCode(countryName);
            
            String[] operators = config.getOperators().split(",");
            for (String op : operators) {
                String operator = op.trim().toUpperCase();
                if (operator.isEmpty()) continue;

                String routeName = provider + "_" + operator + "_" + countryCode;
                
                // Check if route exists, if not create it
                if (!routeRepository.existsByName(routeName)) {
                    com.Api.Fidelitypay.model.Route route = new com.Api.Fidelitypay.model.Route();
                    route.setName(routeName);
                    route.setProvider(provider);
                    route.setOperator(operator);
                    route.setCountry(countryCode);
                    route.setAvailability(true);
                    routeRepository.save(route);
                }
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

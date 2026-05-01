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
        return agregateurRepository.save(agregateur);
    }

    public Agregateur updateAgregateur(Long id, Agregateur agregateurDetails) {
        return agregateurRepository.findById(id).map(agregateur -> {
            agregateur.setNomA(agregateurDetails.getNomA());
            agregateur.setCleApblic(agregateurDetails.getCleApblic());
            agregateur.setCleApr(agregateurDetails.getCleApr());
            agregateur.setCleAtoken(agregateurDetails.getCleAtoken());
            
            // Sync country configs
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

    public void deleteAgregateur(Long id) {
        agregateurRepository.deleteById(id);
    }
}

package com.Api.Fidelitypay.controller;

import com.Api.Fidelitypay.model.Agregateur;
import com.Api.Fidelitypay.service.AgregateurService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/agregateurs")
@CrossOrigin(origins = "http://localhost:4200")
public class AgregateurController {

    @Autowired
    private AgregateurService agregateurService;

    @GetMapping
    public List<Agregateur> getAllAgregateurs() {
        return agregateurService.getAllAgregateurs();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Agregateur> getAgregateurById(@PathVariable Long id) {
        return agregateurService.getAgregateurById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Agregateur createAgregateur(@RequestBody Agregateur agregateur) {
        return agregateurService.createAgregateur(agregateur);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Agregateur> updateAgregateur(@PathVariable Long id, @RequestBody Agregateur agregateurDetails) {
        try {
            Agregateur updatedAgregateur = agregateurService.updateAgregateur(id, agregateurDetails);
            return ResponseEntity.ok(updatedAgregateur);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAgregateur(@PathVariable Long id) {
        agregateurService.deleteAgregateur(id);
        return ResponseEntity.noContent().build();
    }
}

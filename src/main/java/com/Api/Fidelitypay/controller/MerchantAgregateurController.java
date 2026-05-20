package com.Api.Fidelitypay.controller;

import com.Api.Fidelitypay.model.Agregateur;
import com.Api.Fidelitypay.model.User;
import com.Api.Fidelitypay.service.AgregateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/developer/agregateurs")
@RequiredArgsConstructor
public class MerchantAgregateurController {

    private final AgregateurService agregateurService;

    @GetMapping
    public List<Agregateur> getMerchantAgregateurs(Authentication authentication) {
        return agregateurService.getMerchantAgregateurs(resolveUserId(authentication));
    }

    @PostMapping
    public ResponseEntity<Agregateur> createMerchantAgregateur(Authentication authentication, @RequestBody Agregateur agregateur) {
        try {
            return ResponseEntity.ok(agregateurService.createMerchantAgregateur(resolveUserId(authentication), agregateur));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Agregateur> updateMerchantAgregateur(Authentication authentication, @PathVariable Long id,
            @RequestBody Agregateur agregateur) {
        try {
            return ResponseEntity.ok(agregateurService.updateMerchantAgregateur(resolveUserId(authentication), id, agregateur));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Agregateur> setMerchantAgregateurEnabled(Authentication authentication, @PathVariable Long id,
            @RequestBody Map<String, Boolean> payload) {
        try {
            return ResponseEntity.ok(agregateurService.setMerchantAgregateurEnabled(resolveUserId(authentication), id,
                    payload.getOrDefault("enabled", true)));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMerchantAgregateur(Authentication authentication, @PathVariable Long id) {
        try {
            agregateurService.deleteMerchantAgregateur(resolveUserId(authentication), id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private String resolveUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        throw new AccessDeniedException("Authenticated user required");
    }
}

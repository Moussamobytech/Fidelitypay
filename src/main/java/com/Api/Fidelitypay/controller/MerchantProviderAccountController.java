package com.Api.Fidelitypay.controller;

import com.Api.Fidelitypay.controller.dto.MerchantProviderAccountRequest;
import com.Api.Fidelitypay.controller.dto.MerchantProviderAccountResponse;
import com.Api.Fidelitypay.model.User;
import com.Api.Fidelitypay.service.MerchantProviderAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/developer/provider-accounts")
public class MerchantProviderAccountController {

    private final MerchantProviderAccountService accountService;

    @GetMapping
    public List<MerchantProviderAccountResponse> listAccounts(Authentication authentication) {
        return accountService.listAccounts(resolveUserId(authentication));
    }

    @PostMapping
    public ResponseEntity<MerchantProviderAccountResponse> upsertAccount(Authentication authentication,
            @RequestBody MerchantProviderAccountRequest request) {
        return ResponseEntity.ok(accountService.upsertAccount(resolveUserId(authentication), request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<MerchantProviderAccountResponse> setEnabled(Authentication authentication,
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> payload) {
        return ResponseEntity.ok(accountService.setEnabled(resolveUserId(authentication), id,
                payload.getOrDefault("enabled", true)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(Authentication authentication, @PathVariable Long id) {
        accountService.deleteAccount(resolveUserId(authentication), id);
        return ResponseEntity.noContent().build();
    }

    private String resolveUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        throw new AccessDeniedException("Authenticated user required");
    }
}

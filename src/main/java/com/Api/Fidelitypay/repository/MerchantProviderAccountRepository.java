package com.Api.Fidelitypay.repository;

import com.Api.Fidelitypay.model.MerchantProviderAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantProviderAccountRepository extends JpaRepository<MerchantProviderAccount, Long> {
    @Query("""
            select a from MerchantProviderAccount a
            join fetch a.provider
            where a.merchantUserId = :merchantUserId
            order by a.provider.displayName asc
            """)
    List<MerchantProviderAccount> findByMerchantUserIdWithProvider(@Param("merchantUserId") String merchantUserId);

    Optional<MerchantProviderAccount> findByMerchantUserIdAndProviderIdAndEnvironment(
            String merchantUserId,
            Long providerId,
            String environment);

    @Query("""
            select a from MerchantProviderAccount a
            join fetch a.provider p
            where a.merchantUserId = :merchantUserId
              and p.id = :providerId
              and a.environment = :environment
              and a.enabled = true
              and p.status = com.Api.Fidelitypay.enums.PaymentProviderStatus.ACTIVE
            """)
    Optional<MerchantProviderAccount> findEnabledAccount(
            @Param("merchantUserId") String merchantUserId,
            @Param("providerId") Long providerId,
            @Param("environment") String environment);
}

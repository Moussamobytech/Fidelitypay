package com.Api.Fidelitypay.repository;

import com.Api.Fidelitypay.enums.PaymentDirection;
import com.Api.Fidelitypay.model.PaymentProviderRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentProviderRouteRepository extends JpaRepository<PaymentProviderRoute, Long> {

    @Query("""
            select r from PaymentProviderRoute r
            join fetch r.provider
            """)
    List<PaymentProviderRoute> findAllWithProvider();

    @Query("""
            select r from PaymentProviderRoute r
            join fetch r.provider
            where r.id = :id
            """)
    Optional<PaymentProviderRoute> findByIdWithProvider(@Param("id") Long id);
    @Query("""
            select r from PaymentProviderRoute r
            join fetch r.provider p
            where r.direction = :direction
              and r.country = :country
              and r.operator = :operator
              and r.enabled = true
              and p.status = com.Api.Fidelitypay.enums.PaymentProviderStatus.ACTIVE
            order by r.priority asc
            """)
    List<PaymentProviderRoute> findAvailable(
            @Param("direction") PaymentDirection direction,
            @Param("country") String country,
            @Param("operator") String operator);

    List<PaymentProviderRoute> findByDirectionAndCountryAndOperatorOrderByPriorityAsc(
            PaymentDirection direction,
            String country,
            String operator);

    @Query("""
            select distinct r.operator from PaymentProviderRoute r
            join r.provider p
            where r.direction = :direction
              and r.country = :country
              and r.enabled = true
              and p.status = com.Api.Fidelitypay.enums.PaymentProviderStatus.ACTIVE
            order by r.operator asc
            """)
    List<String> findAvailableOperators(@Param("direction") PaymentDirection direction,
            @Param("country") String country);

    @Query("""
            select r from PaymentProviderRoute r
            join fetch r.provider
            where r.direction = :direction
            order by r.country asc, r.operator asc, r.priority asc
            """)
    List<PaymentProviderRoute> findByDirectionWithProviderOrderByCountryAscOperatorAscPriorityAsc(
            @Param("direction") PaymentDirection direction);

    @Query("""
            select r from PaymentProviderRoute r
            join fetch r.provider
            order by r.country asc, r.operator asc, r.priority asc
            """)
    List<PaymentProviderRoute> findAllWithProviderOrderByCountryAscOperatorAscPriorityAsc();

}

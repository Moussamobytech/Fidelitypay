package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.model.Payment;
import com.Api.Fidelitypay.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardService {

    @Autowired
    private PaymentRepository paymentRepository;

    public List<Payment> getAllTransactions() {
        return paymentRepository.findAll();
    }
}
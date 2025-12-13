package com.Api.Fidelitypay.controller;

import com.Api.Fidelitypay.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/dashboard/transactions")
    public String getTransactions(Model model) {
        model.addAttribute("transactions", dashboardService.getAllTransactions());
        return "transactions"; // Nom du template Thymeleaf
    }
}
package com.cardnova.giftchat.controller;

import com.cardnova.giftchat.api.ApiResponse;
import com.cardnova.giftchat.dto.UpdateCurrencyExchangeRateRequest;
import com.cardnova.giftchat.model.CurrencyExchangeRateItem;
import com.cardnova.giftchat.service.CurrencyExchangeRateService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CurrencyExchangeRateController {
    private final CurrencyExchangeRateService service;

    public CurrencyExchangeRateController(CurrencyExchangeRateService service) {
        this.service = service;
    }

    @GetMapping("/api/currency-rates/me")
    public ApiResponse<CurrencyExchangeRateItem> currentRate() {
        return ApiResponse.success(service.currentRate());
    }

    @GetMapping("/api/admin/currency-rates")
    public ApiResponse<List<CurrencyExchangeRateItem>> adminRates() {
        return ApiResponse.success(service.adminRates());
    }

    @PostMapping("/api/admin/currency-rates")
    public ApiResponse<CurrencyExchangeRateItem> update(@Valid @RequestBody UpdateCurrencyExchangeRateRequest request) {
        return ApiResponse.success("currency_rate_updated", service.update(request));
    }
}

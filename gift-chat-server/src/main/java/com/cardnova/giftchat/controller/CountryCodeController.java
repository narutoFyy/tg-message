package com.cardnova.giftchat.controller;

import com.cardnova.giftchat.api.ApiResponse;
import com.cardnova.giftchat.model.CountryCodeRule;
import com.cardnova.giftchat.service.CountryCodeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/country-codes")
public class CountryCodeController {

    private final CountryCodeService countryCodeService;

    public CountryCodeController(CountryCodeService countryCodeService) {
        this.countryCodeService = countryCodeService;
    }

    @GetMapping
    public ApiResponse<List<CountryCodeRule>> countryCodes() {
        return ApiResponse.success(countryCodeService.rules());
    }
}

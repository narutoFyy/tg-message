package com.cardnova.giftchat.controller;

import com.cardnova.giftchat.api.ApiResponse;
import com.cardnova.giftchat.dto.BindBankAccountRequest;
import com.cardnova.giftchat.model.BankAccountItem;
import com.cardnova.giftchat.service.BankAccountService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bank-accounts")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    public BankAccountController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @GetMapping("/me")
    public ApiResponse<BankAccountItem> myBankAccount() {
        return ApiResponse.success(bankAccountService.myBankAccount());
    }

    @PostMapping
    public ApiResponse<BankAccountItem> bind(@Valid @RequestBody BindBankAccountRequest request) {
        return ApiResponse.success("bank_account_bound", bankAccountService.bind(request));
    }
}

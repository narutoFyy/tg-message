package com.cardnova.giftchat.controller;

import com.cardnova.giftchat.api.ApiResponse;
import com.cardnova.giftchat.dto.CreateLoanApplicationRequest;
import com.cardnova.giftchat.dto.UpdateLoanStatusRequest;
import com.cardnova.giftchat.model.LoanApplicationItem;
import com.cardnova.giftchat.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping
    public ApiResponse<List<LoanApplicationItem>> loans() {
        return ApiResponse.success(loanService.getLoans());
    }

    @PostMapping
    public ApiResponse<LoanApplicationItem> create(@Valid @RequestBody CreateLoanApplicationRequest request) {
        return ApiResponse.success("loan_application_created", loanService.create(request));
    }

    @PostMapping("/{loanId}/status")
    public ApiResponse<LoanApplicationItem> updateStatus(
        @PathVariable String loanId,
        @Valid @RequestBody UpdateLoanStatusRequest request
    ) {
        return ApiResponse.success("loan_status_updated", loanService.updateStatus(loanId, request.status(), request.reviewNote()));
    }
}

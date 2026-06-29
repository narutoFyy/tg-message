package com.cardnova.giftchat.controller;

import com.cardnova.giftchat.api.ApiResponse;
import com.cardnova.giftchat.dto.CreateSellOrderRequest;
import com.cardnova.giftchat.dto.CreateTransactionRequest;
import com.cardnova.giftchat.dto.UpdateTransactionStatusRequest;
import com.cardnova.giftchat.model.TransactionItem;
import com.cardnova.giftchat.service.PersistentTransactionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final PersistentTransactionService persistentTransactionService;

    public TransactionController(PersistentTransactionService persistentTransactionService) {
        this.persistentTransactionService = persistentTransactionService;
    }

    @GetMapping
    public ApiResponse<List<TransactionItem>> getTransactions() {
        return ApiResponse.success(persistentTransactionService.getTransactions());
    }

    @GetMapping("/{transactionId}")
    public ApiResponse<TransactionItem> getTransaction(@PathVariable String transactionId) {
        return ApiResponse.success(persistentTransactionService.getTransaction(transactionId));
    }

    @PostMapping
    public ApiResponse<TransactionItem> createTransaction(@Valid @RequestBody CreateTransactionRequest request) {
        return ApiResponse.success("transaction_created", persistentTransactionService.createTransaction(request));
    }

    @PostMapping("/sell-orders")
    public ApiResponse<TransactionItem> createSellOrder(@Valid @RequestBody CreateSellOrderRequest request) {
        return ApiResponse.success("sell_order_created", persistentTransactionService.createSellOrder(request));
    }

    @PostMapping("/{transactionId}/status")
    public ApiResponse<TransactionItem> updateStatus(
        @PathVariable String transactionId,
        @Valid @RequestBody UpdateTransactionStatusRequest request
    ) {
        return ApiResponse.success(
            "transaction_status_updated",
            persistentTransactionService.updateStatus(transactionId, request.status())
        );
    }
}

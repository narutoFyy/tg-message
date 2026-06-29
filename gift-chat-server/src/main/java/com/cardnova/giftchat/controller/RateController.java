package com.cardnova.giftchat.controller;

import com.cardnova.giftchat.api.ApiResponse;
import com.cardnova.giftchat.dto.CreateRateRequest;
import com.cardnova.giftchat.dto.UpdateRateStatusRequest;
import com.cardnova.giftchat.model.RateItem;
import com.cardnova.giftchat.service.PersistentRateService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RateController {

    private final PersistentRateService persistentRateService;

    public RateController(PersistentRateService persistentRateService) {
        this.persistentRateService = persistentRateService;
    }

    @GetMapping("/api/rates")
    public ApiResponse<List<RateItem>> getRates() {
        return ApiResponse.success(persistentRateService.findAll());
    }

    @PostMapping("/api/admin/rates")
    public ApiResponse<RateItem> createRate(@Valid @RequestBody CreateRateRequest request) {
        return ApiResponse.success("rate_created", persistentRateService.create(request));
    }

    @PostMapping("/api/admin/rates/{rateId}/status")
    public ApiResponse<RateItem> updateRateStatus(
        @PathVariable String rateId,
        @Valid @RequestBody UpdateRateStatusRequest request
    ) {
        return ApiResponse.success("rate_status_updated", persistentRateService.updateStatus(rateId, request.status()));
    }

    @PostMapping("/api/admin/rates/{rateId}")
    public ApiResponse<RateItem> updateRate(
        @PathVariable String rateId,
        @Valid @RequestBody CreateRateRequest request
    ) {
        return ApiResponse.success("rate_updated", persistentRateService.update(rateId, request));
    }

    @DeleteMapping("/api/admin/rates/{rateId}")
    public ApiResponse<RateItem> deleteRate(@PathVariable String rateId) {
        return ApiResponse.success("rate_deleted", persistentRateService.delete(rateId));
    }
}

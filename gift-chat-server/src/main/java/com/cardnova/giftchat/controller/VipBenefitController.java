package com.cardnova.giftchat.controller;

import com.cardnova.giftchat.api.ApiResponse;
import com.cardnova.giftchat.dto.ReviewVipBenefitClaimRequest;
import com.cardnova.giftchat.dto.UpdateBirthdayRequest;
import com.cardnova.giftchat.model.VipBenefitClaimItem;
import com.cardnova.giftchat.model.VipBenefitSummary;
import com.cardnova.giftchat.service.VipBenefitService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vip/benefits")
public class VipBenefitController {
    private final VipBenefitService service;
    public VipBenefitController(VipBenefitService service) { this.service = service; }
    @GetMapping("/me") public ApiResponse<VipBenefitSummary> me() { return ApiResponse.success(service.currentSummary()); }
    @PostMapping("/birthday") public ApiResponse<VipBenefitSummary> setBirthday(@Valid @RequestBody UpdateBirthdayRequest request) { return ApiResponse.success("birthday_locked", service.setCurrentBirthday(request.birthDate())); }
    @PostMapping("/birthday/claim") public ApiResponse<VipBenefitClaimItem> claimBirthday() { return ApiResponse.success("birthday_reward_claimed", service.claimBirthday()); }
    @PostMapping("/support-red-packets") public ApiResponse<VipBenefitClaimItem> requestSupportRedPacket() { return ApiResponse.success("support_red_packet_requested", service.requestSupportRedPacket()); }
    @PostMapping("/holidays/{holidayId}/claim") public ApiResponse<VipBenefitClaimItem> claimHoliday(@PathVariable String holidayId) { return ApiResponse.success("holiday_reward_claimed", service.claimHoliday(holidayId)); }
    @GetMapping("/claims/me") public ApiResponse<List<VipBenefitClaimItem>> myClaims() { return ApiResponse.success(service.currentUserClaims()); }
    @GetMapping("/staff/claims") public ApiResponse<List<VipBenefitClaimItem>> staffClaims() { return ApiResponse.success(service.staffClaims()); }
    @PostMapping("/staff/claims/{claimId}/review") public ApiResponse<VipBenefitClaimItem> review(@PathVariable String claimId, @Valid @RequestBody ReviewVipBenefitClaimRequest request) { return ApiResponse.success("vip_benefit_claim_reviewed", service.reviewClaim(claimId, request)); }
}

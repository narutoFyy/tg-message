package com.cardnova.giftchat.controller;

import com.cardnova.giftchat.api.ApiResponse;
import com.cardnova.giftchat.dto.UpdateBirthdayRequest;
import com.cardnova.giftchat.dto.UpdateVipBenefitConfigRequest;
import com.cardnova.giftchat.dto.UpsertVipHolidayRewardRequest;
import com.cardnova.giftchat.model.VipBenefitConfigItem;
import com.cardnova.giftchat.model.VipBenefitSummary;
import com.cardnova.giftchat.model.VipHolidayRewardItem;
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
@RequestMapping("/api/admin/vip-benefits")
public class AdminVipBenefitController {
    private final VipBenefitService service;
    public AdminVipBenefitController(VipBenefitService service) { this.service = service; }
    @GetMapping("/config") public ApiResponse<VipBenefitConfigItem> config() { return ApiResponse.success(service.adminConfig()); }
    @PostMapping("/config") public ApiResponse<VipBenefitConfigItem> updateConfig(@Valid @RequestBody UpdateVipBenefitConfigRequest request) { return ApiResponse.success("vip_benefit_config_updated", service.updateConfig(request)); }
    @GetMapping("/holidays") public ApiResponse<List<VipHolidayRewardItem>> holidays() { return ApiResponse.success(service.adminHolidays()); }
    @PostMapping("/holidays") public ApiResponse<VipHolidayRewardItem> upsertHoliday(@Valid @RequestBody UpsertVipHolidayRewardRequest request) { return ApiResponse.success("vip_holiday_reward_saved", service.upsertHoliday(request)); }
    @PostMapping("/users/{userId}/birthday") public ApiResponse<VipBenefitSummary> updateBirthday(@PathVariable String userId, @Valid @RequestBody UpdateBirthdayRequest request) { return ApiResponse.success("birthday_updated", service.setUserBirthdayByAdmin(userId, request.birthDate())); }
}

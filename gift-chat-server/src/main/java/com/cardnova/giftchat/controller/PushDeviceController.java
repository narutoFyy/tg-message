package com.cardnova.giftchat.controller;

import com.cardnova.giftchat.api.ApiResponse;
import com.cardnova.giftchat.dto.RegisterPushDeviceRequest;
import com.cardnova.giftchat.model.PushDeviceItem;
import com.cardnova.giftchat.service.PushDeviceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/push/devices")
public class PushDeviceController {

    private final PushDeviceService pushDeviceService;

    public PushDeviceController(PushDeviceService pushDeviceService) {
        this.pushDeviceService = pushDeviceService;
    }

    @GetMapping
    public ApiResponse<List<PushDeviceItem>> myDevices() {
        return ApiResponse.success(pushDeviceService.myDevices());
    }

    @PostMapping
    public ApiResponse<PushDeviceItem> register(@Valid @RequestBody RegisterPushDeviceRequest request) {
        return ApiResponse.success("push_device_registered", pushDeviceService.register(request));
    }

    @DeleteMapping("/{deviceId}")
    public ApiResponse<PushDeviceItem> disable(@PathVariable String deviceId) {
        return ApiResponse.success("push_device_disabled", pushDeviceService.disable(deviceId));
    }
}

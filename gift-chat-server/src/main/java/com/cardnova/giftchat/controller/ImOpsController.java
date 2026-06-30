package com.cardnova.giftchat.controller;

import com.cardnova.giftchat.api.ApiResponse;
import com.cardnova.giftchat.model.TencentMirrorFailureItem;
import com.cardnova.giftchat.service.ImOpsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/im")
public class ImOpsController {

    private final ImOpsService imOpsService;

    public ImOpsController(ImOpsService imOpsService) {
        this.imOpsService = imOpsService;
    }

    @GetMapping("/tencent-mirror-failures")
    public ApiResponse<List<TencentMirrorFailureItem>> failedTencentMirrors() {
        return ApiResponse.success(imOpsService.failedTencentMirrors());
    }

    @PostMapping("/tencent-mirror-failures/retry")
    public ApiResponse<Map<String, Integer>> retryFailedTencentMirrors() {
        return ApiResponse.success("tencent_mirror_retry_started", Map.of("count", imOpsService.retryFailedTencentMirrors()));
    }
}

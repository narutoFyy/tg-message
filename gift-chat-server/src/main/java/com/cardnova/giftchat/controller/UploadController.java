package com.cardnova.giftchat.controller;

import com.cardnova.giftchat.api.ApiResponse;
import com.cardnova.giftchat.model.UploadAssetItem;
import com.cardnova.giftchat.service.UploadStorageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final UploadStorageService uploadStorageService;

    public UploadController(UploadStorageService uploadStorageService) {
        this.uploadStorageService = uploadStorageService;
    }

    @PostMapping("/images")
    public ApiResponse<UploadAssetItem> uploadImage(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success("image_uploaded", uploadStorageService.saveImage(file));
    }

    @PostMapping("/voices")
    public ApiResponse<UploadAssetItem> uploadVoice(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success("voice_uploaded", uploadStorageService.saveVoice(file));
    }
}

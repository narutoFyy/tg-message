package com.cardnova.giftchat.controller;

import com.cardnova.giftchat.api.ApiResponse;
import com.cardnova.giftchat.dto.TranslateRequest;
import com.cardnova.giftchat.model.TranslationResult;
import com.cardnova.giftchat.service.TranslationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/translations")
public class TranslationController {

    private final TranslationService translationService;

    public TranslationController(TranslationService translationService) {
        this.translationService = translationService;
    }

    @PostMapping("/zh")
    public ApiResponse<TranslationResult> translateToChinese(@Valid @RequestBody TranslateRequest request) {
        return ApiResponse.success(translationService.translateToChinese(request.text()));
    }
}

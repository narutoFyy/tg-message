package com.cardnova.giftchat.controller;

import com.cardnova.giftchat.api.ApiResponse;
import com.cardnova.giftchat.dto.CreateVideoSessionRequest;
import com.cardnova.giftchat.dto.UpdateVideoSessionStatusRequest;
import com.cardnova.giftchat.model.VideoSessionBootstrap;
import com.cardnova.giftchat.model.VideoSessionItem;
import com.cardnova.giftchat.service.VideoSessionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/video-sessions")
public class VideoSessionController {

    private final VideoSessionService videoSessionService;

    public VideoSessionController(VideoSessionService videoSessionService) {
        this.videoSessionService = videoSessionService;
    }

    @GetMapping
    public ApiResponse<List<VideoSessionItem>> sessions() {
        return ApiResponse.success(videoSessionService.getVideoSessions());
    }

    @GetMapping("/{sessionId}/bootstrap")
    public ApiResponse<VideoSessionBootstrap> bootstrap(@PathVariable String sessionId) {
        return ApiResponse.success(videoSessionService.getBootstrap(sessionId));
    }

    @PostMapping
    public ApiResponse<VideoSessionBootstrap> create(@Valid @RequestBody CreateVideoSessionRequest request) {
        return ApiResponse.success("video_session_created", videoSessionService.create(request));
    }

    @PostMapping("/{sessionId}/status")
    public ApiResponse<VideoSessionItem> updateStatus(
        @PathVariable String sessionId,
        @Valid @RequestBody UpdateVideoSessionStatusRequest request
    ) {
        return ApiResponse.success("video_session_status_updated", videoSessionService.updateStatus(sessionId, request.status()));
    }
}

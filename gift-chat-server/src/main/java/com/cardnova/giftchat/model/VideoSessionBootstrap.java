package com.cardnova.giftchat.model;

public record VideoSessionBootstrap(
    VideoSessionItem session,
    String sdkAppId,
    String userId,
    String userSig,
    boolean sdkConfigured,
    String vendor,
    String note
) {
}

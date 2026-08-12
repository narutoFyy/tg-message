package com.cardnova.giftchat.model;

import java.util.List;

public record PromotionInviteRegistrationPage(
    String code,
    long total,
    int page,
    int pageSize,
    int totalPages,
    List<PromotionInviteRegistrationItem> users
) {
}

package com.cardnova.giftchat.model;

import java.util.List;

public record AdminUserPage(
    List<AdminUserItem> items,
    int page,
    int pageSize,
    long total,
    int totalPages
) {
}

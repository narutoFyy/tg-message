package com.cardnova.giftchat.model;

public record RankingEntry(
    int rank,
    String username,
    String displayName,
    String avatarUrl,
    String score,
    String reward,
    boolean currentUser
) {
}

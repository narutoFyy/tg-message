package com.cardnova.giftchat.model;

public record VipSummary(
    String level,
    String levelName,
    String points,
    String nextLevel,
    String nextThreshold,
    String remainingPoints,
    int progressPercent,
    boolean maxLevel
) {
}

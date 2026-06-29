package com.cardnova.giftchat.model;

import java.util.List;

public record RankingBoard(
    String mode,
    String month,
    List<RankingEntry> leaders,
    RankingEntry currentUser
) {
}
